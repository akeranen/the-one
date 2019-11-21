package movement.nodegrid;

import core.Coord;

import java.util.Arrays;
import java.util.Optional;

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
        Edge intersectingEdge = new Edge(new Coord(-100000, 0), point);
        for (Edge edge: getEdges()) {
            if (edge.getIntersectionPoint(intersectingEdge).isPresent()) {
                intersections++;
            }
        }
        return intersections % 2 != 0;
    }

    public boolean isOutside(Coord point) {
        return !this.isInside(point);
    }

    public Edge[] getEdges() {
        Edge[] edges = new Edge[vertices.length];
        for (int i = 0; i < vertices.length - 1; i++) {
            edges[i] = new Edge(vertices[i], vertices[i + 1]);
        }
        edges[edges.length - 1] = new Edge(vertices[0], vertices[vertices.length - 1]);
        return edges;
    }

    public BoundingBox getBoundingBox() {
        double minX = Arrays.stream(this.vertices).map(Coord::getX).reduce(0.0, Math::min);
        double maxX = Arrays.stream(this.vertices).map(Coord::getX).reduce(0.0, Math::max);
        double minY = Arrays.stream(this.vertices).map(Coord::getY).reduce(0.0, Math::min);
        double maxY = Arrays.stream(this.vertices).map(Coord::getY).reduce(0.0, Math::max);
        return new BoundingBox(new Coord(minX, minY), new Coord(maxX, maxY));
    }

    public static class BoundingBox {
        private Coord topLeft;
        private Coord bottomRight;

        public BoundingBox(Coord topLeft, Coord bottomRight) {
            this.topLeft = topLeft;
            this.bottomRight = bottomRight;
        }

        public Coord getTopLeft() {
            return topLeft;
        }

        public void setTopLeft(Coord topLeft) {
            this.topLeft = topLeft;
        }

        public Coord getBottomRight() {
            return bottomRight;
        }

        public void setBottomRight(Coord bottomRight) {
            this.bottomRight = bottomRight;
        }
    }

    public static class Edge {
        private Coord start;
        private Coord end;

        Edge(Coord start, Coord end) {
            this.start = start;
            this.end = end;
        }

        public Coord getStart() {
            return start;
        }

        public Coord getEnd() {
            return end;
        }

        public void setStart(Coord start) {
            this.start = start;
        }

        public void setEnd(Coord end) {
            this.end = end;
        }

        public double length() {
            return this.start.distance(this.end);
        }

        public Coord getDirection() {
            return new Coord(end.getX() - start.getX(), end.getY() - start.getY());
        }

        public Optional<Coord> getIntersectionPoint(Edge other) {
            Coord p = this.start;
            Coord r = this.getDirection();
            Coord q = other.start;
            Coord s = other.getDirection();

            // solve p + t r = q + u s for t and u
            double t = vectorCrossProduct(vectorSubtraction(q, p), s) / vectorCrossProduct(r, s);
            double u = vectorCrossProduct(vectorSubtraction(q, p), r) / vectorCrossProduct(r, s);

            // use !isAlmostZero instead of != 0 to adjust for rounding errors
            if (!isAlmostZero(vectorCrossProduct(r, s), 6) && 0 <= t && t <= 1 && 0 <= u && u <= 1) {
                return Optional.of(vectorAddition(p, vectorScalarProduct(t, r)));
            }

            return Optional.empty();
        }

        private static double vectorCrossProduct(Coord a, Coord b) {
            return a.getX() * b.getY() - a.getY() * b.getX();
        }

        private static Coord vectorAddition(Coord a, Coord b) {
            return new Coord(a.getX() + b.getX(), a.getY() + b.getY());
        }

        private static Coord vectorSubtraction(Coord a, Coord b) {
            return vectorAddition(a, new Coord(-b.getX(), -b.getY()));
        }

        private static Coord vectorScalarProduct(double scalar, Coord vector) {
            return new Coord(scalar * vector.getX(), scalar * vector.getY());
        }

        private static boolean isAlmostZero(double number, int precision) {
            return Math.abs(number) < Math.pow(10, -precision);
        }
    }
}
