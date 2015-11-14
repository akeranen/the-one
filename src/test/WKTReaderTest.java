/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import input.WKTMapReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;

import junit.framework.TestCase;
import movement.map.MapNode;
import movement.map.SimMap;
import core.Coord;

public class WKTReaderTest extends TestCase {
	private WKTMapReader reader;


	private final int NROF_TST_NODES = 9;
	/* Topology:  n6--n5
	 *            |   |
	 *        n1--n2--n7----n3
	 *         |
	 *        n4--n8--n9 (-- n10; from ADD_TOPOLOGY)
	 *                ( |
	 *                  n11; from ADD_TOPOLOGY2 )
	 */
	//                           				  n1       n2       n7       n3
	private String TST_TOPOLOGY = "LINESTRING (1.0 1.0, 2.0 1.0, 3.0 1.0, 8.0 1.0) \n\r" +
	//              n1       n4
	"LINESTRING (1.0 1.0, 1.0 3.0)\n"+
	//              n2      n6        n5        n7
	"LINESTRING (2.0 1.0, 2.0 0.0, 3.0 0.0, 3.0 1.0)\r\n"+
	"POINT (1.0 2.0)\n" + // should skip this line
	//              n4       n8                   n8      n9
	"LINESTRING (1.0 3.0, 2.0 3.0) LINESTRING (2.0 3.0, 3.0 3.0)";

	//											  n9	 n10
	private String ADD_TOPOLOGY = "LINESTRING (3.0 3.0, 5.0 3.0)";
	//											  n9	 n11
	private String ADD_TOPOLOGY2 = "LINESTRING (3.0 3.0, 3.0 5.0)";


	// coordinates of some map nodes
	private Coord n1c = new Coord(1,1);
	private Coord n2c = new Coord(2,1);
	private Coord n9c = new Coord(3,3);
	private Coord n10c = new Coord(5,3);
	private Coord n11c = new Coord(3,5);


	private WKTMapReader setUpWith(Reader input) {
		reader = new WKTMapReader(true);
		try {
			reader.addPaths(input, 0);
		} catch (IOException e) {
			fail(e.toString());
		}

		return reader;
	}

	public void testFromString() {
		StringReader input = new StringReader(TST_TOPOLOGY);

		WKTMapReader reader = setUpWith(input);
		basicNodesTests(reader);
		topologyTest(reader.getNodesHash());
	}

	private void topologyTest(Map<Coord, MapNode> nh) {
		MapNode n1,n2,n3,n4,n5,n8,n9;

		// right amount of nodes?
		assertEquals(NROF_TST_NODES, nh.size());

		n1 = nh.get(n1c);
		n2 = nh.get(n2c);
		n3 = nh.get(new Coord(8,1));
		n4 = nh.get(new Coord(1,3));
		n5 = nh.get(new Coord(3,0));
		n8 = nh.get(new Coord(2,3));
		n9 = nh.get(n9c);

		// all nodes exist?
		assertNotNull(n1);
		assertNotNull(n2);
		assertNotNull(n3);
		assertNotNull(n4);
		assertNotNull(n5);
		assertNotNull(n8);
		assertNotNull(n9);

		// nodes have correct amount of neighbors?
		assertEquals(2, n1.getNeighbors().size());
		assertEquals(3, n2.getNeighbors().size());
		assertEquals(1, n3.getNeighbors().size());
		assertEquals(2, n4.getNeighbors().size());
		assertEquals(2, n5.getNeighbors().size());
		assertEquals(2, n8.getNeighbors().size());
		assertEquals(1, n9.getNeighbors().size());
	}

	public void testFromFile() throws IOException {
		File wktFile = File.createTempFile("WKTReaderTest","tmp");
		wktFile.deleteOnExit();
		PrintWriter pw = new PrintWriter(wktFile);
		WKTMapReader reader;

		pw.println(TST_TOPOLOGY);
		pw.close();
		reader = setUpWith(new FileReader(wktFile));

		basicNodesTests(reader);
		topologyTest(reader.getNodesHash());
	}

	public void testMultiLineString() {
		String multiline = "MULTILINESTRING ((1.0 1.0, 2.0 1.0, 3.0 1.0),"+
			"(1.0 1.0, 1.0 2.0))";
		StringReader input = new StringReader(multiline);
		Map<Coord, MapNode> nh;

		WKTMapReader reader = setUpWith(input);
		nh = reader.getNodesHash();

		assertEquals(4, nh.size());
	}

