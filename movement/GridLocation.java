/*
 * Copyright 2011 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;
import core.SimScenario;

/**
 * Location (movement) model that sets the nodes in a grid formation
 */
public class GridLocation extends MovementModel {
	/** Sub name space for the grid location settings ({@value}) */
	public static final String GRIDLOC_NS = "GridLocation";
	/** How many rows of nodes there are -setting ({@value}).
	 * Number of columns is calculated from the node count. */
	public static final String ROWS_S = "rows";
	/** Space between the nodes -setting ({@value}) */
	public static final String SPACING_S = "spacing";
	/** Maximum random offset for the location of the nodes
	 * -setting ({@value}). Default = 0. */
	public static final String OFFSET_S = "randomOffset";
	/** Location of the first node (grid's upper left corner)
	 * -setting ({@value}). Two values: x,y */
	public static final String LOCATION_S = "location";

	private double startCoords[];
	private int rows;
	private int cols;
	private double spacing;
	private double offset;
	private int nodeCount;
	private Coord loc;

	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param s The Settings object where the settings are read from
	 */
	public GridLocation(Settings s) {
		super(s);
		int nodeCount = s.getInt(SimScenario.NROF_HOSTS_S);

		s.setSubNameSpace(GRIDLOC_NS);

		this.rows = s.getInt(ROWS_S);
		this.cols = (nodeCount / this.rows);
		this.startCoords = s.getCsvDoubles(LOCATION_S,2);
		this.spacing = s.getInt(SPACING_S);
		this.offset = s.getDouble(OFFSET_S, 0);

		s.restoreSubNameSpace();
	}

	/**
	 * Copy constructor.
	 * @param proto The movement model prototype
	 */
	public GridLocation(GridLocation proto) {
		super(proto);
		double x,y;

		x = proto.startCoords[0] +
			((proto.nodeCount) % proto.cols) * proto.spacing;
		x += rng.nextDouble() * proto.offset;

		y = proto.startCoords[1] +
			((proto.nodeCount) / proto.cols) * proto.spacing;
		y += rng.nextDouble() * proto.offset;

		this.loc = new Coord(x,y);

		proto.nodeCount++;
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
	public GridLocation replicate() {
		return new GridLocation(this);
	}

}
