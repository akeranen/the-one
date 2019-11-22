package movement;

import core.Coord;
import core.Settings;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;
import movement.nodegrid.NodeGridBuilder;
import movement.nodegrid.Polygon;

import java.util.List;

public class NodeGridMovement extends MovementModel implements RenderableMovement {
    private static final String RASTER_INTERVAL = "ngmRasterInterval";

    private SimMap nodeGrid;

    private MapNode currentNode;

    private DijkstraPathFinder pathFinder;

    public NodeGridMovement(Settings settings) {
        super(settings);
        double rasterInterval = settings.getDouble(RASTER_INTERVAL);
        Polygon outerBound = new Polygon(
                new Coord(0, 0),
                new Coord(50, 50),
                new Coord(200, 100),
                new Coord(100, 0)
        );
        nodeGrid = new NodeGridBuilder(rasterInterval).add(outerBound).build();
        pathFinder = new DijkstraPathFinder(null);
    }

    public NodeGridMovement(NodeGridMovement other) {
        super(other);
        nodeGrid = other.nodeGrid;
        pathFinder = other.pathFinder;
    }

    @Override
    public SimMap getMap() {
        return nodeGrid;
    }

    @Override
    public Path getPath() {
        MapNode from = currentNode;
        MapNode to = pickRandomNode(nodeGrid.getNodes());
        currentNode = to;

        List<MapNode> shortestPath = pathFinder.getShortestPath(from, to);

        Path path = new Path();
        if (shortestPath.size() > 0) {
            path.addWaypoint(shortestPath.get(0).getLocation(), 1);
        }
        return path;
    }

    @Override
    public Coord getInitialLocation() {
        currentNode = pickRandomNode(nodeGrid.getNodes());
        return currentNode.getLocation().clone();
    }

    @Override
    public MovementModel replicate() {
        return new NodeGridMovement(this);
    }

    private MapNode pickRandomNode(List<MapNode> graphNodes) {
        int chosenIndex = rng.nextInt(graphNodes.size());
        return graphNodes.get(chosenIndex);
    }
}
