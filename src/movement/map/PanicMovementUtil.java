package movement.map;

import core.Coord;

public class PanicMovementUtil {
    
	private static double safeRangeRadius;
	private static double eventRangeRadius;
	private static Coord eventLocation = null;
	public static final double RIGHT_ANGLE = 90.0;
	public static final double STRAIGHT_ANGLE = 180.0;
	public static final double FULL_ROTATION = 360.0;
	
	/**
	 * Default Constructor
	 */
	private PanicMovementUtil() {
		
	}
	/**
	 * Basic initialization of values
	 * @param parentMap Map the mobility model is based on
	 * @param location Location where the event is located
	 * @param srRadius radius where the safe zone starts
	 * @param erRadius radius where the zone ends where you can help
	 */
	public static synchronized void init(Coord location, double srRadius, double erRadius) {
		eventLocation = new Coord(location.getX(), location.getY());
		safeRangeRadius = srRadius;
		eventRangeRadius = erRadius;
	}
	
	/**
	 * Computes the nearest Node on the map that leads at least 90° away from an event,
	 * i.e. the angle between eventLocation -> locationNode and 
	 * eventLocation -> returnNode should be at least 90°. 
	 * @param locationNode location of the node that is closest to the corresponding host
	 */
	public static MapNode selectDestination(SimMap map, MapNode locationNode) {
		
		if (eventLocation.distance(locationNode.getLocation()) <= eventRangeRadius &&
				eventLocation.distance(locationNode.getLocation()) >= safeRangeRadius) {
			 //everything's fine
			return locationNode;
		}
		
		MapNode bestNode = getBestNode(map, locationNode);
		
		
		// if no better node is found, the node can stay at the current location
		if (bestNode == null) {
			return locationNode;
		} else {
			return bestNode;
		}
	}
	
	/**
	 * Computes the nearest Node in the target zone
	 * @param map Map that the computation refers to
	 * @param locationNode current location of the host
	 * @return nearest safe node
	 */
	private static MapNode getBestNode(SimMap map, MapNode locationNode) {
		MapNode nearestSafeNode = null;
		double shortestDistance = Double.MAX_VALUE;
		
		for (MapNode node : map.getNodes()) {
			double angle;
			if (eventLocation.distance(node.getLocation()) >= safeRangeRadius
				&& eventLocation.distance(node.getLocation()) <= eventRangeRadius
				&& (locationNode.getLocation().distance(node.getLocation()) < shortestDistance)
				&& !isInEventDirection(eventLocation, locationNode, node)) {
						nearestSafeNode = node; 
						shortestDistance = locationNode.getLocation().distance(node.getLocation());
			}
		}
		
		return nearestSafeNode;
	}
	
	/**
	 * Computes if the target node is in event direction from the source node's point of view
	 * @param eventLocation location where the event occurred
	 * @param sourceNode current location of the node
	 * @param targetNode potential target location
	 * @return
	 */
	public static boolean isInEventDirection(Coord eventLocation, MapNode sourceNode, MapNode targetNode) {
		double angle;
		
		if (lengthProduct(eventLocation, sourceNode.getLocation(), targetNode.getLocation()) <= 0.0) {
			// to avoid division by zero. Every angle should be fine
			angle = STRAIGHT_ANGLE; 
		}
		else {
			angle = computeAngleBetween(eventLocation, sourceNode, targetNode);
		}
		
		// Does the node meet all conditions?
		return !(Math.abs(angle - STRAIGHT_ANGLE) < RIGHT_ANGLE 
				|| eventLocation.distance(sourceNode.getLocation()) > eventRangeRadius);
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
			// This case avoids division by zeor
			return STRAIGHT_ANGLE;
		}
		else {
			return Math.acos(scalarProduct / lengthProduct) * FULL_ROTATION/Math.PI;
		}
	}
}
