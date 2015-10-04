/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import core.Coord;
import core.DTNSim;

/**
 * This class controls the group mobility of the people meeting their friends in
 * the evening
 * 
 * @author Frans Ekman
 */
public class EveningActivityControlSystem {

	private HashMap<Integer, EveningActivityMovement> eveningActivityNodes;
	private List<Coord> meetingSpots;
	private EveningTrip[] nextTrips;
	
	private Random rng;
	
	private static HashMap<Integer, EveningActivityControlSystem> 
		controlSystems;
	
	static {
		DTNSim.registerForReset(EveningActivityControlSystem.class.
				getCanonicalName());
		reset();
	}
	
	/**
	 * Creates a new instance of EveningActivityControlSystem without any nodes
	 * or meeting spots, with the ID given as parameter
	 * @param id
	 */
	private EveningActivityControlSystem(int id) {
		eveningActivityNodes = new HashMap<Integer, EveningActivityMovement>();
	}

	public static void reset() {
		controlSystems = new HashMap<Integer, EveningActivityControlSystem>();
	}
	
	/**
	 * Register a evening activity node with the system
	 * @param eveningMovement activity movement
	 */
	public void addEveningActivityNode(EveningActivityMovement eveningMovement) {
		eveningActivityNodes.put(new Integer(eveningMovement.getID()), 
				eveningMovement);
	}
	
	/**
	 * Sets the meeting locations the nodes can choose among
	 * @param meetingSpots
	 */
	public void setMeetingSpots(List<Coord> meetingSpots) {
		this.meetingSpots = meetingSpots;
		this.nextTrips = new EveningTrip[meetingSpots.size()];
	}
	
	/**
	 * This method gets the instruction for a node, i.e. When/where and with 
	 * whom to go.  
	 * @param eveningActivityNodeID unique ID of the node
	 * @return Instructions object
	 */
	public EveningTrip getEveningInstructions(int eveningActivityNodeID) {
		EveningActivityMovement eveningMovement = eveningActivityNodes.get(
				new Integer(eveningActivityNodeID));
		if (eveningMovement != null) {
			int index = eveningActivityNodeID % meetingSpots.size();
			if (nextTrips[index] == null) {
				int nrOfEveningMovementNodes = (int)(eveningMovement.
						getMinGroupSize() + 
						(double)(eveningMovement.getMaxGroupSize() - 
								eveningMovement.getMinGroupSize()) * 
								rng.nextDouble());
				Coord loc = meetingSpots.get(index).clone();
				nextTrips[index] = new EveningTrip(nrOfEveningMovementNodes, 
						loc);
			}
			nextTrips[index].addNode(eveningMovement);
			if (nextTrips[index].isFull()) {
				EveningTrip temp = nextTrips[index];
				nextTrips[index] = null;
				return temp;
			} else {
				return nextTrips[index];
			}
		}
		return null;
	}
	
	/**
	 * Get the meeting spot for the node
	 * @param id
	 * @return Coordinates of the spot
	 */
	public Coord getMeetingSpotForID(int id) {
		int index = id % meetingSpots.size();
		Coord loc = meetingSpots.get(index).clone();
		return loc;
	}
	
	
	/**
	 * Sets the random number generator to be used 
	 * @param rand
	 */
	public void setRandomNumberGenerator(Random rand) {
		this.rng = rand;
	}
	
	/**
	 * Returns a reference to a EveningActivityControlSystem with ID provided as
	 * parameter. If a system does not already exist with the requested ID, a 
	 * new one is created. 
	 * @param id unique ID of the EveningActivityControlSystem
	 * @return The EveningActivityControlSystem with the provided ID
	 */
	public static EveningActivityControlSystem getEveningActivityControlSystem(
			int id) {
		if (controlSystems.containsKey(new Integer(id))) {
			return controlSystems.get(new Integer(id));
		} else {
			EveningActivityControlSystem scs = 
				new EveningActivityControlSystem(id);
			controlSystems.put(new Integer(id), scs);
			return scs;
		}
	}
	
}
