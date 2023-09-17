package movement;

import core.Coord;
import core.DTNHost;
import core.Settings;
import interfaces.ConnectivityOptimizer;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;


/**
 * Provides means to move hosts in the world according to their MovementModel
 */
public abstract class MovementEngine {
    /** MovementEngine namespace ({@value})*/
    public static final String MOVEMENT_ENGINE_NS = "MovementEngine";
    /** The movement engine type -setting id ({@value})*/
    public static final String TYPE = "type";

    /** List of all hosts in the simulation */
    protected List<DTNHost> hosts = null;
    /** List of all host locations in the simulation */
    protected List<Coord> locations = null;
    /** Current simulation tick */
    protected long currentTick = 0;
    /** Queue of hosts waiting for a new path */
    protected final PriorityQueue<PathWaitingHost> pathWaitingHosts = new PriorityQueue<>();

    /**
     * Helper class for managing hosts, which are waiting for a new movement path.
     * When a host reaches the end of its path, it has to query its movement model for a new one.
     * However, it may not be available immediately.
     */
    protected static class PathWaitingHost implements Comparable<PathWaitingHost> {
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

    /**
     * Creates a new MovementEngine based on a Settings object's settings.
     * @param settings The Settings object where the settings are read from
     */
    public MovementEngine(Settings settings) { }

    /**
     * Initializes the movement engine
     * @param hosts to be initialized
     */
    public void init(List<DTNHost> hosts, int worldSizeX, int worldSizeY) {
        this.hosts = hosts;
        this.locations = new ArrayList<>(hosts.size());

        for (int i=0; i<hosts.size(); i++) {
            // Set initial location
            locations.add(hosts.get(i).getMovementModel().getInitialLocation());
            hosts.get(i).notifyInitialLocation();

            // Initially all hosts wait for a path
            //double nextPathAvailableTime = host.movement.nextPathAvailable(); // TODO ?
            double nextPathAvailableTime = 0.0;
            pathWaitingHosts.add(new PathWaitingHost(i, nextPathAvailableTime));
        }
    }

    /**
     * Finalizes the movement engine
     * Called on world shutdown for cleanup
     */
    public abstract void fini();

    /**
     * Moves all hosts in the world for a given amount of time
     * @param timeIncrement how long all nodes should move
     */
    public abstract void warmup(double timeIncrement);

    /**
     * Moves all hosts in the world for a given amount of time
     * @param timeIncrement how long all nodes should move
     */
    public abstract void moveHosts(double timeIncrement);

    /**
     * Returns the ConnectivityOptimizer associated with this MovementEngine, or null if none.
     */
    public abstract ConnectivityOptimizer optimizer();

    /**
     * Returns a hosts current location
     * @param hostID The ID of the host
     * @return the hosts current location
     */
    public Coord getLocation(int hostID) {
        return locations.get(hostID);
    }

    /**
     * Returns a hosts current location
     * @param hostID The ID of the host
     * @param c The new location
     * @return the hosts current location
     */
    public Coord setLocation(int hostID, Coord c) {
        return locations.set(hostID, c.clone());
    }

    protected void debug_output_positions(String name) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileOutputStream(
                    "/home/crydsch/msim/logs/debug/pos_" + name,true));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < hosts.size(); i++) {
            DTNHost host = hosts.get(i);
            //writer.printf("%03d,%04d,%f,%f\n", currentTick, i, host.getLocation().getX(), host.getLocation().getY());
            writer.printf("%03d,%04d,%a,%a\n", currentTick, i, host.getLocation().getX(), host.getLocation().getY());
        }
        writer.close();
    }
}
