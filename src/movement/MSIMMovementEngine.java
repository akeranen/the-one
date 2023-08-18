package movement;

import core.DTNHost;
import core.MovementListener;
import core.Settings;
import core.SimClock;
import core.Coord;
import input.MSIMConnector;

import java.util.ArrayDeque;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Provides a GPU accelerated implementation to move hosts in the world according to their MovementModel
 * by communicating to a MSIM process.
 * Supports accelerated interface contact detection (LinkUp/LinkDown Events).
 */
public class MSIMMovementEngine extends MovementEngine {
    /** movement engines -setting id ({@value})*/
    public static final String NAME = "MSIMMovementEngine";
    /** waypoint buffer size -setting id ({@value})*/
    public static final String WAYPOINT_BUFFER_SIZE_S = "waypointBufferSize";
    /** additional arguments -setting id ({@value})*/
    public static final String ARGS_S = "additionalArgs";

    /** Interface to the MSIM process (communication pipe) */
    private MSIMConnector connector = null;
    /** Number of buffered waypoints per host */
    private int waypointBufferSize = 0;
    /** Additional (override) arguments to be passed to MSIM during initialization */
    private String additionalArgs = null;
    /** queue of hosts waiting for a new path */
    private final PriorityQueue<MSIMMovementEngine.PathWaitingHost> pathWaitingHosts = new PriorityQueue<>();
    /** queue of pending waypoint requests */ // TODO maybe turn into priorityQueue and sort by ID for cache optimization?
    private final ArrayDeque<MSIMMovementEngine.WaypointRequest> waypointRequests = new ArrayDeque<>();


    //private static int PathWaitingHostCount = 0; // Enable if stable ordering is required
    static class PathWaitingHost implements Comparable<MSIMMovementEngine.PathWaitingHost> {
        public int hostID;
        public double nextPathAvailableTime;
        //private final int order; // Enable if stable ordering is required

        public PathWaitingHost(int hostID, double nextPathAvailableTime) {
            this.hostID = hostID;
            this.nextPathAvailableTime = nextPathAvailableTime;
            //this.order = PathWaitingHostCount++; // Enable if stable ordering is required
        }

        @Override
        public int compareTo(MSIMMovementEngine.PathWaitingHost o) {
            int t = (int)(this.nextPathAvailableTime - o.nextPathAvailableTime);
            //return (t != 0) ? t : this.order - o.order; // Enable if stable ordering is required
            return t;
        }
    }

    static class WaypointRequest {
        public int hostID;
        public int numWaypoints; // request size

        public WaypointRequest(int hostID, int numWaypoints) {
            this.hostID = hostID;
            this.numWaypoints = numWaypoints;
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
        additionalArgs = s.getSetting(ARGS_S, "");
        s.restoreNameSpace();

        connector = (MSIMConnector)s.createIntializedObject("input." + MSIMConnector.NAME);
    }

    /**
     * Initializes the movement engine
     * Sends configuration and initial host locations to MSIM
     * Note: Hosts get their initial location on construction, not here!
     * @param hosts to be initialized
     */
    @Override
    public void init(List<DTNHost> hosts, int worldSizeX, int worldSizeY) {
        // Initially all hosts wait for a path
        for (int i=0,n = hosts.size(); i<n; i++) {
            //double nextPathAvailableTime = host.movement.nextPathAvailable();
            double nextPathAvailableTime = 0.0;
            pathWaitingHosts.add(new MSIMMovementEngine.PathWaitingHost(i, nextPathAvailableTime));
        }

        // Initialize
        connector.writeHeader(MSIMConnector.Header.Initialize);

        // Send configuration (in cmd line format)
        String num_entities = String.format("--num-entities=%d ", hosts.size());
        String map_size = String.format("--map-width=%d --map-height=%d ", worldSizeX, worldSizeY);
        String waypoint_buffer_size = String.format("--waypoint-buffer-size=%d ", waypointBufferSize);
        connector.writeString(num_entities + map_size + waypoint_buffer_size + additionalArgs);
        connector.flushOutput();

        // Send initial locations
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
    }

    /**
     * Moves all hosts in the world for a given amount of time
     * Only performs host movement. Even if enabled it does not
     * perform interface contact detection or other functionality.
     * @param hosts to be moved
     * @param timeIncrement how long all nodes should move
     */
    @Override
    public void warmup(List<DTNHost> hosts, double timeIncrement) {
        run_movement_pass(hosts, timeIncrement);
    }

    /**
     * Moves all hosts in the world for a given amount of time
     * @param timeIncrement The time how long all hosts should move
     */
    @Override
    public void moveHosts(List<DTNHost> hosts, double timeIncrement) {

        run_movement_pass(hosts, timeIncrement);

        // TODO if enabled, synchronize host locations

        // TODO if enabled, request interface contact detection
        // TODO receive LinkUp/LinkDown events

    }

    private void run_movement_pass(List<DTNHost> hosts, double timeIncrement) {
        double time = SimClock.getTime();

        // Request movement pass
        connector.writeHeader(MSIMConnector.Header.Move);
        connector.writeFloat((float)timeIncrement);

        // Check hosts waiting for new path
        while (!pathWaitingHosts.isEmpty()) {
            MSIMMovementEngine.PathWaitingHost pwh = pathWaitingHosts.peek();

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
                pathWaitingHosts.add(new MSIMMovementEngine.PathWaitingHost(pwh.hostID, nextPathAvailableTime));
            } else {
                // Just got new path => queue full buffer waypoint request
                waypointRequests.add(new WaypointRequest(pwh.hostID, waypointBufferSize));
            }
        }

        // Send waypoints updates
        assert(waypointRequests.size() < hosts.size());
        connector.writeInt(waypointRequests.size()); // Number of updates (1 per host)
        System.out.printf("MSIMMovementEngine.move() Sending %d waypoint updates\n", waypointRequests.size());
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
        System.out.printf("MSIMMovementEngine.move() Received %d waypoint requests\n", numRequests);
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
