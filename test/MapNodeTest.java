/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package test;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import movement.map.MapNode;
import movement.map.SimMap;
import core.Coord;

public class MapNodeTest extends TestCase {
	private MapNode n1,n2,n3,n4;
	private Coord c1,c2,c3,c4;
	private SimMap map;
	
	public void setUp() {
		c1 = new Coord(10,10);
		c2 = new Coord(20,20);
		c3 = new Coord(30,30);
		c4 = new Coord(2552448.388211649, 6673384.4020657055);
		
		n1 = new MapNode(c1);
		n2 = new MapNode(c2);
		n3 = new MapNode(c3);
		n4 = new MapNode(c4);
		
		Map<Coord, MapNode> cmMap = new HashMap<Coord, MapNode>();
		cmMap.put(c1, n1);
		cmMap.put(c2, n2);
		cmMap.put(c3, n3);
		cmMap.put(c4, n4);
		
		map = new SimMap(cmMap);
	}

	public void testAddNeighbor() {
		assertTrue(n1.getNeighbors().size() == 0);
		assertTrue(n2.getNeighbors().size() == 0);

		// n1--n2
		n1.addNeighbor(n2);
		n2.addNeighbor(n1);
		assertTrue(n1.getNeighbors().size() == 1);
		assertTrue(n2.getNeighbors().size() == 1);
		
		// n1--n2--n3
		n2.addNeighbor(n3);
		n3.addNeighbor(n2);
		assertTrue(n2.getNeighbors().size() == 2);
		assertTrue(n3.getNeighbors().size() == 1);
		assertTrue(n1.getNeighbors().size() == 1);
		
		// add same again
		n2.addNeighbor(n3);
		n3.addNeighbor(n2);
		assertTrue(n2.getNeighbors().size() == 2);
		assertTrue(n3.getNeighbors().size() == 1);		
	}

	public void testNodeByCoord() {
		Coord nearC1 = c1.clone();		
		nearC1.translate(0.1, 0.1);
		
		assertNull(map.getNodeByCoord(nearC1));
		assertNull(map.getNodeByCoord(new Coord(1233213,123123123)));
		
		assertEquals(n1, map.getNodeByCoord(c1));
		assertEquals(n3, map.getNodeByCoord(c3));
		
		assertEquals(n1, map.getNodeByCoord(c1.clone()));
		assertEquals(n1, map.getNodeByCoord(new Coord(c1.getX(), c1.getY())));
		assertEquals(n4, map.getNodeByCoord(new Coord(c4.getX(), c4.getY())));
		
		Coord c4Clone = c4.clone();
		Coord c4Clone2 = c4.clone();

		c4Clone.setLocation(c4Clone.getX(), -c4Clone.getY());
		c4Clone2.setLocation(c4Clone2.getX(), -c4Clone2.getY());
	}

}
