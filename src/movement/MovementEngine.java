package movement;

import core.DTNHost;
import core.Settings;

import java.util.List;


/**
 * Provides means to move hosts in the world according to their MovementModel
 */
public abstract class MovementEngine {
    /** MovementEngine namespace ({@value})*/
    public static final String MOVEMENT_ENGINE_NS = "MovementEngine";
    /** movement models' rng seed -setting id ({@value})*/
    public static final String NAME = "name";

    /**
     * Creates a new MovementEngine based on a Settings object's settings.
     * @param settings The Settings object where the settings are read from
     */
    public MovementEngine(Settings settings) { }

    public abstract void init(List<DTNHost> hosts, int worldSizeX, int worldSizeY);

    /**
     * Moves all hosts in the world for a given amount of time
     * @param timeIncrement The time how long all nodes should move
     */
    public abstract void moveHosts(List<DTNHost> hosts, double timeIncrement);



}
