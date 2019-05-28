/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;

/**
 * Movement model where all nodes move on a line
 * (work in progress)
 */
public class LinearMovement extends MovementModel {
	/** Name space of the settings (append to group name space) */
	public static final String LINEAR_MOVEMENT_NS = "LinearMovement.";
	/** Per node group setting for defining the start coordinates of
	 * the line ({@value}) */
	public static final String START_LOCATION_S = "startLocation";
	/** Per node group setting for defining the end coordinates of
	 * the line ({@value}) */
	public static final String END_LOCATION_S = "endLocation";

	/**
	 * Nodes' initial location type
	 * <ul><li>0: random (evenly distributed)
	 * <li>1: evenly spaced
	 * <li>2: static location at the start of line
	 * <li>3: evenly spaced among subsegment (needs startSegmentEndLocation)
	 * </ul>
	  */
	public static final String INIT_LOC_SEGMENT_END = "initLocSegmentEnd";
	public static final String INIT_LOC_S = "initLocType";
	/**
	 * Nodes' target (where they're heading) type
	 * <ul><li>0: random point on the line
	 * <li>1: far-end of the line
	 * </ul>
	  */
	public static final String TARGET_S = "targetType";
	private Coord initLocSegmentEnd; /** Used for node initial location distribution among segment*/

	/* values for the prototype */
	private Coord startLoc; /** The start location of the line */
	private Coord endLoc; /** The start location of the line */
	private int initLocType;
	private int targetType;
	private int nodeCount; /** how many nodes in this formation */
	private int lastIndex; /** index of the previous node */

	/* values for the per-node models */
	private Path nextPath;
	private Coord initLoc;

	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param s The Settings object where the settings are read from
	 */
	public LinearMovement(Settings s) {
		super(s);
		int coords[];

		coords = s.getCsvInts(LINEAR_MOVEMENT_NS + START_LOCATION_S, 2);
		this.startLoc = new Coord(coords[0], coords[1]);
		coords = s.getCsvInts(LINEAR_MOVEMENT_NS + END_LOCATION_S, 2);
		this.endLoc = new Coord(coords[0], coords[1]);
		if (s.contains(LINEAR_MOVEMENT_NS + INIT_LOC_SEGMENT_END)){
			coords = s.getCsvInts(LINEAR_MOVEMENT_NS + INIT_LOC_SEGMENT_END);
			this.initLocSegmentEnd = new Coord(coords[0], coords[1]);
		}
		this.initLocType = s.getInt(LINEAR_MOVEMENT_NS + INIT_LOC_S);
		this.targetType = s.getInt(LINEAR_MOVEMENT_NS + TARGET_S);
		this.nodeCount = s.getInt(core.SimScenario.NROF_HOSTS_S);

		this.lastIndex = 0;
	}

	/**
	 * Copy constructor.
	 * @param ilm The LinearFormation prototype
	 */
	public LinearMovement(LinearMovement ilm) {
		super(ilm);
		if (ilm.initLocType == 2)
			this.initLoc = ilm.startLoc;
		else if (ilm.initLocType == 3)
			this.initLoc = calculateLocationForSegment(ilm);
		else
			this.initLoc = calculateLocation(ilm, (ilm.initLocType == 1));
		this.nextPath = new Path(generateSpeed());
		this.nextPath.addWaypoint(initLoc);

		if (ilm.targetType == 0) { /* random target */
			this.nextPath.addWaypoint(calculateLocation(ilm, true));
		} else {
			this.nextPath.addWaypoint(calculateEndTarget(ilm, initLoc));
		}

		ilm.lastIndex++;
	}

	/** Spreads locations out evenly among the segment defined by initLocSegmentEnd (initLocType 3)*/
	private Coord calculateLocationForSegment(LinearMovement proto) {
		double dx = 0;
		double dy = 0;
		double placementFraction;

		double xDiff = (proto.initLocSegmentEnd.getX() -  proto.startLoc.getX());
		double yDiff = (proto.initLocSegmentEnd.getY() -  proto.startLoc.getY());
		Coord c = proto.startLoc.clone();


		placementFraction = (1.0 * proto.lastIndex / proto.nodeCount);
		dx = placementFraction * xDiff;
		dy = placementFraction * yDiff;

		c.translate(dx, dy);
		return c;
	}

	/**
	 * Calculates and returns a location in the line
	 * @param proto The movement model prototype
	 * @param isEven Is the distribution evenly spaced (false for random)
	 * @return a location on the line
	 */
	private Coord calculateLocation(LinearMovement proto, boolean isEven) {
		double dx = 0;
		double dy = 0;
		double placementFraction;

		double xDiff = (proto.endLoc.getX() -  proto.startLoc.getX());
		double yDiff = (proto.endLoc.getY() -  proto.startLoc.getY());
		Coord c = proto.startLoc.clone();

		if (isEven) {
			placementFraction = (1.0 * proto.lastIndex / proto.nodeCount);
			dx = placementFraction * xDiff;
			dy = placementFraction * yDiff;
		} else { /* random */
			dx = rng.nextDouble() * xDiff;
			dy = rng.nextDouble() * yDiff;
		}

		c.translate(dx, dy);
		return c;
	}

	/**
	 * Calculates and returns the far-end of the line
	 * @param proto The movement model prototype
	 * @param initLoc The initial location
	 * @return the coordinates for the far-end of the line
	 */
	private Coord calculateEndTarget(LinearMovement proto, Coord initLoc) {
		return (proto.startLoc.distance(initLoc) >
			proto.endLoc.distance(initLoc) ? proto.startLoc: proto.endLoc);
	}

	/**
	 * Returns the the location of the node in the formation
	 * @return the the location of the node in the formation
	 */
	@Override
	public Coord getInitialLocation() {
		return this.initLoc;
	}

	/**
	 * Returns a single coordinate path (using the only possible coordinate)
	 * @return a single coordinate path
	 */
	@Override
	public Path getPath() {
		Path p = nextPath;
		this.nextPath = null;
		return p;
	}

	/**
	 * Returns Double.MAX_VALUE (no paths available)
	 */
	@Override
	public double nextPathAvailable() {
		if (nextPath == null) {
			return Double.MAX_VALUE;	// no new paths available
		} else {
			return 0;
		}
	}

	@Override
	public int getMaxX() {
		return (int)(endLoc.getX() > startLoc.getX() ? endLoc.getX() :
			startLoc.getX());
	}

	@Override
	public int getMaxY() {
		return (int)(endLoc.getY() > startLoc.getY() ? endLoc.getY() :
			startLoc.getY());
	}


	@Override
	public LinearMovement replicate() {
		return new LinearMovement(this);
	}

}
