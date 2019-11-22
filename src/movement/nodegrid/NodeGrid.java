package movement.nodegrid;

import core.Coord;
import movement.map.MapNode;
import movement.map.SimMap;

import java.util.*;

public class NodeGrid extends SimMap {

    private NodeGrid(Map<Coord, MapNode> nodes) {
        super(nodes);
    }

    @Deprecated
    private NodeGrid(Polygon outerBound, double rasterInterval) {
        super(rasterPolygon(outerBound, rasterInterval));
    }

    public static class Builder {
        private final double rasterInterval;

        private List<Polygon> includedPolygons = new ArrayList<>();

        private List<Polygon> excludedPolygons = new ArrayList<>();

        private Map<MapNode, Integer> numberOfAttachmentsByPointOfInterest = new HashMap<>();

        public Builder(double rasterInterval) {
            this.rasterInterval = rasterInterval;
        }

        public Builder add(Polygon... polygons) {
            includedPolygons.addAll(Arrays.asList(polygons));
            return this;
        }

        public Builder subtract(Polygon... polygons) {
            excludedPolygons.addAll(Arrays.asList(polygons));
            return this;
        }

        public Builder attachNodeByClosestNodes(MapNode node, int numberOfClosestNodes) {
            numberOfAttachmentsByPointOfInterest.put(node, numberOfClosestNodes);
            return this;
        }

        public NodeGrid build() {
            // TODO
            if (includedPolygons.size() == 0) {
                return new NodeGrid(new HashMap<>());
            }
            return new NodeGrid(includedPolygons.get(0), rasterInterval);
        }

        private BoundingBox getRasterBoundingBox() {
            BoundingBox[] boundingBoxes = includedPolygons.stream()
                    .map(Polygon::getBoundingBox)
                    .toArray(BoundingBox[]::new);

            return BoundingBox.merge(boundingBoxes);
        }

        private double getInnerCircleRadius() {
            return rasterInterval / 2;
        }

        private double getOuterCircleRadius() {
            return getInnerCircleRadius() / Math.cos(Math.toRadians(30));
        }
    }

    private static Map<Coord, MapNode> rasterPolygon(Polygon outerBound, double rasterInterval) {
        // hexagon parameters
        double innerRadius = rasterInterval / 2;
        double outerRadius = innerRadius / Math.cos(Math.toRadians(30));

        BoundingBox boundingBox = outerBound.getBoundingBox();
        double width = boundingBox.getWidth();
        double height = boundingBox.getHeight();

        // determine raster size
        int horizontalSteps = (int) Math.floor(width / (innerRadius * 2));
        int verticalSteps = (int) Math.floor(height / (outerRadius * 1.5));

        Map<Coord, MapNode> nodes = new HashMap<>();
        MapNode[][] raster = new MapNode[verticalSteps][horizontalSteps];
        Coord anchor = boundingBox.getTopLeft();

        // raster polygon into hexagons
        for (int row = 0; row < verticalSteps; row++) {
            double horizontalOffset = isRowOffset(row) ? innerRadius : 0;
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

    private static boolean isRowOffset(int row) {
        return row % 2 != 0;
    }

    private static List<MapNode> getAdjacentNodes(MapNode[][] mapNodes, int row, int column) {
        List<MapNode> adjacentNodes = new ArrayList<>();

        getNode(mapNodes, row, column - 1).ifPresent(adjacentNodes::add);
        getNode(mapNodes, row, column + 1).ifPresent(adjacentNodes::add);
        getNode(mapNodes, row - 1, column).ifPresent(adjacentNodes::add);
        getNode(mapNodes, row + 1, column).ifPresent(adjacentNodes::add);

        if (isRowOffset(row)) {
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
