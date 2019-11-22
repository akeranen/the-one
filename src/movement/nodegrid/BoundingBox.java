package movement.nodegrid;

import core.Coord;

import java.util.Arrays;
import java.util.stream.Stream;

public class BoundingBox {
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

    public static BoundingBox fromPoints(Coord... points) {
        double minX = Arrays.stream(points)
                .map(Coord::getX)
                .reduce(Math::min)
                .orElse(0.0);

        double minY = Arrays.stream(points)
                .map(Coord::getY)
                .reduce(Math::min)
                .orElse(0.0);

        double maxX = Arrays.stream(points)
                .map(Coord::getX)
                .reduce(Math::max)
                .orElse(0.0);

        double maxY = Arrays.stream(points)
                .map(Coord::getY)
                .reduce(Math::max)
                .orElse(0.0);

        return new BoundingBox(new Coord(minX, minY), new Coord(maxX, maxY));
    }

    public static BoundingBox merge(BoundingBox... boundingBoxes) {
        Coord[] points = Stream.concat(
                Arrays.stream(boundingBoxes).map(BoundingBox::getTopLeft),
                Arrays.stream(boundingBoxes).map(BoundingBox::getBottomRight)
        ).toArray(Coord[]::new);
        return fromPoints(points);
    }
}
