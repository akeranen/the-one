package test;


import core.Coord;
import movement.MovementModel;
import movement.VoluntaryHelperMovement;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

/**
 * Tests for VoluntaryHelperMovement
 * Created by melanie on 31.03.17.
 */
public class VhmTest extends AbstractMovementModelTest{

    private VoluntaryHelperMovement vhm;

    @Override
    public MovementModel initializeModel(TestSettings testSettings) {
        testSettings.setNameSpace("MapBasedMovement");
        testSettings.putSetting("nrofMapFiles", "3");
        testSettings.restoreNameSpace();
        vhm = new VoluntaryHelperMovement(testSettings);
        return vhm;
    }

    @Test
    public void testGetInitialLocation(){
        Coord coord = vhm.getInitialLocation();
        assertNotNull(coord);
    }
}
