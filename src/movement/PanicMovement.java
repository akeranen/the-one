package movement;

import java.util.List;

import core.Coord;
import core.Settings;
import movement.map.MapNode;
import movement.map.PanicMovementUtil;
import movement.map.SimMap;

public class PanicMovement extends ShortestPathMapBasedMovement implements SwitchableMovement {
    
	private Coord eventLocation;	
	//TODO Define a way to set this attribute
	private static double safeRangeRadius;
	//TODO Define a way to set this attribute
	private static double eventRangeRadius;
	
	private static final double SAFE_RANGE_RADIUS = 300.0;
	private static final double EVENT_RANGE_RADIUS = 500.0;
	private static final double C1000 = 1000.0;
	
	
	public PanicMovement (Settings settings, Coord location, double safeRangeRadius, double eventRangeRadius) {
		super(settings);
		eventLocation = location;
		PanicMovementUtil.init(eventLocation, safeRangeRadius, eventRangeRadius);
	}
	
	/**
	 * Constructor setting values for the event and the minimum and maximum distance to 
	 * an event (INNER_ZONE, OUTER_ZONE). 
	 * @param settings Settings for the map, hosts etc.
	 */
	public PanicMovement (Settings settings) {
		this(settings, new Coord(C1000, C1000), SAFE_RANGE_RADIUS, EVENT_RANGE_RADIUS);
	}
	
	/**
	 * Additional constructor for JUnit Tests
	 * @param settings Settings for the map, hosts etc.
	 * @param newMap Map passed instead of reading it from a file
	 * @param nrofMaps Number of WKT files
	 * @param eventLocation Coordinates of an event that occurred
	 * @param securityZone minimum distance from the event to be safe
	 * @param outerZone maximum distance to the event to get help
	 */
	public PanicMovement (Settings settings, SimMap newMap, int nrofMaps,
			Coord location, double srRadius, double erRadius) {
		super(settings, newMap, nrofMaps);
		eventLocation = location;
		safeRangeRadius = srRadius;
		eventRangeRadius = erRadius;
		PanicMovementUtil.init(eventLocation, safeRangeRadius, eventRangeRadius);
	}
	
	/**
	 * Determines a path to the most suitable node in the security zone
	 */
	public Path getPath() {
		Path p = new Path(generateSpeed());
		Coord hostLocation = host.getLocation();
		MapNode hostNode = getNearestNode(map, hostLocation);
		MapNode to = PanicMovementUtil.selectDestination(map, hostNode);
		
		List<MapNode> nodePath = pathFinder.getShortestPath(hostNode, to);

		// this assertion should never fire if the map is checked in read phase
		int pathSize = nodePath.size();
		assert pathSize > 0 : "No path from " + hostNode + " to " +
			to + ". The simulation map isn't fully connected.";

		for (MapNode node : nodePath) { // create a Path from the shortest path
			p.addWaypoint(node.getLocation());
		}

		lastMapNode = to;

		return p;
	}
	
	/**
	 * Copyconstructor.
	 * @param mbm The PanicMovement prototype to base
	 * the new object to
	 */
	protected PanicMovement(PanicMovement pm) {
		super(pm);
		eventLocation = pm.eventLocation;
	}
	
	public void setEventLocation(Coord eventLocation) {
		this.eventLocation = eventLocation;
	}
	
	public Coord getEventLocation() {
		return eventLocation;
	}
	
	@Override
	public PanicMovement replicate() {
		return new PanicMovement(this);
	}
	
	public static double getSafeRangeRadius() {
		return safeRangeRadius;
	}
	
	public static double getEventRangeRadius() {
		return eventRangeRadius;
	}
	
	public static void setSafeRangeRadius(double radius) {
		safeRangeRadius = radius;
	}
	
	public static void setEventRangeRadius(double radius) {
		eventRangeRadius = radius;
	}
	
	/**
	 * Determines the next node to a given location
	 * @param map Map which contains coordinates
	 * @param location Location to find the next node to
	 * @return the next node to parameter location
	 */
	private MapNode getNearestNode(SimMap map, Coord location) {
		List<MapNode> list = map.getNodes();
		MapNode bestNode = null;
		double distance, bestDistance = 0;
		
		for (MapNode node : list) {
			distance = location.distance(node.getLocation());
			if (bestNode == null || distance < bestDistance) {
				bestNode = node;
				bestDistance = distance;
			}
		}
			return bestNode;	
	}
	
}
