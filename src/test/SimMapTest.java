package test;

import core.Coord;
import junit.framework.TestCase;
import movement.map.MapNode;
import movement.map.SimMap;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains tests for {@link SimMap} functions added for the
 * Voluntary Helper Movement
 *
 * Created by Marius Meyer on 05.04.17.
 */
public class SimMapTest {

    private static final String CLOSEST_NODE_MESSAGE = "The closest node should be the node at position (0,0)";

    //coordinates used for the second MapNode far away
    private static final double BIG_X_COORD  = 500;
    private static final double BIG_Y_COORD  = 500;

    //coordinate of the MapNode that is searched for
    private static final Coord ZERO_COORD = new Coord(0,0);
    //start point to search from
    private static final Coord SEARCH_COORD = new Coord(1,1);

    private static final Coord FAR_AWAY_COORD = new Coord(BIG_X_COORD,BIG_Y_COORD);

    private SimMap map;

    public SimMapTest(){
        //set up is done in function annotated with @Before
    }

    @Before
    public void prepareSimMap(){
        //create two example MapNodes
        Map<Coord,MapNode> nodes = new HashMap<>();
        nodes.put(ZERO_COORD,new MapNode(ZERO_COORD));
        nodes.put(FAR_AWAY_COORD,new MapNode(FAR_AWAY_COORD));

        map = new SimMap(nodes);
    }

    @Test
    public void testGetClosestNodeByCoordDirectlyOnNode(){
        TestCase.assertEquals(CLOSEST_NODE_MESSAGE,
                map.getNodeByCoord(ZERO_COORD),map.getClosestNodeByCoord(ZERO_COORD));
    }

    @Test
    public void testGetClosestNodeByCoordNotDirectlyOnNode(){
        TestCase.assertEquals(CLOSEST_NODE_MESSAGE,
                map.getNodeByCoord(ZERO_COORD),map.getClosestNodeByCoord(SEARCH_COORD));
    }

    @Test
    public void testGetClosestNodeWithZeroMapNodesReturnsNull(){
        SimMap emptyMap = new SimMap(new HashMap<>());
        TestCase.assertNull("null should be returned on empty map",
                emptyMap.getClosestNodeByCoord(SEARCH_COORD));
    }

}
