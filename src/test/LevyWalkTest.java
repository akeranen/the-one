package test;

import core.Coord;
import core.Settings;
import junit.framework.TestCase;
import movement.LevyWalkMovement;
import movement.Path;
import org.junit.Before;
import util.LevyDistribution;

import java.util.List;


/**
 *
 * Test for Levy Walk movement model.
 *
 * @author MelanieBruns
 */
public class LevyWalkTest extends TestCase{

    private LevyWalkMovement levy;

    @Before
    @Override
    public void setUp(){
        Settings.init(null);
        java.util.Locale.setDefault(java.util.Locale.US);
        TestSettings testSettings = new TestSettings();
        levy = new LevyWalkMovement(testSettings);
        levy.getInitialLocation();
    }

    public void testGetPath(){
        Path path = levy.getPath();
        assertNotNull(path);
        List<Coord> coords = path.getCoords();
        //Get the first point on the path
        Coord nextWaypoint = coords.get(1);
        assertNotNull(nextWaypoint);
        //Bounds in test Settings are 1000x1000
        assertTrue( nextWaypoint.getX()>=0);
        assertTrue( nextWaypoint.getY()>=0);
        assertTrue( nextWaypoint.getX()<=1000);
        assertTrue( nextWaypoint.getY()<=1000);
    }

    public void testStaysWithinDefaultBounds(){
        for (int i=1; i<2000; i++){
            Path p = levy.getPath();
            List<Coord> coords = p.getCoords();
            Coord nextWaypoint = coords.get(1);
            assertNotNull(nextWaypoint);
            //Bounds in test Settings are 1000x1000
            assertTrue( nextWaypoint.getX()>=0);
            assertTrue( nextWaypoint.getY()>=0);
            assertTrue( nextWaypoint.getX()<=1000);
            assertTrue( nextWaypoint.getY()<=1000);
        }
    }

    public void testStaysWithinRadius(){
        double radius = 100;
        boolean success = levy.setRadius(radius);
        assertTrue(success);
        Coord center = new Coord(400,500);
        success = levy.setCenter(center);
        assertTrue(success);
        for (int i=1; i<2000; i++){
            Path p = levy.getPath();
            List<Coord> coords = p.getCoords();
            Coord nextWaypoint = coords.get(1);
            assertNotNull(nextWaypoint);
            //Bounds in test Settings are 1000x1000
            assertTrue(center.distance(nextWaypoint)<=radius);
        }
    }


    public void testStaysWithinRadiusAndSimulationArea(){
        //Set movement area, so that it is partially cut by simulation area bounds
        double radius = 100;
        boolean success = levy.setRadius(radius);
        assertTrue(success);
        Coord center = new Coord(50,500);
        success = levy.setCenter(center);
        assertTrue(success);
        for (int i=1; i<2000; i++){
            Path p = levy.getPath();
            List<Coord> coords = p.getCoords();
            Coord nextWaypoint = coords.get(1);
            assertNotNull(nextWaypoint);
            //Bounds in test Settings are 1000x1000
            assertTrue(center.distance(nextWaypoint)<=radius);
            assertTrue( nextWaypoint.getX()>=0);
            assertTrue( nextWaypoint.getY()>=0);
            assertTrue( nextWaypoint.getX()<=1000);
            assertTrue( nextWaypoint.getY()<=1000);
        }
    }

}
