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
    /** Optimizer for interface connectivity detection */
    protected List<ConnectivityOptimizer> optimizer = new ArrayList<>();
    /** Current simulation tick */
    protected static long currentTick = 0;
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

        for (DTNHost host : hosts) {
            // Set initial location
            locations.add(host.getMovementModel().getInitialLocation());
            host.notifyInitialLocation();

            // Initially all hosts wait for a path
            double nextPathAvailableTime = host.getMovementModel().nextPathAvailable();
            pathWaitingHosts.add(new PathWaitingHost(host.getID(), nextPathAvailableTime));
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
    public abstract void moveHosts(double timeIncrement);

    /**
     * Detects host connectivity
     * Issues LinkUp/LinkDown events to interfaces
     */
    public void detectConnectivity() {
        for (ConnectivityOptimizer opt : optimizer) {
            opt.detectConnectivity();
        }
    }

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

    public static long getCurrentTick() { return currentTick; }
}
