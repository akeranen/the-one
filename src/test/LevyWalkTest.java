package test;

import core.Coord;
import movement.LevyWalkMovement;
import movement.MovementModel;
import movement.Path;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertNotNull;


/**
 *
 * Tests for Levy Walk movement model.
 *
 * @author MelanieBruns
 */
public class LevyWalkTest extends AbstractMovementModelTest{

    private LevyWalkMovement levy = new LevyWalkMovement(new TestSettings());
    // Radius around the center the nodes should stay in
    private static final int TEST_RADIUS = 100;
    // Coordinate of the center of the 1000x1000 Simulation Area
    private static final int CENTER_COORD = 4000;
    // Coordinate at the edge of the simulation area, so that we can test whether nodes walk out of the simulation
    private static final int EDGE_COORD = 50;
    // Illegal radius to check whether we can set bad values as a radius
    private static final int NEGATIVE_RADIUS=-1;
    //Coordinates outside the simulation area, we should not be able to set these as a center
    private static final int COORD_ABOVE_BOUNDS=10001;
    private static final int COORD_BELOW_BOUNDS=-1;
    private static final String CENTER_OUTSIDE_AREA="Coordinate outside simulation area could be set as center";

    public MovementModel initializeModel(TestSettings settings){
        levy = new LevyWalkMovement(settings);
        return levy;
    }

    @Test
    /**
     * Nodes using levy walk should stay within a predefined radius around
     * a central point.
     * The radius should not be negative or zero.
     * It has to be possible to set a valid value.
     */
    public void testSetRadius(){
        assertTrue("Correct radius could not be set", levy.setRadius(TEST_RADIUS));
        assertFalse("Negative radius could be set",levy.setRadius(NEGATIVE_RADIUS));
        assertFalse("Radius could be set to zero",levy.setRadius(0));
    }

    @Test
    /**
     * Nodes using levy walk should stay within a predefined radius around
     * a central point.
     * The center should be within the simulation bounds.
     */
    public void testSetCenter(){
        Coord centerCoord = new Coord(CENTER_COORD, CENTER_COORD);
        assertTrue("Correct center could not be set", levy.setCenter(centerCoord));
        Coord edgeCoord = new Coord(EDGE_COORD, EDGE_COORD);
        assertTrue("Correct center could not be set", levy.setCenter(edgeCoord));
        Coord oneValueAboveCoord = new Coord(CENTER_COORD, COORD_ABOVE_BOUNDS);
        assertFalse(CENTER_OUTSIDE_AREA, levy.setCenter(oneValueAboveCoord));
        Coord bothValuesAboveCoord = new Coord(COORD_ABOVE_BOUNDS, COORD_ABOVE_BOUNDS);
        assertFalse(CENTER_OUTSIDE_AREA, levy.setCenter(bothValuesAboveCoord));
        Coord oneValueBelowCoord = new Coord(CENTER_COORD, COORD_BELOW_BOUNDS);
        assertFalse(CENTER_OUTSIDE_AREA, levy.setCenter(oneValueBelowCoord));
        Coord bothValuesBelowCoord = new Coord(COORD_BELOW_BOUNDS, COORD_BELOW_BOUNDS);
        assertFalse(CENTER_OUTSIDE_AREA, levy.setCenter(bothValuesBelowCoord));
    }

    @Test
    /**
     * Nodes using levy walk should stay within a predefined radius around
     * a central point.
     */
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
    /**
     * Creates the radius on the side of the simulation area, so that the circle is cut by
     * the bounds of the simulation area. Nodes should stay within that area when using levy
     * walk.
     *
     */
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
            //Bounds in test Settings are 10000x8000
            assertTrue("Path was outside the radius.",center.distance(nextWaypoint)<=TEST_RADIUS);
            assertTrue(MESSAGE_X_BELOW_ZERO, nextWaypoint.getX()>=0);
            assertTrue(MESSAGE_Y_BELOW_ZERO,nextWaypoint.getY()>=0);
            assertTrue(MESSAGE_X_ABOVE_LIMIT,nextWaypoint.getX()<=MAX_COORD_X);
            assertTrue(MESSAGE_Y_ABOVE_LIMIT, nextWaypoint.getY()<=MAX_COORD_Y);
        }
    }

}
