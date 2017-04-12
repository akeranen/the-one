package test;

import core.Coord;
import core.SimClock;
import movement.CarMovement;
import movement.ShortestPathMapBasedMovement;
import movement.VoluntaryHelperMovement;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;

/**
 * Tests for behavior of VoluntaryHelperMovement
 * Created by Marius Meyer on 10.04.17.
 */
public class VhmBehaviorTest extends AbstractVhmTest{

    private static final String INVALID_MODE_SWITCH = "Mode shouldn't have switched";

    //big delta for higher tolerance for probabilistic functions
    private static final double PROB_DELTA = 0.01;
    private static final double TEST_HELP_TIME = 30;
    private static final double TEST_HOSPITAL_WAIT_TIME = 45;
    private static final double TEST_INTENSITY_WEIGHT = 0.34;

    private static final Coord LOCATION_INSIDE_EVENT_RANGE = new Coord(4000,2100);
    private static final Coord LOCATION_INSIDE_SAFE_RANGE = new Coord(4000,2300);
    private static final Coord LOCATION_INSIDE_MAX_RANGE = new Coord(4000,2500);
    private static final Coord LOCATION_OUTSIDE_MAX_RANGE = new Coord(4000,2700);

    public VhmBehaviorTest(){
        //nothing to doe here, Set up is done in super class.
    }

    @Test
    public void testSetHostSetsHostAndChoosesRandomMapBased(){
        assertEquals("Host was not set as expected",host,vhm.getHost());
        assertEquals("Wrong movement mode was chosen",
                VoluntaryHelperMovement.movementMode.RANDOM_MAP_BASED_MODE, vhm.getMode());
        assertEquals("Wrong movement model is used",
                ShortestPathMapBasedMovement.class, vhm.getCurrentMovementModel().getClass());
    }

    @Test
    public void testSetHostSetsHostAndMoveToEventIsUsedWhenDisasterWasChosen(){
        includeDisaster();
        //node will help at disaster with intensity 10.
        vhm.setIntensityWeight(1);
        host.setLocation(LOCATION_INSIDE_SAFE_RANGE);
        vhm.setHost(host);
        assertEquals("Model should choose to move to event",
                VoluntaryHelperMovement.movementMode.MOVING_TO_EVENT_MODE, vhm.getMode());
        assertEquals("CarMovement should be used as movement model",
                CarMovement.class, vhm.getCurrentMovementModel().getClass());
        assertEquals("The destination should be the nearest map node to the event location",
                vhm.getMap().getClosestNodeByCoord(disaster.getLocation()).getLocation(),
                vhm.getCarMM().getPath().getCoords().get(vhm.getCarMM().getPath().getCoords().size() - 1));
    }

    @Test
    public void testHospitalEventStartedAndIsAddedToHospitalsAndNotToDisasters(){
        vhm.vhmEventStarted(hospital);
        assertTrue("Hospital was not added to list of hospitals",vhm.getHospitals().contains(hospital));
        assertFalse("Hospital was falsely added to list of disasters",vhm.getDisasters().contains(hospital));
    }

    @Test
    public void testHospitalEventEndedRemovesHospitalFromList(){
        vhm.vhmEventStarted(hospital);
        vhm.vhmEventEnded(hospital);
        assertFalse("Hospital wasn't removed from list",vhm.getHospitals().contains(hospital));
    }

    @Test
    public void testDisasterEventStartedAndIsAddedToDisastersAndNotToHospitals(){
        vhm.vhmEventStarted(disaster);
        assertTrue("Disaster was not added to list of disasters",vhm.getDisasters().contains(disaster));
        assertFalse("Disaster was falsely added to list of hospitals",vhm.getHospitals().contains(disaster));
    }

    /**
     * Checks, if a node within max range helps when event starts.
     */
    @Test
    public void testDisasterEventStartedHelp(){
        vhm.setIntensityWeight(1);
        host.setLocation(LOCATION_INSIDE_MAX_RANGE);
        vhm.vhmEventStarted(disaster);
        testMoveToState();
    }

    /**
     * Checks, if a node outside max range doesn't help when event starts, even if probability 1.
     */
    @Test
    public void testDisasterEventStartedNodesOutsideRangeDontHelp(){
        vhm.setIntensityWeight(1);
        host.setLocation(LOCATION_OUTSIDE_MAX_RANGE);
        vhm.vhmEventStarted(disaster);
        testRandomMapBasedState();
    }

