package movement;

import core.DTNHost;
import core.Settings;
import interfaces.ConnectivityOptimizer;

import java.io.*;
import java.util.List;


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
    /** Current simulation tick */
    protected long currentTick = 0;

    /**
     * Creates a new MovementEngine based on a Settings object's settings.
     * @param settings The Settings object where the settings are read from
     */
    public MovementEngine(Settings settings) { }

    /**
     * Initializes the movement engine
     * Note: Hosts get their initial location on construction, not here!
     * @param hosts to be initialized
     */
    public abstract void init(List<DTNHost> hosts, int worldSizeX, int worldSizeY);

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

    protected void debug_output_positions() {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileOutputStream(
                    "/home/crydsch/msim/logs/debug/pos_one.txt",true));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        for (DTNHost host : hosts) {
            writer.printf("%a\n%a\n", host.getLocation().getX(), host.getLocation().getY());
        }
        writer.close();
    }
}
