package movement;

import core.VhmListener;
import core.Settings;
import core.Coord;
import core.DTNHost;
import core.SimClock;
import input.VhmEvent;
import input.VhmEventNotifier;
import movement.map.SimMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static input.VhmEvent.VhmEventType.DISASTER;
import static input.VhmEvent.VhmEventType.HOSPITAL;

/**
 * This movement model simulates the movement of voluntary helpers in a disaster region.
 * It makes use of several other movement models for this.
 * (In comments and class-names etc., Vhm is the abbreviation of VoluntaryHelperMovement)
 *
 * Created by Ansgar Mährlein on 08.02.2017.
 *
 * @author Ansgar Mährlein
 */
public class VoluntaryHelperMovement extends ExtendedMovementModel implements VhmListener {

    //strings for the setting keys in the settings file
    /**
     * setting key for the node being a local helper or a "voluntary ambulance"
     */
    private static final String IS_LOCAL_HELPER_SETTING = "isLocalHelper";
    /**
     * setting key for the time the node will help at a disaster site (seconds)
     */
    private static final String HELP_TIME_SETTING = "helpTime";
    /**
     * setting key for the time the node will stay at the hospital after transporting someone to it (seconds)
     */
    private static final String HOSPITAL_WAIT_TIME_SETTING = "hospitalWaitTime";
    /**
     * setting key for the probability that the node gets injured if an event happens to close to it [0, 1]
     */
    private static final String INJURY_PROBABILITY_SETTING = "injuryProbability";
    /**
     * setting key for the probability that the node stays at the hospital after transporting someone to it [0, 1]
     */
    private static final String HOSPITAL_WAIT_PROBABILITY_SETTING = "hospitalWaitProbability";
    /**
     * setting key for the weight of a disasters intensity for determining
     * if the node will help at the disaster site [0, 1]
     */
    private static final String INTENSITY_WEIGHT_SETTING = "intensityWeight";

    //default values for the settings
    /**
     * default value for the time the node will help at a disaster site (seconds)
     */
    private static final double DEFAULT_HELP_TIME = 3600;
    /**
     * default value for the time the node will stay at the hospital after transporting someone to it (seconds)
     */
    private static final double DEFAULT_HOSPITAL_WAIT_TIME = 3600;
    /**
     * default value for the probability that the node gets injured if an event happens to close to it [0, 1]
     */
    private static final double DEFAULT_INJURY_PROBABILITY = 0.5;
    /**
     * default value for the probability that the node stays at the hospital after transporting someone to it [0, 1]
     */
    private static final double DEFAULT_HOSPITAL_WAIT_PROBABILITY = 0.5;
    /**
     * default value for the weight of a disasters intensity for determining
     * if the node will help at the disaster site [0, 1]
     */
    private static final double DEFAULT_INTENSITY_WEIGHT = 0.5;

    /**
     * the movement modes the node can be in
     */
    public enum movementMode {
        /**
         * movement mode for randomly moving around on the map
         */
        RANDOM_MAP_BASED_MODE,
        /**
         * movement mode for moving to a disaster site
         */
        MOVING_TO_EVENT_MODE,
        /**
         * movement mode for helping at the disaster site
         */
        LOCAL_HELP_MODE,
        /**
         * movement mode for transporting injured people from a disaster site to a hospital
         */
        TRANSPORTING_MODE,
        /**
         * movement mode for helping at the hospital after transporting someone there
         */
        HOSPITAL_WAIT_MODE,
        /**
         * movement mode for being injured after an event happened to close to the node
         */
        INJURED_MODE,
        /**
         * movement mode for panicking after an event happened to close to the node
         */
        PANIC_MODE
    }

    //global variables of the movement model
    /**
     * tells, if the node is a local helper or a "voluntary ambulance"
     */
    private boolean isLocalHelper;
    /**
     * how long the node will stay at the hospital (seconds)
     */
    private double hospitalWaitTime;
    /**
     * how long the node will help at a disaster site (seconds)
     */
    private double helpTime;
    /**
     * probability that the node gets injured if an event happens to close to it [0, 1]
     */
    private double injuryProbability;
    /**
     * probability that the node stays at the hospital after transporting someone to it [0, 1]
     */
    private double waitProbability;
    /**
     * weight of a disasters intensity for determining if the node will help at the disaster site [0, 1]
     */
    private double intensityWeight;
    /**
     * the current movement mode of the node
     */
    private movementMode mode;
    /**
     * start time of waiting at the hospital or local helping movement
     */
    private double startTime;
    /**
     * tells, if the movement sub-model was changed forcefully before the set destination was reached
     */
    private boolean justChanged;
    /**
     * the selected disaster
     */
    private VhmEvent chosenDisaster;
    /**
     * the selected hospital
     */
    private VhmEvent chosenHospital;

