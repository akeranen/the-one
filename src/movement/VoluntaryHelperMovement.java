package movement;

import core.*;
import input.VHMEvent;
import movement.map.SimMap;
import routing.ActiveRouter;
import routing.MessageRouter;

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
//TODO implement Panic Movement
    //TODO chose event randomly and don't delete events from list
    //TODO comments + javadoc
public class VoluntaryHelperMovement extends ExtendedMovementModel implements VHMListener, EnergyListener {

    private static final String IS_LOCAL_HELPER_SETTING = "isLocalHelper";
    private static final String HELP_TIME_SETTING = "helpTime";
    private static final String HOSPITAL_WAIT_TIME_SETTING = "hospitalWaitTime";
    private static final String INJURY_PROBABILITY_SETTING = "injuryProbability";
    private static final String HOSPITAL_WAIT_PROBABILITY_SETTING = "hospitalWaitProbability";
    private static final String INTENSITY_WEIGHT_SETTING = "intensityWeight";

    private static final double DEFAULT_HELP_TIME = 3600;
    private static final double DEFAULT_HOSPITAL_WAIT_TIME = 3600;
    private static final double DEFAULT_INJURY_PROBABILITY = 0.5;
    private static final double DEFAULT_HOSPITAL_WAIT_PROBABILITY = 0.5;
    private static final double DEFAULT_INTENSITY_WEIGHT = 0.5;


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
    private VHMEvent chosenHospital;

    private static List<VHMEvent> events = Collections.synchronizedList(new ArrayList<>());
    private static List<VHMEvent> hospitals = Collections.synchronizedList(new ArrayList<>());

    private ShortestPathMapBasedMovement shortestPathMapBasedMM;
    private CarMovement carMM;
    private LevyWalkMovement levyWalkMM;
    private SwitchableStationaryMovement stationaryMM;
    //private panicMovement panicMM;

    private SimMap simMap;

    private static List<VHMListener> listeners = Collections.synchronizedList(new ArrayList<>());

    /**
     * Creates a new VoluntaryHelperMovement.
     * Only called once per nodegroup to create a prototype.
     * @param settings the settings from the settings file
     */
    public VoluntaryHelperMovement(Settings settings) {
        super(settings);

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
        //panicMM = new panicMovement(settings);

        simMap = this.getMap();

        startTime = SimClock.getTime();

        //There shouldn't be any events here at this point, so no need to check for them
        mode = RANDOM_MAP_BASED_MODE;
        setCurrentMovementModel(shortestPathMapBasedMM);
    }

    /**
     * Creates a new VoluntaryHelperMovement from a prototype.
     * Called once per node.
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
        //panicMM = new panicMovement(prototype.panicMM);

        simMap = prototype.getMap();

        VoluntaryHelperMovement.addListener(this);

        startTime = prototype.startTime;

        //There shouldn't be any events here at this point (hopefully), so no need to check for them
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
     * Informs all registered VHMListeners, that a VHMEvent started and adds it to the appropriate List
     * @param event The VHMEvent.
     */
    public static void eventStarted(VHMEvent event) {
        for (VHMListener l : listeners)
            l.vhmEventStarted(event);
        if(event.getType() == HOSPITAL) {
            hospitals.add(event);
        } else if(event.getType() == DISASTER) {
            events.add(event);
        }
    }

    /**
     * Informs all registered VHMListeners, that a VHMEvent ended and removes it from the appropriate list
     * @param event The VHMEvent.
     */
    public static void eventEnded(VHMEvent event) {
        for (VHMListener l : listeners)
            l.vhmEventEnded(event);
        if(event.getType() == HOSPITAL) {
            //remove the ended event from the list of hospitals. Yes i know how that sounds XD.
            for(VHMEvent h : hospitals) {
                if(h.getID() == event.getID()) {
                    hospitals.remove(h);
                    break;
                }
            }
        } else if(event.getType() == DISASTER) {
            //remove the ended event from the list of disasters.
            for(VHMEvent d : events) {
                if(d.getID() == event.getID()) {
                    events.remove(d);
                    break;
                }
            }
        }
    }

    /**
     * Returns a new initial placement for a node
     * @return The initial coordinates for a node
     */
    @Override
    public Coord getInitialLocation(){
        //TODO find a better place for this?
        initEnergyListener();

        return shortestPathMapBasedMM.getInitialLocation();
    }

