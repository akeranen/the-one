package report;

import core.Coord;
import core.SimClock;
import util.EmergencyExitHandler;
import util.Exit;

import java.util.ArrayList;
import java.util.HashMap;

public class EmergencyReport extends Report{


    private HashMap<Exit, ArrayList<Leaving>> hostsReachedExit;

    public EmergencyReport() {
        super();
        this.hostsReachedExit = new HashMap<>();

        for (Exit exit : EmergencyExitHandler.getInstance().getEmergencyExits() ) {
            this.hostsReachedExit.put(exit, new ArrayList<>());
        }
    }

    public void hasReachedEmergencyExit(String hostName, Coord coord) {
        Exit exit = EmergencyExitHandler.getInstance().getExitFromCoord(coord);
        if (!hasAlreadyReachedExit(hostName, exit)) {
            double leavingTime;

            if (this.hostsReachedExit.get(exit).size() == 0) {
                leavingTime = SimClock.getTime();
            } else {
                Leaving lleaving = this.hostsReachedExit.get(exit).get( this.hostsReachedExit.get(exit).size()-1);

                if (SimClock.getTime() >= lleaving.getSimTime()) {
                    double wait = 1 / exit.getExitRate() - (SimClock.getTime() - lleaving.getSimTime());

                    if (wait >= 0) {
                        // der ausgang ist verstopft
                        leavingTime = SimClock.getTime() + wait;
                    } else {
                        leavingTime = SimClock.getTime();
                    }

                } else {
                    leavingTime = lleaving.getSimTime() + 1 / exit.getExitRate();
                }
            }

            write(hostName + " reached exit " + exit.getName() + " at " + leavingTime);
            this.hostsReachedExit.get(exit).add(new Leaving(hostName, leavingTime));
        }
    }

    private boolean hasAlreadyReachedExit(String hostName, Exit exit) {
        for (Leaving leaving : this.hostsReachedExit.get(exit)) {
            if (leaving.getHostname().equals(hostName)) {
                return true;
            }
        }
        return false;
    }

    public void emergencyTriggered() {
        write("Emergency triggered at " + SimClock.getTime());
    }

    private class Leaving {
        String hostname;
        double simTime;

        public Leaving(String hostname, double simTime) {
            this.hostname = hostname;
            this.simTime = simTime;
        }

        public String getHostname() { return this.hostname; }

        public double getSimTime() { return this.simTime; }

        public void setSimTime(double simTime) {
            this.simTime = simTime;
        }
    }
}