    //the sub-movement-models
    /**
     * movement model for randomly walking around on the map
     */
    private ShortestPathMapBasedMovement shortestPathMapBasedMM;
    /**
     * movement model for navigating from the current position to a specific target on the map
     */
    private CarMovement carMM;
    /**
     * movement model for randomly walking around in a specified circular area (hospital or disaster site)
     */
    private LevyWalkMovement levyWalkMM;
    /**
     * movement model for not moving at all
     */
    private SwitchableStationaryMovement stationaryMM;
    /**
     * movement model for fleeing from a disaster
     */
    private PanicMovement panicMM;

    //event lists
    /**
     * List of disasters
     */
    private List<VhmEvent> disasters = Collections.synchronizedList(new ArrayList<VhmEvent>());
    /**
     * List of hospitals
     */
    private List<VhmEvent> hospitals = Collections.synchronizedList(new ArrayList<VhmEvent>());


    /**
     * Creates a new VoluntaryHelperMovement.
     * Only called once per nodegroup to create a prototype.
     *
     * @param settings the settings from the settings file
     */
    public VoluntaryHelperMovement(Settings settings) {
        super(settings);

        //get all of the settings from the settings file, reverting to defaults, if setting absent in the file
        isLocalHelper = settings.getBoolean(IS_LOCAL_HELPER_SETTING, false);
        helpTime = settings.getDouble(HELP_TIME_SETTING, DEFAULT_HELP_TIME);
        hospitalWaitTime = settings.getDouble(HOSPITAL_WAIT_TIME_SETTING, DEFAULT_HOSPITAL_WAIT_TIME);
        injuryProbability = settings.getDouble(INJURY_PROBABILITY_SETTING, DEFAULT_INJURY_PROBABILITY);
        waitProbability = settings.getDouble(HOSPITAL_WAIT_PROBABILITY_SETTING, DEFAULT_HOSPITAL_WAIT_PROBABILITY);
        intensityWeight = settings.getDouble(INTENSITY_WEIGHT_SETTING, DEFAULT_INTENSITY_WEIGHT);

        //create the sub-movement-models
        shortestPathMapBasedMM = new ShortestPathMapBasedMovement(settings);
        carMM = new CarMovement(settings);
        levyWalkMM = new LevyWalkMovement(settings);
        stationaryMM = new SwitchableStationaryMovement(settings);
        panicMM = new PanicMovement(settings);
    }

    /**
     * Creates a new VoluntaryHelperMovement from a prototype.
     * Called once per node.
     *
     * @param prototype The prototype MovementModel
     */
    public VoluntaryHelperMovement(VoluntaryHelperMovement prototype) {
        super(prototype);

        //copy the settings from the prototype
        isLocalHelper = prototype.isLocalHelper;
        helpTime = prototype.helpTime;
        hospitalWaitTime = prototype.hospitalWaitTime;
        injuryProbability = prototype.injuryProbability;
        waitProbability = prototype.waitProbability;
        intensityWeight = prototype.intensityWeight;

        //create copies of the prototypes movement models
        shortestPathMapBasedMM = new ShortestPathMapBasedMovement(prototype.shortestPathMapBasedMM);
        carMM = new CarMovement(prototype.carMM);
        levyWalkMM = new LevyWalkMovement(prototype.levyWalkMM);
        stationaryMM = new SwitchableStationaryMovement(prototype.stationaryMM);
        panicMM = new PanicMovement(prototype.panicMM);

        //register the movement model as a VhmListener
        VhmEventNotifier.addListener(this);

        //just make sure this is initialized
        justChanged = false;
    }

    /**
     * Returns a new initial placement for a node
     *
     * @return The initial coordinates for a node
     */
    @Override
    public Coord getInitialLocation() {
        return shortestPathMapBasedMM.getInitialLocation();
    }

    /**
     * Sets the host and movement mode of this movement model.
     *
     * @param host the host to set
     */
    @Override
    public void setHost(DTNHost host) {
        super.setHost(host);

        //set the movement mode and model
        this.startOver();
    }

    /**
     * Creates a replicate of the movement model.
     *
     * @return A new movement model with the same settings as this model
     */
    @Override
    public MovementModel replicate() {
        return new VoluntaryHelperMovement(this);
    }

