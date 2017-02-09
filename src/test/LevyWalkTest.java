package test;

import core.Settings;
import junit.framework.TestCase;
import movement.MovementModel;
import movement.Path;
import movement.LevyWalkMovement;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

/**
 *
 * Test for Levy Walk movement model.
 *
 * @author MelanieBruns
 */
public class LevyWalkTest extends TestCase{

    private static MovementModel levy;

    @Before
    @Override
    public void setUp(){
        Settings.init(null);
        TestSettings testSettings = new TestSettings();
        levy = new LevyWalkMovement(testSettings);
    }

    public void testGetPath(){
        Path path = levy.getPath();
        assertNotNull(path);

    }

}
