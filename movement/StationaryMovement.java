/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;

/**
 * A dummy stationary "movement" model where nodes do not move.
 * Might be useful for simulations with only external connection events.
 */
public class StationaryMovement extends MovementModel {
	/** Per node group setting for setting the location ({@value}) */
	public static final String LOCATION_S = "nodeLocation";
	private Coord loc; /** The location of the nodes */

	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param s The Settings object where the settings are read from
	 */
	public StationaryMovement(Settings s) {
		super(s);
		int coords[];

		coords = s.getCsvInts(LOCATION_S, 2);
		this.loc = new Coord(coords[0],coords[1]);
	}

	/**
	 * Copy constructor.
	 * @param sm The StationaryMovement prototype
	 */
	public StationaryMovement(StationaryMovement sm) {
		super(sm);
		this.loc = sm.loc;
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
	public StationaryMovement replicate() {
		return new StationaryMovement(this);
	}

}
