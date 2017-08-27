/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import applications.DatabaseApplication;
import core.BroadcastMessage;
import core.DisasterData;
import core.Group;
import core.MulticastMessage;
import org.junit.Assert;
import org.junit.Test;
import routing.MessageRouter;
import routing.ProphetRouter;
import core.Message;

import java.util.Arrays;

/**
 * Tests for PRoPHET router
 */
public class ProphetRouterTest extends AbstractRouterTest {

	private static int SECONDS_IN_TIME_UNIT = 60;

	@Override
	public void setUp() throws Exception {
		ts.putSetting(MessageRouter.B_SIZE_S, ""+BUFFER_SIZE);
		ts.putSetting(ProphetRouter.PROPHET_NS + "." +
				ProphetRouter.SECONDS_IN_UNIT_S , SECONDS_IN_TIME_UNIT+"");
		setRouterProto(new ProphetRouter(ts));
        DatabaseApplicationTest.addDatabaseApplicationSettings(ts);
		super.setUp();
	}

    /**
     * Tears down the fixture, for example, close a network connection.
     * This method is called after a test is executed.
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Group.clearGroups();
    }

    /**
	 * Tests normal routing
	 */
	@Test
	public void testRouting() {
		Message m1 = new Message(h1,h2, MSG_ID2, 1);
		h1.createNewMessage(m1);
		Message m2 = new Message(h1,h3, MSG_ID3, 1);
		h1.createNewMessage(m2);
		Message m3 = new Message(h1,h4, MSG_ID4, 1);
		h1.createNewMessage(m3);
		Message m4 = new Message(h1,h6, "dummy", 1); // this message should not be fwded
		h1.createNewMessage(m4);
		Message m5 = new Message(h1,h5, MSG_ID5, 1);
		h1.createNewMessage(m5);
		Message m6 = new Message(h4,h1, "d1", 1);
		h4.createNewMessage(m6);

		ProphetRouter r4 = (ProphetRouter)h4.getRouter();
		ProphetRouter r5 = (ProphetRouter)h5.getRouter();

		checkCreates(6);

		h4.connect(h5);
		assertEquals(ProphetRouter.P_INIT, r4.getPredFor(h5));
		assertEquals(ProphetRouter.P_INIT, r5.getPredFor(h4));

		updateAllNodes();
		// h4 has message for h1 but it shouldn't forward it to h5 since
		// h5 has not heard about h1
		assertFalse(mc.next());

		disconnect(h5);
		h5.connect(h3); // now h3 knows h5 has met h4

		h1.connect(h3);
		// now h1-h3-h5 connected and h5 knows h4

		// first h1 should transfer MSG_ID2 to h3 (final recipient)
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(MSG_ID3, mc.getLastMsg().getId());
		assertEquals(h1, mc.getLastFrom());
		assertFalse(mc.next());

		clock.advance(10);
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType()); // finished transfer
		assertEquals(MSG_ID3, mc.getLastMsg().getId());
		assertTrue(mc.getLastFirstDelivery());

		// h1 should next transfer MSG_ID5 to h3 because h3 is connected to h5
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(MSG_ID5, mc.getLastMsg().getId());
		assertEquals(h1, mc.getLastFrom());
		assertFalse(mc.next());

