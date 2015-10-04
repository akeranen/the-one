/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package test;

import input.WKTMapReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Random;

import junit.framework.TestCase;
import movement.map.MapNode;
import movement.map.PointsOfInterest;
import movement.map.SimMap;
import core.Coord;
import core.Settings;

/**
 * Test for selecting Points Of Interest from different node groups.
 * Tests that the probability of getting a node from a specific group is
 * approximately same as the defined probability.
 */
public class PointsOfInterestTest extends TestCase {
	
	/* Topology:  n7--n5
	 *            |   |
	 *        n1--n2--n6--n3
	 *         |
	 *        n4
	 */
	private static final String MAP_DATA = 
	//				   n1       n2       n6        n3
		"LINESTRING (1.0 1.0, 2.0 1.0, 3.0 1.0, 4.0 1.0) \n" +
	//              n1        n4
	"LINESTRING (1.0 1.0, 1.0 2.0)\n";
	
	private static final String MAP_DATA2 =
	//              n2       n7       n5       n6
	"LINESTRING (2.0 1.0, 2.0 0.0, 3.0 0.0, 3.0 1.0)\n";
	
	private static final String[] POINTS_IN_MAP = { 
		"POINT (1.0 1.0)\n POINT (2.0 1.0)",
		"POINT (4.0 1.0)\n POINT (1.0 2.0)",
		"POINT (3.0 1.0)\n"
		};
	
	private static final Coord[][] COORDS_IN_MAP = { 
		{new Coord(1,1), new Coord(2,1)}, 
		{new Coord(4,1), new Coord(1,2)},
		{new Coord(3,1)}
		};		 

	private PointsOfInterest pois;
	private int nrofMapNodes;
	
	protected void setUpWith(double[] poiProbs, int rngSeed, int [] okNodes)
			throws Exception {
		super.setUp();
		
		Settings.init(null);
		StringReader input = new StringReader(MAP_DATA);
		
		WKTMapReader reader = new WKTMapReader(true);
		try {
			reader.addPaths(input, 1);
			input = new StringReader(MAP_DATA2);
			reader.addPaths(input, 2);
		} catch (IOException e) {
			fail(e.toString());			
		}
		
		SimMap map = reader.getMap();
		this.nrofMapNodes = map.getNodes().size();
		
		File[] poiFiles = new File[POINTS_IN_MAP.length];
		for (int i=0; i<POINTS_IN_MAP.length; i++) {
			File poiFile = File.createTempFile("poifile"+i, ".tmp");
			poiFile.deleteOnExit();
			PrintWriter out = new PrintWriter(new FileWriter(poiFile));
			out.println(POINTS_IN_MAP[i]);
			out.close();
			poiFiles[i] = poiFile;
		}
		
		TestSettings s = new TestSettings();
		String ns = PointsOfInterest.POI_NS;
		String fset = PointsOfInterest.POI_FILE_S;
		
		for (int i=0; i<poiFiles.length; i++) {
			s.putSetting(ns+"."+fset+i, fixFile(poiFiles[i].getAbsolutePath()));
		}
		
		String probSet = "";
		for (int i=0; i<poiProbs.length; i++) {
			probSet += i + "," + poiProbs[i];
			if (i<poiProbs.length-1) {
				probSet += ",";
			}
		}

		s.putSetting(PointsOfInterest.POI_SELECT_S,probSet);
				
		Random rng = new Random(rngSeed);
		
		pois = new PointsOfInterest(map, okNodes, s, rng);	
	}
	
	private String fixFile(String fileName) {
		return fileName.replace('\\', '/');
	}

	public void testPoiSelection() throws Exception {
		runTestPoiSelection(new double[] {0.1, 0.8, 0.05}, 0);
	}

	public void testDifferentRngSeeds() throws Exception {
		runTestPoiSelection(new double[] {0.1, 0.8, 0.05}, 1);
		runTestPoiSelection(new double[] {0.1, 0.8, 0.05}, 2);
		runTestPoiSelection(new double[] {0.1, 0.8, 0.05}, 3);
		runTestPoiSelection(new double[] {0.1, 0.8, 0.05}, 10);
		runTestPoiSelection(new double[] {0.1, 0.8, 0.05}, 54534543);
	}
	
	/**
	 * Tests full probability for one node group 
	 */
	public void testFullProb() throws Exception {
		runTestPoiSelection(new double[] {1.0, 0.0, 0.0}, 0);
		runTestPoiSelection(new double[] {0.0, 1.0, 0.0}, 0);
		runTestPoiSelection(new double[] {0.0, 0.0, 1.0}, 0);
		// all POIs nodes are random nodes
		runTestPoiSelection(new double[] {0.0, 0.0, 0.0}, 0);
	}
	

	/**
	 * Runs the POI selecting procedure and performs checks
	 */
	public void runTestPoiSelection(double[] poiProbs, int rngSeed) throws Exception {
		setUpWith(poiProbs, rngSeed, null);
		// how many POIs are requested in total
		final int TOTAL = 5000;
		// how much offset from requested probability is allowed
		final double DELTA = 0.02; 
		double probSum = 0; // sum of all requested probs
		double[] expectedProbs = new double[poiProbs.length];
		
		int[] nrofHits = new int[poiProbs.length];
		
		for (int i=0; i<poiProbs.length;i++) {
			nrofHits[i] = 0;
			probSum += poiProbs[i];
		}

		for (int i=0; i<poiProbs.length;i++) {
			// expected probability per group is probability of selecting that
			// group + probability of randomly selecting a node in that group
			double randomProb = (1-probSum) * 
				(COORDS_IN_MAP[i].length/(nrofMapNodes*1.0));
			expectedProbs[i] = poiProbs[i] + randomProb;
		}
		
		// select TOTAL nodes from POIs
		for (int i=0; i<TOTAL; i++) {
			MapNode n = pois.selectDestination();
			for (int j=0; j<POINTS_IN_MAP.length; j++) {
				// check in which POI group the selected nodes belongs to
				if (isPartOf(n.getLocation(), COORDS_IN_MAP[j])) {
					nrofHits[j]++;
				}
			}			
		}
		
		int nrofHitsSum = 0; // nrof hits in POI groups
		// check that probabilites are close to requested 
		for (int i=0; i<nrofHits.length;i++) {
			nrofHitsSum += nrofHits[i];
			double prob = nrofHits[i] / (TOTAL*1.0);
			assertEquals("Prob too far for index " + i, 
					expectedProbs[i], prob, DELTA);
		}
		
		// check that other nodes got their fair share
		double poiProbSum = 0;
		for (int i=0; i<expectedProbs.length; i++) {
			poiProbSum += expectedProbs[i];
		}
		double otherProb = (TOTAL-nrofHitsSum) / (TOTAL*1.0);
		assertEquals(1-poiProbSum, otherProb, DELTA);
		
	}

	public void testOkNodes() throws Exception {
		int nrof = 100;
		setUpWith(new double[] {0.1, 0.1, 0.1}, 1, new int [] {1});
		
		// should choose only nodes of type 1
		for (int i=0; i< nrof; i++)
			assertTrue(pois.selectDestination().isType(1));
		
	}
	
	// return true if c is part of set
	private boolean isPartOf(Coord c, Coord[] set) {
		for (Coord coord : set) {
			if (c.equals(coord)) {
				return true;
			}
		}
		return false;
	}
}
