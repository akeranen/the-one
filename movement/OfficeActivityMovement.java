/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import input.WKTReader;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import util.ParetoRNG;

import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;
import core.Coord;
import core.Settings;
import core.SimClock;

/**
 * This class models movement at an office. If the node happens to be at some 
 * other location than the office, it first walks the shortest path to the 
 * office and then stays there until the end of the work day. A node has only 
 * works at one office.
 * 
 * @author Frans Ekman
 *
 */
public class OfficeActivityMovement extends MapBasedMovement implements 
	SwitchableMovement {

	private static final int WALKING_TO_OFFICE_MODE = 0;
	private static final int AT_OFFICE_MODE = 1;
	
	public static final String WORK_DAY_LENGTH_SETTING = "workDayLength";
	public static final String NR_OF_OFFICES_SETTING = "nrOfOffices";
	
	public static final String OFFICE_SIZE_SETTING = "officeSize";
	public static final String OFFICE_WAIT_TIME_PARETO_COEFF_SETTING = 
		"officeWaitTimeParetoCoeff";
	public static final String OFFICE_MIN_WAIT_TIME_SETTING = 
		"officeMinWaitTime";
	public static final String OFFICE_MAX_WAIT_TIME_SETTING = 
		"officeMaxWaitTime"; 
	public static final String OFFICE_LOCATIONS_FILE_SETTING = 
		"officeLocationsFile";
	
	private static int nrOfOffices = 50;
	
	private int mode;
	private int workDayLength;
	private int startedWorkingTime;
	private boolean ready;;
	private DijkstraPathFinder pathFinder;
	
	private ParetoRNG paretoRNG;
	
	private int distance;
	private double officeWaitTimeParetoCoeff;
	private double officeMinWaitTime;
	private double officeMaxWaitTime;
	
	private List<Coord> allOffices;
	
	private Coord lastWaypoint;
	private Coord officeLocation;
	private Coord deskLocation;
	
	private boolean sittingAtDesk;
	
	/**
	 * OfficeActivityMovement constructor
	 * @param settings
	 */
	public OfficeActivityMovement(Settings settings) {
		super(settings);

		workDayLength = settings.getInt(WORK_DAY_LENGTH_SETTING);
		nrOfOffices = settings.getInt(NR_OF_OFFICES_SETTING);
		
		distance = settings.getInt(OFFICE_SIZE_SETTING);
		officeWaitTimeParetoCoeff = settings.getDouble(
				OFFICE_WAIT_TIME_PARETO_COEFF_SETTING);
		officeMinWaitTime = settings.getDouble(OFFICE_MIN_WAIT_TIME_SETTING);
		officeMaxWaitTime = settings.getDouble(OFFICE_MAX_WAIT_TIME_SETTING);
		
		startedWorkingTime = -1;
		pathFinder = new DijkstraPathFinder(null);
		mode = WALKING_TO_OFFICE_MODE;
		
		String officeLocationsFile = null;
		try {
			officeLocationsFile = settings.getSetting(
					OFFICE_LOCATIONS_FILE_SETTING);
		} catch (Throwable t) {
			// Do nothing;
		}
		
		if (officeLocationsFile == null) {
			MapNode[] mapNodes = (MapNode[])getMap().getNodes().
				toArray(new MapNode[0]);
			int officeIndex = rng.nextInt(mapNodes.length - 1) /
				(mapNodes.length/nrOfOffices);
			officeLocation = mapNodes[officeIndex].getLocation().clone();
		} else {
			try {
				allOffices = new LinkedList<Coord>();
				List<Coord> locationsRead = (new WKTReader()).
					readPoints(new File(officeLocationsFile));
				for (Coord coord : locationsRead) {
					SimMap map = getMap();
					Coord offset = map.getOffset();
					// mirror points if map data is mirrored
					if (map.isMirrored()) { 
						coord.setLocation(coord.getX(), -coord.getY()); 
					}
					coord.translate(offset.getX(), offset.getY());
					allOffices.add(coord);
				}
				officeLocation = allOffices.get(
						rng.nextInt(allOffices.size())).clone();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		deskLocation = getRandomCoorinateInsideOffice();
		paretoRNG = new ParetoRNG(rng, officeWaitTimeParetoCoeff, 
				officeMinWaitTime, officeMaxWaitTime);
	}
	
	/**
	 * Copyconstructor
	 * @param proto
	 */
	public OfficeActivityMovement(OfficeActivityMovement proto) {
		super(proto);
		this.workDayLength = proto.workDayLength;
		startedWorkingTime = -1;
		this.distance = proto.distance;
		this.pathFinder = proto.pathFinder;
		this.mode = proto.mode;
		
		if (proto.allOffices == null) {
			MapNode[] mapNodes = (MapNode[])getMap().getNodes().
				toArray(new MapNode[0]);
			int officeIndex = rng.nextInt(mapNodes.length - 1) / 
				(mapNodes.length/nrOfOffices);
			officeLocation = mapNodes[officeIndex].getLocation().clone();
		} else {
			this.allOffices = proto.allOffices;
			officeLocation = allOffices.get(
					rng.nextInt(allOffices.size())).clone();
		}
		
		officeWaitTimeParetoCoeff = proto.officeWaitTimeParetoCoeff;
		officeMinWaitTime = proto.officeMinWaitTime;
		officeMaxWaitTime = proto.officeMaxWaitTime;
		
		deskLocation = getRandomCoorinateInsideOffice();
		this.paretoRNG = proto.paretoRNG;
	}
	
	public Coord getRandomCoorinateInsideOffice() {
		double x_coord = officeLocation.getX() + 
			(0.5 - rng.nextDouble()) * distance;
		if (x_coord > getMaxX()) {
			x_coord = getMaxX();
		} else if (x_coord < 0) {
			x_coord = 0;
		}
		double y_coord = officeLocation.getY() + 
			(0.5 - rng.nextDouble()) * distance;
		if (y_coord > getMaxY()) {
			y_coord = getMaxY();
		} else if (y_coord < 0) {
			y_coord = 0;
		}
		return new Coord(x_coord, y_coord);
	}
	
	@Override
	public Coord getInitialLocation() {
		double x = rng.nextDouble() * getMaxX();
		double y = rng.nextDouble() * getMaxY();
		Coord c = new Coord(x,y);

		this.lastWaypoint = c;
		return c.clone();
	}

	@Override
	public Path getPath() {
		if (mode == WALKING_TO_OFFICE_MODE) {
			// Try to find to the office
			SimMap map = super.getMap();
			if (map == null) {
				return null;
			}
			MapNode thisNode = map.getNodeByCoord(lastWaypoint);
			MapNode destinationNode = map.getNodeByCoord(officeLocation);
			List<MapNode> nodes = pathFinder.getShortestPath(thisNode, 
					destinationNode);
			Path path = new Path(generateSpeed());
			for (MapNode node : nodes) {
				path.addWaypoint(node.getLocation());
			}
			lastWaypoint = officeLocation.clone();
			mode = AT_OFFICE_MODE;
			return path;
		}
		
		if (startedWorkingTime == -1) {
			startedWorkingTime = SimClock.getIntTime();
		}
		if (SimClock.getIntTime() - startedWorkingTime >= workDayLength) {
			Path path =  new Path(1);
			path.addWaypoint(lastWaypoint.clone());
			ready = true;
			return path;
		}
		Coord c;
		if (sittingAtDesk) {
			c = getRandomCoorinateInsideOffice();
			sittingAtDesk = false;
		} else {
			c = deskLocation.clone();
			sittingAtDesk = true;
		}
		
		Path path =  new Path(1);
		path.addWaypoint(c);
		return path;
	}

	@Override
	protected double generateWaitTime() {
		int timeLeft = workDayLength - 
			(SimClock.getIntTime() - startedWorkingTime);
		
		int waitTime = (int)paretoRNG.getDouble();
		if (waitTime > timeLeft) {
			return timeLeft;
		} 
		return waitTime;
	}
	
	@Override
	public MapBasedMovement replicate() {
		return new OfficeActivityMovement(this);
	}

	/**
	 * @see SwitchableMovement
	 */
	public Coord getLastLocation() {
		return lastWaypoint.clone();
	}

	/**
	 * @see SwitchableMovement
	 */
	public boolean isReady() {
		return ready;
	}

	/**
	 * @see SwitchableMovement
	 */
	public void setLocation(Coord lastWaypoint) {
		this.lastWaypoint = lastWaypoint.clone();
		startedWorkingTime = -1;
		ready = false;
		mode = WALKING_TO_OFFICE_MODE;
	}
	
	/**
	 * @return The location of the office
	 */
	public Coord getOfficeLocation() {
		return officeLocation.clone();
	}

}
