package movement;

import java.util.List;
import core.Settings;
import movement.map.MapNode;

public class TransportingMovement extends ShortestPathMapBasedMovement{
	
	/** the destination of the transport */
	private MapNode transportDestination;
	/** the location of the event */
	private MapNode eventLocation;
	
	/**
	 * Creates a new TransportingMovement model based on a Settings object's settings.
	 * @param settings The Settings object where the settings are read from
	 */
	public TransportingMovement(Settings settings){
		super(settings);
		//to choose some specific points, need to change pois from settings
		this.transportDestination = pois.selectDestination();
		this.eventLocation = pois.selectDestination();
	}
	
	/**
	 * Copyconstructor.
	 * @param tm The TransportingMovement prototype to base the new object to
	 */
	protected TransportingMovement(TransportingMovement tm){
		super(tm);
		this.transportDestination = tm.transportDestination;
		this.eventLocation = tm.eventLocation;
	}
	
	@Override
	public Path getPath(){
		Path path = new Path(generateSpeed());
		MapNode to = selectDestination();
		
		List<MapNode> nodePath = pathFinder.getShortestPath(lastMapNode, to);
		 //this assertion should never fire if the map is checked in read phase
		assert nodePath.size() > 0 : "No path from " + lastMapNode + " to " +
			to + ". The simulation map isn't fully connected";

		for (MapNode node : nodePath) { // create a Path from the shortest path
			path.addWaypoint(node.getLocation());
		}
		
		lastMapNode = to;
		
		return path;
	}
	
	@Override
	public TransportingMovement replicate(){
		return new TransportingMovement(this);
	}
	
	public MapNode selectDestination(){
		MapNode destination;
		if(host.getLocation().compareTo(eventLocation.getLocation()) != 0 && 
				host.getLocation().compareTo(transportDestination.getLocation()) != 0){
			destination = eventLocation;
		}
		else if(host.getLocation().compareTo(eventLocation.getLocation()) == 0){
			destination = transportDestination;
		}
			else {
				isReady();
				destination = transportDestination;
			}
		return destination;
	}
}