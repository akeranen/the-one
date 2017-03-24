package test;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import movement.MovementModel;
import movement.Path;
import movement.map.MapNode;
import movement.map.PanicMovementUtil;
import movement.map.SimMap;
import movement.PanicMovement;
import core.Coord;
import core.DTNHost;
import core.Settings;

/**
 * JUnit Tests for the class PanicMovement
 **/
public class PanicMovementTest extends TestCase {

    private static final int NR_OF_MAP_NODES = 8;

    private MapNode[] node = new MapNode[NR_OF_MAP_NODES];
    private MapNode event;

    private PanicMovement panicMovement;
    private SimMap map;
    private TestSettings settings;
    private DTNHost host;

    /**
     * Constructor. It is called before every test.
     */
    public PanicMovementTest() {
        setupMapDataAndBasicSettings();
        host = setupHost();
        panicMovement.setHost(host);
    }

    /**
     * This method creates the node topology according to this draft:
     * <p>
     * | 1   2   3   4
     * _|_______________
     * 0|     n6--n4
     * |     |   |
     * 1| n0--n1--n5--n2
     * | |
     * 2| n3
     * |  |
     * 3| n7
     **/
    private static void createTopology(MapNode[] node) {
        node[0].addNeighbor(node[1]);
        node[0].addNeighbor(node[3]);
        node[1].addNeighbor(node[0]);
        node[1].addNeighbor(node[5]);
        node[1].addNeighbor(node[6]);
        node[2].addNeighbor(node[5]);
        node[3].addNeighbor(node[0]);
        node[3].addNeighbor(node[7]);
        node[4].addNeighbor(node[5]);
        node[4].addNeighbor(node[6]);
        node[5].addNeighbor(node[1]);
        node[5].addNeighbor(node[2]);
        node[5].addNeighbor(node[4]);
        node[6].addNeighbor(node[1]);
        node[6].addNeighbor(node[4]);
    }

    /**
     * Sets up the map described above, the panic movement and the event, as well as speed and wait time settings
     */
    private void setupMapDataAndBasicSettings() {
        Settings.init(null);

        settings = new TestSettings();
        settings.putSetting(MovementModel.SPEED, "1,1");
        settings.putSetting(MovementModel.WAIT_TIME, "0,0");

        Coord[] coord = new Coord[] {
                new Coord(1, 1),
                new Coord(2, 1),
                new Coord(4, 1),
                new Coord(1, 2),
                new Coord(3, 0),
                new Coord(3, 1),
                new Coord(2, 0),
                new Coord(1, 3)
        };

        Map<Coord, MapNode> cmMap = new HashMap<>();
        for (int i = 0; i < NR_OF_MAP_NODES; i++) {
            node[i] = new MapNode(coord[i]);
            cmMap.put(coord[i], node[i]);
        }

        map = new SimMap(cmMap);
        createTopology(node);
        event = map.getNodeByCoord(new Coord(2, 1));
        panicMovement = new PanicMovement(settings, map, 3);
        panicMovement.setEventLocation(event.getLocation());
        panicMovement.setSafeRange(1.5);
    }

    /**
     * Tests if the host does not move towards the event
     */
    public void testEventDirection() {
        Path path = panicMovement.getPath();
        MapNode start = map.getNodeByCoord(path.getCoords().get(0));
        MapNode end = map.getNodeByCoord(path.getCoords().get(path.getCoords().size() - 1));

        assertTrue("Host should not move towards the event",
                !PanicMovementUtil.isInEventDirection(start, end, panicMovement.getEventLocation()));
    }

    /**
     * Tests if the target node is inside the safe area
     */
    public void testSafeRegion() {

        Path path = panicMovement.getPath();
        MapNode end = map.getNodeByCoord(path.getCoords().get(path.getCoords().size() - 1));

        assertTrue("Target node should be inside the safe area",
                end.getLocation().distance(event.getLocation())
                        >= panicMovement.getSafeRange());
    }

    /**
     * Test if closest possible node to the host is selected
     */
    public void testOptimizationCriterion() {

        Path path = panicMovement.getPath();
        MapNode start = map.getNodeByCoord(path.getCoords().get(0));
        MapNode end = map.getNodeByCoord(path.getCoords().get(path.getCoords().size() - 1));

        for (MapNode m : node) {
            if (end.getLocation().distance(panicMovement.getHost().getLocation())
                    > m.getLocation().distance(panicMovement.getHost().getLocation())) {
                assertTrue("Closest possible node to the host should be selected",
                        m.getLocation().distance(event.getLocation())
                                < panicMovement.getSafeRange()
                                || PanicMovementUtil.isInEventDirection(start, m, panicMovement.getEventLocation()));
            }
        }
    }

    /**
     * Tests if a node in the safe region stays there
     */
    public void testStayInSafeRegion() {
        host.setLocation(new Coord(4, 1));
        Path path = panicMovement.getPath();
        assertTrue("Nodes in the safe area should not move", path.getCoords().size() == 1);
    }

    /**
     * Creates a host for the map
     *
     * @return created host
     */
    private DTNHost setupHost() {
        TestUtils utils = new TestUtils(null, null, settings);
        DTNHost h1 = utils.createHost(panicMovement, null);

        // get a path for the node
        h1.move(0);
        // move node directly to first waypoint
        h1.setLocation(h1.getPath().getCoords().get(0));
        panicMovement.setLocation(h1.getLocation());
        return h1;
    }
}
