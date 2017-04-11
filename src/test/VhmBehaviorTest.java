package test;

import core.Coord;
import movement.CarMovement;
import movement.LevyWalkMovement;
import movement.PanicMovement;
import movement.ShortestPathMapBasedMovement;
import movement.SwitchableStationaryMovement;
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

    private static final String WRONG_MOVEMENT_MODE = "Wrong movement mode is set";
    private static final String WRONG_MOVEMENT_MODEL = "Wrong movement model is selected";

    //big delta for higher tolerance for probabilistic functions
    private static final double PROB_DELTA = 0.01;

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



    /**
     * For now this is just an example and a template for creating state change tests.
     * This test needs to be checked and possibly modified.
     */
    @Test
    public void testMovementAfterPanicMode() {
        //set the state of the VoluntaryHelperMovement object.
        vhm.setMode(VoluntaryHelperMovement.movementMode.PANIC_MODE);
        vhm.setCurrentMovementModel(vhm.getPanicMM());

        //provoke a state change
        vhm.newOrders();

        //test if the state changed to an expected value
        assertTrue((VoluntaryHelperMovement.movementMode.RANDOM_MAP_BASED_MODE.equals(vhm.getMode())
                && ShortestPathMapBasedMovement.class.equals(vhm.getCurrentMovementModel().getClass()))
                ||
                (VoluntaryHelperMovement.movementMode.MOVING_TO_EVENT_MODE.equals(vhm.getMode())
                        && CarMovement.class.equals(vhm.getCurrentMovementModel().getClass())));
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
     * Checks, if a node within event range gets injured when event starts.
     */
    @Test
    public void testDisasterEventStartedInjury(){
        vhm.setInjuryProbability(1);
        host.setLocation(LOCATION_INSIDE_EVENT_RANGE);
        vhm.vhmEventStarted(disaster);
        testInjuredState();
    }

    /**
     * Checks, if a node within event range panics when event starts.
     */
    @Test
    public void testDisasterEventStartedPanic(){
        vhm.setInjuryProbability(0);
        host.setLocation(LOCATION_INSIDE_EVENT_RANGE);
        vhm.vhmEventStarted(disaster);
        testPanicState();
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

    /*
    -----------------------------------------------------------------
    Method test section

    In This section, getter and setter methods are tested
    -----------------------------------------------------------------
     */



    private void testInjuredState(){
        assertEquals(WRONG_MOVEMENT_MODE,
                VoluntaryHelperMovement.movementMode.INJURED_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL, SwitchableStationaryMovement.class,
                vhm.getCurrentMovementModel().getClass());
    }

    private void testPanicState(){
        assertEquals(WRONG_MOVEMENT_MODE,
                VoluntaryHelperMovement.movementMode.PANIC_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                PanicMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    private void testMoveToState(){
        assertEquals(WRONG_MOVEMENT_MODE,
                VoluntaryHelperMovement.movementMode.MOVING_TO_EVENT_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                CarMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    private void testTransportState(){
        assertEquals(WRONG_MOVEMENT_MODE,
                VoluntaryHelperMovement.movementMode.TRANSPORTING_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                CarMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    private void testRandomMapBasedState(){
        assertEquals(WRONG_MOVEMENT_MODE,
                VoluntaryHelperMovement.movementMode.RANDOM_MAP_BASED_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                ShortestPathMapBasedMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    private void testLevyWalkState(){
        assertEquals(WRONG_MOVEMENT_MODE,
                VoluntaryHelperMovement.movementMode.LOCAL_HELP_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                LevyWalkMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    private void testWaitState(){
        assertEquals(WRONG_MOVEMENT_MODE,
                VoluntaryHelperMovement.movementMode.HOSPITAL_WAIT_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                LevyWalkMovement.class,vhm.getCurrentMovementModel().getClass());
    }

}