package test;

import core.Coord;
import core.Settings;
import junit.framework.TestCase;
import movement.SwitchableStationaryMovement;
import org.junit.Test;

/**
 * Contains tests for the switchabel stationary movement
 *
 * Created by Marius Meyer on 01.04.17.
 */
public class SwitchableStationaryMovementTest {

    private Settings settings = new TestSettings();
    private SwitchableStationaryMovement model = new SwitchableStationaryMovement(settings);

    @Test
    public void testSettingsConstructorCreatesMMWithLocationAtCoordinateOrigin(){
        TestCase.assertEquals("Location should be at coordinate origin",
                new Coord(0,0),model.getLastLocation());
    }

    @Test
    public void testCopyConstructorCreatesModelWithSameParameters(){
        model.setLocation(new Coord(1,1));
        SwitchableStationaryMovement copy = new SwitchableStationaryMovement(model);
        checkModelForSameParameters(model,copy);
    }

    @Test
    public void testReplicateShouldReturnModelWithSameParameters(){
        model.setLocation(new Coord(1,1));
        SwitchableStationaryMovement copy = (SwitchableStationaryMovement) model.replicate();
        checkModelForSameParameters(model,copy);
        TestCase.assertNotSame("Model and copy should be two different instances",model,copy);
    }

    @Test
    public void testGetPathShouldReturnNull(){
        TestCase.assertNull(model.getPath());
    }

    @Test
    public void testGetInitialLocationReturnsSetLocation(){
        Coord location = new Coord(1,1);
        model.setLocation(location);
        TestCase.assertEquals("Model should return set location",location,model.getInitialLocation());
    }

    @Test
    public void testGetLastLocationReturnsSetLocation(){
        Coord location = new Coord(1,1);
        model.setLocation(location);
        TestCase.assertEquals("Model should return set location",location,model.getLastLocation());
    }

    @Test
    public void testSetLocationShouldSetLocaton(){
        TestCase.assertEquals(new Coord(0,0),model.getInitialLocation());
        model.setLocation(new Coord(1,1));
        TestCase.assertEquals(new Coord(1,1),model.getInitialLocation());
    }

    @Test
    public void testIsReadyReturnsFalse(){
        TestCase.assertFalse(model.isReady());
    }

    private static void checkModelForSameParameters(SwitchableStationaryMovement original,
                                                    SwitchableStationaryMovement copy){
        TestCase.assertEquals("Initial location should be the same",
                original.getInitialLocation(),copy.getInitialLocation());
        TestCase.assertEquals("Last location should be the same",
                original.getLastLocation(),copy.getLastLocation());
        TestCase.assertEquals("Returned path should be the same",
                original.getPath(),copy.getPath());
        TestCase.assertEquals("Is ready should return the same",original.isReady(),copy.isReady());
    }
}
