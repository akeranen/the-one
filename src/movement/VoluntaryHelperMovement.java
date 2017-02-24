package movement;

import core.Coord;
import core.Settings;
import core.VHMListener;
import input.VHMEvent;
import movement.map.SimMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static input.VHMEvent.VHMEventType.DISASTER;
import static input.VHMEvent.VHMEventType.HOSPITAL;

/**
 * This movement model simulates the movement of voluntary helpers in a disaster region.
 * It makes use of several other movement models for this.
 *
 * Created by Ansgar Mährlein on 08.02.2017.
 * @author Ansgar Mährlein
 */
public class VoluntaryHelperMovement extends ExtendedMovementModel implements VHMListener {

    public static final String IS_LOCAL_HELPER_SETTING = "isLocalHelper";

    private int mode;
    private static final int RANDOM_MAP_BASED_MODE = 0;
    private static final int MOVING_TO_EVENT_MODE = 1;
    private static final int LOCAL_HELP_MODE = 2;
    private static final int TRANSPORTING_MODE = 3;
    private static final int HOSPITAL_WAIT_MODE = 4;
    private static final int INJURED_MODE = 5;
    private static final int PANIC_MODE = 6;

    private boolean isLocalHelper;

    private List<VHMEvent> events = new ArrayList<>();

    private static List<VHMEvent> hospitals = Collections.synchronizedList(new ArrayList<>());

    private ShortestPathMapBasedMovement shortestPathMapBasedMM;
    private LevyWalkMovement levyWalkMM;

    private static List<VHMListener> listeners = Collections.synchronizedList(new ArrayList<>());

    /**
     * Creates a new VoluntaryHelperMovement
     * @param settings the settings from the settings file
     */
    public VoluntaryHelperMovement(Settings settings) {
        super(settings);
        shortestPathMapBasedMM = new ShortestPathMapBasedMovement(settings);
        levyWalkMM = new LevyWalkMovement(settings);
        VoluntaryHelperMovement.addListener(this);
        //TODO check if settings namespace works out
        isLocalHelper = settings.getBoolean(IS_LOCAL_HELPER_SETTING, false);

        mode = RANDOM_MAP_BASED_MODE;
        setCurrentMovementModel(shortestPathMapBasedMM);
    }

    /**
     * Creates a new VoluntaryHelperMovement from a prototype
     * @param prototype The prototype MovementModel
     */
    public VoluntaryHelperMovement(VoluntaryHelperMovement prototype) {
        super(prototype);
        shortestPathMapBasedMM = new ShortestPathMapBasedMovement(prototype.shortestPathMapBasedMM);
        levyWalkMM = new LevyWalkMovement(prototype.levyWalkMM);
        VoluntaryHelperMovement.addListener(this);
        isLocalHelper = prototype.isLocalHelper;

        mode = RANDOM_MAP_BASED_MODE;
        setCurrentMovementModel(shortestPathMapBasedMM);
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
        if(event.getType() == HOSPITAL)
            hospitals.add(event);
    }

    /**
     * Informs all registered VHMListeners, that a VHMEvent ended
     * @param event The VHMEvent.
     */
    public static void eventEnded(VHMEvent event) {
        for (VHMListener l : listeners)
            l.vhmEventEnded(event);
        //TODO remove from hospitals
        if(event.getType() == HOSPITAL);
    }

    /**
     * Returns a new initial placement for a node
     * @return The initial coordinates for a node
     */
    @Override
    public Coord getInitialLocation(){
        return shortestPathMapBasedMM.getInitialLocation();
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
        return shortestPathMapBasedMM.getMap();
    }

    /**
     * This Method is called when a VHMEvent starts
     *
     * @param event The VHMEvent
     */
    @Override
    public void vhmEventStarted(VHMEvent event) {
        //TODO handle event
        if(event.getType() == DISASTER);
            events.add(event);
    }

    /**
     * This Method is called when a VHMEvent ends
     *
     * @param event The VHMEvent
     */
    @Override
    public void vhmEventEnded(VHMEvent event) {
        //TODO remove from List and handle
        if(event.getType() == DISASTER);
    }
}
