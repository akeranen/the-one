package movement;

import core.*;
import input.GSIMConnector;
import interfaces.ConnectivityGrid;
import interfaces.GSIMConnectivityOptimizer;

import java.util.*;

/**
 * Provides a GPU accelerated implementation to move hosts in the world according to their MovementModel
 * by communicating to a GSIM process.
 * Supports accelerated connectivity detection.
 * Attention: Connectivity detection currently only supports a single interface type and only 1 interface per host!
 */
public class GSIMMovementEngine extends MovementEngine {
    /** Class name */
    public static final String NAME = "GSIMMovementEngine";
    /** Waypoint buffer size -setting id ({@value})*/
    public static final String WAYPOINT_BUFFER_SIZE_S = "waypointBufferSize";
    /** Connectivity optimizer -setting id ({@value})*/
    public static final String DISABLE_OPTIMIZER_S = "disableConnectivityOptimizer";

    /** Interface to the GSIM process */
    private GSIMConnector connector = null;
    /** Number of buffered waypoints per host */
    private int waypointBufferSize = 0;
    /** Queue of pending waypoint requests */
    private final PriorityQueue<WaypointRequest> waypointRequests = new PriorityQueue<>();
    /** Keep host locations in sync with gsim */
    private long locationsVersionTick = 0;
    private boolean locationsChanged = true; // Note: Need to set initial locations
    private boolean disableOptimizer = false;

    static class WaypointRequest implements Comparable<WaypointRequest> {
        public int hostID;
        public int numWaypoints; // request size

        public WaypointRequest(int hostID, int numWaypoints) {
            this.hostID = hostID;
            this.numWaypoints = numWaypoints;
        }

        @Override
        public int compareTo(WaypointRequest o) {
            return this.hostID - o.hostID;
        }
    }

    /**
     * Creates a new MovementEngine based on a Settings object's settings.
     * @param s The Settings object where the settings are read from
     */
    public GSIMMovementEngine(Settings s) {
        super(s);

        s.setNameSpace(NAME);
        waypointBufferSize = s.getInt(WAYPOINT_BUFFER_SIZE_S, 4);
        disableOptimizer = s.getBoolean(DISABLE_OPTIMIZER_S, false);

        s.setNameSpace(SimScenario.SCENARIO_NS);
        // No need to run optimizer if connections are not simulated
        disableOptimizer |= !s.getBoolean(SimScenario.SIM_CON_S);
        s.restoreNameSpace();

        s.setNameSpace(World.OPTIMIZATION_SETTINGS_NS);
        boolean randomize_updates = s.getBoolean(World.RANDOMIZE_UPDATES_S, World.DEF_RANDOMIZE_UPDATES);
        s.restoreNameSpace();

        if (!disableOptimizer && !randomize_updates) {
            System.out.println("WARNING: GSIMConnectivityOptimizer is active and update randomization is turned off.\n" +
                    "         The network interface link event update order is not deterministic due to GPU acceleration.");
        }

        connector = (GSIMConnector)s.createIntializedObject("input." + GSIMConnector.NAME);
    }

    /**
     * Initializes the movement engine
     * Sends configuration and initial host locations to GSIM
     * @param hosts to be moved
     */
    @Override
    public void init(List<DTNHost> hosts, int worldSizeX, int worldSizeY) {
        super.init(hosts, worldSizeX, worldSizeY);

        // Start process and open connection
        double interfaceRange = hosts.get(0).getInterface(1).getTransmitRange();
        connector.init(hosts.size(), worldSizeX, worldSizeY, waypointBufferSize, interfaceRange);

        // Initialize optimizer
        Map<String, List<NetworkInterface>> interface_map = new HashMap<>();
        for (DTNHost host : hosts) {
            for (NetworkInterface ni : host.getInterfaces()) {
                if (ni.getTransmitRange() > 0) {
                    interface_map.computeIfAbsent(ni.getInterfaceType(), k -> new ArrayList<>()).add(ni);
                }
            }
        }

        Iterator<List<NetworkInterface>> interfaces_it = interface_map.values().iterator();
        if (!interfaces_it.hasNext()) {
            return;
        }
        if (!disableOptimizer) {
            List<NetworkInterface> interfaces = interfaces_it.next();
            // First list of interfaces will be accelerated by gsim
            if (interfaces.size() != hosts.size()) {
                throw new SettingsError("GSIMConnectivityOptimizer requires one interface of the same type for every host!");
            }
            optimizer.add(new GSIMConnectivityOptimizer(connector, interfaces));
        }

        // All others will be managed by grids
        while (interfaces_it.hasNext()) {
            optimizer.add(new ConnectivityGrid(interfaces_it.next()));
        }
    }

    /**
     * Finalizes the movement engine
     * Called on world shutdown for cleanup
     */
    @Override
    public void fini() {
        connector.writeHeader(GSIMConnector.Header.Shutdown);
        connector.flushOutput();
        try {
            Thread.sleep(1000); // Give it some time to shut down gracefully
        } catch (InterruptedException ignored) { }

        // Close connection and shutdown process
        connector.fini();
    }

