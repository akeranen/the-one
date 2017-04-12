package test;

import core.Coord;
import core.DTNHost;
import input.VhmEvent;
import movement.CarMovement;
import movement.LevyWalkMovement;
import movement.MapBasedMovement;
import movement.MovementModel;
import movement.PanicMovement;
import movement.ShortestPathMapBasedMovement;
import movement.SwitchableStationaryMovement;
import movement.VoluntaryHelperMovement;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;

/**
 * Abstract test class for {@link VoluntaryHelperMovement}.
 * Includes set up and necessary variables and functions as well as basic method functionality tests.
 *
 * Created by Marius Meyer on 10.04.17.
 */
public abstract class AbstractVhmTest  extends AbstractMovementModelTest{

    private static final String WRONG_MOVEMENT_MODE = "Wrong movement mode is set";
    private static final String WRONG_MOVEMENT_MODEL = "Wrong movement model is selected";

    private static final double TEST_TIME = 200;
    protected static final double TEST_PROBABILITY = 0.24;
    protected static final double DELTA = 0.001;

    protected VoluntaryHelperMovement vhm;
    protected DTNHost host;
    protected VhmEvent disaster = new VhmEvent("testDisaster",
            VhmEventTest.createJsonForCompletelySpecifiedEvent());
    protected VhmEvent hospital = new VhmEvent("testHospital",
            VhmEventTest.createMinimalVhmEventBuilder(VhmEvent.VhmEventType.HOSPITAL).build());


    @Override
    public MovementModel initializeModel(TestSettings testSettings) {
        testSettings.setNameSpace(MapBasedMovement.MAP_BASE_MOVEMENT_NS);
        testSettings.putSetting(MapBasedMovement.NROF_FILES_S, "1");
        testSettings.putSetting("mapFile1","data/Manhattan/roads.wkt");
        testSettings.restoreNameSpace();
        vhm = new VoluntaryHelperMovement(testSettings);
        //needed to initialize MM
        host = new TestUtils(new ArrayList<>(),new ArrayList<>(),
                testSettings).createHost();
        host.setLocation(new Coord(0,0));
        vhm.setHost(host);
        return vhm;
    }

    @Test
    public void testReplicate(){
        compareVhmInstances(vhm,(VoluntaryHelperMovement) vhm.replicate());
    }

    @Test
    public void testPrototypeConstructor(){
        compareVhmInstances(vhm,new VoluntaryHelperMovement(vhm));
    }

    @Test
    public void testGetInitialLocation(){
        Coord coord = vhm.getInitialLocation();
        assertNotNull(coord);
    }

    @Test
    public void testMode(){
        for (VoluntaryHelperMovement.movementMode mode : VoluntaryHelperMovement.movementMode.values()){
            vhm.setMode(mode);
            assertEquals("Different mode should be set",mode,vhm.getMode());
        }
    }

    @Test
    public void testIsLocalHelper(){
        vhm.setLocalHelper(true);
        assertTrue("local helper should be set",vhm.isLocalHelper());
        vhm.setLocalHelper(false);
        assertFalse("local helper shouldn't be set",vhm.isLocalHelper());
    }

    @Test
    public void testHospitalWaitTime(){
        vhm.setHospitalWaitTime(TEST_TIME);
        assertEquals("Hospital wait time should be set to different value",
                TEST_TIME,vhm.getHospitalWaitTime(),DELTA);
    }

    @Test
    public void testHelpTime(){
        vhm.setHelpTime(TEST_TIME);
        assertEquals("Help time should be set to different value", TEST_TIME,vhm.getHelpTime(),DELTA);
    }

    @Test
    public void testInjuryProbability(){
        vhm.setInjuryProbability(TEST_PROBABILITY);
        assertEquals("Injury probability should be set to different value",
                TEST_PROBABILITY,vhm.getInjuryProbability(),DELTA);
    }

    @Test
    public void testWaitProbability(){
        vhm.setWaitProbability(TEST_PROBABILITY);
        assertEquals("Wait probability should be set to different value",
                TEST_PROBABILITY, vhm.getWaitProbability(), DELTA);
    }

    @Test
    public void testStartTime(){
        vhm.setStartTime(TEST_TIME);
        assertEquals("Start time should be set to different value", TEST_TIME,
                vhm.getStartTime(),DELTA);
    }

    @Test
    public void testJustChanged(){
        vhm.setJustChanged(true);
        assertTrue("Just changed should be set to true",vhm.isJustChanged());
        vhm.setJustChanged(false);
        assertFalse("Just changed should be set to false",vhm.isJustChanged());
    }

