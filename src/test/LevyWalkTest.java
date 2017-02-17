package test;

import core.Coord;
import core.Settings;
import movement.LevyWalkMovement;
import movement.Path;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;


/**
 *
 * Test for Levy Walk movement model.
 *
 * @author MelanieBruns
 */
public class LevyWalkTest{

    private LevyWalkMovement levy = new LevyWalkMovement(new TestSettings());
    private static final int MAX_COORD = 1000;
    private static final int TEST_RUNS = 2000;
    private static final int TEST_RADIUS = 100;
    private static final int CENTER_COORD = 500;
    private static final int EDGE_COORD = 50;


    @Before
    public void setUp(){
        Settings.init(null);
        java.util.Locale.setDefault(java.util.Locale.US);
        TestSettings testSettings = new TestSettings();
        levy = new LevyWalkMovement(testSettings);
        levy.getInitialLocation();
    }

    @Test
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
        assertTrue( nextWaypoint.getX()<=MAX_COORD);
        assertTrue( nextWaypoint.getY()<=MAX_COORD);
    }

    @Test
    public void testStaysWithinDefaultBounds(){
        for (int i=1; i<TEST_RUNS; i++){
            Path p = levy.getPath();
            List<Coord> coords = p.getCoords();
            Coord nextWaypoint = coords.get(1);
            assertNotNull(nextWaypoint);
            //Bounds in test Settings are 1000x1000
            assertTrue( nextWaypoint.getX()>=0);
            assertTrue( nextWaypoint.getY()>=0);
            assertTrue( nextWaypoint.getX()<=MAX_COORD);
            assertTrue( nextWaypoint.getY()<=MAX_COORD);
        }
    }

    @Test
    public void testStaysWithinRadius(){
        boolean success = levy.setRadius(TEST_RADIUS);
        assertTrue(success);
        Coord center = new Coord(CENTER_COORD, CENTER_COORD);
        success = levy.setCenter(center);
        assertTrue(success);
        for (int i=1; i<TEST_RUNS; i++){
            Path p = levy.getPath();
            List<Coord> coords = p.getCoords();
            Coord nextWaypoint = coords.get(1);
            assertNotNull(nextWaypoint);
            //Bounds in test Settings are 1000x1000
            assertTrue(center.distance(nextWaypoint)<=TEST_RADIUS);
        }
    }

    @Test
    public void testStaysWithinRadiusAndSimulationArea(){
        //Set movement area, so that it is partially cut by simulation area bounds
        boolean success = levy.setRadius(TEST_RADIUS);
        assertTrue(success);
        Coord center = new Coord(EDGE_COORD, CENTER_COORD);
        success = levy.setCenter(center);
        assertTrue(success);
        for (int i=1; i<TEST_RUNS; i++){
            Path p = levy.getPath();
            List<Coord> coords = p.getCoords();
            Coord nextWaypoint = coords.get(1);
            assertNotNull(nextWaypoint);
            //Bounds in test Settings are 1000x1000
            assertTrue(center.distance(nextWaypoint)<=TEST_RADIUS);
            assertTrue( nextWaypoint.getX()>=0);
            assertTrue( nextWaypoint.getY()>=0);
            assertTrue( nextWaypoint.getX()<=MAX_COORD);
            assertTrue( nextWaypoint.getY()<=MAX_COORD);
        }
    }

}
