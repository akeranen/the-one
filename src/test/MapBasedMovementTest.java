/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import input.WKTMapReader;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.List;

import junit.framework.TestCase;
import movement.MapBasedMovement;
import movement.MovementModel;
import movement.Path;
import movement.map.MapNode;
import movement.map.SimMap;
import core.Coord;
import core.DTNHost;
import core.Settings;

public class MapBasedMovementTest extends TestCase {
	/* Topology:  n7--n5
	 *            |   |
	 *        n1--n2--n6--n3
	 *         |
	 *        n4
	 */
	//										   n1       n2       n6        n3
	private final String WKT = "LINESTRING (1.0 1.0, 2.0 1.0, 3.0 1.0, 4.0 1.0) \n" +
	//              n1        n4
	"LINESTRING (1.0 1.0, 1.0 2.0)\n"+
	//              n2       n7       n3       n6
	"LINESTRING (2.0 1.0, 2.0 0.0, 3.0 0.0, 3.0 1.0)\n";

	private MapNode n1;
	private Coord c1 = new Coord(1,1);
	private MapNode n2;
	private Coord c2 = new Coord(2,1);
	private MapNode n6;
	private Coord c6 = new Coord(3,1);

	private MapBasedMovement mbm;
	private SimMap map;
	private TestSettings s;

	public void setUp() throws Exception {
		super.setUp();
		s = new TestSettings();
	}

	private void setupMapData(String okTypes, String speed, String wTime) {
		Settings.init(null);
		StringReader input = new StringReader(WKT);

		WKTMapReader reader = new WKTMapReader(true);
		try {
			reader.addPaths(input, 0);
		} catch (IOException e) {
			fail(e.toString());
		}

		s = new TestSettings();
		s.putSetting(MovementModel.SPEED, (speed != null ? speed : "1,1"));
		s.putSetting(MovementModel.WAIT_TIME, (wTime != null ? wTime : "0,0"));

		if (okTypes != null) {
			s.putSetting(MapBasedMovement.MAP_SELECT_S, okTypes);
		}
		map = reader.getMap();
		mbm = new MapBasedMovement(s, map, 3); // accepts types 1-3

		n1 = map.getNodeByCoord(c1);
		n2 = map.getNodeByCoord(c2);
		n6 = map.getNodeByCoord(c6);
	}

	public void testGetPath() {
		setupMapData(null,null,null);
		Coord c,c2;
		mbm.getInitialLocation();
		Path path = mbm.getPath();
		c = path.getNextWaypoint();

		while (path.hasNext()) {
			c2 = path.getNextWaypoint();
			// adjacent nodes are always 1 meter apart (in the test topology)
			assertEquals(1.0, c.distance(c2));
			c = c2;
		}
	}

	public void testOneMapTypeNode() {
		int NROF = 10;
		setupMapData("1",null,null);
		n1.addType(1);

		mbm.getInitialLocation();
		Path p = mbm.getPath();
		for (int i=0; i<NROF; i++) {
			Coord next = p.getNextWaypoint();
			// 	only allowed location is c1
			assertEquals(c1, next);
		}

		// add n2 to allowed nodes
		n2.addType(1);
		p = mbm.getPath();
		List<Coord> coords = p.getCoords();
		// should move between n1 and n2
		for (int i=0; i<coords.size()-1; i+= 2) {
			assertEquals(c1, coords.get(i));
			assertEquals(c2, coords.get(i+1));
		}

		n6.addType(1);
		p = mbm.getPath();
		coords = p.getCoords();

		assertEquals(c2, coords.get(0)); // starts from n2

		// should move route n1-n2-n6-n2-n1-n2 ...
		for (int i=1; i<coords.size()-4; i+= 4) {
			assertEquals(c1, coords.get(i));
			assertEquals(c2, coords.get(i+1));
			assertEquals(c6, coords.get(i+2));
			assertEquals(c2, coords.get(i+3));
		}
	}

