package util;

import core.Coord;

public class EmergencyExit {
    private final String name;
    private final Coord coord;
    private final int exitSpeed;

    public EmergencyExit (String name, Coord coord, int exitSpeed) {
        this.name = name;
        this.coord = coord;
        this.exitSpeed = exitSpeed;
    }

    public String getName() { return this.name; }

    public Coord getCoord() { return this.coord; }

    public int getExitSpeed() { return this.exitSpeed; }
}