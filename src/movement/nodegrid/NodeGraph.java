package movement.nodegrid;

import core.Coord;
import movement.map.MapNode;
import movement.map.SimMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class NodeGraph extends SimMap {

    public NodeGraph(Polygon outerBound, double rasterInterval) {
        super(rasterPolygon(outerBound, rasterInterval));
    }

    private static Map<Coord, MapNode> rasterPolygon(Polygon outerBound, double rasterInterval) {
        Polygon.BoundingBox boundingBox = outerBound.getBoundingBox();
        Coord anchor = boundingBox.getTopLeft();

        double width = boundingBox.getBottomRight().getX() - boundingBox.getTopLeft().getX();
        double height = boundingBox.getBottomRight().getY() - boundingBox.getTopLeft().getY();
        double innerRadius = rasterInterval / 2;
        double outerRadius = innerRadius / Math.cos(Math.toRadians(30));

        int horizontalSteps = (int) Math.floor(width / (innerRadius * 2));
        int verticalSteps = (int) Math.floor(height / (outerRadius * 1.5));

        Map<Coord, MapNode> nodes = new HashMap<>();
        MapNode[][] raster = new MapNode[verticalSteps][horizontalSteps];

        for (int row = 0; row < verticalSteps; row++) {
            double horizontalOffset = row % 2 == 0 ? 0 : innerRadius;
            double y = anchor.getY() + row * 1.5 * outerRadius;
            for (int column = 0; column < horizontalSteps; column++) {
                double x = anchor.getX() + horizontalOffset + column * 2 * innerRadius;
                Coord location = new Coord(x, y);
                if (outerBound.isInside(location)) {
                    MapNode node = new MapNode(location);
                    raster[row][column] = node;
                    nodes.put(location, node);
                }
            }
        }

        for (int row = 0; row < verticalSteps; row++) {
            for (int column = 0; column < horizontalSteps; column++) {
                MapNode node = raster[row][column];
                if (node == null) {
                    continue;
                }
                if (row % 2 == 0) {
                    getNode(raster, row, column - 1).ifPresent(node::addNeighbor);
                    getNode(raster, row, column + 1).ifPresent(node::addNeighbor);
                    getNode(raster, row - 1, column - 1).ifPresent(node::addNeighbor);
                    getNode(raster, row - 1, column).ifPresent(node::addNeighbor);
                    getNode(raster, row + 1, column - 1).ifPresent(node::addNeighbor);
                    getNode(raster, row + 1, column).ifPresent(node::addNeighbor);
                } else {
                    getNode(raster, row, column - 1).ifPresent(node::addNeighbor);
                    getNode(raster, row, column + 1).ifPresent(node::addNeighbor);
                    getNode(raster, row - 1, column).ifPresent(node::addNeighbor);
                    getNode(raster, row - 1, column + 1).ifPresent(node::addNeighbor);
                    getNode(raster, row + 1, column).ifPresent(node::addNeighbor);
                    getNode(raster, row + 1, column + 1).ifPresent(node::addNeighbor);
                }
            }
        }

        return nodes;
    }

    private static Optional<MapNode> getNode(MapNode[][] mapNodes, int row, int column) {
        if (row < 0 || mapNodes.length <= row || column < 0 || mapNodes[0].length <= column) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapNodes[row][column]);
    }
}
