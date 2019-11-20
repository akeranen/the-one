package movement;

import core.Coord;
import core.Settings;
import movement.nodegrid.GraphNode;
import movement.nodegrid.Polygon;

import java.util.HashSet;
import java.util.Set;

public class NodeGridMovement extends MovementModel {
    private static final String RASTER_INTERVAL = "ngmRasterInterval";

    private Set<GraphNode> graphNodes;

    private GraphNode currentNode;

    public NodeGridMovement(Settings settings) {
        super(settings);
        double rasterInterval = settings.getDouble(RASTER_INTERVAL);
        Polygon outerBound = new Polygon(
                new Coord(0, 0),
                new Coord(100, 0),
                new Coord(100, 100),
                new Coord(0, 100)
        );
        graphNodes = rasterPolygon(outerBound, rasterInterval);
    }

    public NodeGridMovement(NodeGridMovement other) {
        super(other);
        graphNodes = other.graphNodes;
    }

    @Override
    public Path getPath() {
        pickRandomNode(currentNode.getAdjacentNodes());
        Path path = new Path();
        path.addWaypoint(currentNode.getLocation().clone(), 1);
        return path;
    }

    @Override
    public Coord getInitialLocation() {
        pickRandomNode(graphNodes);
        return currentNode.getLocation().clone();
    }

    @Override
    public MovementModel replicate() {
        return new NodeGridMovement(this);
    }

    private void pickRandomNode(Set<GraphNode> graphNodes) {
        int chosenIndex = rng.nextInt(graphNodes.size());
        int index = 0;
        for (GraphNode node: graphNodes) {
            if (index == chosenIndex) {
                currentNode = node;
                return;
            }
            index++;
        }
    }

    /**
     * TODO: raster polygon into hexagonal GraphNodes
     * @param polygon the polygon to raster
     * @param rasterInterval the distance between the centers of two adjacent hexagons
     * @return a set of connected GraphNodes
     */
    private static Set<GraphNode> rasterPolygon(Polygon polygon, double rasterInterval) {
        Set<GraphNode> graphNodes = new HashSet<>();
        GraphNode node1 = new GraphNode(new Coord(0, 0));
        GraphNode node2 = new GraphNode(new Coord(100, 100));
        node1.addAdjacentNode(node2);
        node2.addAdjacentNode(node1);
        graphNodes.add(node1);
        graphNodes.add(node2);
        return graphNodes;
    }
}
