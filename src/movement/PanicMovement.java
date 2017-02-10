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
	
	public PanicMovement (Settings settings) {
		super(settings);
		// Only temporary solution for testing issues!
		//eventLocation = new Coord(getMaxX() / 4, getMaxY() / 4);
		eventLocation = new Coord(1000.0, 1000.0);
		
		this.pois = new PanicPointsOfInterest(getMap(), getOkMapNodeTypes(),
					settings, rng, eventLocation);
	}
	
	public Path getPath(DTNHost host) {
		Path p = new Path(generateSpeed());
		Coord hostLocation = host.getLocation();
		MapNode hostNode = getNextNode(map, hostLocation);
		MapNode to = ((PanicPointsOfInterest)pois).selectDestination(hostNode);
		
		List<MapNode> nodePath = pathFinder.getShortestPath(hostNode, to);

		// this assertion should never fire if the map is checked in read phase
		assert nodePath.size() > 0 : "No path from " + hostNode + " to " +
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
		double distance = 0, bestDistance = 0;
		
		for (MapNode node : list) {
			distance = PanicPointsOfInterest.getDistance(location, node.getLocation());
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
