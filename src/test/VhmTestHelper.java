package test;

import core.Coord;
import core.DTNHost;
import input.VhmEvent;
import junit.framework.TestCase;
import movement.CarMovement;
import movement.LevyWalkMovement;
import movement.MapBasedMovement;
import movement.MovementModel;
import movement.PanicMovement;
import movement.ShortestPathMapBasedMovement;
import movement.SwitchableStationaryMovement;
import movement.VoluntaryHelperMovement;

import static junit.framework.TestCase.assertEquals;

/**
 * Class containing constants and helper functions used to test the {@link VhmProperties}
 *
 * Created by Marius Meyer on 12.04.17.
 */
public final class VhmTestHelper {

    public static final Coord LOCATION_OUTSIDE_MAX_RANGE = new Coord(4000,2700);
    public static final Coord LOCATION_INSIDE_EVENT_RANGE = new Coord(4000,2100);
    public static final Coord LOCATION_INSIDE_SAFE_RANGE = new Coord(4000,2300);
    public static final Coord LOCATION_INSIDE_MAX_RANGE = new Coord(4000,2500);
    /**
     * Used intensity weight for testing the help function
     */
    public static final double INTENSITY_WEIGHT = 0.44;
    /**
     * The injury probability used in the settings, when no default value is used
     */
    public static final double INJURY_PROBABILITY = 0.24;
    /**
     * The hospital wait probability used in the settings, when no default value is used
     */
    public static final double WAIT_PROBABILITY = 0.34;
    /**
     * Time a node will help in a disaster before making new decisions
     */
    public static final double HELP_TIME = 30;
    /**
     * Time a node will wait in a hospital before making new decisions
     */
    public static final double HOSPITAL_WAIT_TIME = 45;
    /**
     * delta used for comparison of float numbers
     */
    public static final double DELTA = 0.001;

    private static final int MAX_COORD_X = 10_000;

    private static final int MAX_COORD_Y = 8000;

    public static final VhmEvent disaster = new VhmEvent("testDisaster",
            VhmEventTest.createJsonForCompletelySpecifiedEvent());
    /**
     * This disaster is only used internally to produce state changes of the VHM
     */
    private static final VhmEvent stateChangeDisaster = new VhmEvent("stateChangeDisaster",
            VhmEventTest.createJsonForCompletelySpecifiedEvent());
    public static final VhmEvent hospital = new VhmEvent("testHospital",
            VhmEventTest.createMinimalVhmEventBuilder(VhmEvent.VhmEventType.HOSPITAL).build());

    private static final String WRONG_MOVEMENT_MODE = "Wrong movement mode is set";
    private static final String WRONG_MOVEMENT_MODEL = "Wrong movement model is selected";

    private VhmTestHelper() {
        //implemented to hide public constructor
    }

    /**
     * Creates settings for the {@link VoluntaryHelperMovement} setting only the necessary parameters.
     * All other parameters will be set to the default values specified in the {@link VoluntaryHelperMovement} class.
     *
     * @return A {@link TestSettings} instance including all necessary parameters
     */
    static TestSettings createMinimalSettingsForVoluntaryHelperMovement(){
        TestSettings testSettings = new TestSettings();
        testSettings.setNameSpace(MovementModel.MOVEMENT_MODEL_NS);
        testSettings.putSetting(MovementModel.WORLD_SIZE,MAX_COORD_X+", "+MAX_COORD_Y);
        testSettings.restoreNameSpace();
        testSettings.setNameSpace(MapBasedMovement.MAP_BASE_MOVEMENT_NS);
        testSettings.putSetting(MapBasedMovement.NROF_FILES_S, "1");
        testSettings.putSetting("mapFile1","data/Manhattan/roads.wkt");
        testSettings.restoreNameSpace();
        return testSettings;
    }

