/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import routing.maxprop.MaxPropDijkstra;
import routing.maxprop.MeetingProbabilitySet;
import core.DTNHost;

/**
 * Tests for MaxProp's shortest path calculation class.
 */
public class MaxPropDijkstraTest extends TestCase {

	private final int NROF_HOSTS = 5;
	/* amount of deviation from expected values that is OK */
	private final double DELTA = 0.0000001;
	private Set<DTNHost> hostsSet;
	private List<MeetingProbabilitySet> msets;
	private Map<Integer, MeetingProbabilitySet> mapping;

	private MaxPropDijkstra mpd;
	private Set<Integer> targets;

	public void setUp() throws Exception {
		super.setUp();
		core.NetworkInterface.reset();
		core.DTNHost.reset();
		TestUtils tu = new TestUtils(null, null, new TestSettings());
		msets = new ArrayList<MeetingProbabilitySet>();
		mapping = new HashMap<Integer, MeetingProbabilitySet>();
		hostsSet = new HashSet<DTNHost>();

		for (int i=0; i<NROF_HOSTS; i++) {
			DTNHost host = tu.createHost();
			MeetingProbabilitySet set = new MeetingProbabilitySet(
					MeetingProbabilitySet.INFINITE_SET_SIZE, 1.0);
			msets.add(set);
			hostsSet.add(host);
			mapping.put(host.getAddress(), set);
		}

		mpd = new MaxPropDijkstra(mapping);
		targets = new HashSet<Integer>();
	}

	/**
	 * Tests the values that are given as an example in the original paper
	 */
	public void testProbabilityValuesFromThePaper() {
		List<Integer> nodes = new ArrayList<Integer>();
		nodes.add(1);
		nodes.add(2);
		nodes.add(3);
		nodes.add(4);
		double unknownProb = 1.0/nodes.size();

		MeetingProbabilitySet mps = new MeetingProbabilitySet(1.0,nodes);
		assertEquals(unknownProb, mps.getProbFor(1));
		assertEquals(unknownProb, mps.getProbFor(2));

		mps.updateMeetingProbFor(1); // h0 meets h1

		assertEquals(0.625, mps.getProbFor(1));
		assertEquals(0.125, mps.getProbFor(2));
	}

	public void testPath() {
		targets.add(1);
		targets.add(2);
		targets.add(3);
		targets.add(4);
		targets.add(5);

		MeetingProbabilitySet mps0 = mapping.get(0);
		MeetingProbabilitySet mps1 = mapping.get(1);

		mps1.updateMeetingProbFor(2); // h1 meets h2
		assertEquals(1.0, mps1.getProbFor(2));

		mps1.updateMeetingProbFor(3); // h1 meets h3
		assertEquals(0.5, mps1.getProbFor(2));
		assertEquals(0.5, mps1.getProbFor(3));

		/* h4 meets h5 and h6 */
		mapping.get(4).updateMeetingProbFor(5);
		mapping.get(4).updateMeetingProbFor(6);

		mps1.updateMeetingProbFor(4); // h1 meets h4
		assertEquals(0.25, mps1.getProbFor(2));
		assertEquals(0.25, mps1.getProbFor(3));
		assertEquals(0.5, mps1.getProbFor(4));

		mps0.updateMeetingProbFor(1); // h0 meets h1
		mps1.updateMeetingProbFor(0); // and vice versa
		assertEquals(1.0, mps0.getProbFor(1));
		assertEquals(0.5, mps1.getProbFor(0));
		assertEquals(0.125, mps1.getProbFor(2));
		assertEquals(0.125, mps1.getProbFor(3));
		assertEquals(0.25, mps1.getProbFor(4));

		mps1.updateMeetingProbFor(4); // h1 meets h4 again
		assertEquals(1.0, mps0.getProbFor(1)); // should stay the same
		assertEquals(0.25, mps1.getProbFor(0));
		assertEquals(0.0625, mps1.getProbFor(2));
		assertEquals(0.0625, mps1.getProbFor(3));
		assertEquals(0.625, mps1.getProbFor(4));

		Map<Integer, Double> result = mpd.getCosts(0, targets);

		assertEquals(0.0, result.get(1));
		assertEquals(1-0.0625, result.get(2));
		assertEquals(1-0.625, result.get(4));
		assertEquals( (1-0.625)+(1-0.5), result.get(5));
	}


	public void testProbabilitySumsToOne() {
		double total;

		mapping.get(0).updateMeetingProbFor(2);
		mapping.get(0).updateMeetingProbFor(4);
		mapping.get(0).updateMeetingProbFor(2);

		mapping.get(1).updateMeetingProbFor(0);
		mapping.get(1).updateMeetingProbFor(0);

		mapping.get(2).updateMeetingProbFor(1);

		mapping.get(3).updateMeetingProbFor(0);
		mapping.get(3).updateMeetingProbFor(1);
		mapping.get(3).updateMeetingProbFor(2);
		mapping.get(3).updateMeetingProbFor(4);

		for (int i=0; i<=3; i++) {
			total = 0;
			for (int j=0; j<NROF_HOSTS; j++) {
				if (i != j) // skip our own prob
					total += mapping.get(i).getProbFor(j);
			}
			assertEquals("Total sum for node "+i+" was " + total, 1.0,
					total, DELTA);
		}


	}

}
