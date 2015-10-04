/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import java.util.List;
import java.util.Random;

import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;
import core.Coord;
import core.Settings;

/**
 * 
 * This class controls the movement of bus travellers. A bus traveller belongs 
 * to a bus control system. A bus traveller has a destination and a start 
 * location. If the direct path to the destination is longer than the path the 
 * node would have to walk if it would take the bus, the node uses the bus. If 
 * the destination is not provided, the node will pass a random number of stops
 * determined by Markov chains (defined in settings).
 * 
 * @author Frans Ekman
 *
 */
public class BusTravellerMovement extends MapBasedMovement implements 
	SwitchableMovement, TransportMovement {

	public static final String PROBABILITIES_STRING = "probs";
	public static final String PROBABILITY_TAKE_OTHER_BUS = "probTakeOtherBus";
	
	public static final int STATE_WAITING_FOR_BUS = 0;
	public static final int STATE_DECIDED_TO_ENTER_A_BUS = 1;
	public static final int STATE_TRAVELLING_ON_BUS = 2;
	public static final int STATE_WALKING_ELSEWHERE = 3;
	
	private int state;
	private Path nextPath;
	private Coord location;
	private Coord latestBusStop;
	private BusControlSystem controlSystem;
	private int id;
	private ContinueBusTripDecider cbtd;
	private double[] probabilities;
	private double probTakeOtherBus;
	private DijkstraPathFinder pathFinder;
	
	private Coord startBusStop;
	private Coord endBusStop;
	
	private boolean takeBus;
	
	private static int nextID = 0;
	
	/**
	 * Creates a BusTravellerModel 
	 * @param settings
	 */
	public BusTravellerMovement(Settings settings) {
		super(settings);
		int bcs = settings.getInt(BusControlSystem.BUS_CONTROL_SYSTEM_NR);
		controlSystem = BusControlSystem.getBusControlSystem(bcs);
		id = nextID++;
		controlSystem.registerTraveller(this);
		nextPath = new Path();
		state = STATE_WALKING_ELSEWHERE;
		if (settings.contains(PROBABILITIES_STRING)) {
			probabilities = settings.getCsvDoubles(PROBABILITIES_STRING);
		}
		if (settings.contains(PROBABILITY_TAKE_OTHER_BUS)) {
			probTakeOtherBus = settings.getDouble(PROBABILITY_TAKE_OTHER_BUS);
		}
		cbtd = new ContinueBusTripDecider(rng, probabilities);
		pathFinder = new DijkstraPathFinder(null);
		takeBus = true;
	}
	
	/**
	 * Creates a BusTravellerModel from a prototype
	 * @param proto
	 */
	public BusTravellerMovement(BusTravellerMovement proto) {
		super(proto);
		state = proto.state;
		controlSystem = proto.controlSystem;
		if (proto.location != null) {
			location = proto.location.clone();
		}
		nextPath = proto.nextPath;
		id = nextID++;
		controlSystem.registerTraveller(this);
		probabilities = proto.probabilities;
		cbtd = new ContinueBusTripDecider(rng, probabilities);
		pathFinder = proto.pathFinder;
		this.probTakeOtherBus = proto.probTakeOtherBus;
		takeBus = true;
	}
	
	@Override
	public Coord getInitialLocation() {
		
		MapNode[] mapNodes = (MapNode[])getMap().getNodes().
			toArray(new MapNode[0]);
		int index = rng.nextInt(mapNodes.length - 1);
		location = mapNodes[index].getLocation().clone();
		
		List<Coord> allStops = controlSystem.getBusStops();
		Coord closestToNode = getClosestCoordinate(allStops, location.clone());
		latestBusStop = closestToNode.clone();
		
		return location.clone();
	}

	@Override
	public Path getPath() {
		if (!takeBus) {
			return null;
		}
		if (state == STATE_WAITING_FOR_BUS) {
			return null;
		} else if (state == STATE_DECIDED_TO_ENTER_A_BUS) {
			state = STATE_TRAVELLING_ON_BUS;
			List<Coord> coords = nextPath.getCoords();
			location = (coords.get(coords.size() - 1)).clone();
			return nextPath;
		} else if (state == STATE_WALKING_ELSEWHERE) {
			// Try to find back to the bus stop
			SimMap map = controlSystem.getMap();
			if (map == null) {
				return null;
			}
			MapNode thisNode = map.getNodeByCoord(location);
			MapNode destinationNode = map.getNodeByCoord(latestBusStop);
			List<MapNode> nodes = pathFinder.getShortestPath(thisNode, 
					destinationNode);
			Path path = new Path(generateSpeed());
			for (MapNode node : nodes) {
				path.addWaypoint(node.getLocation());
			}
			location = latestBusStop.clone();
			return path;
		}
			
		return null;
	}

	/**
	 * Switches state between getPath() calls
	 * @return Always 0 
	 */
	protected double generateWaitTime() {
		if (state == STATE_WALKING_ELSEWHERE) {
			if (location.equals(latestBusStop)) {
				state = STATE_WAITING_FOR_BUS;
			}
		}
		if (state == STATE_TRAVELLING_ON_BUS) {
			state = STATE_WAITING_FOR_BUS;
		}
		return 0;
	}
	
	@Override
	public MapBasedMovement replicate() {
		return new BusTravellerMovement(this);
	}

	public int getState() {
		return state;
	}
	
	/**
	 * Get the location where the bus is located when it has moved its path
	 * @return The end point of the last path returned
	 */
	public Coord getLocation() {
		if (location == null) {
			return null;
		}
		return location.clone();
	}
	
	/**
	 * Notifies the node at the bus stop that a bus is there. Nodes inside 
	 * busses are also notified.
	 * @param nextPath The next path the bus is going to take
	 */
	public void enterBus(Path nextPath) {
		
		if (startBusStop != null && endBusStop != null) {
			if (location.equals(endBusStop)) {
				state = STATE_WALKING_ELSEWHERE;
				latestBusStop = location.clone();
			} else {
				state = STATE_DECIDED_TO_ENTER_A_BUS;
				this.nextPath = nextPath;
			}
			return;
		}
		
		if (!cbtd.continueTrip()) {
			state = STATE_WAITING_FOR_BUS;
			this.nextPath = null;
			/* It might decide not to start walking somewhere and wait 
			   for the next bus */
			if (rng.nextDouble() > probTakeOtherBus) {
				state = STATE_WALKING_ELSEWHERE;
				latestBusStop = location.clone();
			}
		} else {
			state = STATE_DECIDED_TO_ENTER_A_BUS;
			this.nextPath = nextPath;
		}
	}
	
	public int getID() {
		return id;
	}
	
	
	/**
	 * Small class to help nodes decide if they should continue the bus trip. 
	 * Keeps the state of nodes, i.e. how many stops they have passed so far. 
	 * Markov chain probabilities for the decisions. 
	 * 
	 * NOT USED BY THE WORKING DAY MOVEMENT MODEL
	 * 
	 * @author Frans Ekman
	 */
	class ContinueBusTripDecider {
		
		private double[] probabilities; // Probability to travel with bus
		private int state;
		private Random rng;
		
		public ContinueBusTripDecider(Random rng, double[] probabilities) {
			this.rng = rng;
			this.probabilities = probabilities;
			state = 0;
		}
		
		/**
		 * 
		 * @return true if node should continue
		 */
		public boolean continueTrip() {
			double rand = rng.nextDouble();
			if (rand < probabilities[state]) {
				incState();
				return true;
			} else {
				resetState();
				return false;
			}
		}
		
		/**
		 * Call when a stop has been passed
		 */
		private void incState() {
			if (state < probabilities.length  - 1) {
				state++;
			}
		}
		
		/**
		 * Call when node has finished it's trip
		 */
		private void resetState() {
			state = 0;
		}	
	}

	/**
	 * Help method to find the closest coordinate from a list of coordinates,
	 * to a specific location
	 * @param allCoords list of coordinates to compare
	 * @param coord destination node
	 * @return closest to the destination
	 */
	private static Coord getClosestCoordinate(List<Coord> allCoords, 
			Coord coord) {
		Coord closestCoord = null;
		double minDistance = Double.POSITIVE_INFINITY;
		for (Coord temp : allCoords) {
			double distance = temp.distance(coord);
			if (distance < minDistance) {
				minDistance = distance;
				closestCoord = temp;
			}
		}
		return closestCoord.clone();
	}
	
	/**
	 * Sets the next route for the traveller, so that it can decide wether it 
	 * should take the bus or not. 
	 * @param nodeLocation
	 * @param nodeDestination
	 */
	public void setNextRoute(Coord nodeLocation, Coord nodeDestination) {
			
		// Find closest stops to current location and destination
		List<Coord> allStops = controlSystem.getBusStops();
		
		Coord closestToNode = getClosestCoordinate(allStops, nodeLocation);
		Coord closestToDestination = getClosestCoordinate(allStops, 
				nodeDestination);
		
		// Check if it is shorter to walk than take the bus 
		double directDistance = nodeLocation.distance(nodeDestination);
		double busDistance = nodeLocation.distance(closestToNode) + 
			nodeDestination.distance(closestToDestination);
		
		if (directDistance < busDistance) {
			takeBus = false;
		} else {
			takeBus = true;
		}
		
		this.startBusStop = closestToNode;
		this.endBusStop = closestToDestination;
		this.latestBusStop = startBusStop.clone();
	}
	
	/**
	 * @see SwitchableMovement
	 */
	public Coord getLastLocation() {
		return location.clone();
	}

	/**
	 * @see SwitchableMovement
	 */
	public void setLocation(Coord lastWaypoint) {
		location = lastWaypoint.clone();
	}

	/**
	 * @see SwitchableMovement
	 */
	public boolean isReady() {
		if (state == STATE_WALKING_ELSEWHERE) {
			return true;
		} else {
			return false;
		}
	}
	
	public static void reset() {
		nextID = 0;
	}
	
}
