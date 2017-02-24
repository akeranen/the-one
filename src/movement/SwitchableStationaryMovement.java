package movement;

import core.Coord;
import core.Settings;

/**
 * Created by Ansgar Mährlein on 24.02.2017.
 * @author Ansgar Mährlein
 */
public class SwitchableStationaryMovement extends MovementModel implements SwitchableMovement {
    /** The location of the node */
    private Coord loc;

    /**
     * Creates a new movement model based on a Settings object's settings.
     * @param s The Settings object where the settings are read from
     */
    public SwitchableStationaryMovement(Settings s) {
        super(s);
        loc = new Coord(0, 0);
    }

    /**
     * Copy constructor.
     * @param prototype The StationaryMovement prototype
     */
    public SwitchableStationaryMovement(SwitchableStationaryMovement prototype) {
        super(prototype);
        loc = prototype.loc;
    }

    /**
     * Returns a new path by this movement model or null if no new path could
     * be constructed at the moment (node should wait where it is). A new
     * path should not be requested before the destination of the previous
     * path has been reached.
     *
     * @return A new path or null
     */
    @Override
    public Path getPath() {
        Path p = new Path(0);
        p.addWaypoint(loc);
        return p;
    }

    /**
     * Returns a new initial placement for a node
     *
     * @return The initial coordinates for a node
     */
    @Override
    public Coord getInitialLocation() {
        return loc;
    }

    /**
     * Creates a replicate of the movement model.
     *
     * @return A new movement model with the same settings as this model
     */
    @Override
    public MovementModel replicate() {
        return new SwitchableStationaryMovement(this);
    }

    /**
     * Tell the movement model what its current location is
     *
     * @param coord The last location of the node.
     */
    @Override
    public void setLocation(Coord coord) {
        loc = coord;
    }

    /**
     * Get the last location the getPath() of this movement model has returned
     *
     * @return the last location
     */
    @Override
    public Coord getLastLocation() {
        return loc;
    }

    /**
     * Checks if the movement model is finished doing its task and it's time to
     * switch to the next movement model. The method should be called between
     * getPath() calls.
     *
     * @return true if ready
     */
    @Override
    public boolean isReady() {
        //TODO determine what is best here (note: battery can die)
        return false;
    }
}
