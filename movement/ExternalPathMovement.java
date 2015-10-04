/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import input.ExternalPathMovementReader;

import java.util.List;

import core.Coord;
import core.DTNHost;
import core.Settings;
import core.SimClock;

/** 
 * External movement trace reader for traces that are in path format.
 * See <code>ExternalPathMovementReader</code> for details.
 * 
 * @author teemuk
 *
 */
public class ExternalPathMovement extends MovementModel {
	/** external locations file's path -setting id ({@value})*/
	public static final String MOVEMENT_FILE_S = "traceFile";
	/** activity file's path -setting id ({@value})*/
	public static final String ACTIVITY_FILE_S = "activeFile";
	
	// Settings
	private String traceFile;
	private String activeFile;
	
	// Node's paths
	private List<List<ExternalPathMovementReader.Entry>> paths;
	private int curPath=0;
	private List<ExternalPathMovementReader.ActiveTime> active;
	
	public ExternalPathMovement(Settings settings) {
		this.traceFile = settings.getSetting(MOVEMENT_FILE_S);
		this.activeFile = settings.getSetting(ACTIVITY_FILE_S);
	}
	
	/** 
	 * Copy-constructor.
	 * 
	 * @param mm
	 */
	public ExternalPathMovement(ExternalPathMovement mm) {
		this.traceFile = mm.traceFile;
		this.activeFile = mm.activeFile;
	}
	
	/** 
	 * Initializes the movement model. Uses a reader to get the paths for this
	 * host.
	 */
	private void init() {
		// Get paths for this node
		ExternalPathMovementReader reader =
			ExternalPathMovementReader.getInstance(this.traceFile,
					this.activeFile);
		this.paths = reader.getPaths(getHost().getAddress());
		this.active = reader.getActive(getHost().getAddress());
	}
	
	@Override
	public void setHost(DTNHost host) {
		super.setHost(host);
		this.init(); // Can only initialize after the host has been set
	}
	
	@Override
	public boolean isActive() {
		double t = SimClock.getTime();
		
		// Check whether the current time falls in one of the active periods
		for (ExternalPathMovementReader.ActiveTime a : this.active) {
			if (t >= a.start && t <= a.end) return true;
		}
		
		return false;
	}

	@Override
	public Path getPath() {
		// Make sure to not give out paths when the node is not active
		if (!this.isActive()) {
			return null;
		}
		
		// Check whether we're moving or waiting for the next path to start
		double t = SimClock.getTime();
		if (t < this.paths.get(this.curPath).get(0).time) {
			return null;
		}
		
		// Get the path
		List<ExternalPathMovementReader.Entry> path =
			this.paths.get(this.curPath);
		this.curPath++;
		
		// Drop the node to the the beginning of the new path in case the
		// previous path ended somewhere else.
		Coord curPos = super.getHost().getLocation();
		ExternalPathMovementReader.Entry pathStart = path.get(0);
		if (curPos.getX() != pathStart.x ||
				curPos.getY() != pathStart.y) {
			Coord c = new Coord(pathStart.x, pathStart.y);
			super.getHost().setLocation(c);
		}
		
		// If this is a stationary path, return only the fist point
		if (path.size() == 1) {
			Path p = new Path(0);
			ExternalPathMovementReader.Entry e = path.get(0);
			Coord c = new Coord(e.x, e.y);
			p.addWaypoint(c);
			return p;
		}
		
		// Build and return the whole path at once
		Path p = new Path();
		for (int i=1; i < path.size(); i++) {
			ExternalPathMovementReader.Entry e = path.get(i);
			ExternalPathMovementReader.Entry e2 = path.get(i-1);
			Coord c = new Coord(e.x, e.y);
			double dt = e.time - e2.time;
			double ds = Math.sqrt( (e.x - e2.x) * (e.x - e2.x) +
					(e.y - e2.y) * (e.y - e2.y));
			double v = ds/dt;
			p.addWaypoint(c, v);
		}
		
		return p;
	}

	@Override
	public Coord getInitialLocation() {
		// Return the first point of the first path
		if (this.paths.size() > 0 &&
				this.paths.get(0).size() > 0) {
			ExternalPathMovementReader.Entry e = this.paths.get(0).get(0);
			Coord c = new Coord(e.x, e.y);
			return c;
		}
		return new Coord(0.0, 0.0);
	}

	@Override
	public MovementModel replicate() {
		ExternalPathMovement mm = new ExternalPathMovement(this);
		return mm;
	}
	
	@Override
	public double nextPathAvailable() {
		if (this.curPath < this.paths.size())
			return this.paths.get(this.curPath).get(0).time;
		else
			return Double.MAX_VALUE;
	}
}
