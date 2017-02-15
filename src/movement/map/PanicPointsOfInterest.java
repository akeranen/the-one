package movement.map;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import core.Settings;
import util.Tuple;
import core.Coord;

public class PanicPointsOfInterest extends PointsOfInterest {
    
	public double securityZone;
	public double outerZone;
	private Coord locationEvent;
	
	public PanicPointsOfInterest(SimMap parentMap, int [] okMapNodeTypes, 
			Settings settings, Random rng, Coord locationEvent,
			double securityZone, double outerZone) {
		super(parentMap, okMapNodeTypes, settings, rng);
		this.locationEvent = locationEvent;
		this.securityZone = securityZone;
		this.outerZone = outerZone;
	}
	
	public Coord getLocationEvent() {
		return locationEvent;
	}
	
	public void setLocationEvent(Coord locationEvent) {
		this.locationEvent = locationEvent;
	}
	
	public double getSecurityZone() {
		return securityZone;
	}
	
	public void setSecurityZone(double securityZone) {
		this.securityZone = securityZone;
	}
	
	public double getOuterZone() {
		return outerZone;
	}
	
	public void setOuterZone(double outerZone) {
		this.outerZone = outerZone;
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
		
		if (locationEvent.distance(lastMapNode.getLocation()) < outerZone &&
			locationEvent.distance(lastMapNode.getLocation()) > securityZone) {
			return lastMapNode; //everything's fine
		}
		
		for (MapNode node : map.getNodes()) {
			if (locationEvent.distance(node.getLocation()) < securityZone) {
				continue; // point is not far enough away from the event
			}
			if (locationEvent.distance(node.getLocation()) > outerZone) {
				continue; // point is not close enough to the event to search for help
			}
			if (bestNode != null) {
				if (lastMapNode.getLocation().distance(node.getLocation()) > bestDistance) {
					continue; //nearer node is already known		
				}
			}
			if (lengthProduct(locationEvent, lastMapNode.getLocation(), node.getLocation()) == 0) {
				angle = 180; // otherwise, division by zero. Every angle should be fine
			}
			else {
			angle = Math.acos(scalarProduct(locationEvent, lastMapNode.getLocation(), node.getLocation()) /
				              lengthProduct(locationEvent, lastMapNode.getLocation(), node.getLocation())) * 360/Math.PI;
			}
			if (Math.abs(angle - 180) < 90) {
				bestNode = node; // node meets all conditions
				bestDistance = lastMapNode.getLocation().distance(node.getLocation());
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
		
		return source.distance(target1) * source.distance(target2);
	}
}
