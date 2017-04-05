package test;

import core.Coord;
import movement.CarMovement;
import movement.MapBasedMovement;
import movement.MovementModel;
import movement.ShortestPathMapBasedMovement;
import movement.VoluntaryHelperMovement;
import org.junit.Test;

import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for VoluntaryHelperMovement
 * Created by melanie on 31.03.17.
 */
public class VhmTest extends AbstractMovementModelTest{

    private VoluntaryHelperMovement vhm;

    @Override
    public MovementModel initializeModel(TestSettings testSettings) {
        testSettings.setNameSpace(MapBasedMovement.MAP_BASE_MOVEMENT_NS);
        testSettings.putSetting(MapBasedMovement.NROF_FILES_S, "1");
        testSettings.putSetting("mapFile1","data/Manhattan/roads.wkt");
        testSettings.restoreNameSpace();
        vhm = new VoluntaryHelperMovement(testSettings);
        //needed to initialize MM
        vhm.setHost(new TestUtils(new ArrayList<>(),new ArrayList<>(),
                testSettings).createHost());
        return vhm;
    }

    @Test
    public void testGetInitialLocation(){
        Coord coord = vhm.getInitialLocation();
        assertNotNull(coord);
    }

    @Test
    public void testInitialMovementMode() {
        assertEquals(VoluntaryHelperMovement.movementMode.RANDOM_MAP_BASED_MODE, vhm.getMode());
        assertEquals(ShortestPathMapBasedMovement.class, vhm.getCurrentMovementModel().getClass());
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
}