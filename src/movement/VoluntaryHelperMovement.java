package movement;

import core.*;
import input.VhmEvent;
import movement.map.SimMap;
import routing.ActiveRouter;
import routing.MessageRouter;
import routing.util.EnergyModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static input.VhmEvent.VhmEventType.DISASTER;
import static input.VhmEvent.VhmEventType.HOSPITAL;

/**
 * This movement model simulates the movement of voluntary helpers in a disaster region.
 * It makes use of several other movement models for this.
 *
 * Created by Ansgar Mährlein on 08.02.2017.
 * @author Ansgar Mährlein
 */
//TODO implement Panic Movement + more comments + javadoc
public class VoluntaryHelperMovement extends ExtendedMovementModel implements VhmListener, EnergyListener {

    //strings for the setting keys in the settings file
    /** setting key for the node being a local helper or a "voluntary ambulance" */
    private static final String IS_LOCAL_HELPER_SETTING = "isLocalHelper";
    /** setting key for the time the node will help at a disaster site (seconds) */
    private static final String HELP_TIME_SETTING = "helpTime";
    /** setting key for the time the node will stay at the hospital after transporting someone to it (seconds) */
    private static final String HOSPITAL_WAIT_TIME_SETTING = "hospitalWaitTime";
    /** setting key for the probability that the node gets injured if an event happens to close to it [0, 1] */
    private static final String INJURY_PROBABILITY_SETTING = "injuryProbability";
    /** setting key for the probability that the node stays at the hospital after transporting someone to it [0, 1] */
    private static final String HOSPITAL_WAIT_PROBABILITY_SETTING = "hospitalWaitProbability";
    /** setting key for the weight of a disasters intensity for determining if the node will help at the disaster site [0, 1] */
    private static final String INTENSITY_WEIGHT_SETTING = "intensityWeight";

    //default values for the settings
    /** default value for the time the node will help at a disaster site (seconds) */
    private static final double DEFAULT_HELP_TIME = 3600;
    /** default value for the time the node will stay at the hospital after transporting someone to it (seconds) */
    private static final double DEFAULT_HOSPITAL_WAIT_TIME = 3600;
    /** default value for the probability that the node gets injured if an event happens to close to it [0, 1] */
    private static final double DEFAULT_INJURY_PROBABILITY = 0.5;
    /** default value for the probability that the node stays at the hospital after transporting someone to it [0, 1] */
    private static final double DEFAULT_HOSPITAL_WAIT_PROBABILITY = 0.5;
    /** default value for the weight of a disasters intensity for determining if the node will help at the disaster site [0, 1] */
    private static final double DEFAULT_INTENSITY_WEIGHT = 0.5;

    /** the movement modes the node can be in */
    private enum movementMode {
        /** movement mode for randomly moving around on the map */
        RANDOM_MAP_BASED_MODE,
        /** movement mode for moving to a disaster site */
        MOVING_TO_EVENT_MODE,
        /** movement mode for helping at the disaster site */
        LOCAL_HELP_MODE,
        /** movement mode for transporting injured people from a disaster site to a hospital */
        TRANSPORTING_MODE,
        /** movement mode for helping at the hospital after transporting someone there */
        HOSPITAL_WAIT_MODE,
        /** movement mode for being injured after an event happened to close to the node */
        INJURED_MODE,
        /** movement mode for panicking after an event happened to close to the node */
        PANIC_MODE
    }

