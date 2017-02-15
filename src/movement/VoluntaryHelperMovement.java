package movement;

import core.Coord;
import core.Settings;
import movement.map.SimMap;

/**
 * This movement model simulates the movement of voluntary helpers in a disaster region.
 * It makes use of several other movement models for this.
 *
 * Created by Ansgar Mährlein on 08.02.2017.
 * @author Ansgar Mährlein
 */
public class VoluntaryHelperMovement extends ExtendedMovementModel{

    private ShortestPathMapBasedMovement spmbm;

    /**
     * Creates a new VoluntaryHelperMovement
     * @param settings the settings from the settings file
     */
    public VoluntaryHelperMovement(Settings settings) {
        super(settings);
        spmbm = new ShortestPathMapBasedMovement(settings);
        setCurrentMovementModel(spmbm);
    }

    /**
     * Creates a new VoluntaryHelperMovement from a prototype
     * @param prototype The prototype MovementModel
     */
    public VoluntaryHelperMovement(VoluntaryHelperMovement prototype) {
        super(prototype);
        spmbm = new ShortestPathMapBasedMovement(prototype.spmbm);
        setCurrentMovementModel(spmbm);
    }

    /**
     * Returns a new initial placement for a node
     * @return The initial coordinates for a node
     */
    @Override
    public Coord getInitialLocation(){
        return spmbm.getInitialLocation();
    }

    /**
     * Creates a replicate of the movement model.
     * @return A new movement model with the same settings as this model
     */
    @Override
    public MovementModel replicate(){
        return new VoluntaryHelperMovement(this);
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

        return true;
    }

    /**
     * Returns the SimMap this movement model uses
     * @return The SimMap this movement model uses
     */
    public SimMap getMap() {
        return spmbm.getMap();
    }
}
