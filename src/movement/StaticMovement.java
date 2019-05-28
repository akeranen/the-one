/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;
import core.SimError;
import input.WKTMapReader;
import movement.map.SimMap;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static core.SimScenario.NROF_HOSTS_S;

/**
 * A model where nodes do not move. Locations are defined in reference to a map,
 * as points in .wkt file.
 * Example settings:
 * 	Group1.movementModel = StaticMovement
 * 	Group1.staticLocationFile = staticLocs.wkt
 * 	MapBasedMovement.nrofMapFiles = 1
 * 	MapBasedMovement.mapFile1 = map.wkt
 *
 * Might be useful for simulations with only external connection events.
 */
public class StaticMovement extends MapBasedMovement {
	/** Per node group setting for setting the location ({@value}) */
	public static final String LOCATION_S = "nodeLocations";
	private static final String STATIC_MOVEMENT_NS = "StaticMovement";
	private static final String LOCATION_FILE = "staticLocationFile";
	private Coord loc; /** The location of the nodes */

	private List<Coord> locations;
	private int lastIndex; /** index of the previous node */

	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param s The Settings object where the settings are read from
	 */
	public StaticMovement(Settings s) {
		super(s);

		String path = s.getSetting(LOCATION_FILE);
		int nodeCount = s.getInt(NROF_HOSTS_S);

		try {
			WKTMapReader r = new WKTMapReader(true);

			locations = r.readPoints(new File(path));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (locations.size() < nodeCount){
			throw new SimError("Locations file has less points than nrOfHosts!");
		}
		SimMap map = getMap();
		Coord offset = map.getOffset();

		for (Coord c : locations) {
			if (map.isMirrored()) { // mirror POIs if map data is also mirrored
				c.setLocation(c.getX(), -c.getY()); // flip around X axis
			}
			// translate to match map data
			c.translate(offset.getX(), offset.getY());
		}

		lastIndex = 0;
	}

	/**
	 * Copy constructor.
	 * @param sm The StationaryMovement prototype
	 */
	public StaticMovement(StaticMovement sm) {
		super(sm);
		this.loc = sm.locations.get(sm.lastIndex++);
	}

	/**
	 * Returns the only location of this movement model
	 * @return the only location of this movement model
	 */
	@Override
	public Coord getInitialLocation() {
		return loc;
	}

	/**
	 * Returns a single coordinate path (using the only possible coordinate)
	 * @return a single coordinate path
	 */
	@Override
	public Path getPath() {
		Path p = new Path(0);
		p.addWaypoint(loc);
		return p;
	}

	@Override
	public double nextPathAvailable() {
		return Double.MAX_VALUE;	// no new paths available
	}

	@Override
	public StaticMovement replicate() {
		return new StaticMovement(this);
	}

}
