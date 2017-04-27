package test;

import core.Coord;
import core.DTNHost;
import input.VhmEvent;
import input.VhmEventNotifier;
import junit.framework.TestCase;
import movement.MovementModel;
import movement.SwitchableStationaryMovement;
import movement.VoluntaryHelperMovement;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Includes basic method tests for {@link VoluntaryHelperMovement}.
 * Tests only correct initialization and getter and setter methods.
 * The behavior of the model is tested in {@link VhmBehaviorTest}
 *
 * Created by Marius Meyer on 10.04.17.
 */
public class VhmBasicTest extends AbstractMovementModelTest{


    public static final String MOVEMENT_MODE_DIFFERS = "The wrong movement mode was returned";
    private static final String CHOSEN_DISASTER_TYPE = "VhmEvent chosen as disaster should have the type disaster";

    protected VoluntaryHelperMovement vhm;
    protected DTNHost host;


    @Override
    public MovementModel initializeModel(TestSettings testSettings) {
        host = new TestUtils(new ArrayList<>(),new ArrayList<>(),
                testSettings).createHost();
        VhmTestHelper.createMinimalSettingsForVoluntaryHelperMovement(testSettings);
        vhm = VhmTestHelper.createMinimalVhm(testSettings,host);
        host.setLocation(new Coord(0,0));
        return vhm;
    }

    @Test
    public void testReplicate(){
        VhmTestHelper.compareVhmInstances(vhm,
                (VoluntaryHelperMovement) vhm.replicate());
    }

    @Test
    public void testGetProperties(){
        TestCase.assertNotNull("Properties class should never be null",vhm.getProperties());
    }

    @Test
    public void testGetInitialLocation(){
        //Returns an initial location using the ShortestPathMapBasedMovement.
        //This location is generated at random but shouldn't be null.
        TestCase.assertNotNull("An initial location should be set",vhm.getInitialLocation());
    }

    @Test
    public void testModelUsesDefaultValuesWhenNoOthersAreGiven(){
        VhmPropertiesTest.checkPropertiesUseDefaultValues(vhm.getProperties());
    }

    @Test
    public void testModelUsesGivenSettings(){
        vhm = VhmTestHelper.createVhmWithoutDefaultSettings(host);
        VhmPropertiesTest.checkPropertiesUseGivenSettings(vhm.getProperties());
    }

    @Test
    public void testModelListensToVhmEventNotifierAfterReplication(){
        VoluntaryHelperMovement replicate = (VoluntaryHelperMovement) vhm.replicate();
        replicate.setHost(host);
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_EVENT_RANGE);
        replicate.getProperties().setInjuryProbability(1);
        VhmEventNotifier.eventStarted(VhmTestHelper.disaster);
        VhmTestHelper.testInjuredState(replicate);
    }

    @Test
    public void testGetChosenDisaster(){
        TestCase.assertNull("No disaster should be chosen at this point",vhm.getChosenDisaster());
        vhm.getProperties().setIntensityWeight(1);
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_SAFE_RANGE);
        vhm.vhmEventStarted(VhmTestHelper.disaster);
        TestCase.assertEquals("Unexpected disaster was chosen",
                VhmTestHelper.disaster,vhm.getChosenDisaster());
    }

    @Test
    public void testGetChosenHospital(){
        TestCase.assertNull("No hospital should be chosen at this point",vhm.getChosenHospital());
        VhmTestHelper.setToTransportMode(vhm);
        TestCase.assertEquals("Unexpected hospital was chosen",
                VhmTestHelper.hospital,vhm.getChosenHospital());
    }

    @Test
    public void testChosenDisasterIsAlwaysADisaster(){
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_SAFE_RANGE);
        for (int i = 0; i < TEST_RUNS; i++){
            VhmTestHelper.setToRandomMapBasedState(vhm);
            vhm.vhmEventStarted(VhmTestHelper.disaster);
            vhm.vhmEventStarted(VhmTestHelper.hospital);
            if (vhm.getChosenDisaster() != null){
                TestCase.assertEquals(CHOSEN_DISASTER_TYPE,
                        VhmEvent.VhmEventType.DISASTER,vhm.getChosenDisaster().getType());
            }
            VhmTestHelper.setToRandomMapBasedState(vhm);
            vhm.vhmEventStarted(VhmTestHelper.hospital);
            vhm.vhmEventStarted(VhmTestHelper.disaster);
            if (vhm.getChosenDisaster() != null){
                TestCase.assertEquals(CHOSEN_DISASTER_TYPE,
                        VhmEvent.VhmEventType.DISASTER,vhm.getChosenDisaster().getType());
            }
        }
    }

    @Test
    public void testChosenHospitalIsAlwaysAHospital(){
        host.setLocation(VhmTestHelper.LOCATION_INSIDE_SAFE_RANGE);
        for (int i = 0; i < TEST_RUNS; i++){
            vhm.vhmEventStarted(VhmTestHelper.disaster);
            VhmTestHelper.setToTransportMode(vhm);
            TestCase.assertEquals("VhmEvent chosen as hospital should have the type hospital",
                    VhmEvent.VhmEventType.HOSPITAL,vhm.getChosenHospital().getType());
        }
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

    @Test
    public void testGetRegisteredDisasters(){
        vhm.vhmEventStarted(VhmTestHelper.disaster);
        TestCase.assertEquals("List of disasters should only include one item",
                1,vhm.getDisasters().size());
        TestCase.assertEquals("Item in the list should be the started disaster",
                VhmTestHelper.disaster,vhm.getDisasters().get(0));
    }

    @Test
    public void testGetRegisteredHospitals(){
        vhm.vhmEventStarted(VhmTestHelper.hospital);
        TestCase.assertEquals("List of hospitals should only include one item",
                1,vhm.getHospitals().size());
        TestCase.assertEquals("Item in the list should be the started hospital",
                VhmTestHelper.hospital,vhm.getHospitals().get(0));
    }

    @Test
    public void testGetMode(){
        TestCase.assertEquals(MOVEMENT_MODE_DIFFERS,
                VoluntaryHelperMovement.movementMode.RANDOM_MAP_BASED_MODE,vhm.getMode());
        VhmTestHelper.setToTransportMode(vhm);
        TestCase.assertEquals(MOVEMENT_MODE_DIFFERS,
                VoluntaryHelperMovement.movementMode.TRANSPORTING_MODE, vhm.getMode());
        VhmTestHelper.setToHospitalWaitMode(vhm);
        TestCase.assertEquals(MOVEMENT_MODE_DIFFERS,
                VoluntaryHelperMovement.movementMode.HOSPITAL_WAIT_MODE, vhm.getMode());
        VhmTestHelper.setToLocalHelperMode(vhm);
        TestCase.assertEquals(MOVEMENT_MODE_DIFFERS,
                VoluntaryHelperMovement.movementMode.LOCAL_HELP_MODE, vhm.getMode());
        VhmTestHelper.setToMoveToMode(vhm);
        TestCase.assertEquals(MOVEMENT_MODE_DIFFERS,
                VoluntaryHelperMovement.movementMode.MOVING_TO_EVENT_MODE, vhm.getMode());
        VhmTestHelper.setToInjuredMode(vhm);
        TestCase.assertEquals(MOVEMENT_MODE_DIFFERS,
                VoluntaryHelperMovement.movementMode.INJURED_MODE, vhm.getMode());
        VhmTestHelper.setToPanicMode(vhm);
        TestCase.assertEquals(MOVEMENT_MODE_DIFFERS,
                VoluntaryHelperMovement.movementMode.PANIC_MODE, vhm.getMode());
    }
}
