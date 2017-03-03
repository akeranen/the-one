package movement;

import java.util.List;

import core.Coord;
import core.Settings;
import movement.map.MapNode;
import movement.map.PanicMovementUtil;
import movement.map.SimMap;

/**
 * 
 * @author Nils Weidmann
 * Implementation of the Panic Mobility Model. If an event occurs, all DTNHosts move towards the "Safe Zone" 
 * between the save range radius and the event range radius. Furthermore, they do not cross the event, such that
 * the angle between them, the event and the target is at most 90°
 */
public class PanicMovement extends ShortestPathMapBasedMovement implements SwitchableMovement {
    
	private Coord eventLocation;	
	private double safeRangeRadius;
	private double eventRangeRadius;
	
	private PanicMovementUtil pmu;
	private static final double SAFE_RANGE_RADIUS = 1000.0;
	private static final double EVENT_RANGE_RADIUS = 1500.0;
	private static final double COORD1000 = 1500.0;
	
	
	public PanicMovement (Settings settings, Coord location, double safeRangeRadius, double eventRangeRadius) {
		super(settings);
		setLocalFields(location, safeRangeRadius, eventRangeRadius);
	}
	
	/**
	 * Constructor setting values for the event and the minimum and maximum distance to 
	 * an event (SAFE_RANGE_RADIUS, EVENT_RANGE_RADIUS). 
	 * @param settings Settings for the map, hosts etc.
	 */
	public PanicMovement (Settings settings) {
		this(settings, new Coord(COORD1000, COORD1000), SAFE_RANGE_RADIUS, EVENT_RANGE_RADIUS);
	}

    /**
     * Copyconstructor.
     * @param pm The PanicMovement prototype to base
     * the new object to
     */
    protected PanicMovement(PanicMovement pm) {
        super(pm);
        setLocalFields(pm.eventLocation, pm.safeRangeRadius, pm.eventRangeRadius);
    }
	
	/**
	 * Additional constructor for JUnit Tests
	 * @param settings Settings for the map, hosts etc.
	 * @param newMap Map passed instead of reading it from a file
	 * @param nrofMaps Number of WKT files
	 */
	public PanicMovement (Settings settings, SimMap newMap, int nrofMaps,
			Coord location, double safeRangeRadius, double eventRangeRadius) {
		super(settings, newMap, nrofMaps);
		setLocalFields(location, safeRangeRadius, eventRangeRadius);
	}
	
	/**
	 * Sub routine called from the Constructors
	 * @param eventLocation location where the event occurs 
	 * @param safeRangeRadius distance to the event from which on the nodes are safe
	 * @param eventRangeRadius distance within which the nodes react to an event
	 */
	private void setLocalFields (Coord eventLocation, double safeRangeRadius, double eventRangeRadius) {
		this.eventLocation = eventLocation;
		this.safeRangeRadius = safeRangeRadius;
		this.eventRangeRadius = eventRangeRadius;
		pmu = new PanicMovementUtil(eventLocation, safeRangeRadius, eventRangeRadius);
	}
	/**
	 * Determines a path to the most suitable node in the security zone
	 */
	@Override
	public Path getPath() {
		Path path = new Path(generateSpeed());
		Coord hostLocation = host.getLocation();
		MapNode hostNode = getNearestNode(getMap(), hostLocation);
		MapNode destNode = pmu.selectDestination(getMap(), hostNode);
		
		List<MapNode> nodePath = pathFinder.getShortestPath(hostNode, destNode);

		// this assertion should never fire if the map is checked in read phase
		int pathSize = nodePath.size();
		assert pathSize > 0 : "No path from " + hostNode + " to " +
		destNode + ". The simulation map isn't fully connected.";

		for (MapNode node : nodePath) { // create a Path from the shortest path
			path.addWaypoint(node.getLocation());
		}

		lastMapNode = destNode;

		return path;
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
	
	public double getSafeRangeRadius() {
		return safeRangeRadius;
	}
	
	public double getEventRangeRadius() {
		return eventRangeRadius;
	}
	
	public void setSafeRangeRadius(double radius) {
		safeRangeRadius = radius;
	}
	
	public void setEventRangeRadius(double radius) {
		eventRangeRadius = radius;
	}
	
	public PanicMovementUtil getPanicMovementUtil() {
		return pmu;
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
		double distance;
		double bestDistance = 0;
		
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