    @Test
    public void testChosenDisaster(){
        assertNull("No disaster should be chosen at this point",vhm.getChosenDisaster());
        vhm.setChosenDisaster(disaster);
        assertEquals("Not the correct disaster was set as chosen",disaster,vhm.getChosenDisaster());
    }

    @Test
    public void testChosenHospital(){
        assertNull("No hospital should be chosen at this point",vhm.getChosenHospital());
        vhm.setChosenHospital(hospital);
        assertEquals("Not the correct hospital was set as chosen",hospital,vhm.getChosenHospital());
    }

    @Test
    public void testDisasters(){
        assertEquals("No disaster should be in the list at this point",0,vhm.getDisasters().size());
        includeDisaster();
        assertEquals("Exactly one disaster should be in list",1,vhm.getDisasters().size());
        assertEquals("The disaster in the list should be the one added",disaster,vhm.getDisasters().get(0));
    }

    @Test
    public void testHospitals(){
        assertEquals("No hospital should be in the list at this point",0,vhm.getHospitals().size());
        List<VhmEvent> hospitals = new ArrayList<>(1);
        hospitals.add(hospital);
        vhm.setHospitals(hospitals);
        assertEquals("Exactly one hospital should be in list",1,vhm.getHospitals().size());
        assertEquals("The hospital in the list should be the one added",hospital,vhm.getHospitals().get(0));
    }

    /*
    -----------------------------------------------------------------
    State testing

    In This section, the different states of the VHM are tested in separate
    functions. They can be called from other tests to check, if a state was
    fully reached.
    -----------------------------------------------------------------
     */

    void testInjuredState(){
        assertEquals(WRONG_MOVEMENT_MODE,
                VoluntaryHelperMovement.movementMode.INJURED_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL, SwitchableStationaryMovement.class,
                vhm.getCurrentMovementModel().getClass());
    }

    void testPanicState(){
        assertEquals(WRONG_MOVEMENT_MODE,
                VoluntaryHelperMovement.movementMode.PANIC_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                PanicMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    void testMoveToState(){
        assertEquals(WRONG_MOVEMENT_MODE,
                VoluntaryHelperMovement.movementMode.MOVING_TO_EVENT_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                CarMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    void testTransportState(){
        assertEquals(WRONG_MOVEMENT_MODE,
                VoluntaryHelperMovement.movementMode.TRANSPORTING_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                CarMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    void testRandomMapBasedState(){
        assertEquals(WRONG_MOVEMENT_MODE,
                VoluntaryHelperMovement.movementMode.RANDOM_MAP_BASED_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                ShortestPathMapBasedMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    void testLevyWalkState(){
        assertEquals(WRONG_MOVEMENT_MODE,
                VoluntaryHelperMovement.movementMode.LOCAL_HELP_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                LevyWalkMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    void testWaitState(){
        assertEquals(WRONG_MOVEMENT_MODE,
                VoluntaryHelperMovement.movementMode.HOSPITAL_WAIT_MODE,vhm.getMode());
        assertEquals(WRONG_MOVEMENT_MODEL,
                LevyWalkMovement.class,vhm.getCurrentMovementModel().getClass());
    }

    /*
    -----------------------------------------------------------------
    Help methods

    In this section, help methods used in the tests are implemented
    -----------------------------------------------------------------
     */

    private static void compareVhmInstances(VoluntaryHelperMovement m1, VoluntaryHelperMovement m2){
        assertEquals("local helper should be set to the same value",m1.isLocalHelper(),m2.isLocalHelper());
        assertEquals("help time should be set to the same value",m1.getHelpTime(),m2.getHelpTime(),DELTA);
        assertEquals("Hospital wait time should be set to the same value",
                m1.getHospitalWaitTime(),m2.getHospitalWaitTime(),DELTA);
        assertEquals("Injury probability should be set to the same value",
                m1.getInjuryProbability(),m2.getInjuryProbability(),DELTA);
        assertEquals("Wait probability should be set to the same value",
                m1.getWaitProbability(),m2.getWaitProbability(),DELTA);
        assertEquals("Intensity weight should be set to the same value",
                m1.getIntensityWeight(),m2.getIntensityWeight(),DELTA);

    }


    void includeDisaster(){
        List<VhmEvent> disasters = new ArrayList<>(1);
        disasters.add(disaster);
        vhm.setDisasters(disasters);
    }

    void includeHospital(){
        List<VhmEvent> hospitals = new ArrayList<>(1);
        hospitals.add(hospital);
        vhm.setHospitals(hospitals);
    }
}