    /**
     * Adds non-default help and hospital wait times to a settings instance.
     *
     * @param settings the settings that should be extended
     * @return the same settings instance extended by new parameters
     */
    private static TestSettings addHelpAndWaitTimesToSettings(TestSettings settings){
        settings.putSetting(VhmProperties.HELP_TIME_SETTING,
                Double.toString(HELP_TIME));
        settings.putSetting(VhmProperties.HOSPITAL_WAIT_TIME_SETTING,
                Double.toString(HOSPITAL_WAIT_TIME));
        return settings;
    }

    /**
     * Creates settings for the {@link VoluntaryHelperMovement} such that all properties are set using the settings file
     * and no default values are used
     *
     * @return a {@link TestSettings} instance inculding entries for every parameter of the movement model
     */
    private static TestSettings createSettingsWithoutDefaultValues(){
        TestSettings settings = createMinimalSettingsForVoluntaryHelperMovement();
        addHelpAndWaitTimesToSettings(settings);
        settings.putSetting(VoluntaryHelperMovement.HOSPITAL_WAIT_PROBABILITY_SETTING,
                Double.toString(WAIT_PROBABILITY));
        settings.putSetting(VoluntaryHelperMovement.INJURY_PROBABILITY_SETTING,Double.toString(INJURY_PROBABILITY));
        settings.putSetting(VoluntaryHelperMovement.INTENSITY_WEIGHT_SETTING,Double.toString(INTENSITY_WEIGHT));
        settings.putSetting(VoluntaryHelperMovement.IS_LOCAL_HELPER_SETTING,Boolean.toString(true));
        return settings;
    }

    /**
     * Creates a instance of the {@link VoluntaryHelperMovement} using the minimal settings to use as much default
     * parameters as possible.
     *
     * @param host the host this movement model is used in
     * @return the {@link VoluntaryHelperMovement} instance
     */
    static VhmProperties createMinimalVhm(DTNHost host){
        VhmProperties model = new VhmProperties(createMinimalSettingsForVoluntaryHelperMovement());
        model.setHost(host);
        return model;
    }

    /**
     * Creates a {@link VoluntaryHelperMovement} instance using the minimal settings.
     * Moreover, settings for non-default help and hospital wait times will be added to the settings, before creating
     * the instance.
     *
     * @param host the host this movement model is used in
     * @return the {@link VoluntaryHelperMovement} instance
     */
    static VhmProperties createVhmWithHelpAndWaitTimes(DTNHost host){
        TestSettings testSettings = createMinimalSettingsForVoluntaryHelperMovement();
        addHelpAndWaitTimesToSettings(testSettings);
        VhmProperties model = new VhmProperties(testSettings);
        model.setHost(host);
        return model;
    }

    /**
     * Creates a {@link VoluntaryHelperMovement} instance using the maximal settings.
     * Every possible parameter in the settings will be set to a non-default value.
     *
     * @param host the host this movement model is used in
     * @return the {@link VoluntaryHelperMovement} instance
     */
    static VhmProperties createVhmWithoutDefaultSettings(DTNHost host){
        VhmProperties model = new VhmProperties(createSettingsWithoutDefaultValues());
        model.setHost(host);
        return model;
    }

    /**
     * Compares the parameters of two given movement models and tests if they are equal.
     *
     * @param m1 the first {@link VoluntaryHelperMovement} for comparison
     * @param m2 the second {@link VoluntaryHelperMovement} for comparison
     */
    static void compareVhmInstances(VhmProperties m1, VhmProperties m2){
        assertEquals("local helper should be set to the same value",m1.isLocalHelper(),m2.isLocalHelper());
        TestCase.assertEquals("Injury probability should be set to the same value",
                m1.getInjuryProbability(),m2.getInjuryProbability(), DELTA);
        TestCase.assertEquals("Wait probability should be set to the same value",
                m1.getWaitProbability(),m2.getWaitProbability(), DELTA);
        TestCase.assertEquals("Intensity weight should be set to the same value",
                m1.getIntensityWeight(),m2.getIntensityWeight(), DELTA);
        TestCase.assertEquals("Help time should be set to the same value",
                m1.getHelpTime(),m2.getHelpTime(), DELTA);
        TestCase.assertEquals("Hospital wait time should be set to the same value",
                m1.getHospitalWaitTime(),m2.getHospitalWaitTime(), DELTA);

    }

