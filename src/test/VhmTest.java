package test;


import core.ConnectionListener;
import core.Coord;
import core.MessageListener;
import movement.MapBasedMovement;
import movement.MovementModel;
import movement.VoluntaryHelperMovement;
import org.junit.Test;

import java.util.ArrayList;

import static junit.framework.Assert.assertNotNull;

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
        vhm.setHost(new TestUtils(new ArrayList<ConnectionListener>(),new ArrayList<MessageListener>(),
                testSettings).createHost());
        return vhm;
    }

    @Test
    public void testGetInitialLocation(){
        Coord coord = vhm.getInitialLocation();
        assertNotNull(coord);
    }
}
