/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import input.EventQueue;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import core.DTNHost;
import core.ModuleCommunicationBus;
import core.NetworkInterface;
import core.SimClock;
import core.UpdateListener;
import core.World;


/**
 * Tests for the World class
 * TODO: much more tests
 */
public class WorldTest extends TestCase {
	/* for rounding errors with SimClock */
	private static final double TIME_DELTA = 0.00001;
	private World world;
	private boolean simulateConnections = true;
	private int worldSizeX = 100;
	private int worldSizeY = 100;
	private double upInterval = 0.1;
	private List<TestDTNHost> testHosts;
	private List<EventQueue> eQueues;

	protected void setUp() throws Exception {
		super.setUp();
		SimClock.reset();
		TestSettings testSettings = new TestSettings();
		testSettings.setNameSpace(TestUtils.IFACE_NS);
		testSettings.putSetting(NetworkInterface.TRANSMIT_RANGE_S, "1.0");
		testSettings.putSetting(NetworkInterface.TRANSMIT_SPEED_S, "1");

		this.eQueues = new ArrayList<EventQueue>();
		this.testHosts = new ArrayList<TestDTNHost>();
		for (int i=0; i<10; i++) {
			NetworkInterface ni = new TestInterface(testSettings);
			List<NetworkInterface> li = new ArrayList<NetworkInterface>();
			li.add(ni);
			ModuleCommunicationBus comBus = new ModuleCommunicationBus();

			this.testHosts.add(new TestDTNHost(li, comBus, testSettings));
		}

		TestScenario ts = new TestScenario();
		this.world = new World(ts.getHosts(),ts.getWorldSizeX(),
				ts.getWorldSizeY(),ts.getUpdateInterval(),
				ts.getUpdateListeners(), ts.simulateConnections(),
				ts.getExternalEvents() );
	}

	public void testUpdate() {
		double endTime = 1000;
		int nrofRounds = (int)(endTime/upInterval);

		for (int i=0; i<nrofRounds; i++) {
			world.update();
		}

		/* time matches (approximately) the expected */
		assertEquals(endTime, SimClock.getTime(), TIME_DELTA);

		/* all hosts are correctly updated */
		assertNrofUpdates(nrofRounds);
	}

	private void assertNrofUpdates(int nrof) {
		for (TestDTNHost h : testHosts) {
			assertEquals(nrof, h.nrofUpdate);
		}
	}

	public void testUpdateScheduling() {
		world.scheduleUpdate(0.25);

		assertNrofUpdates(0);
		world.update();
		assertNrofUpdates(1);
		assertEquals(0.1, SimClock.getTime(), TIME_DELTA);
		world.update();
		assertEquals(0.2, SimClock.getTime(), TIME_DELTA);
		assertNrofUpdates(2);

		world.update();
		assertEquals(0.3, SimClock.getTime(), TIME_DELTA);
		assertNrofUpdates(4); // the extra scheduled update happened

		world.update();
		assertEquals(0.4, SimClock.getTime(), TIME_DELTA);
		assertNrofUpdates(5);

	}


	/** Dummy scenario for providing test values for the World */
	@SuppressWarnings("serial")
	private class TestScenario extends core.SimScenario {
		public TestScenario() {	}

		public int getWorldSizeX() {
			return worldSizeX;
		}
		public int getWorldSizeY() {
			return worldSizeY;
		}

		public double getUpdateInterval() {
			return upInterval;
		}

		public List<UpdateListener> getUpdateListeners() {
			return new ArrayList<UpdateListener>();
		}

		public List<DTNHost> getHosts() {
			ArrayList<DTNHost> hs = new ArrayList<DTNHost>();
			for (TestDTNHost h : testHosts) {
				hs.add(h);
			}
			return hs;
		}

		public boolean simulateConnections() {
			return simulateConnections;
		}

		public List<EventQueue> getExternalEvents() {
			return eQueues;
		}

		public double getMaxHostRange() {
			return 10;
		}

		protected void createHosts() {
			this.hosts = new ArrayList<DTNHost>();
		}
	}
}
