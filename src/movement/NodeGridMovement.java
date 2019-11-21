package movement;

import core.Coord;
import core.Settings;
import movement.map.MapNode;
import movement.map.SimMap;
import movement.nodegrid.NodeGraph;
import movement.nodegrid.Polygon;

import java.util.List;

public class NodeGridMovement extends MovementModel implements RenderableMovement {
    private static final String RASTER_INTERVAL = "ngmRasterInterval";

    private NodeGraph nodeGraph;

    private MapNode currentNode;

    public NodeGridMovement(Settings settings) {
        super(settings);
        double rasterInterval = settings.getDouble(RASTER_INTERVAL);
        Polygon outerBound = new Polygon(
                new Coord(0, 0),
                new Coord(50, 50),
                new Coord(200, 100),
                new Coord(100, 0)
        );
        nodeGraph = new NodeGraph(outerBound, rasterInterval);
    }

    public NodeGridMovement(NodeGridMovement other) {
        super(other);
        nodeGraph = other.nodeGraph;
    }

    @Override
    public SimMap getMap() {
        return nodeGraph;
    }

    @Override
    public Path getPath() {
        pickRandomNode(currentNode.getNeighbors());
        Path path = new Path();
        path.addWaypoint(currentNode.getLocation().clone(), 1);
        return path;
    }

    @Override
    public Coord getInitialLocation() {
        pickRandomNode(nodeGraph.getNodes());
        return currentNode.getLocation().clone();
    }

    @Override
    public MovementModel replicate() {
        return new NodeGridMovement(this);
    }

    private void pickRandomNode(List<MapNode> graphNodes) {
        int chosenIndex = rng.nextInt(graphNodes.size());
        currentNode = graphNodes.get(chosenIndex);
    }
}
