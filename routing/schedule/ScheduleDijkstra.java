/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */

package routing.schedule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * Dijkstra's shortest path implementation for schedule data
 */
/* TODO: combine this with movement.map.DijkstraPathFinder? */
public class ScheduleDijkstra {
	/** Value for infinite distance  */
	private static final Double INFINITY = Double.MAX_VALUE;
	/** Initial size of the priority queue */
	private static final int PQ_INIT_SIZE = 11;

	/** Map of the times when one could be at certain node */
	private TimeMap times;
	/** Set of already visited nodes (where the shortest path is known) */
	private Set<Integer> visited;
	/** Priority queue of unvisited nodes discovered so far */
	private Queue<Integer> unvisited;
	/** Map of previous schedule on the shortest path(s) */
	private Map<Integer, ScheduleEntry> prevHops;
	/** Oracle that know all schedules */
	private ScheduleOracle oracle;
	
	/**
	 * Constructor.
	 * @param oracle The schedule oracle
	 * all nodes are OK
	 */
	public ScheduleDijkstra(ScheduleOracle oracle) {
		this.oracle = oracle;
	}

	/**
	 * Initializes a new search with a source node
	 * @param node The path's source node
	 * @param time The time when the path starts
	 */
	private void initWith(Integer node, double time) {	
		this.unvisited = new PriorityQueue<Integer>(PQ_INIT_SIZE, 
				new DurationComparator());
		this.visited = new HashSet<Integer>();
		this.prevHops = new HashMap<Integer, ScheduleEntry>();
		this.times = new TimeMap();
		
		this.times.put(node, time);
		this.unvisited.add(node);
	}
	
	/**
	 * Finds and returns the fastest path between two destinations
	 * @param from The source of the path
	 * @param to The destination of the path
	 * @param time The time when the path starts
	 * @return a shortest path between the source and destination nodes in
	 * a list of Integers or an empty list if such path is not available
	 */
	public List<ScheduleEntry> getShortestPath(Integer from, Integer to, 
			double time){
		List<ScheduleEntry> path = new ArrayList<ScheduleEntry>();		
		assert time >= 0.0 : "Can't use negative start time";
		
		if (from.compareTo(to) == 0) { 
			return path;
		}
		
		initWith(from, time);
		Integer node = null;
		
		while ((node = unvisited.poll()) != null) {
			if (node.equals(to)) {
				break; 
			}
			
			visited.add(node); 
			relax(node); 
		}		

		if (node != null) { // found a path
			ScheduleEntry prev = prevHops.get(to); 
			while (prev.getFrom() != from) { 
				path.add(0, prev);
				prev = prevHops.get(prev.getFrom());
			}
			
			path.add(0, prev);
		}
		
		return path;
	}
	
	/**
	 * Relaxes the neighbors of a node (updates the shortest distances).
	 * @param node The node whose neighbors are relaxed
	 */
	private void relax(Integer node) {
		double timeNow = times.get(node);
		int to;
		double timeTo;
		
		for (ScheduleEntry se : oracle.getConnected(node, timeNow)) {
			to = se.getTo();
			if (visited.contains(to)) {
				continue; // skip visited nodes
			}
			
			timeTo = se.getTime() +  se.getDuration();
			
			if (timeTo < times.get(to)) {
				prevHops.put(to, se);
				setTime(to, timeTo);
			}
		}
	}
	
	/**
	 * Sets the time when at a node
	 * @param n The node whose time is set
	 * @param time The time when at given node
	 */
	private void setTime(Integer n, double time) {
		unvisited.remove(n);
		times.put(n, time); 
		unvisited.add(n);
	}
	
	/**
	 * Comparator that compares two nodes by their journey duration
	 */
	private class DurationComparator implements Comparator<Integer> {
		
		/**
		 * Compares two nodes by their time to get there 
		 * @return -1, 0 or 1 if node1's time is smaller, equal to, or
		 * bigger than node2's
		 */
		public int compare(Integer node1, Integer node2) {
			double time1 = times.get(node1);
			double time2 = times.get(node2);
			
			if (time1 > time2) {
				return 1;
			}
			else if (time1 < time2) {
				return -1;
			}
			else {
				return 0;
			}
		}
	}
	
	private class TimeMap {
		private HashMap<Integer, Double> map;
		
		/**
		 * Constructor. Creates an empty map
		 */
		public TimeMap() {
			this.map = new HashMap<Integer, Double>(); 
		}
		
		/**
		 * Returns the currently known smallest time one has a path for to the
		 * given node. If no time value is found, returns 
		 * {@link ScheduleDijkstra#INFINITY} as the value.
		 * @param node The node whose time is requested
		 * @return The time when one could be at that node
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
		 * Puts a new time value for a node
		 * @param node The node
		 * @param time Time at that node
		 */
		public void put(Integer node, double time) {
			map.put(node, time);
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