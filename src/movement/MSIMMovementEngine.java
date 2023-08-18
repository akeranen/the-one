package movement;

import core.DTNHost;
import core.MovementListener;
import core.Settings;
import core.SimClock;
import input.MSIMConnector;

import java.util.List;

/**
 * Provides a GPU accelerated implementation to move hosts in the world according to their MovementModel
 * by communicating to a MSIM process.
 * Supports accelerated interface contact detection (LinkUp/LinkDown Events).
 */
public class MSIMMovementEngine extends MovementEngine {
    /** movement engines -setting id ({@value})*/
    public static final String NAME = "MSIMMovementEngine";
    /** additional arguments -setting id ({@value})*/
    public static final String ARGS_S = "args";

    /** Interface to the MSIM process (communication pipe) */
    private MSIMConnector connector = null;
    /** Additional arguments to be passed to MSIM during initialization */
    private String additionalArgs = null;

    /**
     * Creates a new MovementEngine based on a Settings object's settings.
     * @param s The Settings object where the settings are read from
     */
    public MSIMMovementEngine(Settings s) {
        super(s);

        s.setNameSpace(MovementEngine.MOVEMENT_ENGINE_NS);
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

        // Initialize
        connector.writeHeader(MSIMConnector.Header.Initialize);

        // Send configuration (in cmd line format)
        String num_entities = String.format("--num-entities=%d ", hosts.size());
        String map_size = String.format("--map-width=%d --map-height=%d ", worldSizeX, worldSizeY);
        connector.writeString(num_entities + map_size + additionalArgs);
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

        // get path from movement model and advance until msim waypoints are populated
        // when current path runs empty, try to get new one
        //  set entity speed==0 as special condition


        // Datastructures:
        //  PathWaitingHosts priority queue (hosts waiting for a new path)
        //  int[] HostWaypointRequests (array with number of waypoints requested per host)
        //  WaypointRequestQueue (ids of hosts, waiting for new waypoints)

        // for all path waiting hosts, check if path available
        //  then add path to host
        //  add host to waypoint request queue (wprq)

        // for all hosts in wprq
        //  send new waypoints to MSIM

        // request movement pass

        // receive waypoint requests
        //  if (path.has next)
        //   wprq.add()
        //  else /*path is empty*/
        //   if (full buffer request) /*reached end of path*/
        //    pathwaiting.add()
        //   else /*ignore threshold request*/


        // if enabled, synchronize host locations

        // if enabled, request interface contact detection
        // receive LinkUp/LinkDown events

    }

}
