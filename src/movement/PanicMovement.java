package movement;

import java.util.List;

import core.Coord;
import core.Settings;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.PanicMovementUtil;
import movement.map.SimMap;

/**
 * @author Nils Weidmann
 *         Implementation of the Panic Mobility Model. If an event occurs, all DTNHosts move towards the "Safe Zone"
 *         between the save range radius and the event range radius. Furthermore, they do not cross the event, such that
 *         the angle between them, the event and the target is at most 90°
 */
public class PanicMovement extends MapBasedMovement {


    private PanicMovementUtil pmu;
    private static final double SAFE_RANGE_RADIUS = 1000.0;
    private static final double EVENT_RANGE_RADIUS = 1500.0;
    private static final double COORD1000 = 1500.0;

    private DijkstraPathFinder pathFinder;

    /**
     * Constructor setting values for the event and the minimum and maximum distance to
     * an event (SAFE_RANGE_RADIUS, EVENT_RANGE_RADIUS).
     *
     * @param settings Settings for the map, hosts etc.
     */
    public PanicMovement(Settings settings) {
        super(settings);
        pmu = new PanicMovementUtil(new Coord(COORD1000, COORD1000), SAFE_RANGE_RADIUS, EVENT_RANGE_RADIUS);
        pathFinder = new DijkstraPathFinder(getOkMapNodeTypes());
    }

    /**
     * Copyconstructor.
     *
     * @param pm The PanicMovement prototype to base
     *           the new object to
     */
    protected PanicMovement(PanicMovement pm) {
        super(pm);
        pmu = new PanicMovementUtil(pm.pmu.getEventLocation(),
                                    pm.pmu.getSafeRangeRadius(), pm.pmu.getEventRangeRadius());
        this.pathFinder = pm.pathFinder;
    }

    /**
     * Additional constructor for JUnit Tests
     *
     * @param settings Settings for the map, hosts etc.
     * @param newMap   Map passed instead of reading it from a file
     * @param nrofMaps Number of WKT files
     */
    public PanicMovement(Settings settings, SimMap newMap, int nrofMaps,
                         Coord location, double safeRangeRadius, double eventRangeRadius) {
        super(settings, newMap, nrofMaps);
        pmu = new PanicMovementUtil(location, safeRangeRadius, eventRangeRadius);
        pathFinder = new DijkstraPathFinder(getOkMapNodeTypes());
    }

    /**
     * Determines a path to the most suitable node in the security zone
     */
    @Override
    public Path getPath() {
        Path path = new Path(generateSpeed());
        MapNode hostNode = getNearestNode(getMap(), getLastLocation());
        MapNode destNode = pmu.selectDestination(getMap(), hostNode);

        List<MapNode> nodePath = pathFinder.getShortestPath(hostNode, destNode);

        for (MapNode node : nodePath) {
            // create a Path from the shortest path
            path.addWaypoint(node.getLocation());
        }

        lastMapNode = destNode;

        return path;
    }

    @Override
    public PanicMovement replicate() {
        return new PanicMovement(this);
    }

    public PanicMovementUtil getPanicMovementUtil() {
        return pmu;
    }

    /**
     * Determines the next node to a given location
     *
     * @param map      Map which contains coordinates
     * @param location Location to find the next node to
     * @return the next node to parameter location
     */
    private static MapNode getNearestNode(SimMap map, Coord location) {
        List<MapNode> list = map.getNodes();
        MapNode bestNode = null;
        double distance;
        double bestDistance = 0;

        for (MapNode node : list) {
            distance = location.distance(node.getLocation());
            if (bestNode == null || distance < bestDistance) {
                bestNode = node;
                bestDistance = distance;
            }
        }
        return bestNode;
    }

}
