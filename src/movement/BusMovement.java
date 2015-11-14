/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import java.util.LinkedList;
import java.util.List;

import movement.map.MapNode;
import core.Coord;
import core.Settings;

/**
 * This class controls the movement of busses. It informs the bus control system
 * the bus is registered with every time the bus stops.
 *
 * @author Frans Ekman
 */
public class BusMovement extends MapRouteMovement {

	private BusControlSystem controlSystem;
	private int id;
	private static int nextID = 0;
	private boolean startMode;
	private List<Coord> stops;

	/**
	 * Creates a new instance of BusMovement
	 * @param settings
	 */
	public BusMovement(Settings settings) {
		super(settings);
		int bcs = settings.getInt(BusControlSystem.BUS_CONTROL_SYSTEM_NR);
		controlSystem = BusControlSystem.getBusControlSystem(bcs);
		controlSystem.setMap(super.getMap());
		this.id = nextID++;
		controlSystem.registerBus(this);
		startMode = true;
		stops = new LinkedList<Coord>();
		List<MapNode> stopNodes = super.getStops();
		for (MapNode node : stopNodes) {
			stops.add(node.getLocation().clone());
		}
		controlSystem.setBusStops(stops);
	}

	/**
	 * Create a new instance from a prototype
	 * @param proto
	 */
	public BusMovement(BusMovement proto) {
		super(proto);
		this.controlSystem = proto.controlSystem;
		this.id = nextID++;
		controlSystem.registerBus(this);
		startMode = true;
	}

	@Override
	public Coord getInitialLocation() {
		return (super.getInitialLocation()).clone();
	}

	@Override
	public Path getPath() {
		Coord lastLocation = (super.getLastLocation()).clone();
		Path path = super.getPath();
		if (!startMode) {
			controlSystem.busHasStopped(id, lastLocation, path);
		}
		startMode = false;
		return path;
	}

	@Override
	public BusMovement replicate() {
		return new BusMovement(this);
	}

	/**
	 * Returns unique ID of the bus
	 * @return unique ID of the bus
	 */
	public int getID() {
		return id;
	}

}