    //global variables of the movement model
    /** tells, if the node is a local helper or a "voluntary ambulance" */
    private boolean isLocalHelper;
    /** how long the node will stay at the hospital (seconds) */
    private double hospitalWaitTime;
    /** how long the node will help at a disaster site (seconds) */
    private double helpTime;
    /** probability that the node gets injured if an event happens to close to it [0, 1] */
    private double injuryProbability;
    /** probability that the node stays at the hospital after transporting someone to it [0, 1] */
    private double waitProbability;
    /** weight of a disasters intensity for determining if the node will help at the disaster site [0, 1] */
    private double intensityWeight;
    /** the current movement mode of the node */
    private movementMode mode;
    /** tells, if energy modelling is enabled for this node */
    private boolean energyModelled;
    /** initial battery level for nodes with energy modelling enabled */
    private double initialEnergy;
    /** start time of waiting at the hospital or local helping movement */
    private double startTime;
    /** tells, if the movement sub-model was changed forcefully before the set destination was reached */
    private boolean justChanged;
    /** the selected disaster */
    private VhmEvent chosenDisaster;
    /** the selected hospital */
    private VhmEvent chosenHospital;
    /** the map for map based movement */
    private SimMap simMap;

    //the sub-movement-models
    /** movement model for randomly walking around on the map */
    private ShortestPathMapBasedMovement shortestPathMapBasedMM;
    /** movement model for navigating from the current position to a specific target on the map */
    private CarMovement carMM;
    /** movement model for randomly walking around in a specified circular area (hospital or disaster site) */
    private LevyWalkMovement levyWalkMM;
    /** movement model for not moving at all */
    private SwitchableStationaryMovement stationaryMM;
    /** movement model for panically runing to a safe location */
    //private panicMovement panicMM;

    //TODO make everything local, non static? Compare performance!
    //event lists
    /** List of disasters */
    private List<VhmEvent> disasters = Collections.synchronizedList(new ArrayList<>());
    /** List of hospitals */
    private List<VhmEvent> hospitals = Collections.synchronizedList(new ArrayList<>());

    //TODO make this non static by moving it to the appropriate class?
    /** List of VhmListeners */
    private static List<VhmListener> listeners = Collections.synchronizedList(new ArrayList<>());

    /**
     * Creates a new VoluntaryHelperMovement.
     * Only called once per nodegroup to create a prototype.
     * @param settings the settings from the settings file
     */
    public VoluntaryHelperMovement(Settings settings) {
        super(settings);

        //get all of the settings from the settings file, reverting to defaults, if setting absent in the file
        isLocalHelper = settings.getBoolean(IS_LOCAL_HELPER_SETTING, false);
        energyModelled = settings.contains(EnergyModel.INIT_ENERGY_S);
        if(energyModelled) {
            //get the initial energy of a node
            initialEnergy = settings.getDouble(EnergyModel.INIT_ENERGY_S);
        }
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
        //panicMM = new panicMovement(settings);
    }

    /**
     * Creates a new VoluntaryHelperMovement from a prototype.
     * Called once per node.
     * @param prototype The prototype MovementModel
     */
    public VoluntaryHelperMovement(VoluntaryHelperMovement prototype) {
        super(prototype);

        //copy the settings from the prototype
        isLocalHelper = prototype.isLocalHelper;
        energyModelled = prototype.energyModelled;
        initialEnergy = prototype.initialEnergy;
        helpTime = prototype.helpTime;
        hospitalWaitTime = prototype.hospitalWaitTime;
        injuryProbability = prototype.injuryProbability;
        waitProbability = prototype.waitProbability;
        intensityWeight = prototype.intensityWeight;

        //create copies of the prototypes movement models
        shortestPathMapBasedMM = new ShortestPathMapBasedMovement(prototype.shortestPathMapBasedMM);
        carMM = new CarMovement(prototype.carMM);
        levyWalkMM = new LevyWalkMovement(prototype.levyWalkMM);
        stationaryMM =  new SwitchableStationaryMovement(prototype.stationaryMM);
        //panicMM = new PanicMovement(prototype.panicMM);

        //register the movement model as a VhmListener
        VoluntaryHelperMovement.addListener(this);

        //get the map for map based movement
        simMap = this.getMap();
        //just make sure this is initialized
        justChanged = false;

        //set the movement mode and model
        mode = movementMode.RANDOM_MAP_BASED_MODE;
        setCurrentMovementModel(shortestPathMapBasedMM);
    }

    /**
     * Adds a VhmListener that will be notified of VhmEvents starting and ending.
     * @param listener The listener that is added.
     */
    public static void addListener(VhmListener listener) {
        listeners.add(listener);
    }

