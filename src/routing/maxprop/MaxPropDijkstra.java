/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing.maxprop;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * Dijkstra's shortest path implementation for MaxProp Router module.
 */
public class MaxPropDijkstra {
	/** Value for infinite distance  */
	private static final Double INFINITY = Double.MAX_VALUE;
	/** Initial size of the priority queue */
	private static final int PQ_INIT_SIZE = 11;

	/** Map of node distances from the source node */
	private DistanceMap distancesFromStart;
	/** Set of already visited nodes (where the shortest path is known) */
	private Set<Integer> visited;
	/** Priority queue of unvisited nodes discovered so far */
	private Queue<Integer> unvisited;
	/** Map of previous nodes on the shortest path(s) -- only used for
	 * debugging purposes */
	private Map<Integer, Integer> prevNodes;
	/** Mapping of to other nodes' (whom this node has met) probability sets */
	private Map<Integer, MeetingProbabilitySet> probs;

	/**
	 * Constructor.
	 * @param probs A reference to the mapping of the known hosts meeting
	 * probability sets
	 */
	public MaxPropDijkstra(Map<Integer, MeetingProbabilitySet> probs) {
		this.probs = probs;
	}

	/**
	 * Initializes a new search with the first hop router node
	 * @param firstHop The first hop router node
	 */
	private void initWith(Integer firstHop) {
		this.unvisited = new PriorityQueue<Integer>(PQ_INIT_SIZE,
				new DistanceComparator());
		this.visited = new HashSet<Integer>();
		this.prevNodes = new HashMap<Integer, Integer>();
		this.distancesFromStart = new DistanceMap();

		// set distance to source 0 and initialize unvisited queue
		this.distancesFromStart.put(firstHop, 0);
		this.unvisited.add(firstHop);
	}

	/**
	 * Calculates total costs to the given set of target nodes. The cost to
	 * a node is the sum of complements of probabilities that all the links
	 * come up as the next contact of the nodes.
	 * @param from The index (address) of the start node
	 * @param to The address set of destination nodes
	 * @return A map of (destination node, cost) tuples
	 */
	public Map<Integer, Double> getCosts(Integer from, Set<Integer> to) {
		Map<Integer, Double> distMap = new HashMap<Integer, Double>();
		int nrofNodesToFind = to.size();

		initWith(from);
		Integer node = null;

		// always take the node with shortest distance
		while ((node = unvisited.poll()) != null) {
			if (to.contains(node)) {
				// found one of the requested nodes
				distMap.put(node, distancesFromStart.get(node));
				nrofNodesToFind--;
				if (nrofNodesToFind == 0) {
					break; // all requested nodes found
				}
			}

			visited.add(node); // mark the node as visited
			relax(node);       // add/update neighbor nodes' distances
		}

		return distMap;
	}

	/**
	 * Relaxes the neighbors of a node (updates the shortest distances).
	 * @param node The node whose neighbors are relaxed
	 */
	private void relax(Integer node) {
		double nodeDist = distancesFromStart.get(node);
		Collection<Integer> neighbors;

		if (!this.probs.containsKey(node)) {
			return; // node's neighbors are not known
		}

		neighbors =	this.probs.get(node).getAllProbs().keySet();

		for (Integer n : neighbors) {
			if (visited.contains(n)) {
				continue; // skip visited nodes
			}

			// n node's distance from path's source node
			double nDist = nodeDist + getDistance(node, n);

			if (distancesFromStart.get(n) > nDist) {
				// stored distance > found dist -> update
				prevNodes.put(n, node); // for debugging
				setDistance(n, nDist);
			}
		}
	}

	/**
	 * Sets the distance from source node to a node
	 * @param n The node whose distance is set
	 * @param distance The distance of the node from the source node
	 */
	private void setDistance(Integer n, double distance) {
		unvisited.remove(n); // remove node from old place in the queue
		distancesFromStart.put(n, distance); // update distance
		unvisited.add(n); // insert node to the new place in the queue
	}

	/**
	 * Returns the "distance" between two nodes, i.e., the complement of the
	 * probability that the next node "from" meets is "to". Works only if there
	 * exist a probability value for "from" meeting "to".
	 * @param from The first node
	 * @param to The second node
	 * @return The distance between the two nodes
	 */
	private double getDistance(Integer from, Integer to) {
		assert probs.containsKey(from) : "Node " + from + " has not met " + to +
			" (it has met nodes " + probs.keySet() + ")";
		return ( 1 - probs.get(from).getProbFor(to) );
	}

	/**
	 * Comparator that compares two nodes by their distance from
	 * the source node.
	 */
	private class DistanceComparator implements
		Comparator<Integer> {

		/**
		 * Compares two map nodes by their distance from the source node
		 * @return -1, 0 or 1 if node1's distance is smaller, equal to, or
		 * bigger than node2's distance
		 */
		public int compare(Integer node1, Integer node2) {
			double dist1 = distancesFromStart.get(node1);
			double dist2 = distancesFromStart.get(node2);

			if (dist1 > dist2) {
				return 1;
			}
			else if (dist1 < dist2) {
				return -1;
			}
			else {
				return node1.compareTo(node2);
			}
		}
	}

	/**
	 * Simple Map implementation for storing distances.
	 */
	private class DistanceMap {
		private HashMap<Integer, Double> map;

		/**
		 * Constructor. Creates an empty distance map
		 */
		public DistanceMap() {
			this.map = new HashMap<Integer, Double>();
		}

		/**
		 * Returns the distance to a node. If no distance value
		 * is found, returns {@link MaxPropDijkstra#INFINITY} as the value.
		 * @param node The node whose distance is requested
		 * @return The distance to that node
		 */
		public double get(Integer node) {
			Double value = map.get(node);
			if (value != null) {
				return value;
			}
			else {
				return INFINITY;
			}
		}

		/**
		 * Puts a new distance value for a map node
		 * @param node The node
		 * @param distance Distance to that node
		 */
		public void  put(Integer node, double distance) {
			map.put(node, distance);
		}

		/**
		 * Returns a string representation of the map's contents
		 * @return a string representation of the map's contents
		 */
		public String toString() {
			return map.toString();
		}
	}
}
