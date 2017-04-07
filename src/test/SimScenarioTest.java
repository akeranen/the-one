package test;

import core.Settings;
import core.SimScenario;
import junit.framework.TestCase;
import movement.VoluntaryHelperMovement;
import movement.map.SimMap;
import org.junit.Before;
import org.junit.Test;

/**
 * Contains tests for the changes done on {@link SimScenario} for the VoluntaryHelperMovement
 *
 * Created by Marius Meyer on 01.04.17.
 */
public class SimScenarioTest {

    private static final String MM_PACKAGE = "movement.";
    private static final String SETTINGS_TEST_FILE_PATH = "configurations/VoluntaryHelperMovementTest.txt";
    private Settings settings = new Settings();

    @Before
    public void loadTestSettingsForVhm(){
        Settings.init(SETTINGS_TEST_FILE_PATH);
    }


    @Test
    public void testMapIsSetCorrectlyAfterCreatingHostsWhenVhmIsUsed(){
        SimMap scenarioMap = SimScenario.getInstance().getMap();
        settings.setNameSpace(SimScenario.GROUP_NS);
        SimMap originalMap =
                ((VoluntaryHelperMovement) settings.createIntializedObject(MM_PACKAGE +
                        settings.getSetting(SimScenario.MOVEMENT_MODEL_S))).getMap();
        TestCase.assertEquals("Map offered by VHM and SimScenario should be the same",
                originalMap,scenarioMap);
    }
}