    @Test
    public void testDisasterEventEndedRemoveDisasterFromList(){
        vhm.vhmEventStarted(disaster);
        vhm.vhmEventEnded(disaster);
        assertFalse("Disaster wasn't removed from list",vhm.getDisasters().contains(hospital));
    }

    @Test
    public void testNodeWorkingOnDisasterStartOverAfterDisasterEnds(){
        vhm.setIntensityWeight(1);
        host.setLocation(LOCATION_INSIDE_MAX_RANGE);
        vhm.vhmEventStarted(disaster);
        vhm.vhmEventEnded(disaster);
        testRandomMapBasedState();
    }

    @Test
    public void testPanicingNodesIgnoreEndOfDisaster(){
        vhm.setInjuryProbability(0);
        host.setLocation(LOCATION_INSIDE_EVENT_RANGE);
        vhm.vhmEventStarted(disaster);
        vhm.vhmEventEnded(disaster);
        testPanicState();
    }

    @Test
    public void testInjuredNodesIgnoreEndOfDisaster(){
        vhm.setInjuryProbability(1);
        host.setLocation(LOCATION_INSIDE_EVENT_RANGE);
        vhm.vhmEventStarted(disaster);
        vhm.vhmEventEnded(disaster);
        testInjuredState();
    }

    @Test
    public void testInjuryProbabilityIsUsedCorrectly(){
        int injuredCount = 0;
        vhm.setInjuryProbability(TEST_PROBABILITY);
        host.setLocation(LOCATION_INSIDE_EVENT_RANGE);
        for (int i = 0; i < TEST_RUNS; i++){
            vhm.vhmEventStarted(disaster);
            vhm.vhmEventEnded(disaster);
            if (vhm.getMode() == VoluntaryHelperMovement.movementMode.INJURED_MODE){
                injuredCount++;
            }
            vhm.setMode(VoluntaryHelperMovement.movementMode.RANDOM_MAP_BASED_MODE);
        }
        assertEquals("Measured injury probability differs from value specified",
                TEST_PROBABILITY,(double)injuredCount / TEST_RUNS,PROB_DELTA);
    }

    @Test
    public void testHospitalWaitProbability(){
        int waitingCount = 0;
        vhm.setWaitProbability(TEST_PROBABILITY);
        host.setLocation(hospital.getLocation());
        vhm.setMode(VoluntaryHelperMovement.movementMode.TRANSPORTING_MODE);
        vhm.setChosenDisaster(disaster);
        vhm.setChosenHospital(hospital);
        for (int i = 0; i < TEST_RUNS; i++){
            vhm.newOrders();
            if (vhm.getMode() == VoluntaryHelperMovement.movementMode.HOSPITAL_WAIT_MODE){
                waitingCount++;
            }
            vhm.setMode(VoluntaryHelperMovement.movementMode.TRANSPORTING_MODE);
        }
        assertEquals("Measured wait probability differs from value specified",
                TEST_PROBABILITY,(double) waitingCount / TEST_RUNS,PROB_DELTA);
    }

    @Test
    public void testAfterArrivalSwitchToLevyWalkIfLocalHelper(){
        vhm.setLocalHelper(true);
        vhm.setMode(VoluntaryHelperMovement.movementMode.MOVING_TO_EVENT_MODE);
        includeDisaster();
        vhm.setChosenDisaster(disaster);
        vhm.newOrders();
        testLevyWalkState();
    }

    @Test
    public void testAfterArrivalSwitchToTransportIfNotLocalHelper(){
        vhm.setLocalHelper(false);
        vhm.setMode(VoluntaryHelperMovement.movementMode.MOVING_TO_EVENT_MODE);
        includeDisaster();
        includeHospital();
        vhm.setChosenDisaster(disaster);
        vhm.newOrders();
        testTransportState();
    }

    @Test
    public void testAfterTransportingDoLevyWalkIfDecideToWait(){
        vhm.setMode(VoluntaryHelperMovement.movementMode.TRANSPORTING_MODE);
        vhm.setChosenDisaster(disaster);
        vhm.setChosenHospital(hospital);
        vhm.setWaitProbability(1);
        vhm.newOrders();
        testWaitState();
    }

    @Test
    public void testAfterTransportingMoveToNextEventIfNotDecideToWait(){
        vhm.setMode(VoluntaryHelperMovement.movementMode.TRANSPORTING_MODE);
        vhm.setChosenDisaster(disaster);
        vhm.setChosenHospital(hospital);
        includeDisaster();
        includeHospital();
        vhm.setWaitProbability(0);
        vhm.newOrders();
        testMoveToState();
    }

