/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.*;

public class AlarmNodesStationaryMovement extends MovementModel {
    public static final String LOCATIONS_S = "alarmNodeLocations";
    private Coord[] locs;

    public AlarmNodesStationaryMovement(Settings s) {
        super(s);

        int[] coords = s.getCsvInts(LOCATIONS_S);
        int nrOfHosts = s.getInt(SimScenario.NROF_HOSTS_S);

        if (coords.length % 2 != 0) {
            throw new SettingsError("Alarm node locations must be given as pairs of coordinates");
        }

        if (coords.length / 2 != nrOfHosts) {
            throw new SettingsError("Please specify the same amount of coordinates as there are hosts");
        }

        this.locs = new Coord[coords.length / 2];

        for (int i = 0; i < coords.length; i += 2) {
            this.locs[i / 2] = new Coord(coords[i], coords[i + 1]);
        }
    }

    public AlarmNodesStationaryMovement(AlarmNodesStationaryMovement sm) {
        super(sm);
        this.locs = sm.locs;
    }

    @Override
    public void setHost(DTNHost host) {
        super.setHost(host);
    }

    @Override
    public Coord getInitialLocation() {
        return locs[host.getAddress() % locs.length];
    }

    @Override
    public Path getPath() {
        Path p = new Path(0);
        p.addWaypoint(getInitialLocation());
        return p;
    }

    @Override
    public double nextPathAvailable() {
        return Double.MAX_VALUE;	// no new paths available
    }

    @Override
    public AlarmNodesStationaryMovement replicate() {
        return new AlarmNodesStationaryMovement(this);
    }

}
