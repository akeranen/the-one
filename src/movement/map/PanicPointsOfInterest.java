package movement.map;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import core.Settings;
import util.Tuple;
import core.Coord;

public class PanicPointsOfInterest extends PointsOfInterest {
    
	public static final double SECURITY_ZONE = 1000.0;
	public static final double OUTER_ZONE = 1100.0;
	private Coord locationEvent;
	
	public PanicPointsOfInterest(SimMap parentMap, int [] okMapNodeTypes, 
			Settings settings, Random rng, Coord locationEvent) {
		super(parentMap, okMapNodeTypes, settings, rng);
		this.locationEvent = locationEvent;
	}
	
	public Coord getLocationEvent() {
		return locationEvent;
	}
	
	public void setLocationEvent(Coord locationEvent) {
		this.locationEvent = locationEvent;
	}
	
	/**
	 * Computes the POI on the map that leads straightest away from an event,
	 * i.e. the angle between locationNode -> location Event and 
	 * locationEvent -> locationPOI should be as close to 180° as possible
	 */
	
	public MapNode selectDestination(MapNode lastMapNode) {
		
		double angle = 0;
		MapNode bestNode = null;
		double bestDistance = 0;
		
		if (getDistance(locationEvent, lastMapNode.getLocation()) < OUTER_ZONE &&
			getDistance(locationEvent, lastMapNode.getLocation()) > SECURITY_ZONE) {
			return lastMapNode; //everything's fine
		}
		
		for (MapNode node : map.getNodes()) {
			if (getDistance(locationEvent,node.getLocation()) < SECURITY_ZONE) {
				continue; // point is not far enough away from the event
			}
			if (getDistance(locationEvent,node.getLocation()) > OUTER_ZONE) {
				continue; // point is not close enough to the event to search for help
			}
			if (bestNode != null) {
				if (getDistance(lastMapNode.getLocation(), node.getLocation()) > bestDistance) {
					continue; //nearer node is already known		
				}
			}
			if (lengthProduct(locationEvent, lastMapNode.getLocation(), node.getLocation()) == 0) {
				continue; // otherwise, division by zero
			}
			angle = Math.acos(scalarProduct(locationEvent, lastMapNode.getLocation(), node.getLocation()) /
				              lengthProduct(locationEvent, lastMapNode.getLocation(), node.getLocation())) * 180/Math.PI;
			if (Math.abs(angle - 180) < 90) {
				bestNode = node; // node meets all conditions
				bestDistance = getDistance(lastMapNode.getLocation(), node.getLocation());
			}
		}
		
		// if no better node is found, the node can stay at the current location
		if (bestNode == null) {
			return lastMapNode;
		}
		else {
			return bestNode;
		}
	}
	
	/**
	 * Computes the scalar product between the vectors v1 (source -> target1) and v2 (source -> target2)
	 */
	private static double scalarProduct(Coord target1, Coord source, Coord target2) {
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
	private static double lengthProduct(Coord target1, Coord source, Coord target2) {
		
		return getDistance(source, target1) * getDistance(source, target2);
	}
	
	public static double getDistance(Coord source, Coord target) {
		double discriminante = 0;
		double[] v = {target.getX() - source.getX(), target.getY() - source.getY()};
		
		for (int i = 0; i < v.length; i++) {
			discriminante += v[i]*v[i];
		}
		
		return Math.sqrt(discriminante);
	}
}
