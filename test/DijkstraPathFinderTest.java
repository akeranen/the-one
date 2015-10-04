/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package test;

import java.util.List;

import junit.framework.TestCase;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import core.Coord;

public class DijkstraPathFinderTest extends TestCase {
	private DijkstraPathFinder r;

	private final MapNode n1 = newNode(0,0);
	private final MapNode n2 = newNode(10,0);
	private final MapNode n3 = newNode(20,0);
	private final MapNode n4 = newNode(0,10);
	private final MapNode n5 = newNode(10,10);
	private final MapNode n6 = newNode(15,10);
	private final MapNode n7 = newNode(20,10);
	private final MapNode n8 = newNode(25,10);
	
	protected void setUp() throws Exception {
		super.setUp();
		r = new DijkstraPathFinder(null);
		createTopology();
	}
	
	/**
	 * Creates a topology:
	 * 
	 * n1-10-n2---10---n3
	 * 10    10      / 10
	 * n4-10-n5-5-n6-5-n7-5-n8
	 */
	private void createTopology() {
		n1.addNeighbor(n2);
		n1.addNeighbor(n4);
		n2.addNeighbor(n1);
		n2.addNeighbor(n5);
		n2.addNeighbor(n3);
		n3.addNeighbor(n2);
		n3.addNeighbor(n6);
		n3.addNeighbor(n7);
		n4.addNeighbor(n1);
		n4.addNeighbor(n5);
		n5.addNeighbor(n4);
		n5.addNeighbor(n2);
		n5.addNeighbor(n6);
		n6.addNeighbor(n5);
		n6.addNeighbor(n3);
		n6.addNeighbor(n7);
		n7.addNeighbor(n6);
		n7.addNeighbor(n3);
		n7.addNeighbor(n8);
		n8.addNeighbor(n7);
	}
	
	private MapNode newNode(double x, double y) {
		return new MapNode(new Coord(x,y));
	}

	public void testPathFinding() {
		checkPath(getPath(n1,n1), n1);
		checkPath(getPath(n1,n3), n1, n2, n3);
		checkPath(getPath(n1,n6), n1, n2, n5, n6);
		checkPath(getPath(n5,n3), n5, n6, n3);
		checkPath(getPath(n3,n5), n3, n6, n5);
		checkPath(getPath(n4,n8), n4, n5, n6, n7, n8);
		checkPath(getPath(n8,n4), n8, n7, n6, n5, n4);
	}
	
	private void checkPath(List<MapNode> path, MapNode ... nodes) {
		assertEquals(nodes.length,path.size());
		
		for (int i=0; i< nodes.length; i++) {
			assertEquals((i+1)+"th node was wrong",nodes[i],path.get(i));
		}
	}
	
	private List<MapNode> getPath(MapNode from, MapNode to) {
		List<MapNode> path = r.getShortestPath(from, to);
		return path;
	}
	
	
}
