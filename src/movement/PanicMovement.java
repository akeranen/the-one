package movement;

import java.util.List;

import core.Coord;
import core.DTNHost;
import core.Settings;
import movement.map.MapNode;
import movement.map.PanicPointsOfInterest;
import movement.map.SimMap;

public class PanicMovement extends ShortestPathMapBasedMovement implements SwitchableMovement {
    
	private Coord eventLocation;
	private static final double c1000 = 1000.0;
	private static final double INNER_ZONE = 300.0;
	private static final double OUTER_ZONE = 500.0;
	
	public PanicMovement (Settings settings, Coord eventLocation, double securityZone, double outerZone) {
		super(settings);
		this.eventLocation = eventLocation;
		this.pois = new PanicPointsOfInterest(getMap(), getOkMapNodeTypes(),
					settings, rng, eventLocation, securityZone, outerZone);
	}
	
	public PanicMovement (Settings settings) {
		super(settings);
		this.eventLocation = new Coord(c1000, c1000);
		this.pois = new PanicPointsOfInterest(getMap(), getOkMapNodeTypes(),
				settings, rng, eventLocation, INNER_ZONE, OUTER_ZONE);
	}
	
	public PanicMovement (Settings settings, SimMap newMap, int nrofMaps,
			Coord eventLocation, double securityZone, double outerZone) {
		super(settings, newMap, nrofMaps );
		this.eventLocation = eventLocation;
		this.pois = new PanicPointsOfInterest(newMap, getOkMapNodeTypes(),
					settings, rng, eventLocation, securityZone, outerZone);
	}
	
	public Path getPath(DTNHost host) {
		Path p = new Path(generateSpeed());
		Coord hostLocation = host.getLocation();
		MapNode hostNode = getNextNode(map, hostLocation);
		MapNode to = ((PanicPointsOfInterest)pois).selectDestination(hostNode);
		
		List<MapNode> nodePath = pathFinder.getShortestPath(hostNode, to);

		// this assertion should never fire if the map is checked in read phase
		int pathSize = nodePath.size();
		assert pathSize > 0 : "No path from " + hostNode + " to " +
			to + ". The simulation map isn't fully connected";

		for (MapNode node : nodePath) { // create a Path from the shortest path
			p.addWaypoint(node.getLocation());
		}

		lastMapNode = to;

		return p;
	}
	
	/**
	 * Copyconstructor.
	 * @param mbm The ShortestPathMapBasedMovement prototype to base
	 * the new object to
	 */
	protected PanicMovement(PanicMovement pm) {
		super(pm);
		this.eventLocation = pm.eventLocation;
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
	
	private MapNode getNextNode(SimMap map, Coord location) {
		List<MapNode> list = map.getNodes();
		MapNode bestNode = null;
		double distance, bestDistance = 0;
		
		for (MapNode node : list) {
			distance = location.distance(node.getLocation());
			if (bestNode == null) {
				bestNode = node;
				bestDistance = distance;
			}
			else if (distance < bestDistance){
				bestNode = node;
				bestDistance = distance;
			}
		}
			return bestNode;
	}
	
}