    /**
     * Method is called between each getPath() request when the current MM is
     * ready (isReady() method returns true).
     * This is the place where the movement model (mostly) switches between the submodels.
     *
     * @return true if success
     */
    @Override
    public boolean newOrders() {
        //when the just changed flag is set, the path and destination of the host were just set to null,
        //so getPath and thus newOrders is called,
        //but here a new movement model would possibly be selected, so to prevent that, if justChanged is true,...
        if (!justChanged) {
            //...switching to a different movement mode and model is not done and...
            switch (mode) {
                case RANDOM_MAP_BASED_MODE:
                case PANIC_MODE:
                    startOver();
                    break;
                case MOVING_TO_EVENT_MODE:
                    chooseMovementAfterMovingToEventMode();
                    break;
                case LOCAL_HELP_MODE:
                    chooseMovementAfterLocalHelpMode();
                    break;
                case TRANSPORTING_MODE:
                    chooseMovementAfterTransportingMode();
                    break;
                case HOSPITAL_WAIT_MODE:
                    chooseMovementAfterHospitalWaitMode();
                    break;
                case INJURED_MODE:
                    //No change
                    break;

                default:
                    chooseRandomMapBasedMode();
                    break;
            }
        } else {
            //...in the end, nothing is done, except for setting justChanged to false
            justChanged = false;
        }
        return true;
    }

    /**
     * Sets the map based movement model carMM of type CarMovement as the current movement model
     * and takes care of all neccessary paramter initialization/updates,
     * to make the host of this movement model move towards a disaster.
     */
    private void chooseMovingToEventMode() {
        mode = movementMode.MOVING_TO_EVENT_MODE;
        carMM.setLocation(host.getLocation());
        carMM.setNextRoute(carMM.getLastLocation(),
                carMM.getMap().getClosestNodeByCoord(chosenDisaster.getLocation()).getLocation());
        setCurrentMovementModel(carMM);
    }

    /**
     * Sets the random map based movement model as the current movement model
     * and takes care of all neccessary paramter initialization/updates.
     */
    private void chooseRandomMapBasedMode() {
        mode = movementMode.RANDOM_MAP_BASED_MODE;
        shortestPathMapBasedMM.setLocation(host.getLocation());
        setCurrentMovementModel(shortestPathMapBasedMM);
    }

    /**
     * Lets this mobility model start at the beginning.
     * This means checking all disasters, and deciding to help at one of them and moving there or,
     * if for no disaster helping was chosen, starting random map based movement.
     */
    private void startOver() {
        //start over at the beginning
        if (chooseNextDisaster()) {
            chooseMovingToEventMode();
        } else {
            chooseRandomMapBasedMode();
        }
    }

    /**
     * Chooses the current movement model after the host of this movement model has arrived
     * at the location of the selected disaster.
     */
    private void chooseMovementAfterMovingToEventMode() {
        //if the host is a local helper...
        if (isLocalHelper) {
            //...simulate the host helping at the disaster site by performing a levy walk
            mode = movementMode.LOCAL_HELP_MODE;
            levyWalkMM.setLocation(host.getLocation());
            levyWalkMM.setCenter(chosenDisaster.getLocation());
            levyWalkMM.setRadius(chosenDisaster.getEventRange());
            startTime = SimClock.getTime();
            setCurrentMovementModel(levyWalkMM);
        } else {
            //if the host is not a local helper,
            //simulate a volunteer transporting injured people to the hospital by car
            if (chooseNextHospital()) {
                mode = movementMode.TRANSPORTING_MODE;
                carMM.setLocation(host.getLocation());
                carMM.setNextRoute(carMM.getLastLocation(),
                        carMM.getMap().getClosestNodeByCoord(chosenHospital.getLocation()).getLocation());
                setCurrentMovementModel(carMM);
            } else {
                //if choosing a new hospital fails because there are no hospitals...
                //...just move on with your day
                chooseRandomMapBasedMode();
            }
        }
    }

    /**
     * Chooses the current movement model after the host of this movement model has arrived
     * at the location of the selected hospital after transporting injured people to it.
     */
    private void chooseMovementAfterTransportingMode() {
        if (rng.nextDouble() >= waitProbability) {
            //have the host move back to the disaster he came from again
            chooseMovingToEventMode();
        } else {
            //simulate a volunteer helping/waiting at the hospital
            mode = movementMode.HOSPITAL_WAIT_MODE;
            levyWalkMM.setLocation(host.getLocation());
            levyWalkMM.setCenter(chosenHospital.getLocation());
            levyWalkMM.setRadius(chosenHospital.getEventRange());
            startTime = SimClock.getTime();
            setCurrentMovementModel(levyWalkMM);
        }
    }

