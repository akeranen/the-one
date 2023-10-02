package movement;

import core.*;
import interfaces.ConnectivityGrid;
import interfaces.ConnectivityOptimizer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;

/**
 * Provides a default implementation to move hosts in the world according to their MovementModel
 */
public class DefaultMovementEngine extends MovementEngine {
    /** movement engines -setting id ({@value})*/
    public static final String NAME = "DefaultMovementEngine";
    private final Coord ORIGIN = new Coord(0.0, 0.0);

    /**
     * Creates a new MovementEngine based on a Settings object's settings.
     * @param settings The Settings object where the settings are read from
     */
    public DefaultMovementEngine(Settings settings) {
        super(settings);
    }

    /**
     * Initializes the movement engine
     * @param hosts to be moved
     */
    @Override
    public void init(List<DTNHost> hosts, int worldSizeX, int worldSizeY) {
        super.init(hosts, worldSizeX, worldSizeY);

        // Initialize optimizer
        Map<String, List<NetworkInterface>> interface_map = new HashMap<>();
        for (DTNHost host : hosts) {
            for (NetworkInterface ni : host.getInterfaces()) {
                if (ni.getTransmitRange() > 0) {
                    interface_map.computeIfAbsent(ni.getInterfaceType(), k -> new ArrayList<>()).add(ni);
                }
            }
        }

        for (List<NetworkInterface> interfaces : interface_map.values()) {
            optimizer.add(new ConnectivityGrid(interfaces));
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
        currentTick++;

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

            host.setPath(host.getMovementModel().getPath());

            if (host.getPath() == null) {
                // Still no path available
                double nextPathAvailableTime = host.getMovementModel().nextPathAvailable();
                pathWaitingHosts.add(new PathWaitingHost(pwh.hostID, nextPathAvailableTime));
            } else {
                //debug_output_paths(pwh.hostID, host.getPath().getCoords().get(1));
            }
        }

        // Move all hosts
        for (int i=0,n = hosts.size(); i<n; i++) {
            move(i, timeIncrement);
        }

        //debug_output_positions("one");
    }

    /**
     * Moves the host towards the next waypoint or waits if it is
     * not time to move yet
     * @param hostID The ID of the host to be moved
     * @param timeIncrement The time how long the node should move
     */
    public void move(int hostID, double timeIncrement) {
        DTNHost host = hosts.get(hostID);
        if (!host.isMovementActive() || host.getPath() == null) {
            return;
        }
        if (host.getDestination() == null) {
            if (!setNextWaypoint(hostID, host)) {
                return;
            }
        }

        Coord target = host.getDestination();
        double speed = host.getSpeed();

        double dtt = host.getLocation().distance(target); // distance to target
        double ttt = dtt / speed; // time to target

        while (timeIncrement >= ttt) {
            // node can move past its next destination
            host.setLocation(target); // snap to destination
            timeIncrement -= ttt;

            if (!setNextWaypoint(hostID, host)) { // get a new waypoint
                return; // no more waypoints left
            }

            target = host.getDestination();
            speed = host.getSpeed();
            dtt = host.getLocation().distance(target);
            ttt = dtt / speed;
        }

        // move towards the target
        Coord pos = host.getLocation();
        Coord dir = new Coord(target.getX() - pos.getX(), target.getY() - pos.getY());
        double length = dir.distance(ORIGIN);

        if (length > 0) {
            dir.setLocation(dir.getX() / length, dir.getY() / length); // Normalize
            pos.translate(dir.getX() * timeIncrement, dir.getY() * timeIncrement);
            host.setLocation(pos);
        }
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
            host.setDestination(null, 0.0);
            double nextPathAvailableTime = host.getMovementModel().nextPathAvailable();
            pathWaitingHosts.add(new PathWaitingHost(hostID, nextPathAvailableTime));
            return false;
        }

        host.setDestination(host.getPath().getNextWaypoint(), host.getPath().getSpeed());

        return true;
    }

    private void debug_output_destinations(int hostID, Coord target) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileOutputStream(
                    "/home/crydsch/msim/logs/debug/dest_one.txt",true));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        // Format: tick,ID,x,y
        writer.printf("%d,%d,%f,%f\n", currentTick, hostID, target.getX(), target.getY());
        writer.close();
    }

}
