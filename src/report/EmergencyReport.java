package report;

import core.Coord;
import core.SimClock;
import util.EmergencyExitHandler;

import java.util.ArrayList;

public class EmergencyReport extends Report{

    private ArrayList<String> hostsReachedExit;

    public EmergencyReport() {
        super();
        this.hostsReachedExit = new ArrayList<>();
    }

    public void hasReachedEmergencyExit(String hostName, Coord coord) {
        if (!hasAlreadyReachedExit(hostName)) {
            write(hostName + " reached exit " + EmergencyExitHandler.getInstance().getExitFromCoord(coord).getName() + " at " + SimClock.getTime());
            this.hostsReachedExit.add(hostName);
        }
    }

    private boolean hasAlreadyReachedExit(String hostName) {
        return this.hostsReachedExit.contains(hostName);
    }

    public void emergencyTriggerd() {
        write("Emergency triggered at " + SimClock.getTime());
    }

}
