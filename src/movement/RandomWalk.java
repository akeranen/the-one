/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;

/**
 * Random Walk movement model
 *
 * @author Frans Ekman
 */
public class RandomWalk extends MovementModel implements SwitchableMovement {

	private Coord lastWaypoint;
	private double minDistance;
	private double maxDistance;

	public RandomWalk(Settings settings) {
		super(settings);
		minDistance = 0;
		maxDistance = 50;
	}

	private RandomWalk(RandomWalk rwp) {
		super(rwp);
		minDistance = rwp.minDistance;
		maxDistance = rwp.maxDistance;
	}

	/**
	 * Returns a possible (random) placement for a host
	 * @return Random position on the map
	 */
	@Override
	public Coord getInitialLocation() {
		assert rng != null : "MovementModel not initialized!";
		double x = rng.nextDouble() * getMaxX();
		double y = rng.nextDouble() * getMaxY();
		Coord c = new Coord(x,y);

		this.lastWaypoint = c;
		return c;
	}

	@Override
	public Path getPath() {
		Path p;
		p = new Path(generateSpeed());
		p.addWaypoint(lastWaypoint.clone());
		double maxX = getMaxX();
		double maxY = getMaxY();

		Coord c = null;
		while (true) {

			double angle = rng.nextDouble() * 2 * Math.PI;
			double distance = minDistance + rng.nextDouble() *
				(maxDistance - minDistance);

			double x = lastWaypoint.getX() + distance * Math.cos(angle);
			double y = lastWaypoint.getY() + distance * Math.sin(angle);

			c = new Coord(x,y);

			if (x > 0 && y > 0 && x < maxX && y < maxY) {
				break;
			}
		}

		p.addWaypoint(c);

		this.lastWaypoint = c;
		return p;
	}

	@Override
	public RandomWalk replicate() {
		return new RandomWalk(this);
	}

	public Coord getLastLocation() {
		return lastWaypoint;
	}

	public void setLocation(Coord lastWaypoint) {
		this.lastWaypoint = lastWaypoint;
	}

	public boolean isReady() {
		return true;
	}
}
