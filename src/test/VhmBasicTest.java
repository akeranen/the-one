package test;

import core.Coord;
import core.DTNHost;
import input.VhmEventNotifier;
import junit.framework.TestCase;
import movement.MovementModel;
import movement.SwitchableStationaryMovement;
import movement.VoluntaryHelperMovement;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Abstract test class for {@link VoluntaryHelperMovement}.
 * Includes set up and necessary variables and functions as well as basic method functionality tests.
 *
 * Created by Marius Meyer on 10.04.17.
 */
public class VhmBasicTest extends AbstractMovementModelTest{

    private static final String HELP_TIME_DIFFERS = "Help time differs from specified one";
    private static final String WAIT_TIME_DIFFERS = "Hospital wait time differs from specified one";
    private static final String INJURY_PROB_DIFFERS = "Injury probability differs from specified one";
    private static final String WAIT_PROB_DIFFERS = "Wait probability differs from specified one";
    private static final String INTESITY_WEIGHT_DIFFERS = "Intensity weight differs from specified one";

    protected VhmProperties vhm;
    protected DTNHost host;


    @Override
    public MovementModel initializeModel(TestSettings testSettings) {
        host = new TestUtils(new ArrayList<>(),new ArrayList<>(),
                testSettings).createHost();
        vhm = VhmTestHelper.createMinimalVhm(host);
        host.setLocation(new Coord(0,0));
        return vhm;
    }

    @Test
    public void testReplicate(){
        VhmTestHelper.compareVhmInstances(vhm,(VhmProperties) vhm.replicate());
    }

    @Test
    public void testGetInitialLocation(){
        TestCase.assertNotNull("An initial location should be set",vhm.getInitialLocation());
    }

    @Test
    public void testModelUsesDefaultValuesWhenNoOthersAreGiven(){
        TestCase.assertEquals(HELP_TIME_DIFFERS,
                VoluntaryHelperMovement.DEFAULT_HELP_TIME,vhm.getHelpTime(),VhmTestHelper.DELTA);
        TestCase.assertEquals(WAIT_TIME_DIFFERS,
                VoluntaryHelperMovement.DEFAULT_HOSPITAL_WAIT_TIME,vhm.getHospitalWaitTime(),VhmTestHelper.DELTA);
        TestCase.assertEquals(WAIT_PROB_DIFFERS,
                VoluntaryHelperMovement.DEFAULT_HOSPITAL_WAIT_PROBABILITY,vhm.getWaitProbability(),VhmTestHelper.DELTA);
        TestCase.assertEquals(INJURY_PROB_DIFFERS,
                VoluntaryHelperMovement.DEFAULT_INJURY_PROBABILITY,vhm.getInjuryProbability(),VhmTestHelper.DELTA);
        TestCase.assertEquals(INTESITY_WEIGHT_DIFFERS,
                VoluntaryHelperMovement.DEFAULT_INTENSITY_WEIGHT,vhm.getIntensityWeight(),VhmTestHelper.DELTA);
        TestCase.assertFalse("Node shouldn't be local helper by default",vhm.isLocalHelper());
    }

    @Test
    public void testModelUsesGivenSettings(){
        vhm = VhmTestHelper.createVhmWithoutDefaultSettings(host);
        TestCase.assertEquals(HELP_TIME_DIFFERS,
                VhmTestHelper.HELP_TIME,vhm.getHelpTime(),VhmTestHelper.DELTA);
        TestCase.assertEquals(WAIT_TIME_DIFFERS,
                VhmTestHelper.HOSPITAL_WAIT_TIME,vhm.getHospitalWaitTime(),VhmTestHelper.DELTA);
        TestCase.assertEquals(WAIT_PROB_DIFFERS,
                VhmTestHelper.PROBABILITY,vhm.getWaitProbability(),VhmTestHelper.DELTA);
        TestCase.assertEquals(INJURY_PROB_DIFFERS,
                VhmTestHelper.PROBABILITY,vhm.getInjuryProbability(),VhmTestHelper.DELTA);
        TestCase.assertEquals(INTESITY_WEIGHT_DIFFERS,
                VhmTestHelper.PROBABILITY,vhm.getIntensityWeight(),VhmTestHelper.DELTA);
        TestCase.assertTrue("Node should be local helper",vhm.isLocalHelper());
    }

    @Test
    public void testModelListensToVhmEventNotifierAfterReplication(){
        VhmProperties replicate = (VhmProperties) vhm.replicate();
        replicate.setHost(host);
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_EVENT_RANGE);
        replicate.setInjuryProbability(1);
        VhmEventNotifier.eventStarted(VhmTestHelper.disaster);
        VhmTestHelper.testInjuredState(replicate);
    }

    @Test
    public void testIsLocalHelper(){
        vhm.setLocalHelper(true);
        TestCase.assertTrue("local helper should be set",vhm.isLocalHelper());
        vhm.setLocalHelper(false);
        TestCase.assertFalse("local helper shouldn't be set",vhm.isLocalHelper());
    }

    @Test
    public void testInjuryProbability(){
        vhm.setInjuryProbability(VhmTestHelper.PROBABILITY);
        TestCase.assertEquals("Injury probability should be set to different value",
                VhmTestHelper.PROBABILITY,vhm.getInjuryProbability(), VhmTestHelper.DELTA);
    }

    @Test
    public void testWaitProbability(){
        vhm.setWaitProbability(VhmTestHelper.PROBABILITY);
        TestCase.assertEquals("Wait probability should be set to different value",
                VhmTestHelper.PROBABILITY, vhm.getWaitProbability(), VhmTestHelper.DELTA);
    }

    @Test
    public void testChosenDisaster(){
        TestCase.assertNull("No disaster should be chosen at this point",vhm.getChosenDisaster());
        vhm.setIntensityWeight(1);
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_SAFE_RANGE);
        vhm.vhmEventStarted(VhmTestHelper.disaster);
        TestCase.assertEquals("Not the correct disaster was set as chosen",
                VhmTestHelper.disaster,vhm.getChosenDisaster());
    }

    @Test
    public void testIntensityWeight(){
        vhm.setIntensityWeight(VhmTestHelper.PROBABILITY);
        TestCase.assertEquals("Wrong intensity weight was set", VhmTestHelper.PROBABILITY,vhm.getIntensityWeight());
    }

    @Test
    public void testChosenHospital(){
        TestCase.assertNull("No hospital should be chosen at this point",vhm.getChosenHospital());
        VhmTestHelper.setToTransportMode(vhm);
        TestCase.assertEquals("Not the correct hospital was set as chosen",
                VhmTestHelper.hospital,vhm.getChosenHospital());
    }

    @Test
    public void testSetCurrentMovementModelSetsMovementModelAndLocation(){
        SwitchableStationaryMovement testMovement = new SwitchableStationaryMovement(new TestSettings());
        vhm.setCurrentMovementModel(testMovement);
        TestCase.assertEquals("Wrong movement model is set as current movement model",
                testMovement,vhm.getCurrentMovementModel());
        TestCase.assertEquals("Wrong location is set for switched movement model",
                host.getLocation(),vhm.getCurrentMovementModel().getLastLocation());
    }
}
