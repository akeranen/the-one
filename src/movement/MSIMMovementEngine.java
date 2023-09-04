package movement;

import core.*;
import input.MSIMConnector;
import interfaces.ConnectivityOptimizer;
import interfaces.MSIMConnectivityOptimizer;

import java.util.*;

/**
 * Provides a GPU accelerated implementation to move hosts in the world according to their MovementModel
 * by communicating to a MSIM process.
 * Supports accelerated connectivity detection.
 * Attention: Connectivity detection currently only supports a single interface type and only 1 interface per host!
 */
public class MSIMMovementEngine extends MovementEngine {
    /** Class name */
    public static final String NAME = "MSIMMovementEngine";
    /** Waypoint buffer size -setting id ({@value})*/
    public static final String WAYPOINT_BUFFER_SIZE_S = "waypointBufferSize";

    /** Interface to the MSIM process */
    private MSIMConnector connector = null;
    /** Number of buffered waypoints per host */
    private int waypointBufferSize = 0;
    /** Queue of hosts waiting for a new path */
    private final PriorityQueue<PathWaitingHost> pathWaitingHosts = new PriorityQueue<>();
    /** Queue of pending waypoint requests */
    //private final ArrayDeque<WaypointRequest> waypointRequests = new ArrayDeque<>(); // TODO benchmark
    private final PriorityQueue<WaypointRequest> waypointRequests = new PriorityQueue<>();
    /** The MSIMConnectivityOptimizer associated with this MSIMMovementEngine */
    private MSIMConnectivityOptimizer optimizer = null;

    static class PathWaitingHost implements Comparable<PathWaitingHost> {
        public int hostID;
        public double nextPathAvailableTime;

        public PathWaitingHost(int hostID, double nextPathAvailableTime) {
            this.hostID = hostID;
            this.nextPathAvailableTime = nextPathAvailableTime;
        }

        @Override
        public int compareTo(PathWaitingHost o) {
            int t = (int)(this.nextPathAvailableTime - o.nextPathAvailableTime);
            return (t != 0) ? t : this.hostID - o.hostID;
        }
    }

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
    public MSIMMovementEngine(Settings s) {
        super(s);

        s.setNameSpace(NAME);
        waypointBufferSize = s.getInt(WAYPOINT_BUFFER_SIZE_S);
        s.restoreNameSpace();

        connector = (MSIMConnector)s.createIntializedObject("input." + MSIMConnector.NAME);
    }

    /**
     * Initializes the movement engine
     * Sends configuration and initial host locations to MSIM
     * Note: Hosts get their initial location on construction, not here!
     * @param hosts to be moved
     */
    @Override
    public void init(List<DTNHost> hosts, int worldSizeX, int worldSizeY) {
        this.hosts = hosts;

        // Initially all hosts wait for a path
        for (int i=0,n = hosts.size(); i<n; i++) {
            //double nextPathAvailableTime = host.movement.nextPathAvailable();
            double nextPathAvailableTime = 0.0;
            pathWaitingHosts.add(new PathWaitingHost(i, nextPathAvailableTime));
        }

        // Start process and open connection
        connector.init(hosts.size(), worldSizeX, worldSizeY, waypointBufferSize);

        // Send initial locations
        connector.writeHeader(MSIMConnector.Header.SetPositions);
        for (int i = 0; i < hosts.size(); i++) {
            DTNHost host = hosts.get(i);
            connector.writeCoord(host.getLocation());

            // Periodically flush output, to allow receiver to work in parallel
            if (i % 1024 == 0) { // TODO benchmark optimal value
                connector.flushOutput();
            }
        }
        connector.flushOutput();
    }

    /**
     * Finalizes the movement engine
     * Called on world shutdown for cleanup
     */
    @Override
    public void fini() {
        connector.writeHeader(MSIMConnector.Header.Shutdown);
        connector.flushOutput();
        try {
            Thread.sleep(500); // Give it some time to shut down gracefully
        } catch (InterruptedException ignored) { }

        // Close connection and shutdown process
        connector.fini();
    }

    /**
     * Moves all hosts in the world for a given amount of time
     * Only performs host movement. Even if enabled it does not
     * perform interface contact detection or other functionality.
     * @param timeIncrement how long all nodes should move
     */
    @Override
    public void warmup(double timeIncrement) {
        run_movement_pass(timeIncrement);
    }

    /**
     * Moves all hosts in the world for a given amount of time
     * @param timeIncrement The time how long all hosts should move
     */
    @Override
    public void moveHosts(double timeIncrement) {

        run_movement_pass(timeIncrement);

        // TODO if enabled, synchronize host locations
        sync_positions();

        // TODO if enabled, request interface contact detection
        run_contact_detection_pass();
        // TODO receive LinkUp/LinkDown events

    }

    /**
     * Returns the MSIMConnectivityOptimizer associated with a MSIMMovementEngine.
     * Returns null if the optimizer is disabled.
     */
    @Override
    public ConnectivityOptimizer optimizer() {
        return optimizer;
    }

    private void sync_positions() {
        connector.writeHeader(MSIMConnector.Header.GetPositions);
        connector.flushOutput();

        for (int i = 0; i < hosts.size(); i++) {
            Coord coord = connector.readCoord();
            hosts.get(i).setLocation(coord);
        }
    }

    private void run_contact_detection_pass() {
        connector.writeHeader(MSIMConnector.Header.ContactDetection);
        connector.flushOutput();
    }

    private void run_movement_pass(double timeIncrement) {
        double time = SimClock.getTime();

        // Request movement pass
        connector.writeHeader(MSIMConnector.Header.Move);
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

            host.setPath(host.getMovement().getPath());

            if (host.getPath() == null) {
                // Still no path available
                double nextPathAvailableTime = host.getMovement().nextPathAvailable();
                pathWaitingHosts.add(new PathWaitingHost(pwh.hostID, nextPathAvailableTime));
            } else {
                // Just got new path => queue full buffer waypoint request
                waypointRequests.add(new WaypointRequest(pwh.hostID, waypointBufferSize));
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
                    double nextPathAvailableTime = host.getMovement().nextPathAvailable();
                    pathWaitingHosts.add(new PathWaitingHost(hostID, nextPathAvailableTime));
                }
                // else ignore threshold request (until hosts reaches end of path)
            }
        }
    }

}
