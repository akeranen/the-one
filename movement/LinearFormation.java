/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;
import movement.MovementModel;
import movement.Path;

/**
 * A stationary "movement" model where nodes do not move but are in linear
 * formation (i.e., in a line).
 */
public class LinearFormation extends MovementModel {
	/** Name space of the settings (append to group name space) */
	public static final String LINEAR_FORMATION_NS = "LinearFormation.";
	/** Per node group setting for defining the start coordinates of
	 * the line ({@value}) */
	public static final String START_LOCATION_S = "startLocation";
	/** Per node group setting for defining the end coordinates of
	 * the line ({@value}) */
	public static final String END_LOCATION_S = "endLocation";

	/* values for the prototype */
	private Coord startLoc; /** The start location of the line */
	private Coord endLoc; /** The start location of the line */
	private int nodeCount; /** how many nodes in this formation */
	private int lastIndex; /** index of the previous node */

	/* values for the per-node models */
	private Coord loc;

	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param s The Settings object where the settings are read from
	 */
	public LinearFormation(Settings s) {
		super(s);
		int coords[];

		coords = s.getCsvInts(LINEAR_FORMATION_NS + START_LOCATION_S, 2);
		this.startLoc = new Coord(coords[0], coords[1]);
		coords = s.getCsvInts(LINEAR_FORMATION_NS + END_LOCATION_S, 2);
		this.endLoc = new Coord(coords[0], coords[1]);

		this.nodeCount = s.getInt(core.SimScenario.NROF_HOSTS_S);
		this.lastIndex = 0;
	}

	/**
	 * Copy constructor.
	 * @param lf The LinearFormation prototype
	 */
	public LinearFormation(LinearFormation lf) {
		super(lf);
		this.loc = calculateInitLocation(lf);
	}

	/**
	 * Calculates and returns the location of this node in the formation
	 * @param proto The movement model prototype
	 * @return the location of the node
	 */
	private Coord calculateInitLocation(LinearFormation proto) {
		double dx, dy;
		double placementFraction;
		int formationIndex = proto.lastIndex++;

		Coord c = proto.startLoc.clone();

		placementFraction = (1.0 * formationIndex / proto.nodeCount);
		dx = placementFraction *
			(proto.endLoc.getX() -  proto.startLoc.getX());
		dy = placementFraction *
			(proto.endLoc.getY() -  proto.startLoc.getY());
		c.translate(dx, dy);

		return c;
	}

	/**
	 * Returns the the location of the node in the formation
	 * @return the the location of the node in the formation
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

	/**
	 * Returns Double.MAX_VALUE (no paths available)
	 */
	@Override
	public double nextPathAvailable() {
		return Double.MAX_VALUE;	// no new paths available
	}

	@Override
	public LinearFormation replicate() {
		return new LinearFormation(this);
	}

}
