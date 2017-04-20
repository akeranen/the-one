package test;

import core.Coord;
import core.DTNHost;
import core.SimClock;
import movement.CarMovement;
import movement.ShortestPathMapBasedMovement;
import movement.VoluntaryHelperMovement;
import org.junit.Test;

import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;

/**
 * Tests for behavior of VoluntaryHelperMovement
 * Created by Marius Meyer on 10.04.17.
 */
public class VhmBehaviorTest {

    private static final String INVALID_MODE_SWITCH = "Mode shouldn't have switched";

    //big delta for higher tolerance for probabilistic functions
    private static final double PROB_DELTA = 0.05;

    private static final int TEST_RUNS = 2000;

    private TestSettings testSettings = VhmTestHelper.createMinimalSettingsForVoluntaryHelperMovement();
    private VhmProperties vhm;
    private DTNHost host = new TestUtils(new ArrayList<>(),new ArrayList<>(),testSettings).createHost();

    public VhmBehaviorTest(){
        host.setLocation(new Coord(0,0));
        vhm = VhmTestHelper.createMinimalVhm(host);
    }

    @Test
    public void testSetHostSetsHost(){
        assertEquals("Host was not set as expected",host,vhm.getHost());
    }

    @Test
    public void testInitialMMIsRandomMapBasedIfNoEventIsStartedOrWasNotChosen(){
        assertEquals("Wrong movement mode was chosen",
                VoluntaryHelperMovement.movementMode.RANDOM_MAP_BASED_MODE, vhm.getMode());
        assertEquals("Wrong movement model is used",
                ShortestPathMapBasedMovement.class, vhm.getCurrentMovementModel().getClass());
    }