    /**
     * Starts an event without influencing the movement model.
     * @param event the starting event
     * @param model the {@link VhmProperties} the event should be added to
     */
    public static void includeEvent(VhmEvent event, VhmProperties model){
        Coord oldHostLocation = model.getHost().getLocation();
        model.getHost().setLocation(LOCATION_OUTSIDE_MAX_RANGE);
        model.vhmEventStarted(event);
        model.getHost().setLocation(oldHostLocation);
    }

    /**
     * Sets the given {@link VoluntaryHelperMovement} to random map based movement, by resetting the
     * movement model.
     *
     * @param vhm the movement model that should be set to random map based movement
     */
    static void setToRandomMapBasedState(VhmProperties vhm){
        Coord oldLocation = vhm.getHost().getLocation();
        //set the host outside of the max event range, so the starting event will be ignored
        vhm.getHost().setLocation(LOCATION_OUTSIDE_MAX_RANGE);
        //reset the host. This calls the startOver method and resets the movement.
        //Because no event is in range, the node will start with random map based movement.
        vhm.setHost(vhm.getHost());
        //Resets the host to its old location
        vhm.getHost().setLocation(oldLocation);
    }

    /**
     * Lets the given {@link VoluntaryHelperMovement} move towards an event.
     *
     * @param vhm the movement model that should be modified
     */
    static void setToMoveToMode(VhmProperties vhm){
        setToRandomMapBasedState(vhm);
        Coord oldLocation = vhm.getHost().getLocation();
        double oldIntensityWeight = vhm.getIntensityWeight();
        //sets the host in the correct distance to choose the stateChangeDisaster
        vhm.getHost().setLocation(LOCATION_INSIDE_SAFE_RANGE);
        //set choose probability to 1.0 by only using the event intensity for calculation
        vhm.setIntensityWeight(1);
        vhm.vhmEventStarted(stateChangeDisaster);
        vhm.setIntensityWeight(oldIntensityWeight);
        vhm.getHost().setLocation(oldLocation);
        vhm.newOrders();
    }

    /**
     * Lets the given {@link VoluntaryHelperMovement} help at an event.
     *
     * @param vhm the movement model that should be modified
     */
    static void setToLocalHelperMode(VhmProperties vhm){
        setToMoveToMode(vhm);
        //sets the movement model to simulate a local helper for the case it is not already set
        vhm.setLocalHelper(true);
        vhm.newOrders();
    }

    /**
     * Lets the given {@link VoluntaryHelperMovement} execute the transport mode between a disaster and a hospital
     *
     * @param vhm the movement model that should be modified
     */
    static void setToTransportMode(VhmProperties vhm){
        setToMoveToMode(vhm);
        //add a hospital to the scenario in case there isn't already one
        vhm.vhmEventStarted(hospital);
        //sets the movement model to not simulate a local helper for the case it is set
        vhm.setLocalHelper(false);
        vhm.newOrders();
    }

    /**
     * Lets the given {@link VoluntaryHelperMovement} wait at a hospital
     *
     * @param vhm the movement model that should be modified
     */
    static void setToHospitalWaitMode(VhmProperties vhm){
        double oldWaitTime = vhm.getWaitProbability();
        //set the wait probability to 1, so the node will wait at the hospital
        vhm.setWaitProbability(1);
        setToTransportMode(vhm);
        //after transport mode wait mode will be executed
        vhm.newOrders();
        vhm.setWaitProbability(oldWaitTime);
    }

