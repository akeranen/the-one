/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

/**
 * Random waypoint movement where the coordinates are restricted to circular
 * area defined by a central point and range.
 * @author teemuk
 */
package movement;

import core.Coord;
import core.Settings;

public class ClusterMovement extends RandomWaypoint {
	/** Range of the cluster */
	public static final String	CLUSTER_RANGE = "clusterRange";
	/** Center point of the cluster */
	public static final String	CLUSTER_CENTER = "clusterCenter";

	private int		p_x_center = 100, p_y_center = 100;
	private double	p_range = 100.0;

	public ClusterMovement(Settings s) {
		super(s);

		if (s.contains(CLUSTER_RANGE)){
			this.p_range = s.getDouble(CLUSTER_RANGE);
		}
		if (s.contains(CLUSTER_CENTER)){
			int[] center = s.getCsvInts(CLUSTER_CENTER,2);
			this.p_x_center = center[0];
			this.p_y_center = center[1];
		}
	}

	private ClusterMovement(ClusterMovement cmv) {
		super(cmv);
		this.p_range = cmv.p_range;
		this.p_x_center = cmv.p_x_center;
		this.p_y_center = cmv.p_y_center;
	}

	@Override
	protected Coord randomCoord() {
		double x = (rng.nextDouble()*2 - 1)*this.p_range;
		double y = (rng.nextDouble()*2 - 1)*this.p_range;
		while (x*x + y*y>this.p_range*this.p_range) {
			x = (rng.nextDouble()*2 - 1)*this.p_range;
			y = (rng.nextDouble()*2 - 1)*this.p_range;
		}
		x += this.p_x_center;
		y += this.p_y_center;
		return new Coord(x,y);
	}

	@Override
	public int getMaxX() {
		return (int)Math.ceil(this.p_x_center + this.p_range);
	}

	@Override
	public int getMaxY() {
		return (int)Math.ceil(this.p_y_center + this.p_range);
	}

	@Override
	public ClusterMovement replicate() {
		return new ClusterMovement(this);
	}
}
