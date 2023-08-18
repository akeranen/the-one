package movement;

import core.*;

import java.util.List;
import java.util.PriorityQueue;

/**
 * Provides a default implementation to move hosts in the world according to their MovementModel
 */
public class DefaultMovementEngine extends MovementEngine {
    /** movement engines -setting id ({@value})*/
    public static final String NAME = "DefaultMovementEngine";
    /** queue of hosts waiting for a new path */
    private final PriorityQueue<PathWaitingHost> pathWaitingHosts = new PriorityQueue<>();

    //private static int PathWaitingHostCount = 0; // Enable if stable ordering is required
    static class PathWaitingHost implements Comparable<PathWaitingHost> {
        public DTNHost host;
        public double nextPathAvailableTime;
        //private final int order; // Enable if stable ordering is required

        public PathWaitingHost(DTNHost host, double nextPathAvailableTime) {
            this.host = host;
            this.nextPathAvailableTime = nextPathAvailableTime;
            //this.order = PathWaitingHostCount++; // Enable if stable ordering is required
        }

        @Override
        public int compareTo(PathWaitingHost o) {
            int t = (int)(this.nextPathAvailableTime - o.nextPathAvailableTime);
            //return (t != 0) ? t : this.order - o.order; // Enable if stable ordering is required
            return t;
        }
    }

    /**
     * Creates a new MovementEngine based on a Settings object's settings.
     * @param settings The Settings object where the settings are read from
     */
    public DefaultMovementEngine(Settings settings) {
        super(settings);
    }

    /**
     * Initializes the movement engine
     * Note: Hosts get their initial location on construction, not here!
     * @param hosts to be initialized
     */
    @Override
    public void init(List<DTNHost> hosts, int worldSizeX, int worldSizeY) {
        // Initially all hosts wait for a path
        for (int i=0,n = hosts.size(); i<n; i++) {
            DTNHost host = hosts.get(i);

            //double nextPathAvailableTime = host.movement.nextPathAvailable();
            double nextPathAvailableTime = 0.0;
            pathWaitingHosts.add(new PathWaitingHost(host, nextPathAvailableTime));
        }
    }

    /**
     * Finalizes the movement engine
     * Called on world shutdown for cleanup
     */
    @Override
    public void fini() {
        // No cleanup necessary
    }

    /**
     * Moves all hosts in the world for a given amount of time
     * @param hosts to be moved
     * @param timeIncrement how long all nodes should move
     */
    @Override
    public void warmup(List<DTNHost> hosts, double timeIncrement) {
        moveHosts(hosts, timeIncrement);
    }

    /**
     * Moves all hosts in the world for a given amount of time
     * @param hosts to be moved
     * @param timeIncrement The time how long all nodes should move
     */
    @Override
    public void moveHosts(List<DTNHost> hosts, double timeIncrement) {
        double time = SimClock.getTime();

        // Check hosts waiting for new path
        while (!pathWaitingHosts.isEmpty()) {
            PathWaitingHost pwh = pathWaitingHosts.peek();

            if (time < pwh.nextPathAvailableTime) {
                // All remaining hosts have their time in the future
                break;
            }

            // Else we can now try to retrieve the next path
            pathWaitingHosts.poll(); // remove front element
            DTNHost host = pwh.host;

            host.setPath(host.getMovement().getPath());

            if (host.getPath() == null) {
                // Still no path available
                double nextPathAvailableTime = host.getMovement().nextPathAvailable();
                pathWaitingHosts.add(new PathWaitingHost(host, nextPathAvailableTime));
            }
        }

        // Move all hosts
        for (int i=0,n = hosts.size(); i<n; i++) {
            DTNHost host = hosts.get(i);
            move(host, timeIncrement);
        }
    }

    /**
     * Moves the host towards the next waypoint or waits if it is
     * not time to move yet
     * @param host The host to be moved
     * @param timeIncrement The time how long the node should move
     */
    public void move(DTNHost host, double timeIncrement) {
        double possibleMovement;
        double distance;
        double dx, dy;

        if (!host.isMovementActive() || host.getPath() == null) {
            return;
        }
        if (host.getDestination() == null) {
            if (!setNextWaypoint(host)) {
                return;
            }
        }

        possibleMovement = timeIncrement * host.getSpeed();
        distance = host.getLocation().distance(host.getDestination());

        while (possibleMovement >= distance) {
            // node can move past its next destination
            host.setLocation(host.getDestination()); // snap to destination
            possibleMovement -= distance;
            if (!setNextWaypoint(host)) { // get a new waypoint
                return; // no more waypoints left
            }
            distance = host.getLocation().distance(host.getDestination());
        }

        // move towards the point for possibleMovement amount
        Coord location = host.getLocation();
        Coord destination = host.getDestination();
        dx = (possibleMovement/distance) *
            (destination.getX() - location.getX());
        dy = (possibleMovement/distance) *
            (destination.getY() - location.getY());
        location.translate(dx, dy);
        host.setLocation(location);
    }

    /**
     * Sets the next destination and speed to correspond to the next waypoint
     * on the path.
     * @param host whose next waypoint should be set
     * @return True if there was a next waypoint to set, false if node still
     * should wait
     */
    private boolean setNextWaypoint(DTNHost host) {
        assert(host.getPath() != null);

        if (!host.getPath().hasNext()) {
            host.setPath(null);
            double nextPathAvailableTime = host.getMovement().nextPathAvailable();
            pathWaitingHosts.add(new PathWaitingHost(host, nextPathAvailableTime));
            return false;
        }

        host.setDestination(host.getPath().getNextWaypoint(), host.getPath().getSpeed());

        return true;
    }



}