    @Test
    public void testNodesAreHelpingDuringTheSpecifiedHelpTimeAndSwitchToRandomIfNoEventChosen(){
        SimClock.getInstance().setTime(0);
        vhm.setHelpTime(TEST_HELP_TIME);
        vhm.setMode(VoluntaryHelperMovement.movementMode.LOCAL_HELP_MODE);
        vhm.setCurrentMovementModel(vhm.getLevyWalkMM());
        SimClock.getInstance().setTime(TEST_HELP_TIME - DELTA);
        vhm.newOrders();
        testLevyWalkState();
        SimClock.getInstance().setTime(TEST_HELP_TIME + DELTA);
        vhm.newOrders();
        testRandomMapBasedState();
    }

    @Test
    public void testNodesAreWaitingDuringTheSpecifiedWaitTimeAndSwitchToRandomIfNoEventChosen(){
        SimClock.getInstance().setTime(0);
        vhm.setHospitalWaitTime(TEST_HOSPITAL_WAIT_TIME);
        vhm.setMode(VoluntaryHelperMovement.movementMode.HOSPITAL_WAIT_MODE);
        vhm.setCurrentMovementModel(vhm.getLevyWalkMM());
        SimClock.getInstance().setTime(TEST_HOSPITAL_WAIT_TIME - DELTA);
        vhm.newOrders();
        testWaitState();
        SimClock.getInstance().setTime(TEST_HOSPITAL_WAIT_TIME + DELTA);
        vhm.newOrders();
        testRandomMapBasedState();
    }

    @Test
    public void testAfterLevyWalkDoNextEventIfEventIsAvailable(){
        SimClock.getInstance().setTime(0);
        vhm.setIntensityWeight(1);
        includeDisaster();
        host.setLocation(LOCATION_INSIDE_SAFE_RANGE);
        vhm.setHelpTime(TEST_HELP_TIME);
        vhm.setMode(VoluntaryHelperMovement.movementMode.LOCAL_HELP_MODE);
        SimClock.getInstance().setTime(TEST_HELP_TIME + DELTA);
        vhm.newOrders();
        testMoveToState();
    }

    @Test
    public void testAfterWaitHospitalDoNextEventIfEventIsAvailable(){
        SimClock.getInstance().setTime(0);
        vhm.setIntensityWeight(1);
        includeDisaster();
        host.setLocation(LOCATION_INSIDE_SAFE_RANGE);
        vhm.setHospitalWaitTime(TEST_HOSPITAL_WAIT_TIME);
        vhm.setMode(VoluntaryHelperMovement.movementMode.HOSPITAL_WAIT_MODE);
        SimClock.getInstance().setTime(TEST_HOSPITAL_WAIT_TIME + DELTA);
        vhm.newOrders();
        testMoveToState();
    }

    @Test
    public void testAfterPanicSwitchToRandomIfNoEventChosen(){
        vhm.setMode(VoluntaryHelperMovement.movementMode.PANIC_MODE);
        vhm.newOrders();
        testRandomMapBasedState();
    }

    @Test
    public void testAfterPanicDoNextEventIfEventIsAvailable(){
        vhm.setMode(VoluntaryHelperMovement.movementMode.PANIC_MODE);
        host.setLocation(LOCATION_INSIDE_SAFE_RANGE);
        includeDisaster();
        vhm.setIntensityWeight(1);
        vhm.newOrders();
        testMoveToState();
    }

    @Test
    public void testAllStatesExceptRandomMapBasedIgnoreStartingEventsInSafeRange(){
        host.setLocation(LOCATION_INSIDE_SAFE_RANGE);
        vhm.setIntensityWeight(1);
        checkIfDisasterIsIgnoredForMode(VoluntaryHelperMovement.movementMode.HOSPITAL_WAIT_MODE);
        checkIfDisasterIsIgnoredForMode(VoluntaryHelperMovement.movementMode.LOCAL_HELP_MODE);
        checkIfDisasterIsIgnoredForMode(VoluntaryHelperMovement.movementMode.MOVING_TO_EVENT_MODE);
        checkIfDisasterIsIgnoredForMode(VoluntaryHelperMovement.movementMode.PANIC_MODE);
        checkIfDisasterIsIgnoredForMode(VoluntaryHelperMovement.movementMode.TRANSPORTING_MODE);
        checkIfDisasterIsIgnoredForMode(VoluntaryHelperMovement.movementMode.INJURED_MODE);
    }

