/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import movement.MovementModel;
import movement.Path;
import core.Coord;

/**
 * A dummy stationary "movement" model where nodes do not move for testing
 * purposes
 */
public class StationaryMovement extends MovementModel {
	private Coord loc;

	public StationaryMovement(Coord location) {
		if (location == null) {
			this.loc = new Coord(0,0);
		}
		else {
			this.loc = location;
		}
	}

	/**
	 * Returns the only location of this movement model
	 * @return the only location of this movement model
	 */
	@Override
	public Coord getInitialLocation() {
		return loc;
	}

	@Override
	public boolean isActive() {
		return true;
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
		return new StationaryMovement(loc);
	}

}
