package movement.map;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import core.Settings;
import util.Tuple;
import core.Coord;

public class PanicPointsOfInterest extends PointsOfInterest {
    
	private Coord locationNode;
	private Coord locationEvent;
	
	public PanicPointsOfInterest(SimMap parentMap, int [] okMapNodeTypes, 
			Settings settings, Random rng, Coord locationNode, Coord locationEvent) {
		super(parentMap, okMapNodeTypes, settings, rng);
		this.locationNode = locationNode;
		this.locationEvent = locationEvent;
		/*this.poiLists = new ArrayList<List<MapNode>>();
		this.poiProbs = new LinkedList<Tuple<Double, Integer>>();
		this.map = parentMap;
		this.okMapNodeTypes = okMapNodeTypes;
		this.rng = rng;
		readPois(settings);*/
	}
	
	public Coord getLocationNode() {
		return locationNode;
	}
	
	public Coord getLocationEvent() {
		return locationEvent;
	}
	
	public void setLocationNode(Coord locationNode) {
		this.locationNode = locationNode;
	}
	
	public void setLocationEvent(Coord locationEvent) {
		this.locationEvent = locationEvent;
	}
	
	/**
	 * Computes the POI on the map that leads straightest away from an event,
	 * i.e. the angle between locationNode -> location Event and 
	 * locationEvent -> locationPOI should be as close to 180° as possible
	 */
	@Override
	public MapNode selectDestination() {
		
		double bestAngle = 0, angle = 0;
		
		MapNode bestNode = null;
		
		// if List of Points of Interest is empty, add all nodes
		if (poiLists.size() == 0) {	
			poiLists.add(map.getNodes());
		}
		
		for (List<MapNode> list : poiLists) {
			for (MapNode node : list) {
				if (bestNode == null) {
					bestNode = node;
					bestAngle = Math.acos(scalarProduct(locationEvent, locationNode, bestNode.getLocation()) /
							              lengthProduct(locationEvent, locationNode, bestNode.getLocation())) * 180/Math.PI;
				}
				else {
					angle = Math.acos(scalarProduct(locationEvent, locationNode, node.getLocation()) /
				              		  lengthProduct(locationEvent, locationNode, node.getLocation())) * 180/Math.PI;
					if (Math.abs(angle - 180) < Math.abs(bestAngle - 180)) {
						bestNode = node;
						bestAngle = angle;
					}
				}
			}
		}
		
		return bestNode;
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
	 * Computes the lenght product between the vectors v1 (source -> target1) and v2 (source -> target2)
	 */
	private static double lengthProduct(Coord target1, Coord source, Coord target2) {
		double discriminante1 = 0;
		double discriminante2 = 0;
		double[] v1 = {target1.getX() - source.getX(), target1.getY() - source.getY()}; 
		double[] v2 = {target2.getX() - source.getX(), target2.getY() - source.getY()};
		
		for (int i = 0; i < v1.length; i++) {
			discriminante1 += v1[i]*v1[i];
		}
		
		for (int i = 0; i < v2.length; i++) {
			discriminante2 += v2[i]*v2[i];
		}
		
		return Math.sqrt(discriminante1) * Math.sqrt(discriminante2);
	}
}
