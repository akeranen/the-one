package movement;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import core.SettingsError;
import input.WKTReader;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.MapRoute;
import core.Coord;
import core.Settings;
import movement.map.SimMap;
import static movement.map.MapRoute.CIRCULAR;
import static movement.map.MapRoute.PINGPONG;

/**
 * Map based movement model that uses predetermined paths within the map area.
 * Nodes using this model (can) stop on every route waypoint and find their
 * way to next waypoint using {@link DijkstraPathFinder}. There can be
 * different type of routes; see {@link #ROUTE_TYPE_S}.
 */
public class MapRoomMovement extends MapRouteMovement implements SwitchableMovement {

    /** the Dijkstra shortest path finder */
    private DijkstraPathFinder pathFinder;

    /** Prototype's reference to all routes read for the group */
    private List<MapRoute> allRoutes = null;
    /** next route's index to give by prototype */
    private Integer nextRouteIndex = null;
    /** Index of the first stop for a group of nodes (or -1 for random) */
    private int firstStopIndex = -1;

    /** Route of the movement model's instance */
    private MapRoute route;

    private List<MapRoute> tempRoutes;

    /**
     * Creates a new movement model based on a Settings object's settings.
     * @param settings The Settings object where the settings are read from
     */
    public MapRoomMovement(Settings settings) {
        super(settings);

        String fileName = settings.getSetting(ROUTE_FILE_S);
        int type = settings.getInt(ROUTE_TYPE_S);

        tempRoutes = MapRoute.readRoutes(fileName, type, getMap());

        pathFinder = new DijkstraPathFinder(getOkMapNodeTypes());
        allRoutes = generateRoomRoutes(fileName, type, getMap());

        nextRouteIndex = 0;

        this.route = this.allRoutes.get(this.nextRouteIndex).replicate();

        if (this.nextRouteIndex >= this.allRoutes.size()) {
            this.nextRouteIndex = 0;
        }

        if (settings.contains(ROUTE_FIRST_STOP_S)) {
            this.firstStopIndex = settings.getInt(ROUTE_FIRST_STOP_S);
            if (this.firstStopIndex >= this.route.getNrofStops()) {
                throw new SettingsError("Too high first stop's index (" +
                        this.firstStopIndex + ") for route with only " +
                        this.route.getNrofStops() + " stops");
            }
        }
    }

    /**
     * Copyconstructor. Gives a route to the new movement model from the
     * list of routes and randomizes the starting position.
     * @param proto The MapRouteMovement prototype
     */
    protected MapRoomMovement(MapRoomMovement proto) {
        super(proto);
        this.route = proto.allRoutes.get(proto.nextRouteIndex).replicate();
        this.firstStopIndex = proto.firstStopIndex;

        if (firstStopIndex < 0) {
			/* set a random starting position on the route */
            this.route.setNextIndex(rng.nextInt(route.getNrofStops()-1));
        } else {
			/* use the one defined in the config file */
            this.route.setNextIndex(this.firstStopIndex);
        }

        this.pathFinder = proto.pathFinder;

        proto.nextRouteIndex++; // give routes in order
        if (proto.nextRouteIndex >= proto.allRoutes.size()) {
            proto.nextRouteIndex = 0;
        }
    }

    @Override
    public Path getPath() {
        Path p = new Path(generateSpeed());
        MapNode to = route.nextStop();

        List<MapNode> nodePath = pathFinder.getShortestPath(lastMapNode, to);

        // this assertion should never fire if the map is checked in read phase
        assert nodePath.size() > 0 : "No path from " + lastMapNode + " to " +
                to + ". The simulation map isn't fully connected";

        for (MapNode node : nodePath) { // create a Path from the shortest path
            p.addWaypoint(node.getLocation());
        }

        lastMapNode = to;

        return p;
    }

    @Override
    public MapRoomMovement replicate() {
        return new MapRoomMovement(this);
    }

    private List<MapRoute> generateRoomRoutes(String fileName, int type, SimMap map) {

        List<MapRoute> routes = new ArrayList<>();

        WKTReader reader = new WKTReader();
        List<Coord> points;
        File routeFile;
        boolean mirror = map.isMirrored();
        double xOffset = map.getOffset().getX();
        double yOffset = map.getOffset().getY();

        if (type != CIRCULAR && type != PINGPONG) {
            throw new SettingsError("Invalid route type (" + type + ")");
        }

        try {
            routeFile = new File(fileName);
            points = reader.readPoints(routeFile);
        }
        catch (IOException ioe){
            throw new SettingsError(ioe.getMessage());
        }

        for (Coord point : points) {
            if (mirror) {
                point.setLocation(point.getX(), -point.getY());
            }
            point.translate(xOffset, yOffset);
        }

        HashMap<Coord, MapNode> roomNodes = new HashMap<>();
        for (MapRoute route : tempRoutes) {
            for (MapNode node : route.getStops()) {
                for (Coord point : points) {
                    if (node.getLocation().equals(point)) {
                        roomNodes.put(point, node);
                    }
                }
            }
        }

        for (MapNode i : roomNodes.values()){
            for (MapNode j : roomNodes.values()){
                if (!i.equals(j)) {
                    routes.add(new MapRoute(type, pathFinder.getShortestPath(i, j)));
                }
            }
        }
        return routes;
    }

}
