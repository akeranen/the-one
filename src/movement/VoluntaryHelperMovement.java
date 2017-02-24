package movement;

import core.Coord;
import core.Settings;
import core.VHMListener;
import input.VHMEvent;
import movement.map.SimMap;

import java.util.ArrayList;
import java.util.List;

/**
 * This movement model simulates the movement of voluntary helpers in a disaster region.
 * It makes use of several other movement models for this.
 *
 * Created by Ansgar Mährlein on 08.02.2017.
 * @author Ansgar Mährlein
 */
public class VoluntaryHelperMovement extends ExtendedMovementModel implements VHMListener {

    private ShortestPathMapBasedMovement spmbm;
    private static List<VHMListener> listeners = new ArrayList<>();

    /**
     * Creates a new VoluntaryHelperMovement
     * @param settings the settings from the settings file
     */
    public VoluntaryHelperMovement(Settings settings) {
        super(settings);
        spmbm = new ShortestPathMapBasedMovement(settings);
        setCurrentMovementModel(spmbm);
        VoluntaryHelperMovement.addListener(this);
    }

    /**
     * Creates a new VoluntaryHelperMovement from a prototype
     * @param prototype The prototype MovementModel
     */
    public VoluntaryHelperMovement(VoluntaryHelperMovement prototype) {
        super(prototype);
        spmbm = new ShortestPathMapBasedMovement(prototype.spmbm);
        setCurrentMovementModel(spmbm);
        VoluntaryHelperMovement.addListener(this);
    }

    /**
     * Adds a VHMListener that will be notified of events starting and ending.
     * @param listener The listener that is added.
     */
    public static void addListener(VHMListener listener) {
        listeners.add(listener);
    }

    /**
     * Informs all registered VHMListeners, that a VHMEvent started
     * @param event The VHMEvent.
     */
    public static void eventStarted(VHMEvent event) {
        for (VHMListener l : listeners)
            l.vhmEventStarted(event);
    }

    /**
     * Informs all registered VHMListeners, that a VHMEvent ended
     * @param event The VHMEvent.
     */
    public static void eventEnded(VHMEvent event) {
        for (VHMListener l : listeners)
            l.vhmEventEnded(event);
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

    /**
     * This Method is called when a VHMEvent starts
     *
     * @param event The VHMEvent
     */
    @Override
    public void vhmEventStarted(VHMEvent event) {
        //TODO handle the event
    }

    /**
     * This Method is called when a VHMEvent ends
     *
     * @param event The VHMEvent
     */
    @Override
    public void vhmEventEnded(VHMEvent event) {
        //TODO handle the event
    }

    /**
     * Switches the movement model and resets the host to use it after the next update
     *
     * @param mm the new movement model
     */
    private void switchToMovement(SwitchableMovement mm){
        this.setCurrentMovementModel(mm);
        this.host.interruptMovement();
    }
}
