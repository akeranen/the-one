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

    /**
     * the current movement mode of the node
     */
    protected VoluntaryHelperMovement.movementMode mode;

    /**
     * the selected disaster
     */
    private VhmEvent chosenDisaster;
    /**
     * the selected hospital
     */
    private VhmEvent chosenHospital;

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
     * Class containing all parameters of the {@link VoluntaryHelperMovement}.
     * They can be modified using the getter and setter methods.
     */
    private VhmProperties properties;

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

    /**
     * Indicates that the internal movement model has just changed
     */
    private boolean justChanged;

    /**
     * Time the last movement model started
     */
    private double startTime;

    /**
     * Creates a new VoluntaryHelperMovement.
     * Only called once per nodegroup to create a prototype.
     *
     * @param settings the settings from the settings file
     */
    public VoluntaryHelperMovement(Settings settings) {
        super(settings);

        this.properties = new VhmProperties(settings);

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
    protected VoluntaryHelperMovement(VoluntaryHelperMovement prototype) {
        super(prototype);

        properties = new VhmProperties(prototype.getProperties());

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
     * This is the place where the movement model (mostly) switches between the sub-models.
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
     * and takes care of all necessary parameter initialization/updates,
     * to make the host of this movement model move towards a disaster.
     */
    private void chooseMovingToEventMode() {
        mode = movementMode.MOVING_TO_EVENT_MODE;
        setCurrentMovementModel(carMM);
        carMM.setNextRoute(carMM.getLastLocation(),
                carMM.getMap().getClosestNodeByCoord(chosenDisaster.getLocation()).getLocation());
    }

    /**
     * Sets the random map based movement model as the current movement model
     * and takes care of all necessary parameter initialization/updates.
     */
    private void chooseRandomMapBasedMode() {
        mode = movementMode.RANDOM_MAP_BASED_MODE;
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
        if (properties.isLocalHelper()) {
            //...simulate the host helping at the disaster site by performing a levy walk
            mode = movementMode.LOCAL_HELP_MODE;
            startTime = SimClock.getTime();
            setCurrentMovementModel(levyWalkMM);
            levyWalkMM.setCenter(chosenDisaster.getLocation());
            levyWalkMM.setRadius(chosenDisaster.getEventRange());
        } else {
            //if the host is not a local helper,
            //simulate a volunteer transporting injured people to the hospital by car
            if (chooseNextHospital()) {
                mode = movementMode.TRANSPORTING_MODE;
                setCurrentMovementModel(carMM);
                carMM.setNextRoute(carMM.getLastLocation(),
                        carMM.getMap().getClosestNodeByCoord(
                                chosenHospital.getLocation()).getLocation());
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
        if (rng.nextDouble() >= properties.getWaitProbability()) {
            //have the host move back to the disaster he came from again
            chooseMovingToEventMode();
        } else {
            //simulate a volunteer helping/waiting at the hospital
            mode = movementMode.HOSPITAL_WAIT_MODE;
            startTime = SimClock.getTime();
            setCurrentMovementModel(levyWalkMM);
            levyWalkMM.setCenter(chosenHospital.getLocation());
            levyWalkMM.setRadius(chosenHospital.getEventRange());
        }
    }

    /**
     * Lets the this mobility model start at the beginning, if the host of this movement model has waited/helped
     * for at least the specified wait time at the location of the selected hospital.
     */
    private void chooseMovementAfterHospitalWaitMode() {
        if (SimClock.getTime() - startTime >= properties.getHospitalWaitTime()) {
            startOver();
        }
    }

    /**
     * Lets the this mobility model start at the beginning, if the host of this movement model has helped
     * for at least the specified help time at the location of the selected disaster.
     */
    private void chooseMovementAfterLocalHelpMode() {
        if (SimClock.getTime() - startTime >= properties.getHelpTime()) {
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
     * Sets the current movement model to be used the next time getPath() is
     * called, and make sure,
     * that the new movement model is initialized with the hosts location as the current location,
     * instead of the destination of the movement model that was used before.
     * @param mm Next movement model
     */
    @Override
    public void setCurrentMovementModel(SwitchableMovement mm) {
        super.setCurrentMovementModel(mm);
        //overwrite the effect of ExtendedMovementModel.setCurrentMovementModel(),
        //where the last location of the movement model, that was used before,
        //is set as the new movement model's location
        getCurrentMovementModel().setLocation(host.getLocation());
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
            help = rng.nextDouble() <=
                    (properties.getIntensityWeight() * (event.getIntensity() / VhmEvent.MAX_INTENSITY)
                    + (1 - properties.getIntensityWeight()) * ((event.getMaxRange() - distance) / event.getMaxRange()));
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
                if (rng.nextDouble() <= properties.getInjuryProbability()) {
                    //simulate that the host was injured and is immobile
                    setMovementAsForcefullySwitched();
                    mode = movementMode.INJURED_MODE;
                    setCurrentMovementModel(stationaryMM);
                } else {
                    //let the host flee from the disaster
                    setMovementAsForcefullySwitched();
                    mode = movementMode.PANIC_MODE;
                    setCurrentMovementModel(panicMM);
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
        if (event.getType() == DISASTER && mode != movementMode.INJURED_MODE && mode != movementMode.PANIC_MODE) {
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
            chosenDisaster = null;
            setMovementAsForcefullySwitched();
            startOver();
        }
    }

    /**
     * Return the properties object of the movement model
     * @return the properties object
     */
    public VhmProperties getProperties() {
        return properties;
    }

    /**
     * Returns the movement mode the model is currently using
     * @return the current movement mode
     */
    public movementMode getMode(){
        return mode;
    }

    /**
     * Returns the disasters, that are started and registered in the movement model.
     * @return list of started diasters
     */
    public List<VhmEvent> getDisasters(){
        return new ArrayList<>(disasters);
    }

    /**
     * Returns the hospitals, that are currently available registered in the movement model.
     * @return list of available hospitals
     */
    public List<VhmEvent> getHospitals(){
        return new ArrayList<>(hospitals);
    }

    /**
     * Returns the disaster that is currently chosen by the movement model.
     * @return the selected disaster or null, if no disaster is chosen
     */
    public VhmEvent getChosenDisaster(){
        return chosenDisaster;
    }

    /**
     * Returns the hospital that is currently chosen by the movement model.
     * @return the selected hospital or null, if no hospital is chosen yet
     */
    public VhmEvent getChosenHospital(){
        return chosenHospital;
    }
}
