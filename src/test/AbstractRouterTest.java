/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import routing.MessageRouter;
import core.Coord;
import core.DTNHost;
import core.MessageListener;
import core.NetworkInterface;
import core.SimClock;

/**
 * Superclass for router tests. Sets up the environment by creating
 * multiple hosts with router set by {@link #setRouterProto(MessageRouter)}
 */
public abstract class AbstractRouterTest extends TestCase {
	protected MessageChecker mc;
	protected TestUtils utils;
	protected static TestSettings ts = new TestSettings();

	protected static final int BUFFER_SIZE = 100;
	protected static final int TRANSMIT_SPEED = 10;
	protected SimClock clock;

	protected Coord c0 = new Coord(0,0);
	protected Coord farAway = new Coord(100000,100000);
	protected static final Coord disconnectLocation = new Coord(900000,900000);
	protected DTNHost h0;
	protected DTNHost h1;
	protected DTNHost h2;
	protected DTNHost h3;
	protected DTNHost h4;
	protected DTNHost h5;
	protected DTNHost h6;
	protected static final String msgId1 = "MSG_ID1";
	protected static final String msgId2 = "MSG_ID2";
	protected static final String msgId3 = "MSG_ID3";
	protected static final String msgId4 = "MSG_ID4";
	protected static final String msgId5 = "MSG_ID5";

	protected MessageRouter routerProto;

	protected void setUp() throws Exception {
		super.setUp();
		this.mc = new MessageChecker();
		mc.reset();
		this.clock = SimClock.getInstance();
		clock.setTime(0);

		List<MessageListener> ml = new ArrayList<MessageListener>();
		ml.add(mc);

		ts.setNameSpace(TestUtils.IFACE_NS);
		ts.putSetting(NetworkInterface.TRANSMIT_SPEED_S, ""+TRANSMIT_SPEED);

		this.utils = new TestUtils(null,ml,ts);
		this.utils.setMessageRouterProto(routerProto);
		core.NetworkInterface.reset();
		core.DTNHost.reset();
		this.h0 = utils.createHost(c0, "h0");
		this.h1 = utils.createHost(c0, "h1");
		this.h2 = utils.createHost(c0, "h2");
		this.h3 = utils.createHost(c0, "h3");
		this.h4 = utils.createHost(c0, "h4");
		this.h5 = utils.createHost(c0, "h5");
		this.h6 = utils.createHost(c0, "h6");
	}

	protected void setRouterProto(MessageRouter r) {
		this.routerProto = r;
	}

	/**
	 * Checks that mc contains only nrof create-events and nothing else
	 * @param nrof how many creates to expect
	 */
	protected void checkCreates(int nrof) {
		for (int i=0; i<nrof; i++) {
			assertTrue(mc.next());
			assertEquals(mc.TYPE_CREATE, mc.getLastType());
		}
		assertFalse("MC contained " + mc.getLastType(), mc.next());
	}

	protected void updateAllNodes() {
		for (DTNHost node : utils.getAllHosts()) {
			node.update(true);
		}
	}

	protected void checkTransferStart(DTNHost from, DTNHost to, String msgId) {
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(msgId, mc.getLastMsg().getId());
		assertEquals(from, mc.getLastFrom());
		assertEquals(to, mc.getLastTo());
	}

	protected void checkDelivered(DTNHost from, DTNHost to, String msgId,
			boolean isFirstDelivery) {
		if (isFirstDelivery) {
			// message delivered -> delete ACKed message
			assertTrue(mc.next());
			assertEquals(mc.TYPE_DELETE, mc.getLastType());
			assertEquals(msgId, mc.getLastMsg().getId());
			assertEquals(from, mc.getLastFrom());
		}

		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType()); // finished transfer
		assertEquals(msgId, mc.getLastMsg().getId());
		assertEquals(from, mc.getLastFrom());
		assertEquals(to, mc.getLastTo());

		if (isFirstDelivery) {
			assertTrue(mc.getLastFirstDelivery());
		}
	}

	protected void deliverMessage(DTNHost from, DTNHost to, String msgId,
			int msgSize, boolean firstDelivery) {
		assertFalse("MC contained " + mc.getLastType(), mc.next());
		from.update(true);
		to.update(true);
		checkTransferStart(from, to, msgId);
		clock.advance((1.0*msgSize)/TRANSMIT_SPEED);
		from.update(true);
		to.update(true);
		checkDelivered(from, to, msgId, firstDelivery);
	}

	/**
	 * Moves node to disconnectLocation (far away from c0), updates it and
	 * restores the node location
	 * @param node Node to disconnect
	 */
	protected static void disconnect(DTNHost node) {
		Coord loc = node.getLocation();
		node.setLocation(disconnectLocation);
		node.update(true);
		node.setLocation(loc);
	}

	public String toString() {
		return "MC: " + mc.toString();
	}

}