    /**
     * Lets the this mobility model start at the beginning, if the host of this movement model has waited/helped
     * for at least the specified wait time at the location of the selected hospital.
     */
    private void chooseMovementAfterHospitalWaitMode() {
        if (SimClock.getTime() - startTime >= hospitalWaitTime) {
            startOver();
        }
    }

    /**
     * Lets the this mobility model start at the beginning, if the host of this movement model has helped
     * for at least the specified help time at the location of the selected disaster.
     */
    private void chooseMovementAfterLocalHelpMode() {
        if (SimClock.getTime() - startTime >= helpTime) {
            startOver();
        }
    }

    /**
     * Returns the SimMap this movement model uses
     *
     * @return The SimMap this movement model uses
     */
    public SimMap getMap() {
        return shortestPathMapBasedMM.getMap();
    }

    /**
     * sets a flag to indicate, that the movement model was forcefully changed
     * and resets the hosts path and destination, so that it has to get a new one,
     * effectively leading to an immediate change to whatever movement model is set directly after calling this method.
     */
    private void setMovementAsForcefullySwitched() {
        justChanged = true;
        host.interruptMovement();
    }

    /**
     * Go through the list of disasters in random order, and decide for or against helping at the disaster site.
     * If the decision to help is made, the disaster is automatically set as the chosen disaster.
     *
     * @return true if the decision was made to help at a disaster site, false otherwise.
     */
    private boolean chooseNextDisaster() {
        boolean helping = false;

        //shuffle the list of disasters
        Collections.shuffle(disasters);

        //check for each one, if the host should help there
        for(VhmEvent d : disasters) {
            if (decideHelp(d)) {
                chosenDisaster = d;
                helping = true;
                break;
            }
        }

        return helping;
    }

    /**
     * Decide for helping at a specific disaster site.
     *
     * @param event The disaster event.
     * @return true if the decision is to help, false otherwise
     */
    private boolean decideHelp(VhmEvent event) {
        boolean help = false;
        double distance = host.getLocation().distance(event.getLocation());
        //decide if the host helps at the disaster site,
        // based on the distance to the disaster, as well as the intensity and maximum range of the disaster
        // and the intensity weight factor.
        if(distance <= event.getMaxRange()) {
            help = rng.nextDouble() <= (intensityWeight * (event.getIntensity() / VhmEvent.MAX_INTENSITY)
                    + (1 - intensityWeight) * ((event.getMaxRange() - distance) / event.getMaxRange()));
        }

        return help;
    }

    /**
     * Randomly select a hospital from the list of hospitals.
     *
     * @return true, if a hospital was selected, false if no hospitals exist.
     */
    private boolean chooseNextHospital() {
        if (!hospitals.isEmpty()) {
            chosenHospital = hospitals.get(rng.nextInt(hospitals.size()));
            return true;
        } else {
            return false;
        }
    }

    /**
     * This Method is called when a VhmEvent starts.
     * It is used for handling the event, that is, make the nodes movement react to it.
     *
     * @param event The VhmEvent
     */
    @Override
    public void vhmEventStarted(VhmEvent event) {
        //add the event to the appropriate list
        if (event.getType() == HOSPITAL) {
            hospitals.add(event);
        } else if (event.getType() == DISASTER) {
            disasters.add(event);
        }

        //handle the event
        handleStartedVhmEvent(event);
    }

    /**
     * Reacts to started disaster events by deciding on a new movement model to be used.
     *
     * @param event The VhmEvent that started.
     */
    private void handleStartedVhmEvent(VhmEvent event) {
        //handle the event
        if (event.getType() == DISASTER && mode != movementMode.INJURED_MODE) {
            //check if the node is to close to the disaster
            if (host != null && host.getLocation().distance(event.getLocation()) <= event.getEventRange()) {
                if (rng.nextDouble() <= injuryProbability) {
                    //simulate that the host was injured and is immobile
                    setMovementAsForcefullySwitched();
                    mode = movementMode.INJURED_MODE;
                    setCurrentMovementModel(stationaryMM);
                    stationaryMM.setLocation(host.getLocation());
                } else {
                    //let the host flee from the disaster
                    setMovementAsForcefullySwitched();
                    mode = movementMode.PANIC_MODE;
                    setCurrentMovementModel(panicMM);
                    panicMM.setLocation(host.getLocation());
                    panicMM.setEventLocation(event.getLocation());
                    panicMM.setSafeRange(event.getSafeRange());
                }
            } else if (host != null && mode == movementMode.RANDOM_MAP_BASED_MODE && decideHelp(event)) {
                //chose the disaster and immediately switch to moving there
                chosenDisaster = event;
                setMovementAsForcefullySwitched();
                chooseMovingToEventMode();
            }
        }
    }