		clock.advance(10);
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType()); // finished transfer
		assertEquals(MSG_ID5, mc.getLastMsg().getId());
		assertTrue(mc.next());

		// next h1 should transfer MSG_ID4 since h3 knows h4 trough h5
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(MSG_ID4, mc.getLastMsg().getId());
		assertEquals(h1, mc.getLastFrom());
		assertFalse(mc.next());

		doRelay(); // relaying should be tested by now..
		assertTrue(mc.next());

		// now h3 should transfer MSG_ID5 to h5
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(MSG_ID5, mc.getLastMsg().getId());
		assertEquals(h3, mc.getLastFrom());

		doRelay(); // id5 delivered to h5
		assertTrue(mc.getLastFirstDelivery());
		assertTrue(mc.next());

		// next h3 should transfer id4 to h5
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(MSG_ID4, mc.getLastMsg().getId());
		assertEquals(h3, mc.getLastFrom());

		doRelay();

		// now no new transfers should be started
		assertFalse(mc.next());

	}

    /**
     * Checks that {@link ProphetRouter} correctly sorts direct messages, broadcasts, non-direct one-to-one messages,
     * direct and non-direct multicasts, and data messages.
     */
    @Test
    public void testPrioritization() {
        // Create groups for multicasts.
        Group directGroup = Group.createGroup(0);
        directGroup.addHost(h1);
        directGroup.addHost(h2);
        Group indirectGroup = Group.createGroup(1);
        indirectGroup.addHost(h1);
        indirectGroup.addHost(h3);
        indirectGroup.addHost(h4);

        // Create all kinds of messages.
        Message broadcast = new BroadcastMessage(this.h1, "B1", 0);
        Message directMessage = new Message(this.h1, this.h2, "M1", 0);
        Message nonDirectMessage = new Message(this.h1, this.h3, "M2", 0);
        Message nonDirectMessage2 = new Message(this.h1, this.h5, "M3", 0);
        Message directMulticast = new MulticastMessage(this.h1, directGroup, "m1", 0);
        Message indirectMulticast = new MulticastMessage(this.h1, indirectGroup, "m2", 0);
        this.h1.createNewMessage(broadcast);
        this.h1.createNewMessage(directMessage);
        this.h1.createNewMessage(nonDirectMessage);
        this.h1.createNewMessage(nonDirectMessage2);
        this.h1.createNewMessage(directMulticast);
        this.h1.createNewMessage(indirectMulticast);

        // Add data for data message.
        this.clock.advance(24 * 60 * 60);
        DisasterData data =
                new DisasterData(DisasterData.DataType.MARKER, 0, 0, this.h1.getLocation());
        DatabaseApplication app = new DatabaseApplication(this.ts);
        this.h1.getRouter().addApplication(app);
        app.update(this.h1);
        app.disasterDataCreated(h1, data);

        // Modify utilities for testing:
        // First, increase delivery predictability H2 --> H5, then advance clock to lower it again.
        this.h2.connect(this.h5);
        this.clock.advance(3600);
        // Then, do the same for delivery predictability H2 --> H4.
        this.h2.connect(this.h4);
        this.clock.advance(3600);
        // Finally, make sure the delivery predictability H2 --> H3 is high
        this.h2.connect(this.h3);
        disconnect(this.h2);
        // And ensure that the multicast message was already delivered to H3.
        for (Message m : this.h1.getMessageCollection()) {
            if (m.getId().equals(indirectMulticast.getId())) {
                m.addNodeOnPath(this.h3);
            }
        }

        // Connect h1 to h2.
        this.h1.connect(this.h2);

        // Check order of messages: First direct messages, then sorted by predictability / data utility
        // Direct messages are pseudo-randomly sorted, so you might have to change the order if changing the clock.
        String dataMessageId = "D" + Arrays.asList(data).hashCode();
        String[] idsInExpectedOrder = {
                broadcast.getId(), directMessage.getId(), directMulticast.getId(),
                nonDirectMessage.getId(), dataMessageId, indirectMulticast.getId(), nonDirectMessage2.getId()
        };
        this.mc.reset();
        for (String expectedId : idsInExpectedOrder) {
            h1.update(true);
            do {
                this.mc.next();
            } while (!this.mc.TYPE_START.equals(this.mc.getLastType()));
            Assert.assertEquals("Expected different message.", expectedId, mc.getLastMsg().getId());
        }
    }

	private void doRelay() {
		clock.advance(10);
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());
	}

	@Test
	public void testAging() {
		ProphetRouter r4 = (ProphetRouter)h4.getRouter();
		ProphetRouter r5 = (ProphetRouter)h5.getRouter();

		h4.connect(h5);
		assertEquals(ProphetRouter.P_INIT, r4.getPredFor(h5));
		assertEquals(ProphetRouter.P_INIT, r5.getPredFor(h4));

		disconnect(h5);

		clock.advance(SECONDS_IN_TIME_UNIT * 2);
		double newPred = ProphetRouter.P_INIT * Math.pow(ProphetRouter.GAMMA,2);

		assertEquals(newPred, r4.getPredFor(h5));
		assertEquals(newPred, r5.getPredFor(h4));

		clock.advance(SECONDS_IN_TIME_UNIT / 10);
		newPred = newPred *	Math.pow(ProphetRouter.GAMMA, 1.0/10);

		assertEquals(newPred, r4.getPredFor(h5));
		assertEquals(newPred, r5.getPredFor(h4));
	}

}
