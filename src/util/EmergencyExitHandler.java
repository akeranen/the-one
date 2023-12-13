package util;

import core.Coord;
import core.DTNSim;
import core.Settings;

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

        Settings settings = new Settings("Exit");

        int numberOfExits = settings.getInt("nrof", 0);

        for (int i=1; i<=numberOfExits; i++) {
            Settings exit = new Settings("Exit"+i);

            String name = exit.getSetting("name");
            String[] coordStrings = exit.getCsvSetting("coord", 2);
            Coord exitCoord = new Coord(Double.parseDouble(coordStrings[0]), Double.parseDouble(coordStrings[1]));
            double exitRate = exit.getDouble("exitRate");

            this.emergencyExits.add(new Exit(name, exitCoord, exitRate));
        }
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

