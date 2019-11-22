package movement.nodegrid;

import core.Coord;
import movement.map.MapNode;
import movement.map.SimMap;

import java.util.*;
import java.util.stream.Collectors;

public class NodeGridBuilder {
    private final double rasterInterval;

    private List<Polygon> includedPolygons = new ArrayList<>();

    private List<Polygon> excludedPolygons = new ArrayList<>();

    private Map<MapNode, Integer> numberOfAttachmentsByPointOfInterest = new HashMap<>();

    public NodeGridBuilder(double rasterInterval) {
        this.rasterInterval = rasterInterval;
    }

    public NodeGridBuilder add(Polygon... polygons) {
        includedPolygons.addAll(Arrays.asList(polygons));
        return this;
    }

    public NodeGridBuilder subtract(Polygon... polygons) {
        excludedPolygons.addAll(Arrays.asList(polygons));
        return this;
    }

    public NodeGridBuilder attachNodeByClosestNodes(MapNode node, int numberOfClosestNodes) {
        numberOfAttachmentsByPointOfInterest.put(node, numberOfClosestNodes);
        return this;
    }

    public SimMap build() {
        Map<Coord, MapNode> nodes = new HashMap<>();

        int verticalResolution = getVerticalRasterResolution();
        int horizontalResolution = getHorizontalRasterResolution();
        MapNode[][] raster = new MapNode[verticalResolution][horizontalResolution];

        double verticalStepSize = getVerticalRasterStepSize();
        double horizontalStepSize = getHorizontalRasterStepSize();
        for (int rowIndex = 0; rowIndex < verticalResolution; rowIndex++) {
            for (int columnIndex = 0; columnIndex < horizontalResolution; columnIndex++) {
                double x = columnIndex * horizontalStepSize + getHorizontalRowOffset(rowIndex);
                double y = rowIndex * verticalStepSize;
                Coord location = new Coord(x, y);

                boolean isInsideIncludedPolygon = includedPolygons.stream().anyMatch(p -> p.isInside(location));
                boolean isOutsideExcludedPolygons = excludedPolygons.stream().allMatch(p -> p.isOutside(location));
                if (isInsideIncludedPolygon && isOutsideExcludedPolygons) {
                    MapNode node = new MapNode(location);
                    raster[rowIndex][columnIndex] = node;
                    nodes.put(location, node);
                }
            }
        }

        connectAdjacentNodes(raster);

        SimMap nodeGrid = new SimMap(nodes);
        nodeGrid.translate(getRasterAttachmentPoint().getX(), getRasterAttachmentPoint().getY());
        return nodeGrid;
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

    private Coord getRasterAttachmentPoint() {
        return getRasterBoundingBox().getTopLeft();
    }

    private double getHorizontalRasterStepSize() {
        return getInnerCircleRadius() * 2;
    }

    private double getVerticalRasterStepSize() {
        return getOuterCircleRadius() * 1.5;
    }

    private double getHorizontalRowOffset(int rowIndex) {
        return isRowHorizontallyOffset(rowIndex) ? getInnerCircleRadius() : 0;
    }

    private int getHorizontalRasterResolution() {
        return (int) (getRasterBoundingBox().getWidth() / getHorizontalRasterStepSize());
    }

    private int getVerticalRasterResolution() {
        return (int) (getRasterBoundingBox().getHeight() / getVerticalRasterStepSize());
    }

    private boolean isRowHorizontallyOffset(int rowIndex) {
        return rowIndex % 2 != 0;
    }

    private void connectAdjacentNodes(MapNode[][] raster) {
        for (int rowIndex = 0; rowIndex < raster.length; rowIndex++) {
            for (int columnIndex = 0; columnIndex < raster[rowIndex].length; columnIndex++) {
                MapNode node = raster[rowIndex][columnIndex];
                if (node != null) {
                    getAdjacentNodes(raster, rowIndex, columnIndex).forEach(node::addNeighbor);
                }
            }
        }
    }

    private List<MapNode> getAdjacentNodes(MapNode[][] raster, int rowIndex, int columnIndex) {
        int horizontalRasterOffset = isRowHorizontallyOffset(rowIndex) ? 1 : 0;

        List<Optional<MapNode>> adjacentNodes = Arrays.asList(
                getRasterNode(raster, rowIndex, columnIndex - 1),
                getRasterNode(raster, rowIndex, columnIndex + 1),
                getRasterNode(raster, rowIndex - 1, columnIndex - 1 + horizontalRasterOffset),
                getRasterNode(raster, rowIndex + 1, columnIndex - 1 + horizontalRasterOffset),
                getRasterNode(raster, rowIndex - 1, columnIndex + horizontalRasterOffset),
                getRasterNode(raster, rowIndex + 1, columnIndex + horizontalRasterOffset)
        );

        return adjacentNodes.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private Optional<MapNode> getRasterNode(MapNode[][] raster, int rowIndex, int columnIndex) {
        if (rowIndex < 0 || raster.length <= rowIndex || columnIndex < 0 || raster[rowIndex].length <= columnIndex) {
            return Optional.empty();
        }
        return Optional.ofNullable(raster[rowIndex][columnIndex]);
    }
}
