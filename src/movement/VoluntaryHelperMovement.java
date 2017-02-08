package movement;

import core.Coord;
import core.Settings;

/**
 * This movement model simulates the movement of voluntary helpers in a disaster region.
 * It makes use of several other movement models for this.
 *
 * Created by Ansgar Mährlein on 08.02.2017.
 * @author Ansgar Mährlein
 */
public class VoluntaryHelperMovement extends ExtendedMovementModel{

    /**
     * Creates a new VoluntaryHelperMovement
     */
    public VoluntaryHelperMovement() {
        super();
    }

    /**
     * Creates a new VoluntaryHelperMovement
     * @param settings the settings from the settings file
     */
    public VoluntaryHelperMovement(Settings settings) {
        super(settings);
    }

    /**
     * Creates a new VoluntaryHelperMovement from a prototype
     * @param prototype The prototype MovementModel
     */
    public VoluntaryHelperMovement(VoluntaryHelperMovement prototype) {
        super(prototype);
    }

    /**
     * Returns a new path by this movement model or null if no new path could
     * be constructed at the moment (node should wait where it is). A new
     * path should not be requested before the destination of the previous
     * path has been reached.
     * @return A new path or null
     */
    @Override
    public Path getPath(){
        return null;
    }

    /**
     * Returns a new initial placement for a node
     * @return The initial coordinates for a node
     */
    @Override
    public Coord getInitialLocation(){
        return null;
    }

    /**
     * Creates a replicate of the movement model.
     * @return A new movement model with the same settings as this model
     */
    @Override
    public MovementModel replicate(){
        return null;
    }

    /**
     * Method is called between each getPath() request when the current MM is
     * ready (isReady() method returns true). Subclasses should implement all
     * changes of state that need to be made here, for example switching
     * mobility model, etc.
     * @return true if success
     */
    @Override
    public boolean newOrders(){
        return false;
    }
}