    @Test
    public void testInitialMMisMoveToNodeIfAnEventIsAvailiableAndWasChosen(){
        VhmTestHelper.includeEvent(VhmTestHelper.disaster, vhm);
        vhm.setIntensityWeight(1);
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_SAFE_RANGE);
        vhm.setHost(host);
        assertEquals("Model should choose to move to event",
                VoluntaryHelperMovement.movementMode.MOVING_TO_EVENT_MODE, vhm.getMode());
        assertEquals("CarMovement should be used as movement model",
                CarMovement.class, vhm.getCurrentMovementModel().getClass());
        vhm.getPath();
        assertEquals("The destination should be the nearest map node to the event location",
                vhm.getMap().getClosestNodeByCoord(VhmTestHelper.disaster.getLocation()).getLocation(),
                        vhm.getCurrentMovementModel().getLastLocation());
    }

    @Test
    public void testHospitalEventStartedAndIsAddedToHospitalsAndNotToDisasters(){
        vhm.vhmEventStarted(VhmTestHelper.hospital);
        assertTrue("Hospital was not added to list of hospitals",
                vhm.getHospitals().contains(VhmTestHelper.hospital));
        assertFalse("Hospital was falsely added to list of disasters",
                vhm.getDisasters().contains(VhmTestHelper.hospital));
    }

    @Test
    public void testHospitalEventEndedRemovesHospitalFromList(){
        vhm.vhmEventStarted(VhmTestHelper.hospital);
        vhm.vhmEventEnded(VhmTestHelper.hospital);
        assertFalse("Hospital wasn't removed from list",vhm.getHospitals().contains(VhmTestHelper.hospital));
    }

    @Test
    public void testDisasterEventStartedAndIsAddedToDisastersAndNotToHospitals(){
        vhm.vhmEventStarted(VhmTestHelper.disaster);
        assertTrue("Disaster was not added to list of disasters",
                vhm.getDisasters().contains(VhmTestHelper.disaster));
        assertFalse("Disaster was falsely added to list of hospitals",
                vhm.getHospitals().contains(VhmTestHelper.disaster));
    }

    /**
     * Checks, if a node within max range helps when event starts.
     */
    @Test
    public void testDisasterEventStartedHelp(){
        vhm.setIntensityWeight(1);
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_MAX_RANGE);
        vhm.vhmEventStarted(VhmTestHelper.disaster);
        VhmTestHelper.testMoveToState(vhm);
    }

    /**
     * Checks, if a node outside max range doesn't help when event starts, even if probability 1.
     */
    @Test
    public void testDisasterEventStartedNodesOutsideRangeDontHelp(){
        vhm.setIntensityWeight(1);
        host.setLocation(VhmTestHelper.LOCATION_OUTSIDE_MAX_RANGE);
        vhm.vhmEventStarted(VhmTestHelper.disaster);
        VhmTestHelper.testRandomMapBasedState(vhm);
    }

    @Test
    public void testDisasterEventEndedRemovesDisasterFromList(){
        vhm.vhmEventStarted(VhmTestHelper.disaster);
        vhm.vhmEventEnded(VhmTestHelper.disaster);
        assertFalse("Disaster wasn't removed from list",vhm.getDisasters().contains(VhmTestHelper.hospital));
    }

    @Test
    public void testNodeWorkingOnDisasterStartOverAfterDisasterEnds(){
        vhm.setIntensityWeight(1);
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_MAX_RANGE);
        vhm.vhmEventStarted(VhmTestHelper.disaster);
        vhm.vhmEventEnded(VhmTestHelper.disaster);
        VhmTestHelper.testRandomMapBasedState(vhm);

    }

    @Test
    public void testPanicingNodesIgnoreEndOfDisaster(){
        vhm.setInjuryProbability(0);
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_EVENT_RANGE);
        vhm.vhmEventStarted(VhmTestHelper.disaster);
        vhm.vhmEventEnded(VhmTestHelper.disaster);
        VhmTestHelper.testPanicState(vhm);
    }

    @Test
    public void testInjuredNodesIgnoreEndOfDisaster(){
        vhm.setInjuryProbability(1);
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_EVENT_RANGE);
        vhm.vhmEventStarted(VhmTestHelper.disaster);
        vhm.vhmEventEnded(VhmTestHelper.disaster);
        VhmTestHelper.testInjuredState(vhm);
    }

    /**
     * Tests, if the probability a node gets injured is close to the probability specified in the settings
     */
    @Test
    public void testInjuryProbabilityIsUsedCorrectly(){
        int injuredCount = 0;
        vhm.setInjuryProbability(VhmTestHelper.INJURY_PROBABILITY);
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_EVENT_RANGE);
        for (int i = 0; i < TEST_RUNS; i++){
            vhm.vhmEventStarted(VhmTestHelper.disaster);
            vhm.vhmEventEnded(VhmTestHelper.disaster);
            if (vhm.getMode() == VoluntaryHelperMovement.movementMode.INJURED_MODE){
                injuredCount++;
            }
            VhmTestHelper.setToRandomMapBasedState(vhm);
        }
        assertEquals("Measured injury probability differs from value specified",
                VhmTestHelper.INJURY_PROBABILITY,(double)injuredCount / TEST_RUNS,PROB_DELTA);
    }

    /**
     * Tests, if the probability a node waits at a hospital is close to the probability specified in the settings
     */
    @Test
    public void testHospitalWaitProbabilityIsUsedCorrectly(){
        int waitingCount = 0;
        vhm.setWaitProbability(VhmTestHelper.WAIT_PROBABILITY);
        host.setLocation(VhmTestHelper.hospital.getLocation());
        VhmTestHelper.setToTransportMode(vhm);
        for (int i = 0; i < TEST_RUNS; i++){
            vhm.newOrders();
            if (vhm.getMode() == VoluntaryHelperMovement.movementMode.HOSPITAL_WAIT_MODE){
                waitingCount++;
            }
            VhmTestHelper.setToTransportMode(vhm);
        }
        assertEquals("Measured wait probability differs from value specified",
                VhmTestHelper.WAIT_PROBABILITY,(double) waitingCount / TEST_RUNS,PROB_DELTA);
    }

    @Test
    public void testAfterArrivalSwitchToLevyWalkIfLocalHelper(){
        vhm.setLocalHelper(true);
        VhmTestHelper.setToMoveToMode(vhm);
        vhm.newOrders();
        VhmTestHelper.testLevyWalkState(vhm);
    }

    @Test
    public void testAfterArrivalSwitchToTransportIfNotLocalHelperAndHospitalAvailable(){
        vhm.setLocalHelper(false);
        vhm.vhmEventStarted(VhmTestHelper.hospital);
        VhmTestHelper.setToMoveToMode(vhm);
        vhm.newOrders();
        VhmTestHelper.testTransportState(vhm);
    }

    @Test
    public void testAfterArrivalSwitchToRandomMapBasedIfNotLocalHelperAndNoHospitalAvailable(){
        vhm.setLocalHelper(false);
        VhmTestHelper.setToMoveToMode(vhm);
        vhm.newOrders();
        VhmTestHelper.testRandomMapBasedState(vhm);
    }

    @Test
    public void testAfterTransportingDoLevyWalkIfDecideToWait(){
        VhmTestHelper.setToTransportMode(vhm);
        vhm.setWaitProbability(1);
        vhm.newOrders();
        VhmTestHelper.testWaitState(vhm);
    }

    @Test
    public void testAfterTransportingMoveToChosenDisasterIfNotDecideToWait(){
        VhmTestHelper.setToTransportMode(vhm);
        vhm.setWaitProbability(0);
        vhm.newOrders();
        VhmTestHelper.testMoveToState(vhm);
    }

    @Test
    public void testNodesAreHelpingDuringTheSpecifiedHelpTimeAndSwitchToRandomIfNoEventChosen(){
        vhm = VhmTestHelper.createVhmWithHelpAndWaitTimes(host);
        SimClock.getInstance().setTime(0);
        VhmTestHelper.setToLocalHelperMode(vhm);
        SimClock.getInstance().setTime(VhmTestHelper.HELP_TIME - VhmTestHelper.DELTA);
        vhm.newOrders();
        assertEquals("The mode should not have been switched",
                VoluntaryHelperMovement.movementMode.LOCAL_HELP_MODE,vhm.getMode());
        SimClock.getInstance().setTime(VhmTestHelper.HELP_TIME + VhmTestHelper.DELTA);
        vhm.newOrders();
        VhmTestHelper.testRandomMapBasedState(vhm);
    }

    @Test
    public void testNodesAreWaitingDuringTheSpecifiedWaitTimeAndSwitchToRandomIfNoEventChosen(){
        vhm = VhmTestHelper.createVhmWithHelpAndWaitTimes(host);
        SimClock.getInstance().setTime(0);
        VhmTestHelper.setToHospitalWaitMode(vhm);
        SimClock.getInstance().setTime(VhmTestHelper.HOSPITAL_WAIT_TIME - VhmTestHelper.DELTA);
        vhm.newOrders();
        assertEquals("The mode should not have been switched",
                VoluntaryHelperMovement.movementMode.HOSPITAL_WAIT_MODE,vhm.getMode());
        SimClock.getInstance().setTime(VhmTestHelper.HOSPITAL_WAIT_TIME + VhmTestHelper.DELTA);
        vhm.newOrders();
        VhmTestHelper.testRandomMapBasedState(vhm);
    }

    @Test
    public void testAfterLevyWalkDoNextEventIfEventIsAvailable(){
        vhm = VhmTestHelper.createVhmWithHelpAndWaitTimes(host);
        SimClock.getInstance().setTime(0);
        vhm.setIntensityWeight(1);
        VhmTestHelper.includeEvent(VhmTestHelper.disaster, vhm);
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_SAFE_RANGE);
        VhmTestHelper.setToLocalHelperMode(vhm);
        SimClock.getInstance().setTime(VhmTestHelper.HELP_TIME + VhmTestHelper.DELTA);
        vhm.newOrders();
        VhmTestHelper.testMoveToState(vhm);
    }

    @Test
    public void testAfterWaitHospitalDoNextEventIfEventIsAvailable(){
        vhm = VhmTestHelper.createVhmWithHelpAndWaitTimes(host);
        SimClock.getInstance().setTime(0);
        vhm.setIntensityWeight(1);
        VhmTestHelper.includeEvent(VhmTestHelper.disaster, vhm);
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_SAFE_RANGE);
        VhmTestHelper.setToHospitalWaitMode(vhm);
        SimClock.getInstance().setTime(VhmTestHelper.HOSPITAL_WAIT_TIME + VhmTestHelper.DELTA);
        vhm.newOrders();
        VhmTestHelper.testMoveToState(vhm);
    }

    @Test
    public void testAfterPanicSwitchToRandomIfNoEventChosen(){
        VhmTestHelper.setToPanicMode(vhm);
        vhm.newOrders();
        VhmTestHelper.testRandomMapBasedState(vhm);
    }

    @Test
    public void testAfterPanicDoNextEventIfEventIsAvailable(){
        VhmTestHelper.setToPanicMode(vhm);
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_SAFE_RANGE);
        vhm.vhmEventStarted(VhmTestHelper.disaster);
        vhm.setIntensityWeight(1);
        vhm.newOrders();
        VhmTestHelper.testMoveToState(vhm);
    }

    @Test
    public void testAllStatesExceptRandomMapBasedIgnoreStartingEventsInSafeRange(){
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_SAFE_RANGE);
        vhm.setIntensityWeight(1);
        VhmTestHelper.setToHospitalWaitMode(vhm);
        checkIfDisasterIsIgnoredForMode(VoluntaryHelperMovement.movementMode.HOSPITAL_WAIT_MODE);
        VhmTestHelper.setToLocalHelperMode(vhm);
        checkIfDisasterIsIgnoredForMode(VoluntaryHelperMovement.movementMode.LOCAL_HELP_MODE);
        VhmTestHelper.setToMoveToMode(vhm);
        checkIfDisasterIsIgnoredForMode(VoluntaryHelperMovement.movementMode.MOVING_TO_EVENT_MODE);
        VhmTestHelper.setToPanicMode(vhm);
        checkIfDisasterIsIgnoredForMode(VoluntaryHelperMovement.movementMode.PANIC_MODE);
        VhmTestHelper.setToTransportMode(vhm);
        checkIfDisasterIsIgnoredForMode(VoluntaryHelperMovement.movementMode.TRANSPORTING_MODE);
        VhmTestHelper.setToInjuredMode(vhm);
        checkIfDisasterIsIgnoredForMode(VoluntaryHelperMovement.movementMode.INJURED_MODE);
    }

    private void checkIfDisasterIsIgnoredForMode(VoluntaryHelperMovement.movementMode mode){
        vhm.vhmEventStarted(VhmTestHelper.disaster);
        assertEquals(INVALID_MODE_SWITCH,mode,vhm.getMode());
        vhm.vhmEventEnded(VhmTestHelper.disaster);
        assertEquals(INVALID_MODE_SWITCH,mode,vhm.getMode());
    }

    @Test
    public void testAllStatesSwitchToPanicIfInEventRangeExceptInjured(){
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_EVENT_RANGE);
        vhm.setInjuryProbability(0);
        VhmTestHelper.setToHospitalWaitMode(vhm);
        checkIfModeSwitchesToPanic();
        VhmTestHelper.setToLocalHelperMode(vhm);
        checkIfModeSwitchesToPanic();
        VhmTestHelper.setToMoveToMode(vhm);
        checkIfModeSwitchesToPanic();
        VhmTestHelper.setToTransportMode(vhm);
        checkIfModeSwitchesToPanic();
        VhmTestHelper.setToRandomMapBasedState(vhm);
        checkIfModeSwitchesToPanic();
        VhmTestHelper.setToPanicMode(vhm);
        checkIfModeSwitchesToPanic();
    }

    private void checkIfModeSwitchesToPanic(){
        vhm.vhmEventStarted(VhmTestHelper.disaster);
        VhmTestHelper.testPanicState(vhm);
    }

    @Test
    public void testAllStatesSwitchToInjuredIfInEventRange(){
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_EVENT_RANGE);
        vhm.setInjuryProbability(1);
        VhmTestHelper.setToHospitalWaitMode(vhm);
        checkIfModeSwitchesToInjured();
        VhmTestHelper.setToLocalHelperMode(vhm);
        checkIfModeSwitchesToInjured();
        VhmTestHelper.setToMoveToMode(vhm);
        checkIfModeSwitchesToInjured();
        VhmTestHelper.setToPanicMode(vhm);
        checkIfModeSwitchesToInjured();
        VhmTestHelper.setToTransportMode(vhm);
        checkIfModeSwitchesToInjured();
        VhmTestHelper.setToRandomMapBasedState(vhm);
        checkIfModeSwitchesToInjured();
    }

    private void checkIfModeSwitchesToInjured(){
        vhm.vhmEventStarted(VhmTestHelper.disaster);
        VhmTestHelper.testInjuredState(vhm);
    }

    @Test
    public void testInjuredDoNotPanic(){
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_EVENT_RANGE);
        vhm.setInjuryProbability(0);
        VhmTestHelper.setToInjuredMode(vhm);
        vhm.vhmEventStarted(VhmTestHelper.disaster);
        assertEquals("Injured mode should not be changed at this point",
                VoluntaryHelperMovement.movementMode.INJURED_MODE, vhm.getMode());
    }

    @Test
    public void testHelpFunction(){
        checkHelpFunctionForLocation(VhmTestHelper.LOCATION_INSIDE_SAFE_RANGE);
        checkHelpFunctionForLocation(VhmTestHelper.LOCATION_INSIDE_MAX_RANGE);
        checkHelpFunctionForLocation(VhmTestHelper.LOCATION_OUTSIDE_MAX_RANGE);
    }

    /**
     * Sets the host to the given location and checks, if the resulting help probability is close to the calculated
     * one
     * @param location the location of the host
     */
    private void checkHelpFunctionForLocation(Coord location){
        int helpCount = 0;
        vhm.setIntensityWeight(VhmTestHelper.INTENSITY_WEIGHT);
        host.setLocation(location);
        for (int i = 0; i < TEST_RUNS; i++){
            VhmTestHelper.setToRandomMapBasedState(vhm);
            vhm.vhmEventStarted(VhmTestHelper.disaster);
            if (vhm.getMode().equals(VoluntaryHelperMovement.movementMode.MOVING_TO_EVENT_MODE)){
                helpCount++;
            }
        }
        double calculatedHelpProb = 0;
        if (location.distance(VhmTestHelper.disaster.getLocation()) < VhmTestHelper.disaster.getMaxRange()) {
            calculatedHelpProb = VhmTestHelper.INTENSITY_WEIGHT +
                    (1 - VhmTestHelper.INTENSITY_WEIGHT) *
                            (VhmTestHelper.disaster.getMaxRange() -
                                    location.distance(VhmTestHelper.disaster.getLocation())) /
                            VhmTestHelper.disaster.getMaxRange();
        }
        assertEquals("Help probability differs from calculation given in specification",
                calculatedHelpProb,(double) helpCount / TEST_RUNS,PROB_DELTA);
    }

    //TODO: Add tests for dying battery, after feature is implemented
    //TODO: Add tests for node reset, after feature is implemented
}