package movement.nodegrid;

import core.Coord;

import java.util.Optional;

public class Line {
    private Coord start;
    private Coord end;

    Line(Coord start, Coord end) {
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

    public Optional<Coord> getIntersectionPoint(Line other) {
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