	public void testManyMapTypeNodes() {
		setupMapData("1,2",null,null);
		n1.addType(1);
		n2.addType(2);

		mbm.getInitialLocation();
		Path p = mbm.getPath();
		List<Coord> coords = p.getCoords();
		// should move between n1 and n2
		for (int i=0; i<coords.size()-1; i+= 2) {
			assertEquals(c2, coords.get(i+1));
			assertEquals(c1, coords.get(i));
		}

		n6.addType(1); // n6 is both 1 and 2
		n6.addType(2);

		p = mbm.getPath();
		coords = p.getCoords();

		assertEquals(c2, coords.get(0)); // starts from n2
		// should move route n6-n2-n1-n2...
		for (int i=1; i<coords.size()-4; i+= 4) {
			assertEquals(c6, coords.get(i));
			assertEquals(c2, coords.get(i+1));
			assertEquals(c1, coords.get(i+2));
			assertEquals(c2, coords.get(i+3));
		}

	}


	/**
	 * Tests the SimMap caching feature
	 */
	public void testMapCache() throws IOException {
		String mmbClass = "movement.MapBasedMovement";
		writeToNewFile();
		assertEquals("1", new TestSettings(MapBasedMovement.MAP_BASE_MOVEMENT_NS).getSetting(MapBasedMovement.NROF_FILES_S));
		mbm = (MapBasedMovement)s.createIntializedObject(mmbClass);
		SimMap firstMap = mbm.getMap();
		mbm = (MapBasedMovement)s.createIntializedObject(mmbClass);
		SimMap secondMap = mbm.getMap();

		// second call should return the same map object
		assertTrue(firstMap == secondMap);

		writeToNewFile(); // change the map file
		mbm = (MapBasedMovement)s.createIntializedObject(mmbClass);
		SimMap thirdMap = mbm.getMap();

		// after reading from different file, should return a different instance
		assertTrue(firstMap != thirdMap);

		mbm = (MapBasedMovement)s.createIntializedObject(mmbClass);
		SimMap fourthMap = mbm.getMap();

		// now should return the same map object as with previous read
		assertTrue(thirdMap == fourthMap);
	}

	public void testHostMoving() {
		final int NROF = 15;

		setupMapData(null, "1,1", null);
		DTNHost h1 = setupHost();
		Coord loc = h1.getLocation().clone();

		for (int i=0; i<NROF; i++) {
			h1.move(1);
			// should always be 1 meter a way from the previous place
			assertEquals(1.0, loc.distance(h1.getLocation()));
			loc = h1.getLocation().clone();
		}

		h1 = setupHost();
		loc = h1.getLocation().clone();
		for (int i=0; i<NROF; i++ ) {
			h1.move(2);
			// should move 2 steps away from previous location
			double dist = loc.distance(h1.getLocation());
			assertTrue(dist == 2 || dist == 0 || dist == Math.sqrt(2));
			loc = h1.getLocation().clone();
		}

		h1 = setupHost();
		loc = h1.getLocation().clone();
		for (int i=0; i<NROF; i++ ) {
			h1.move(3);
			// should move 3 steps away from previous location
			double dist = loc.distance(h1.getLocation());
			assertTrue(dist == 3 || dist == 1 || dist == Math.sqrt(1+2*2) ||
					dist == Math.sqrt(2));
			loc = h1.getLocation().clone();
		}
	}

	private DTNHost setupHost() {
		TestUtils utils = new TestUtils(null, null, s);
		DTNHost h1 = utils.createHost(mbm, null);

		h1.move(0); // get a path for the node
		// move node directly to first waypoint
		h1.setLocation(h1.getPath().getCoords().get(0));
		return h1;
	}

	private String writeToNewFile() throws IOException {
		File tempFile = File.createTempFile("mapCachingTest", ".tmp");
		tempFile.deleteOnExit();
		writeToFile(tempFile.getAbsolutePath());
		return tempFile.getAbsolutePath();
	}

	private void writeToFile(String path) throws IOException {
		File tempFile = new File(path);
		PrintWriter out = new PrintWriter(tempFile);
		out.println(WKT);
		out.close();
		String ns = MapBasedMovement.MAP_BASE_MOVEMENT_NS + ".";

		// override sim map settings
		File settingsFile = File.createTempFile("settingsFile", ".tmp");
		settingsFile.deleteOnExit();
		PrintWriter pw = new PrintWriter(settingsFile);
		pw.println(ns + MapBasedMovement.NROF_FILES_S + " = 1");
		// need to change path separators for settings file
		pw.println(ns + MapBasedMovement.FILE_S + "1 = " +
				tempFile.getAbsolutePath().replace('\\', '/'));
		pw.close();
		Settings.init(settingsFile.getAbsolutePath());

	}

}