    /**
     * Returns a hosts current location
     * @param hostID The ID of the host
     * @return the hosts current location
     */
    @Override
    public Coord getLocation(int hostID) {
        if (locationsVersionTick != currentTick) {
            long start = System.nanoTime();
            get_locations(); // Update locations
            locationsVersionTick = currentTick;
            System.out.printf(" %d:  get_locations = %s\n", currentTick, toHumanTime(System.nanoTime() - start));
        }
        return locations.get(hostID);
    }

    /**
     * Returns a hosts current location
     * @param hostID The ID of the host
     * @param c The new location
     * @return the hosts current location
     */
    @Override
    public Coord setLocation(int hostID, Coord c) {
        locationsChanged = true;
        return locations.set(hostID, c.clone());
    }

    /**
     * Moves all hosts in the world for a given amount of time
     * @param timeIncrement The time how long all hosts should move
     */
    @Override
    public void moveHosts(double timeIncrement) {
        currentTick++;

        if (locationsChanged) {
            long start = System.nanoTime();
            set_locations();
            locationsChanged = false;
            System.out.printf(" %d:  set_locations = %s\n", currentTick, toHumanTime(System.nanoTime() - start));
        }

        long start = System.nanoTime();
        run_movement_pass(timeIncrement);
        System.out.printf(" %d:  movement = %s\n", currentTick, toHumanTime(System.nanoTime() - start));

        //get_locations();
        //debug_output_positions("gsim");
    }

    private void run_movement_pass(double timeIncrement) {
        double time = SimClock.getTime();

        // Request movement pass
        connector.writeHeader(GSIMConnector.Header.Move);
        connector.writeFloat((float)timeIncrement);

        // Check hosts waiting for new path
        while (!pathWaitingHosts.isEmpty()) {
            PathWaitingHost pwh = pathWaitingHosts.peek();

            if (time < pwh.nextPathAvailableTime) {
                // All remaining hosts have their time in the future
                break;
            }

            // Else we can now try to retrieve the next path
            pathWaitingHosts.poll(); // remove front element
            DTNHost host = hosts.get(pwh.hostID);

            host.setPath(host.getMovementModel().getPath());

            if (host.getPath() == null) {
                // Still no path available
                double nextPathAvailableTime = host.getMovementModel().nextPathAvailable();
                pathWaitingHosts.add(new PathWaitingHost(pwh.hostID, nextPathAvailableTime));
            } else {
                // Just got new path => queue full buffer waypoint request
                waypointRequests.add(new WaypointRequest(pwh.hostID, waypointBufferSize));
                //debug_output_paths(pwh.hostID, host.getPath().getCoords().get(1));
            }
        }

        // Send waypoints updates
        assert(waypointRequests.size() < hosts.size());
        connector.writeInt(waypointRequests.size()); // Number of updates (1 per host)
        while (!waypointRequests.isEmpty()) {
            WaypointRequest request = waypointRequests.poll();
            DTNHost host = hosts.get(request.hostID);
            Path path = host.getPath();

            int available = Math.min(path.remainingWaypoints(), request.numWaypoints);
            assert(available > 0); // Or there should not have been a request

            connector.writeInt(request.hostID); // The following waypoints are for this host
            connector.writeShort(available); // Number of waypoints in this update

            for (int i=0; i < available; i++) {
                connector.writeCoord(path.getNextWaypoint());
                connector.writeFloat((float)path.getSpeed());
            }
        }
        connector.flushOutput();

        // Receive waypoint requests
        int numRequests = connector.readInt();
        for (int i = 0; i < numRequests; i++) {
            int hostID = connector.readInt();
            int numWaypoints = connector.readShort();
            DTNHost host = hosts.get(hostID);

            if (host.getPath().hasNext()) {
                waypointRequests.add(new WaypointRequest(hostID, numWaypoints));
            } else /* path is empty */ {
                if (numWaypoints == waypointBufferSize) /* reached end of path */ {
                    double nextPathAvailableTime = host.getMovementModel().nextPathAvailable();
                    pathWaitingHosts.add(new PathWaitingHost(hostID, nextPathAvailableTime));
                }
                // else ignore threshold request (until hosts reaches end of path)
            }
        }
    }

    private void set_locations() {
        connector.writeHeader(GSIMConnector.Header.SetPositions);
        for (int i = 0; i < locations.size(); i++) {
            connector.writeCoord(locations.get(i));
        }
        connector.flushOutput();
    }

    private void get_locations() {
        connector.writeHeader(GSIMConnector.Header.GetPositions);
        connector.flushOutput();
        for (int i = 0; i < hosts.size(); i++) {
            Coord coord = connector.readCoord();
            locations.set(i, coord);
        }
    }

}