    /**
     * This Method is called when a VhmEvent ends
     * It is used for handling this, i.e. making the movement react to it.
     *
     * @param event The VhmEvent
     */
    @Override
    public void vhmEventEnded(VhmEvent event) {
        //remove the event from the appropriate list
        if (event.getType() == HOSPITAL) {
            hospitals.remove(event);
        } else if (event.getType() == DISASTER) {
            disasters.remove(event);
        }

        //handle the event
        if (event.getType() == DISASTER && mode != movementMode.INJURED_MODE) {
            handleEndedDisaster(event);
        }
    }

    /**
     * Handles the end of a disaster, i.e. makes the movement react to it.
     *
     * @param event the VhmEvent associated with the end of the disaster.
     */
    private void handleEndedDisaster(VhmEvent event) {
        //if the ended event was chosen...
        if (chosenDisaster != null && event.getID() == chosenDisaster.getID()) {
            //..handle the loss of the chosen event by immediately starting over
            setMovementAsForcefullySwitched();
            startOver();
        }
    }

    /**
     * Returns the current movement mode
     * @return the current movement mode
     */
    public movementMode getMode() {
        return mode;
    }

    /**
     * Sets the current movement mode
     *
     * @param mode the new movement mode
     */
    public void setMode(movementMode mode) {
        this.mode = mode;
    }

    public ShortestPathMapBasedMovement getShortestPathMapBasedMM() {
        return shortestPathMapBasedMM;
    }

    public CarMovement getCarMM() {
        return carMM;
    }

    public LevyWalkMovement getLevyWalkMM() {
        return levyWalkMM;
    }

    public SwitchableStationaryMovement getStationaryMM() {
        return stationaryMM;
    }

    public PanicMovement getPanicMM() {
        return panicMM;
    }

    public boolean isLocalHelper() {
        return isLocalHelper;
    }

    public void setLocalHelper(boolean localHelper) {
        isLocalHelper = localHelper;
    }

    public double getHospitalWaitTime() {
        return hospitalWaitTime;
    }

    public void setHospitalWaitTime(double hospitalWaitTime) {
        this.hospitalWaitTime = hospitalWaitTime;
    }

    public double getHelpTime() {
        return helpTime;
    }

    public void setHelpTime(double helpTime) {
        this.helpTime = helpTime;
    }

    public double getInjuryProbability() {
        return injuryProbability;
    }

    public void setInjuryProbability(double injuryProbability) {
        this.injuryProbability = injuryProbability;
    }

    public double getWaitProbability() {
        return waitProbability;
    }

    public void setWaitProbability(double waitProbability) {
        this.waitProbability = waitProbability;
    }

    public double getIntensityWeight() {
        return intensityWeight;
    }

    public void setIntensityWeight(double intensityWeight) {
        this.intensityWeight = intensityWeight;
    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public boolean isJustChanged() {
        return justChanged;
    }

    public void setJustChanged(boolean justChanged) {
        this.justChanged = justChanged;
    }

    public VhmEvent getChosenDisaster() {
        return chosenDisaster;
    }

    public void setChosenDisaster(VhmEvent chosenDisaster) {
        this.chosenDisaster = chosenDisaster;
    }

    public VhmEvent getChosenHospital() {
        return chosenHospital;
    }

    public void setChosenHospital(VhmEvent chosenHospital) {
        this.chosenHospital = chosenHospital;
    }

    public List<VhmEvent> getDisasters() {
        return Collections.synchronizedList(new ArrayList<>(disasters));
    }

    public void setDisasters(List<VhmEvent> disasters) {
        this.disasters = Collections.synchronizedList(new ArrayList<>(disasters));
    }

    public List<VhmEvent> getHospitals() {
        return Collections.synchronizedList(new ArrayList<>(hospitals));
    }

    public void setHospitals(List<VhmEvent> hospitals) {
        this.hospitals = Collections.synchronizedList(new ArrayList<>(hospitals));
    }
}