    /**
     * Informs all registered VhmListeners, that a VhmEvent started and adds it to the appropriate List
     * @param event The VhmEvent.
     */
    public static void eventStarted(VhmEvent event) {
        for (VhmListener l : listeners)
            l.vhmEventStarted(event);
    }

    /**
     * Informs all registered VhmListeners, that a VhmEvent ended and removes it from the appropriate list
     * @param event The VhmEvent.
     */
    public static void eventEnded(VhmEvent event) {
        for (VhmListener l : listeners)
            l.vhmEventEnded(event);
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
     * Sets the host of this movement model and registers this movement model as an EnergyListener.
     * @param host the host to set
     */
    @Override
    public void setHost(DTNHost host) {
        super.setHost(host);
        //Register the MM as an EnergyListener.
        initEnergyListener();
    }

    /**
     * Registers the movement model as an EnergyListener, if energy modelling is enabled for the host.
     */
    private void initEnergyListener() {
        //only register the listener if energy modeling active for this node
        if(energyModelled) {
            //register the EnergyListener
            MessageRouter router = this.host.getRouter();
            if (router instanceof ActiveRouter) {
                ((ActiveRouter) router).addEnergyListener(this);
            }
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
     * This is the place where the movement model (mostly) switches between the submodels.
     * @return true if success
     */
    @Override
    public boolean newOrders(){
        if(!justChanged) {
            switch (mode) {
                case RANDOM_MAP_BASED_MODE:
                    chooseMovementAfterRandomMapBasedMode();
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
                    //No change (unless the battery runs out, which is handled asynchronically in batteryDied())
                    break;
                case PANIC_MODE:
                    chooseMovementAfterPanicMode();
                    break;
                default:
                    chooseRandomMapBasedMode();
                    break;
            }
        } else {
            justChanged = false;
        }
        return true;
    }

    private void chooseMovingToEventMode() {
        mode = movementMode.MOVING_TO_EVENT_MODE;
        carMM.setLocation(host.getLocation());
        carMM.setNextRoute(carMM.getLastLocation(), carMM.getMap().getClosestNodeByCoord(chosenDisaster.getLocation()).getLocation());
        setCurrentMovementModel(carMM);
    }

    private void chooseRandomMapBasedMode() {
        mode = movementMode.RANDOM_MAP_BASED_MODE;
        shortestPathMapBasedMM.setLocation(host.getLocation());
        setCurrentMovementModel(shortestPathMapBasedMM);
    }

    private void chooseMovementAfterRandomMapBasedMode() {
        //this could be changed to "startOver();", but it could have a minimal negative performance impact
        if (chooseNextDisaster()) {
            chooseMovingToEventMode();
        }
    }

    private void chooseMovementAfterMovingToEventMode() {
        if (isLocalHelper) {
            mode = movementMode.LOCAL_HELP_MODE;
            levyWalkMM.setLocation(host.getLocation());
            levyWalkMM.setCenter(chosenDisaster.getLocation());
            levyWalkMM.setRadius(chosenDisaster.getEventRange());
            startTime = SimClock.getTime();
            setCurrentMovementModel(levyWalkMM);
        } else {
            if (chooseNextHospital()) {
                mode = movementMode.TRANSPORTING_MODE;
                carMM.setLocation(host.getLocation());
                carMM.setNextRoute(carMM.getLastLocation(), carMM.getMap().getClosestNodeByCoord(chosenHospital.getLocation()).getLocation());
                setCurrentMovementModel(carMM);
            } else {
                //if choosing a new hospital fails because there are no hospitals...
                //...just move on with your day
                chooseRandomMapBasedMode();
            }
        }
    }

    private void chooseMovementAfterTransportingMode() {
        if (rng.nextDouble() >= waitProbability) {
            chooseMovingToEventMode();
        } else {
            mode = movementMode.HOSPITAL_WAIT_MODE;
            levyWalkMM.setLocation(host.getLocation());
            levyWalkMM.setCenter(chosenHospital.getLocation());
            levyWalkMM.setRadius(chosenHospital.getEventRange());
            startTime = SimClock.getTime();
            setCurrentMovementModel(levyWalkMM);
        }
    }

    private void chooseMovementAfterHospitalWaitMode() {
        if (SimClock.getTime() - startTime >= hospitalWaitTime) {
            startOver();
        }
    }

    private void chooseMovementAfterLocalHelpMode() {
        if (SimClock.getTime() - startTime >= helpTime) {
            startOver();
        }
    }

    private void chooseMovementAfterPanicMode() {
        startOver();
    }

    private void startOver() {
        //start over at the beginning
        if (chooseNextDisaster()) {
            chooseMovingToEventMode();
        } else {
            chooseRandomMapBasedMode();
        }
    }

    /**
     * Returns the SimMap this movement model uses
     * @return The SimMap this movement model uses
     */
    //TODO what aboiut carMM?
    public SimMap getMap() {
        return shortestPathMapBasedMM.getMap();
    }

    /**
     * Switches the movement model and resets the host to use it after the next update
     *
     * @param mm the new movement model
     */
    private void switchToMovement(SwitchableMovement mm) {
        justChanged = true;
        setCurrentMovementModel(mm);
        host.interruptMovement();
    }

    /**
     * Pick a random disaster from the list of disasters, and decide for opr against helping at the disaster site.
     * If the decision to help is made, the selected disaster is automatically set as the chosen disaster.
     * @return true if the decision was made to help at a disaster site, false otherwise.
     */
    private boolean chooseNextDisaster() {
        boolean helping = false;

        if(!disasters.isEmpty()) {
            VhmEvent event = disasters.get(rng.nextInt(disasters.size()));
            if(decideHelp(event)) {
                chosenDisaster = event;
                helping = true;
            }
        }

        return helping;
    }

    /**
     * Decide for helping at a specific disaster site.
     * @param event The disaster event.
     * @return true if the decision is to help, false otherwise
     */
    private boolean decideHelp(VhmEvent event) {
        boolean help;
        double distance = host.getLocation().distance(event.getLocation());

        help = rng.nextDouble() <= (intensityWeight * (event.getIntensity() / VhmEvent.MAX_INTENSITY) + (1 - intensityWeight) * ((event.getMaxRange() - distance) / event.getMaxRange()));

        return help;
    }

    /**
     * Randomly select a hospital from the list of hospitals.
     * @return true, if a hospital was selected, false if no hospitals exist.
     */
    private boolean chooseNextHospital() {
        if(!hospitals.isEmpty()) {
            chosenHospital = hospitals.get(rng.nextInt(hospitals.size()));
            return true;
        } else {
            return false;
        }
    }

    /**
     * This Method is called when the battery of the node ran empty.
     * (Only if the node has energy modelling enabled, as only then the Listener is registered.)
     * It resets the node's battery and movement model.
     */
    @Override
    public void batteryDied() {
        //do not call "super.reset();" or the rng seed will be reset, so the new random location would always be the same
        //reset the energy value. Yes, it has to be done like this.
        host.getComBus().updateProperty("Energy.value", initialEnergy);

        //reset the host (network address, name, message buffer and connections)
        //do not call "host.reset();" as it interferes with host network address assignment for all hosts
        //resetting the routing table is not possible without knowing the exact routing protocol
        //TODO implement an interface for resetting the routing table
        //get a new network address and name
        host.setNewAddress();
        //empty the message buffer
        for(Message m: host.getMessageCollection()) {
            host.deleteMessage(m.getId(), true);
        }
        //update all connections
        host.update(true);

        //reset the Location to a new random one
        Coord min = simMap.getMinBound();
        Coord max = simMap.getMaxBound();
        double x = min.getX() + rng.nextDouble() * (max.getX() - min.getX());
        double y = min.getY() + rng.nextDouble() * (max.getY() - min.getY());
        host.setLocation(simMap.getClosestNodeByCoord(new Coord(x, y)).getLocation());

        //select an event and help there or randomly move around the map
        forceStartOver();
    }

    /**
     * This Method is called when a VhmEvent starts.
     * It is used for handling the event, that is, make the nodes movement react to it.
     * @param event The VhmEvent
     */
    @Override
    public void vhmEventStarted(VhmEvent event) {
        //add the event to the appropriate list
        if(event.getType() == HOSPITAL) {
            hospitals.add(event);
        } else if(event.getType() == DISASTER) {
            disasters.add(event);
        }

        //handle the event
        if(event.getType() == DISASTER &&mode != movementMode.INJURED_MODE) {
            //check if the node is to close to the disaster
            if(host != null && host.getLocation().distance(event.getLocation()) <= event.getEventRange()) {
                if(rng.nextDouble() <= injuryProbability) {
                    mode = movementMode.INJURED_MODE;
                    switchToMovement(stationaryMM);
                    stationaryMM.setLocation(host.getLocation());
                } else {
                    mode = movementMode.PANIC_MODE;
                    //TODO tell the panicMM all about the disaster and panic
                    /*switchToMovement(panicMM);
                    panicMM.setLocation(host.getLocation());*/
                }
            } else if(host != null && mode == movementMode.RANDOM_MAP_BASED_MODE && decideHelp(event)){
                //chose the disaster
                chosenDisaster = event;
                //if the node is not already busy, decide if it helps with the new disaster
                switchToMovingToEventMode();
            }
        }

    }

    /**
     * This Method is called when a VhmEvent ends
     * It is used for handling this, i.e. making the movement react to it.
     * @param event The VhmEvent
     */
    @Override
    public void vhmEventEnded(VhmEvent event) {
        //remove the event from the appropriate list
        if(event.getType() == HOSPITAL) {
            hospitals.remove(event);
        } else if(event.getType() == DISASTER) {
            disasters.remove(event);
        }

        //handle the event
        if(event.getType() == DISASTER && mode != movementMode.INJURED_MODE) {
            handleEndedDisaster(event);
        } else if(event.getType() == HOSPITAL && mode != movementMode.INJURED_MODE) {
            handleEndedHospital(event);
        }
    }

    /**
     * Handles the end of a disaster, i.e. makes the movement react to it.
     * @param event the VhmEvent associated with the end of the disaster.
     */
    private void handleEndedDisaster(VhmEvent event) {
        //if the ended event was chosen...
        if(chosenDisaster != null && event.getID() == chosenDisaster.getID()) {
            //..handle the loss of the chosen event by starting over
            forceStartOver();
        }
    }

    /**
     * Handles the end of a hospital, i.e. makes the movement react to it.
     * @param event the VhmEvent associated with the end of the hospital.
     */
    private void handleEndedHospital(VhmEvent event) {
        //test if the vanished hospital was selected, and select a new one with chooseNextHospital()
        //if choosing a new one fails because there are no hospitals anymore...
        boolean affected = chosenHospital != null && chosenHospital.getID() == event.getID();
        if(affected && (mode == movementMode.TRANSPORTING_MODE || mode == movementMode.HOSPITAL_WAIT_MODE) && !chooseNextHospital()) {
            //...just move on with your day
            switchToRandomMapBasedMode();
        }
    }

    private void switchToMovingToEventMode() {
        mode = movementMode.MOVING_TO_EVENT_MODE;
        switchToMovement(carMM);
        carMM.setLocation(host.getLocation());
        carMM.setNextRoute(carMM.getLastLocation(), carMM.getMap().getClosestNodeByCoord(chosenDisaster.getLocation()).getLocation());
    }

    private void switchToRandomMapBasedMode() {
        mode = movementMode.RANDOM_MAP_BASED_MODE;
        switchToMovement(shortestPathMapBasedMM);
        shortestPathMapBasedMM.setLocation(host.getLocation());
    }

    private void forceStartOver() {
        if(chooseNextDisaster()) {
            switchToMovingToEventMode();
        } else {
            switchToRandomMapBasedMode();
        }
    }
}