    private void checkIfDisasterIsIgnoredForMode(VoluntaryHelperMovement.movementMode mode){
        vhm.setMode(mode);
        vhm.vhmEventStarted(disaster);
        assertEquals(INVALID_MODE_SWITCH,mode,vhm.getMode());
        vhm.vhmEventEnded(disaster);
        assertEquals(INVALID_MODE_SWITCH,mode,vhm.getMode());
    }

    @Test
    public void testAllStatesSwitchToPanicIfInEventRange(){
        host.setLocation(LOCATION_INSIDE_EVENT_RANGE);
        vhm.setInjuryProbability(0);
        checkIfModeSwitchesToPanic(VoluntaryHelperMovement.movementMode.HOSPITAL_WAIT_MODE);
        checkIfModeSwitchesToPanic(VoluntaryHelperMovement.movementMode.LOCAL_HELP_MODE);
        checkIfModeSwitchesToPanic(VoluntaryHelperMovement.movementMode.MOVING_TO_EVENT_MODE);
        checkIfModeSwitchesToPanic(VoluntaryHelperMovement.movementMode.TRANSPORTING_MODE);
        checkIfModeSwitchesToPanic(VoluntaryHelperMovement.movementMode.RANDOM_MAP_BASED_MODE);
    }

    private void checkIfModeSwitchesToPanic(VoluntaryHelperMovement.movementMode mode){
        vhm.setMode(mode);
        vhm.vhmEventStarted(disaster);
        testPanicState();
    }

    @Test
    public void testAllStatesSwitchToInjuredIfInEventRange(){
        host.setLocation(LOCATION_INSIDE_EVENT_RANGE);
        vhm.setInjuryProbability(1);
        checkIfModeSwitchesToInjured(VoluntaryHelperMovement.movementMode.HOSPITAL_WAIT_MODE);
        checkIfModeSwitchesToInjured(VoluntaryHelperMovement.movementMode.LOCAL_HELP_MODE);
        checkIfModeSwitchesToInjured(VoluntaryHelperMovement.movementMode.MOVING_TO_EVENT_MODE);
        checkIfModeSwitchesToInjured(VoluntaryHelperMovement.movementMode.PANIC_MODE);
        checkIfModeSwitchesToInjured(VoluntaryHelperMovement.movementMode.TRANSPORTING_MODE);
        checkIfModeSwitchesToInjured(VoluntaryHelperMovement.movementMode.RANDOM_MAP_BASED_MODE);
    }

    private void checkIfModeSwitchesToInjured(VoluntaryHelperMovement.movementMode mode){
        vhm.setMode(mode);
        vhm.vhmEventStarted(disaster);
        testInjuredState();
    }

    @Test
    public void testInjuredDoNotPanic(){
        host.setLocation(LOCATION_INSIDE_EVENT_RANGE);
        vhm.setInjuryProbability(0);
        vhm.setMode(VoluntaryHelperMovement.movementMode.INJURED_MODE);
        vhm.vhmEventStarted(disaster);
        testInjuredState();
    }

    @Test
    public void testHelpFunction(){
        checkHelpFunctionForLocation(LOCATION_INSIDE_SAFE_RANGE);
        checkHelpFunctionForLocation(LOCATION_INSIDE_MAX_RANGE);
        checkHelpFunctionForLocation(LOCATION_OUTSIDE_MAX_RANGE);
    }

    private void checkHelpFunctionForLocation(Coord location){
        int helpCount = 0;
        vhm.setIntensityWeight(TEST_INTENSITY_WEIGHT);
        host.setLocation(location);
        for (int i = 0; i < TEST_RUNS; i++){
            vhm.setMode(VoluntaryHelperMovement.movementMode.RANDOM_MAP_BASED_MODE);
            vhm.vhmEventStarted(disaster);
            if (vhm.getMode().equals(VoluntaryHelperMovement.movementMode.MOVING_TO_EVENT_MODE)){
                helpCount++;
            }
        }
        double calculatedHelpProb = 0;
        if (location.distance(disaster.getLocation()) < disaster.getMaxRange()) {
            calculatedHelpProb = TEST_INTENSITY_WEIGHT +
                    (1 - TEST_INTENSITY_WEIGHT) *
                            (disaster.getMaxRange() - location.distance(disaster.getLocation())) /
                            disaster.getMaxRange();
        }
        assertEquals("Help probability differs from calculation given in specification",
                calculatedHelpProb,(double) helpCount / TEST_RUNS,PROB_DELTA);
    }

}