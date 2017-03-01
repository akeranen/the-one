package movement.map;

import core.Coord;

public class PanicMovementUtil {
    
	private double safeRangeRadius;
	private double eventRangeRadius;
	private Coord eventLocation = null;
	public static final double RIGHT_ANGLE = 90.0;
	public static final double STRAIGHT_ANGLE = 180.0;
	public static final double FULL_ROTATION = 360.0;
	
	/**
	 * Constructor
	 * @param location Location where the event is located
	 * @param safeRangeRadius radius where the safe zone starts
	 * @param eventRangeRadius radius where the zone ends where you can help
	 */
	public PanicMovementUtil(Coord eventLocation, double safeRangeRadius, double eventRangeRadius) {
		this.eventLocation = eventLocation;
		this.safeRangeRadius = safeRangeRadius;
		this.eventRangeRadius = eventRangeRadius;
	}
	
	/**
	 * Computes the nearest Node on the map that leads at least 90° away from an event,
	 * i.e. the angle between eventLocation -> locationNode and 
	 * eventLocation -> returnNode should be at least 90°. 
	 * @param locationNode location of the node that is closest to the corresponding host
	 */
	public MapNode selectDestination(SimMap map, MapNode locationNode) {
		double distance = eventLocation.distance(locationNode.getLocation());
		if (distance <= eventRangeRadius && distance>= safeRangeRadius ||
			// The host is within the safe area or
			eventLocation.distance(locationNode.getLocation()) > eventRangeRadius) {
			// The host is not concerned by the event 	
			return locationNode;
		}
		
		MapNode bestNode = getBestNode(map, locationNode);
		
		return bestNode;
	}
	
	/**
	 * Computes the nearest Node in the target zone
	 * @param map Map that the computation refers to
	 * @param locationNode current location of the host
	 * @return nearest safe node
	 */
	private MapNode getBestNode(SimMap map, MapNode locationNode) {
		MapNode nearestSafeNode = null;
		double shortestDistance = Double.MAX_VALUE;
		
		for (MapNode node : map.getNodes()) {
			double distance = eventLocation.distance(node.getLocation());
			if ( distance >= safeRangeRadius && distance <= eventRangeRadius
				&& (locationNode.getLocation().distance(node.getLocation()) < shortestDistance)
				&& !isInEventDirection(locationNode, node)) {
						nearestSafeNode = node; 
						shortestDistance = locationNode.getLocation().distance(node.getLocation());
			}
		}
		
		// if no better node is found, the node can stay at the current location
		if (nearestSafeNode == null) {
			return locationNode;
		}
		else {
			return nearestSafeNode;
		}
	}
	
	/**
	 * Computes if the target node is in event direction from the source node's point of view
	 * @param eventLocation location where the event occurred
	 * @param sourceNode current location of the node
	 * @param targetNode potential target location
	 * @return
	 */
	public boolean isInEventDirection(MapNode sourceNode, MapNode targetNode) {
		double angle;
		
		if (lengthProduct(eventLocation, sourceNode.getLocation(), targetNode.getLocation()) <= 0.0) {
			// to avoid division by zero. Every angle should be fine
			if (eventLocation.distance(targetNode.getLocation()) <= 0.0) {
				// event = target --> IN event direction
				return true;
			}
			else {
				// event = source --> NOT IN event direction
				return false;
			}
		}
		else {
			angle = computeAngleBetween(eventLocation, sourceNode, targetNode);
		}
		
		return (Math.abs(angle - STRAIGHT_ANGLE) < RIGHT_ANGLE);
	}
	
	/**
	 * Computes the scalar product between the vectors v1 (source -> target1) and v2 (source -> target2)
	 */
	public static double scalarProduct(Coord target1, Coord source, Coord target2) {
		double scalarProduct = 0;
		double[] v1 = {target1.getX() - source.getX(), target1.getY() - source.getY()}; 
		double[] v2 = {target2.getX() - source.getX(), target2.getY() - source.getY()};
		
		for (int i = 0; i < v1.length; i++) {
			scalarProduct += v1[i] * v2[i];
		}
		
		return scalarProduct;
	}
	
	/**
	 * Computes the length product between the vectors v1 (source -> target1) and v2 (source -> target2)
	 */
	public static double lengthProduct(Coord target1, Coord source, Coord target2) {
		
		return source.distance(target1) * source.distance(target2);
	}
	
	public static double computeAngleBetween(Coord angleLocation, MapNode sourceNode, MapNode targetNode) {
		double scalarProduct = scalarProduct(angleLocation, sourceNode.getLocation(), targetNode.getLocation());
		double lengthProduct = lengthProduct(angleLocation, sourceNode.getLocation(), targetNode.getLocation());
		
		if (lengthProduct <= 0) {
			// This case avoids division by zero. Since this case is also handled at the caller, it should never happen
			return STRAIGHT_ANGLE;
		}
		else {
			return Math.acos(scalarProduct / lengthProduct) * FULL_ROTATION/Math.PI;
		}
	}
}