	public void testReadContents() throws IOException {
		String c1 = "lorem ipsum dolor sit amet";
		String c2 = "lorem ipsum\r\ndolor sit\n\r amet";
		String internal = "(lorem),(ipsum)";
		String cont = "ACTION ("+c1+")";
		String cont2 = "ACTION ("+c2+")";
		String cont3 = "MLS ("+internal+")";

		WKTMapReader r = new WKTMapReader(true);

		StringReader s = new StringReader(cont3);
		assertEquals(internal, r.readNestedContents(s));

		s = new StringReader(cont);
		assertEquals(c1,r.readNestedContents(s));

		s = new StringReader(cont2);
		// should convert newline to space
		c2 = c2.replaceAll("(\r|\n)", " ");
		assertEquals(c2,r.readNestedContents(s));
	}

	public void testMapOperations() {
		String wkt = "LINESTRING (1.0 1.0, 2.0 5.0)\n" +
			"LINESTRING (1.0 1.0, 1.0 3.0)\n";
		WKTMapReader reader = setUpWith(new StringReader(wkt));
		SimMap map = reader.getMap();

		Coord max = map.getMaxBound();
		Coord min = map.getMinBound();

		assertEquals(2.0, max.getX());
		assertEquals(5.0, max.getY());
		assertEquals(1.0, min.getX());
		assertEquals(1.0, min.getY());

		map.translate(-1, -1);
		max = map.getMaxBound();
		min = map.getMinBound();

		assertEquals(1.0, max.getX());
		assertEquals(4.0, max.getY());
		assertEquals(0.0, min.getX());
		assertEquals(0.0, min.getY());

	}

	public void testMultipleMapFiles() throws Exception {
		File wktFile1 = File.createTempFile("WKTReaderTest","tmp");
		File wktFile2 = File.createTempFile("WKTReaderTest","tmp");
		File wktFile3 = File.createTempFile("WKTReaderTest","tmp");
		wktFile1.deleteOnExit();
		wktFile2.deleteOnExit();
		wktFile3.deleteOnExit();

		PrintWriter pw = new PrintWriter(wktFile1);
		pw.println(TST_TOPOLOGY);
		pw.close();
		pw = new PrintWriter(wktFile2);
		pw.println(ADD_TOPOLOGY);
		pw.close();
		pw = new PrintWriter(wktFile3);
		pw.println(ADD_TOPOLOGY2);
		pw.close();

		WKTMapReader reader = new WKTMapReader(true);
		reader.addPaths(wktFile1, 1);
		reader.addPaths(wktFile2, 2);
		reader.addPaths(wktFile3, 31);

		basicNodesTests(reader);

		SimMap map = reader.getMap();

		// n1 should be of type 1
		MapNode n1 = map.getNodeByCoord(n1c);
		assertTrue(n1.isType(1));
		assertTrue(n1.isType(new int [] {2,1}));
		assertFalse(n1.isType(2));

		// n10 should be of type 2
		assertTrue(map.getNodeByCoord(n10c).isType(2));
		assertFalse(map.getNodeByCoord(n10c).isType(1));

		// n9 should be type1, type2 and type31
		MapNode n9 = map.getNodeByCoord(n9c);
		assertTrue(n9.isType(2));
		assertTrue(n9.isType(1));
		assertTrue(n9.isType(31));
		assertFalse(n9.isType(4));

		assertTrue(n9.isType(new int [] {1,2,31}));
		assertTrue(n9.isType(new int [] {5,2}));
		assertTrue(n9.isType(new int [] {1,5}));
		assertTrue(n9.isType(new int [] {5,7,31}));
		assertFalse(n9.isType(new int [] {5,7,8}));

		// n11 should be only 31
		assertTrue(map.getNodeByCoord(n11c).isType(31));
		assertFalse(map.getNodeByCoord(n11c).isType(2));
	}

	private void basicNodesTests(WKTMapReader reader) {
		Collection<MapNode> col = reader.getNodes();

		// contains something
		assertTrue(col.size() > 0);

		for (MapNode n : col) {
			// no lonely nodes
			assertTrue(n.getNeighbors().size() >= 1);
		}
	}

}
