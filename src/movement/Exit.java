package movement;

import core.Coord;

public class Exit {

    private final String name;
    private final Coord coord;

    private final double exitRate;


    public Exit(Coord coord, String name, double exitRate) {
        this.coord = coord;
        this.name = name;
        this.exitRate = exitRate;
    }

    public Coord getCoord() {
        return coord;
    }

    public String getName() {
        return name;
    }

    public void useExit() {

    }

    public void flushExit() {
    }
}