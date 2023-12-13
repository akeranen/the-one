package util;

import core.Coord;
import core.DTNHost;
import core.DTNSim;

import java.util.ArrayList;

public class EmergencyExitHandler {

    static {
        DTNSim.registerForReset(EmergencyExitHandler.class.getCanonicalName());
        reset();
    }

    private static EmergencyExitHandler instance;

    private ArrayList<Exit> emergencyExits;

    private EmergencyExitHandler() {
        this.emergencyExits = new ArrayList<>();
        this.emergencyExits.add(new Exit("1. Hauptausgang", new Coord(83.1390380859375, 124.48645858573059), 3));
        this.emergencyExits.add(new Exit("2. Hauptausgang", new Coord(292.89550781249996, 112.96934440596303), 3));
        this.emergencyExits.add(new Exit("3. Hauptausgang", new Coord(120.65734863281252, 139.58057772547917), 3));
    }

    public static EmergencyExitHandler getInstance() {
        if (instance == null) {
            instance =  new EmergencyExitHandler();
        }
        return instance;
    }

    public Exit getExitFromCoord(Coord coord) {
        for (Exit exit : this.emergencyExits) {
            if (exit.getCoord().compareTo(coord) == 0) {
                return exit;
            }
        }
        return null;
    }

    public ArrayList<Exit> getEmergencyExits() { return this.emergencyExits; }


    public static void reset() {
        instance = new EmergencyExitHandler();
    }
}

