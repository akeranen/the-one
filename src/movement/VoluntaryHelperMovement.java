package movement;

import core.*;
import input.VHMEvent;
import movement.map.MapNode;
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
//TODO implement reaction to dead battery
    //TODO comments + javadoc
public class VoluntaryHelperMovement extends ExtendedMovementModel implements VHMListener {

    public static final String IS_LOCAL_HELPER_SETTING = "isLocalHelper";
    public static final String HELP_TIME_SETTING = "helpTime";
    public static final String HOSPITAL_WAIT_TIME_SETTING = "hospitalWaitTime";
    public static final String INJURY_PROBABILITY_SETTING = "injuryProbability";
    public static final String HOSPITAL_WAIT_PROBABILITY_SETTING = "hospitalWaitProbability";
    public static final String INTENSITY_WEIGHT_SETTING = "intensityWeight";

    public static final double DEFAULT_HELP_TIME = 3600;
    public static final double DEFAULT_HOSPITAL_WAIT_TIME = 3600;
    public static final double DEFAULT_INJURY_PROBABILITY = 0.5;
    public static final double DEFAULT_HOSPITAL_WAIT_PROBABILITY = 0.5;
    public static final double DEFAULT_INTENSITY_WEIGHT = 0.5;


    private int mode;
    private static final int RANDOM_MAP_BASED_MODE = 0;
    private static final int MOVING_TO_EVENT_MODE = 1;
    private static final int LOCAL_HELP_MODE = 2;
    private static final int TRANSPORTING_MODE = 3;
    private static final int HOSPITAL_WAIT_MODE = 4;
    private static final int INJURED_MODE = 5;
    private static final int PANIC_MODE = 6;

    private boolean isLocalHelper;
    private double hospitalWaitTime;
    private double helpTime;
    private double injuryProbability;
    private double waitProbability;
    private double intensityWeight;
    private double startTime;

    private VHMEvent chosenEvent;

    private List<VHMEvent> events = new ArrayList<>();

    private static List<VHMEvent> hospitals = Collections.synchronizedList(new ArrayList<>());

    private ShortestPathMapBasedMovement shortestPathMapBasedMM;
    private CarMovement carMM;
    private LevyWalkMovement levyWalkMM;
    private SwitchableStationaryMovement stationaryMM;

    private SimMap simMap;

    private static List<VHMListener> listeners = Collections.synchronizedList(new ArrayList<>());

    /**
     * Creates a new VoluntaryHelperMovement
     * @param settings the settings from the settings file
     */
    public VoluntaryHelperMovement(Settings settings) {
        super(settings);

        //TODO check if settings namespace works out
        isLocalHelper = settings.getBoolean(IS_LOCAL_HELPER_SETTING, false);
        helpTime = settings.getDouble(HELP_TIME_SETTING, DEFAULT_HELP_TIME);
        hospitalWaitTime = settings.getDouble(HOSPITAL_WAIT_TIME_SETTING, DEFAULT_HOSPITAL_WAIT_TIME);
        injuryProbability = settings.getDouble(INJURY_PROBABILITY_SETTING, DEFAULT_INJURY_PROBABILITY);
        waitProbability = settings.getDouble(HOSPITAL_WAIT_PROBABILITY_SETTING, DEFAULT_HOSPITAL_WAIT_PROBABILITY);
        intensityWeight = settings.getDouble(INTENSITY_WEIGHT_SETTING, DEFAULT_INTENSITY_WEIGHT);

        shortestPathMapBasedMM = new ShortestPathMapBasedMovement(settings);
        carMM = new CarMovement(settings);
        levyWalkMM = new LevyWalkMovement(settings);
        stationaryMM = new SwitchableStationaryMovement(settings);

        simMap = this.getMap();

        VoluntaryHelperMovement.addListener(this);

        startTime = SimClock.getTime();
        mode = RANDOM_MAP_BASED_MODE;
        setCurrentMovementModel(shortestPathMapBasedMM);
    }