    private void initEnergyListener() {
        //TODO only register if energy modeling active for this node's Group
        MessageRouter router = this.host.getRouter();
        if(router instanceof ActiveRouter) {
            ((ActiveRouter) router).addEnergyListener(this);
        }
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
     * ready (isReady() method returns true).
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
                    if(chooseNextHospital()) {
                        mode = TRANSPORTING_MODE;
                        carMM.setLocation(host.getLocation());
                        carMM.setNextRoute(carMM.getLastLocation(), simMap.getClosestNodeByCoord(chosenHospital.getLocation()).getLocation());
                        setCurrentMovementModel(carMM);
                    } else {
                        //if choosing a new hospital fails because there are no hospitals...
                        //...just move on with your day
                        mode = RANDOM_MAP_BASED_MODE;
                        shortestPathMapBasedMM.setLocation(host.getLocation());
                        switchToMovement(shortestPathMapBasedMM);
                    }
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
                    levyWalkMM.setCenter(chosenHospital.getLocation());
                    levyWalkMM.setRadius(chosenHospital.getEventRange());
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
    public void vhmEventStarted(VHMEvent event) {
        if(event.getType() == DISASTER) {
            //check if the node is to close to the disaster
            if(host != null && host.getLocation().distance(event.getLocation()) <= event.getEventRange()) {
                if(rng.nextDouble() <= injuryProbability) {
                    mode = INJURED_MODE;
                    stationaryMM.setLocation(host.getLocation());
                    switchToMovement(stationaryMM);
                } else {
                    mode = PANIC_MODE;
                    //TODO panic
                    /*panicMM.setLocation(host.getLocation());
                    //TODO tell the panicMM all about the disaster
                    switchToMovement(panicMM);*/
                }
            } else if(host != null && mode == RANDOM_MAP_BASED_MODE && selectNextEvent()){
                //if the node is not already busy, decide if it helps with the new disaster
                mode = MOVING_TO_EVENT_MODE;
                carMM.setLocation(host.getLocation());
                carMM.setNextRoute(carMM.getLastLocation(), simMap.getClosestNodeByCoord(chosenEvent.getLocation()).getLocation());
                switchToMovement(carMM);
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
        if(event.getType() == DISASTER) {
            handleEndedDisaster(event);
        } else if(event.getType() == HOSPITAL) {
            handleEndedHospital(event);
        }
    }

    private void handleEndedDisaster(VHMEvent event) {
        //if the ended event was chosen...
        if(chosenEvent != null && event.getID() == chosenEvent.getID()) {
            //..handle the loss of the chosen event by starting over
            if(selectNextEvent()) {
                mode = MOVING_TO_EVENT_MODE;
                carMM.setLocation(host.getLocation());
                carMM.setNextRoute(carMM.getLastLocation(), simMap.getClosestNodeByCoord(chosenEvent.getLocation()).getLocation());
                switchToMovement(carMM);
            } else {
                mode = RANDOM_MAP_BASED_MODE;
                shortestPathMapBasedMM.setLocation(host.getLocation());
                switchToMovement(shortestPathMapBasedMM);
            }
        }
    }

    private void handleEndedHospital(VHMEvent event) {
        //test if the vanished hospital was selected, and select a new one with chooseNextHospital()
        //if choosing a new one fails because there are no hospitals anymore...
        if(chosenHospital != null && chosenHospital.getID() == event.getID() && !chooseNextHospital()) {
            //...just move on with your day
            mode = RANDOM_MAP_BASED_MODE;
            shortestPathMapBasedMM.setLocation(host.getLocation());
            switchToMovement(shortestPathMapBasedMM);
        }
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
        if(!events.isEmpty()) {
            //the bound for the rng mustn't be 0
            if(events.size() == 1) {
                chosenEvent = events.get(0);
            } else {
                chosenEvent = events.get(rng.nextInt(events.size() - 1));
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean decideHelp(VHMEvent event) {
        boolean help;
        double distance = host.getLocation().distance(event.getLocation());

        help = rng.nextDouble() <= (intensityWeight * (event.getIntensity() / VHMEvent.MAX_INTENSITY) + (1 - intensityWeight) * ((event.getMaxRange() - distance) / event.getMaxRange()));

        return help;
    }

    private boolean chooseNextHospital() {
        if(!hospitals.isEmpty()) {
            //the bound for the rng mustn't be 0
            if(hospitals.size() == 1) {
                chosenHospital = hospitals.get(0);
            } else {
                chosenHospital = hospitals.get(rng.nextInt(hospitals.size() - 1));
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * This Method is called when the battery of the node ran empty.
     * It resets the node's battery and movement model.
     */
    @Override
    public void batteryDied() {
        //do not call "super.reset();" or the rng seed will be reset, so the new random location would always be the same
        //reset the energy value. Yes, it has to be done like this.
        //TODO only do this, if host has energy modelling enabled
        //TODO get initial value from settings
        host.getComBus().updateProperty("Energy.value", new Double(100));

        //reset the Location to a new random one
        host.setLocation(shortestPathMapBasedMM.getInitialLocation());

        //reset the host (network address, name, message buffer and connections)
        //do not call "host.reset();" as it interferes with host network address assignment for all hosts
        //TODO get a new network address, a new name and reset the routing table or whatever
        //empty the message buffer
        for(Message m: host.getMessageCollection()) {
            host.deleteMessage(m.getId(), true);
        }
        //update all connections
        host.update(true);

        //select an event and help there or randomly move around the map
        if(selectNextEvent()) {
            mode = MOVING_TO_EVENT_MODE;
            carMM.setLocation(host.getLocation());
            carMM.setNextRoute(carMM.getLastLocation(), simMap.getClosestNodeByCoord(chosenEvent.getLocation()).getLocation());
            switchToMovement(carMM);
        } else {
            mode = RANDOM_MAP_BASED_MODE;
            shortestPathMapBasedMM.setLocation(host.getLocation());
            switchToMovement(shortestPathMapBasedMM);
        }
    }
}
