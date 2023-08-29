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
     * @param timeIncrement how long all nodes should move
     */
    @Override
    public void warmup(double timeIncrement) {
        moveHosts(timeIncrement);
    }

    /**
     * Moves all hosts in the world for a given amount of time
     * @param timeIncrement The time how long all nodes should move
     */
    @Override
    public void moveHosts(double timeIncrement) {
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
            DTNHost host = hosts.get(pwh.hostID);

            host.setPath(host.getMovement().getPath());

            if (host.getPath() == null) {
                // Still no path available
                double nextPathAvailableTime = host.getMovement().nextPathAvailable();
                pathWaitingHosts.add(new PathWaitingHost(pwh.hostID, nextPathAvailableTime));
            }
        }

        // Move all hosts
        for (int i=0,n = hosts.size(); i<n; i++) {
            move(i, timeIncrement);
        }
    }

    /**
     * Moves the host towards the next waypoint or waits if it is
     * not time to move yet
     * @param hostID The ID of the host to be moved
     * @param timeIncrement The time how long the node should move
     */
    public void move(int hostID, double timeIncrement) {
        double possibleMovement;
        double distance;
        double dx, dy;

        DTNHost host = hosts.get(hostID);
        if (!host.isMovementActive() || host.getPath() == null) {
            return;
        }
        if (host.getDestination() == null) {
            if (!setNextWaypoint(hostID, host)) {
                return;
            }
        }

        possibleMovement = timeIncrement * host.getSpeed();
        distance = host.getLocation().distance(host.getDestination());

        while (possibleMovement >= distance) {
            // node can move past its next destination
            host.setLocation(host.getDestination()); // snap to destination
            possibleMovement -= distance;
            if (!setNextWaypoint(hostID, host)) { // get a new waypoint
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
    private boolean setNextWaypoint(int hostID, DTNHost host) {
        assert(host.getPath() != null);

        if (!host.getPath().hasNext()) {
            host.setPath(null);
            double nextPathAvailableTime = host.getMovement().nextPathAvailable();
            pathWaitingHosts.add(new PathWaitingHost(hostID, nextPathAvailableTime));
            return false;
        }

        host.setDestination(host.getPath().getNextWaypoint(), host.getPath().getSpeed());

        return true;
    }



}
