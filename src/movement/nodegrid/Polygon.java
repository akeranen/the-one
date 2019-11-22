package movement.nodegrid;

import core.Coord;

public class Polygon {
    private Coord[] vertices;

    public Polygon(Coord... vertices) {
        this.vertices = vertices;
    }

    public boolean isInside(Coord point) {
        if (vertices.length < 3) {
            return false;
        }

        BoundingBox boundingBox = getBoundingBox();
        if (point.getX() < boundingBox.getTopLeft().getX()
                || point.getY() < boundingBox.getTopLeft().getY()
                || boundingBox.getBottomRight().getX() < point.getX()
                || boundingBox.getBottomRight().getY() < point.getY()) {
            return false;
        }

        int intersections = 0;
        // use -100000 instead of Double.MINUS_INFINITY
        // otherwise all calculations would evaluate to minus infinity, resulting in no intersections
        Line intersectingEdge = new Line(new Coord(-100000, 0), point);
        for (Line edge: getEdges()) {
            if (edge.getIntersectionPoint(intersectingEdge).isPresent()) {
                intersections++;
            }
        }
        return intersections % 2 != 0;
    }

    public boolean isOutside(Coord point) {
        return !this.isInside(point);
    }

    public Coord[] getVertices() {
        return vertices;
    }

    public Line[] getEdges() {
        Line[] edges = new Line[vertices.length];
        for (int i = 0; i < vertices.length - 1; i++) {
            edges[i] = new Line(vertices[i], vertices[i + 1]);
        }
        edges[edges.length - 1] = new Line(vertices[0], vertices[vertices.length - 1]);
        return edges;
    }

    public BoundingBox getBoundingBox() {
        return BoundingBox.fromPoints(vertices);
    }
}
