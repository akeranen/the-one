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
import static test.AbstractMovementModelTest.MAX_COORD_X;
import static test.AbstractMovementModelTest.MAX_COORD_Y;

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
    public static final double INTENSITY_WEIGHT = 0.34;
    /**
     * Used to test getter and setter methods which are using probability values
     */
    public static final double PROBABILITY = 0.24;
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

    private static TestSettings addHelpAndWaitTimesToSettings(TestSettings settings){
        settings.putSetting(VhmProperties.HELP_TIME_SETTING,
                Double.toString(HELP_TIME));
        settings.putSetting(VhmProperties.HOSPITAL_WAIT_TIME_SETTING,
                Double.toString(HOSPITAL_WAIT_TIME));
        return settings;
    }

    private static TestSettings createSettingsWithoutDefaultValues(){
        TestSettings settings = createMinimalSettingsForVoluntaryHelperMovement();
        addHelpAndWaitTimesToSettings(settings);
        settings.putSetting(VoluntaryHelperMovement.HOSPITAL_WAIT_PROBABILITY_SETTING,Double.toString(PROBABILITY));
        settings.putSetting(VoluntaryHelperMovement.INJURY_PROBABILITY_SETTING,Double.toString(PROBABILITY));
        settings.putSetting(VoluntaryHelperMovement.INTENSITY_WEIGHT_SETTING,Double.toString(PROBABILITY));
        settings.putSetting(VoluntaryHelperMovement.IS_LOCAL_HELPER_SETTING,Boolean.toString(true));
        return settings;
    }

    static VhmProperties createMinimalVhm(DTNHost host){
        VhmProperties model = new VhmProperties(createMinimalSettingsForVoluntaryHelperMovement());
        model.setHost(host);
        return model;
    }

    static VhmProperties createVhmWithHelpAndWaitTimes(DTNHost host){
        TestSettings testSettings = createMinimalSettingsForVoluntaryHelperMovement();
        addHelpAndWaitTimesToSettings(testSettings);
        VhmProperties model = new VhmProperties(testSettings);
        model.setHost(host);
        return model;
    }

    static VhmProperties createVhmWithoutDefaultSettings(DTNHost host){
        VhmProperties model = new VhmProperties(createSettingsWithoutDefaultValues());
        model.setHost(host);
        return model;
    }

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

    static void setToRandomMapBasedState(VhmProperties vhm){
        Coord oldLocation = vhm.getHost().getLocation();
        vhm.getHost().setLocation(LOCATION_OUTSIDE_MAX_RANGE);
        vhm.setHost(vhm.getHost());
        vhm.getHost().setLocation(oldLocation);
    }

    static void setToMoveToMode(VhmProperties vhm){
        setToRandomMapBasedState(vhm);
        Coord oldLocation = vhm.getHost().getLocation();
        double oldIntensityWeight = vhm.getIntensityWeight();
        vhm.getHost().setLocation(LOCATION_INSIDE_SAFE_RANGE);
        vhm.setIntensityWeight(1);
        vhm.vhmEventStarted(stateChangeDisaster);
        vhm.setIntensityWeight(oldIntensityWeight);
        vhm.getHost().setLocation(oldLocation);
        vhm.newOrders();
    }

    static void setToLocalHelperMode(VhmProperties vhm){
        setToMoveToMode(vhm);
        vhm.setLocalHelper(true);
        vhm.newOrders();
    }

    static void setToTransportMode(VhmProperties vhm){
        setToMoveToMode(vhm);
        vhm.vhmEventStarted(hospital);
        vhm.setLocalHelper(false);
        vhm.newOrders();
    }

    static void setToHospitalWaitMode(VhmProperties vhm){
        double oldWaitTime = vhm.getWaitProbability();
        vhm.setWaitProbability(1);
        setToTransportMode(vhm);
        vhm.newOrders();
        vhm.setWaitProbability(oldWaitTime);
    }

    static void setToPanicMode(VhmProperties vhm){
        setToRandomMapBasedState(vhm);
        Coord oldLocation = vhm.getHost().getLocation();
        double oldInjuryProb = vhm.getInjuryProbability();
        vhm.getHost().setLocation(LOCATION_INSIDE_EVENT_RANGE);
        vhm.setInjuryProbability(0);
        vhm.vhmEventStarted(stateChangeDisaster);
        vhm.setInjuryProbability(oldInjuryProb);
        vhm.getHost().setLocation(oldLocation);
        vhm.newOrders();
    }

    static void setToInjuredMode(VhmProperties vhm){
        setToRandomMapBasedState(vhm);
        Coord oldLocation = vhm.getHost().getLocation();
        double oldInjuryProb = vhm.getInjuryProbability();
        vhm.getHost().setLocation(LOCATION_INSIDE_EVENT_RANGE);
        vhm.setInjuryProbability(1);
        vhm.vhmEventStarted(stateChangeDisaster);
        vhm.setInjuryProbability(oldInjuryProb);
        vhm.getHost().setLocation(oldLocation);
        vhm.newOrders();
    }

    static void testInjuredState(VhmProperties vhm){
        assertEquals(WRONG_MOVEMENT_MODE,
                VhmProperties.movementMode.INJURED_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL, SwitchableStationaryMovement.class,
                vhm.getCurrentMovementModel().getClass());
    }

    static void testPanicState(VhmProperties vhm){
        assertEquals(WRONG_MOVEMENT_MODE,
                VhmProperties.movementMode.PANIC_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                PanicMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    static void testMoveToState(VhmProperties vhm){
        assertEquals(WRONG_MOVEMENT_MODE,
                VhmProperties.movementMode.MOVING_TO_EVENT_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                CarMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    static void testTransportState(VhmProperties vhm){
        assertEquals(WRONG_MOVEMENT_MODE,
                VhmProperties.movementMode.TRANSPORTING_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                CarMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    static void testRandomMapBasedState(VhmProperties vhm){
        assertEquals(WRONG_MOVEMENT_MODE,
                VhmProperties.movementMode.RANDOM_MAP_BASED_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                ShortestPathMapBasedMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    static void testLevyWalkState(VhmProperties vhm){
        assertEquals(WRONG_MOVEMENT_MODE,
                VhmProperties.movementMode.LOCAL_HELP_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                LevyWalkMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    static void testWaitState(VhmProperties vhm){
        assertEquals(WRONG_MOVEMENT_MODE,
                VhmProperties.movementMode.HOSPITAL_WAIT_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                LevyWalkMovement.class,vhm.getCurrentMovementModel().getClass());
    }
}