    /**
     * Lets the given {@link VoluntaryHelperMovement} execute panic movement
     *
     * @param vhm the movement model that should be modified
     */
    static void setToPanicMode(VhmProperties vhm){
        //reset node
        setToRandomMapBasedState(vhm);
        Coord oldLocation = vhm.getHost().getLocation();
        double oldInjuryProb = vhm.getInjuryProbability();
        //set node into event range
        vhm.getHost().setLocation(LOCATION_INSIDE_EVENT_RANGE);
        //node shouldn't get injured
        vhm.setInjuryProbability(0);
        //start diasaster
        vhm.vhmEventStarted(stateChangeDisaster);
        //reset to old settings
        vhm.setInjuryProbability(oldInjuryProb);
        vhm.getHost().setLocation(oldLocation);
        vhm.newOrders();
    }

    /**
     * Lets the given {@link VoluntaryHelperMovement} switch to injury mode.
     *
     * @param vhm the movement model that should be modified
     */
    static void setToInjuredMode(VhmProperties vhm){
        setToRandomMapBasedState(vhm);
        Coord oldLocation = vhm.getHost().getLocation();
        double oldInjuryProb = vhm.getInjuryProbability();
        vhm.getHost().setLocation(LOCATION_INSIDE_EVENT_RANGE);
        //same as for panic mode, only the injury probability is set to 1.
        vhm.setInjuryProbability(1);
        vhm.vhmEventStarted(stateChangeDisaster);
        vhm.setInjuryProbability(oldInjuryProb);
        vhm.getHost().setLocation(oldLocation);
        vhm.newOrders();
    }

    /**
     * Tests, if the movement model as set the mode and movement model correctly to represent the
     * injury state
     * @param vhm the movement model that is checked
     */
    static void testInjuredState(VhmProperties vhm){
        assertEquals(WRONG_MOVEMENT_MODE,
                VhmProperties.movementMode.INJURED_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL, SwitchableStationaryMovement.class,
                vhm.getCurrentMovementModel().getClass());
    }

    /**
     * Tests, if the movement model as set the mode and movement model correctly to represent the
     * panic state
     * @param vhm the movement model that is checked
     */
    static void testPanicState(VhmProperties vhm){
        assertEquals(WRONG_MOVEMENT_MODE,
                VhmProperties.movementMode.PANIC_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                PanicMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    /**
     * Tests, if the movement model as set the mode and movement model correctly to represent the
     * move to state
     * @param vhm the movement model that is checked
     */
    static void testMoveToState(VhmProperties vhm){
        assertEquals(WRONG_MOVEMENT_MODE,
                VhmProperties.movementMode.MOVING_TO_EVENT_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                CarMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    /**
     * Tests, if the movement model as set the mode and movement model correctly to represent the
     * transport state
     * @param vhm the movement model that is checked
     */
    static void testTransportState(VhmProperties vhm){
        assertEquals(WRONG_MOVEMENT_MODE,
                VhmProperties.movementMode.TRANSPORTING_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                CarMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    /**
     * Tests, if the movement model as set the mode and movement model correctly to represent the
     * random map based state
     * @param vhm the movement model that is checked
     */
    static void testRandomMapBasedState(VhmProperties vhm){
        assertEquals(WRONG_MOVEMENT_MODE,
                VhmProperties.movementMode.RANDOM_MAP_BASED_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                ShortestPathMapBasedMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    /**
     * Tests, if the movement model as set the mode and movement model correctly to represent the
     * local help state
     * @param vhm the movement model that is checked
     */
    static void testLevyWalkState(VhmProperties vhm){
        assertEquals(WRONG_MOVEMENT_MODE,
                VhmProperties.movementMode.LOCAL_HELP_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                LevyWalkMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    /**
     * Tests, if the movement model as set the mode and movement model correctly to represent the
     * wait at hospital state
     * @param vhm the movement model that is checked
     */
    static void testWaitState(VhmProperties vhm){
        assertEquals(WRONG_MOVEMENT_MODE,
                VhmProperties.movementMode.HOSPITAL_WAIT_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                LevyWalkMovement.class,vhm.getCurrentMovementModel().getClass());
    }
}
