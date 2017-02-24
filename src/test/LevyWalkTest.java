package test;

import core.Coord;
import movement.LevyWalkMovement;
import movement.MovementModel;
import movement.Path;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;


/**
 *
 * Tests for Levy Walk movement model.
 *
 * @author MelanieBruns
 */
public class LevyWalkTest extends AbstractMovementModelTest{

    private LevyWalkMovement levy = new LevyWalkMovement(new TestSettings());
    private static final int TEST_RUNS = 2000;
    private static final int TEST_RADIUS = 100;
    private static final int CENTER_COORD = 500;
    private static final int EDGE_COORD = 50;

    public MovementModel initializeModel(TestSettings settings){
        levy = new LevyWalkMovement(settings);
        return levy;
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
            assertNotNull("Path did not include a valid next waypoint.", nextWaypoint);
            //Bounds in test Settings are 1000x1000
            assertTrue("Path was outside the allowed radius.", center.distance(nextWaypoint)<=TEST_RADIUS);

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
            assertNotNull("Path did not include a valid next waypoint.",nextWaypoint);
            //Bounds in test Settings are 1000x1000
            assertTrue("Path was outside the radius.",center.distance(nextWaypoint)<=TEST_RADIUS);
            assertTrue(MESSAGE_X_BELOW_ZERO, nextWaypoint.getX()>=0);
            assertTrue(MESSAGE_Y_BELOW_ZERO,nextWaypoint.getY()>=0);
            assertTrue(MESSAGE_X_ABOVE_LIMIT,nextWaypoint.getX()<=MAX_COORD);
            assertTrue(MESSAGE_Y_ABOVE_LIMIT, nextWaypoint.getY()<=MAX_COORD);
        }
    }

}