    /**
     * Creates a new VoluntaryHelperMovement from a prototype
     * @param prototype The prototype MovementModel
     */
    public VoluntaryHelperMovement(VoluntaryHelperMovement prototype) {
        super(prototype);

        isLocalHelper = prototype.isLocalHelper;
        helpTime = prototype.helpTime;
        hospitalWaitTime = prototype.hospitalWaitTime;
        injuryProbability = prototype.injuryProbability;
        waitProbability = prototype.waitProbability;
        intensityWeight = prototype.intensityWeight;

        shortestPathMapBasedMM = new ShortestPathMapBasedMovement(prototype.shortestPathMapBasedMM);
        carMM = prototype.carMM;
        levyWalkMM = new LevyWalkMovement(prototype.levyWalkMM);
        stationaryMM = new SwitchableStationaryMovement(prototype.stationaryMM);

        simMap = prototype.getMap();

        VoluntaryHelperMovement.addListener(this);

        startTime = prototype.startTime;
        mode = prototype.mode;
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
        switch(mode){
            case RANDOM_MAP_BASED_MODE : {
                if(selectNextEvent()) {
                    mode = MOVING_TO_EVENT_MODE;
                    carMM.setLocation(host.getLocation());
                    carMM.setNextRoute(shortestPathMapBasedMM.getLastLocation(), simMap.getClosestNodeByCoord(chosenEvent.getLocation()).getLocation());
                    setCurrentMovementModel(carMM);
                }
                break;
            }
            case MOVING_TO_EVENT_MODE : {
                if(isLocalHelper){
                    mode = LOCAL_HELP_MODE;
                    levyWalkMM.setLocation(host.getLocation());
                    levyWalkMM.setCenter(chosenEvent.getLocation());
                    levyWalkMM.setRadius(chosenEvent.getEventRange());
                    startTime = SimClock.getTime();
                    setCurrentMovementModel(levyWalkMM);
                } else {
                    mode = TRANSPORTING_MODE;
                    carMM.setLocation(host.getLocation());
                    //TODO make it chose the closest hospital/random instead of the first in the list
                    carMM.setNextRoute(carMM.getLastLocation(), simMap.getClosestNodeByCoord(hospitals.get(0).getLocation()).getLocation());
                    setCurrentMovementModel(carMM);
                }
                break;
            }
            case LOCAL_HELP_MODE : {
                if(SimClock.getTime() - startTime >= helpTime) {
                    if(selectNextEvent()) {
                        mode = MOVING_TO_EVENT_MODE;
                        carMM.setLocation(host.getLocation());
                        carMM.setNextRoute(carMM.getLastLocation(), simMap.getClosestNodeByCoord(chosenEvent.getLocation()).getLocation());
                        setCurrentMovementModel(carMM);
                    } else {
                        mode = RANDOM_MAP_BASED_MODE;
                        shortestPathMapBasedMM.setLocation(host.getLocation());
                        setCurrentMovementModel(shortestPathMapBasedMM);
                    }
                }
                break;
            }
            case TRANSPORTING_MODE : {
                if(rng.nextDouble() <= waitProbability) {
                    mode = MOVING_TO_EVENT_MODE;
                    carMM.setLocation(host.getLocation());
                    carMM.setNextRoute(carMM.getLastLocation(), simMap.getClosestNodeByCoord(chosenEvent.getLocation()).getLocation());
                    setCurrentMovementModel(carMM);
                } else {
                    mode = HOSPITAL_WAIT_MODE;
                    levyWalkMM.setLocation(host.getLocation());
                    //TODO make it chose the closest hospital/random instead of the first in the list
                    levyWalkMM.setCenter(hospitals.get(0).getLocation());
                    levyWalkMM.setRadius(hospitals.get(0).getEventRange());
                    startTime = SimClock.getTime();
                    setCurrentMovementModel(levyWalkMM);
                }
                break;
            }
            case HOSPITAL_WAIT_MODE : {
                if(SimClock.getTime() - startTime >= hospitalWaitTime) {
                    if(selectNextEvent()) {
                        mode = MOVING_TO_EVENT_MODE;
                        carMM.setLocation(host.getLocation());
                        carMM.setNextRoute(carMM.getLastLocation(), simMap.getClosestNodeByCoord(chosenEvent.getLocation()).getLocation());
                        setCurrentMovementModel(carMM);
                    } else {
                        mode = RANDOM_MAP_BASED_MODE;
                        setCurrentMovementModel(shortestPathMapBasedMM);
                    }
                }
                break;
            }
            case INJURED_MODE : {
                //to be safe: make sure the node stays injured
                mode = INJURED_MODE;
                setCurrentMovementModel(stationaryMM);
                break;
            }
            case PANIC_MODE : {
                if(selectNextEvent()) {
                    mode = MOVING_TO_EVENT_MODE;
                    carMM.setLocation(host.getLocation());
                    carMM.setNextRoute(carMM.getLastLocation(), simMap.getClosestNodeByCoord(chosenEvent.getLocation()).getLocation());
                } else {
                    mode = RANDOM_MAP_BASED_MODE;
                    shortestPathMapBasedMM.setLocation(host.getLocation());
                    setCurrentMovementModel(shortestPathMapBasedMM);
                }
                break;
            }
            default : {
                mode = RANDOM_MAP_BASED_MODE;
                shortestPathMapBasedMM.setLocation(host.getLocation());
                setCurrentMovementModel(shortestPathMapBasedMM);
                break;
            }
        }
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
    //TODO force MM change
    public void vhmEventStarted(VHMEvent event) {
        if(event.getType() == DISASTER) {
            events.add(event);
            if(host.getLocation().distance(event.getLocation()) <= event.getEventRange()) {
                if(rng.nextDouble() <= injuryProbability) {
                    mode = INJURED_MODE;
                    setCurrentMovementModel(stationaryMM);
                } else {
                    //TODO panic
                }
            }
        }

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


    /**
     * Switches the movement model and resets the host to use it after the next update
     *
     * @param mm the new movement model
     */
    private void switchToMovement(SwitchableMovement mm) {
        this.setCurrentMovementModel(mm);
        this.host.interruptMovement();
    }

    private boolean selectNextEvent() {
        boolean chosen = false;

        while(events.size() > 0) {
            if (decideHelp(events.get(0))) {
                chosenEvent = events.get(0);
                chosen = true;
            }
            events.remove(0);
        }

        return chosen;
    }

    private boolean decideHelp(VHMEvent event) {
        boolean help;
        double distance = host.getLocation().distance(event.getLocation());

        help = (rng.nextDouble() <= (intensityWeight * (event.getIntensity() / VHMEvent.MAX_INTENSITY) + (1 - intensityWeight) * ((event.getMaxRange() - distance) / event.getMaxRange())));

        return help;
    }
}
