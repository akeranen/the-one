package movement.nodegrid;

import core.Coord;
import movement.map.MapNode;
import movement.map.SimMap;

import java.util.*;

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

        // raster polygon into hexagons
        for (int row = 0; row < verticalSteps; row++) {
            double horizontalOffset = isOffsetRow(row) ? innerRadius : 0;
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

        // add edges between nodes
        for (int row = 0; row < verticalSteps; row++) {
            for (int column = 0; column < horizontalSteps; column++) {
                MapNode node = raster[row][column];
                if (node == null) {
                    continue;
                }
                getAdjacentNodes(raster, row, column).forEach(node::addNeighbor);
            }
        }

        return nodes;
    }

    private static boolean isOffsetRow(int row) {
        return row % 2 != 0;
    }

    private static List<MapNode> getAdjacentNodes(MapNode[][] mapNodes, int row, int column) {
        List<MapNode> adjacentNodes = new ArrayList<>();
        getNode(mapNodes, row, column - 1).ifPresent(adjacentNodes::add);
        getNode(mapNodes, row, column + 1).ifPresent(adjacentNodes::add);
        getNode(mapNodes, row - 1, column).ifPresent(adjacentNodes::add);
        getNode(mapNodes, row + 1, column).ifPresent(adjacentNodes::add);

        if (isOffsetRow(row)) {
            getNode(mapNodes, row - 1, column + 1).ifPresent(adjacentNodes::add);
            getNode(mapNodes, row + 1, column + 1).ifPresent(adjacentNodes::add);
        } else {
            getNode(mapNodes, row - 1, column - 1).ifPresent(adjacentNodes::add);
            getNode(mapNodes, row + 1, column - 1).ifPresent(adjacentNodes::add);
        }

        return adjacentNodes;
    }

    private static Optional<MapNode> getNode(MapNode[][] mapNodes, int row, int column) {
        if (row < 0 || mapNodes.length <= row || column < 0 || mapNodes[0].length <= column) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapNodes[row][column]);
    }
}
