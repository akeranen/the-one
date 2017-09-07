/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import applications.DatabaseApplication;
import core.BroadcastMessage;
import core.Coord;
import core.DTNHost;
import core.DisasterData;
import core.Group;
import core.MulticastMessage;
import core.SimClock;
import org.junit.Assert;
import org.junit.Test;
import routing.MessageRouter;
import routing.ProphetRouter;
import core.Message;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Tests for PRoPHET router
 */
public class ProphetRouterTest extends AbstractRouterTest {

	private static int SECONDS_IN_TIME_UNIT = 60;

	private static final double ONE_HOUR = 60 * 60D;
	private static final double ONE_DAY = 24 * ONE_HOUR;
	private static final double ONE_WEEK = 7 * ONE_DAY;

	private static final Coord FAR_AWAY_LOCATION = new Coord(30_000, 40_000);

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
    public void testPrioritization() {
        // Create groups for multicasts.
        Group directGroup = ProphetRouterTest.createGroup(0, this.h1, this.h2);
        Group indirectGroup = ProphetRouterTest.createGroup(1, this.h1, this.h3, this.h4);

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
        this.clock.advance(ONE_DAY);
        DisasterData data =
                new DisasterData(DisasterData.DataType.MARKER, 0, 0, this.h1.getLocation());
        DatabaseApplication app = this.createDatabaseApplicationFor(this.h1);
        app.disasterDataCreated(h1, data);

        // Modify utilities for testing:
        // First, increase delivery predictability H2 --> H5, then advance clock to lower it again.
        this.h2.connect(this.h5);
        this.clock.advance(ONE_HOUR);
        // Then, do the same for delivery predictability H2 --> H4.
        this.h2.connect(this.h4);
        this.clock.advance(ONE_HOUR);
        // Finally, make sure the delivery predictability H2 --> H3 is high
        this.h2.connect(this.h3);
        disconnect(this.h2);
        // And ensure that the multicast message was already delivered to H3.
        ProphetRouterTest.addNodeOnPath(this.h1, indirectMulticast.getId(), this.h3);

        // Connect h1 to h2.
        this.h1.connect(this.h2);

        // Check order of messages: First direct messages, then sorted by predictability / data utility
        // Direct messages are pseudo-randomly sorted, so you might have to change the order if changing the clock.
        String dataMessageId = "D" + Arrays.asList(data).hashCode();
        String[] idsInExpectedOrder = {
                broadcast.getId(), directMessage.getId(), directMulticast.getId(),
                nonDirectMessage.getId(), dataMessageId, indirectMulticast.getId(), nonDirectMessage2.getId()
        };
        this.checkMessagesAreSentInOrder(idsInExpectedOrder);
    }

	/**
	 * Checks that {@link ProphetRouter} sends {@link core.DataMessage}s wrapping useful data, but data with too low
	 * utility is not sent.
	 */
	public void testUsefulDataGetsExchanged() {
        // Add data.
        DatabaseApplication app = this.createDatabaseApplicationFor(this.h1);
        this.clock.advance(ONE_WEEK);
        this.h1.setLocation(FAR_AWAY_LOCATION);
        DisasterData uselessData = new DisasterData(DisasterData.DataType.MARKER, 0, 0, new Coord(0, 0));
        DisasterData usefulData =
                new DisasterData(DisasterData.DataType.MARKER, 0, SimClock.getTime(), this.h1.getLocation());
        app.disasterDataCreated(this.h1, uselessData);
        app.disasterDataCreated(this.h1, usefulData);

        // Check that H1 only sends the useful data item to H2.
        this.h1.connect(this.h2);
        this.mc.reset();
        h1.update(false);
        do {
            this.mc.next();
        } while (!this.mc.TYPE_START.equals(this.mc.getLastType()));
        Assert.assertEquals(
                "Expected the useful data item to be sent.",
                "D" + Arrays.asList(usefulData).hashCode(), mc.getLastMsg().getId());
        Assert.assertFalse("Did not expect any additional message.", this.mc.next());
	}

    /**
     * Checks that both one-to-one messages and multicast messages are only transferred to a neighbor if those messages
     * have higher delivery predictability at the neighbor than at the current host.
     */
	public void testMessagesAreOnlyTransferredToHostsWithHigherPredictability() {
	    // Make sure H1 has higher delivery predictability for H3, while H2 has higher delivery predictability for H4.
        // First, increase delivery predictability H1 --> H4, then advance clock to lower it again.
        this.h1.connect(this.h4);
        this.clock.advance(ONE_HOUR);
        // Then set high delivery predictabilities for H1 --> H3 and H2 --> H4.
        this.h1.connect(this.h3);
        this.h2.connect(this.h4);
        disconnect(this.h1);
        disconnect(this.h2);

        // Create group including both H3 and H4.
        Group group = ProphetRouterTest.createGroup(0, this.h1, this.h3, this.h4);

        // Create multicasts and one to one messages to H3 and H4.
        Message messageToH3 = new Message(this.h1, this.h3, "M1", 0);
        Message messageToH4 = new Message(this.h1, this.h4, "M2", 0);
        Message receivedMulticast = new MulticastMessage(this.h1, group, "m1", 0);
        Message newMulticast = new MulticastMessage(this.h1, group, "m2", 0);
        this.h1.createNewMessage(messageToH3);
        this.h1.createNewMessage(messageToH4);
        this.h1.createNewMessage(receivedMulticast);
        this.h1.createNewMessage(newMulticast);

        // Ensure that one of the multicast messages was already delivered to H4.
        ProphetRouterTest.addNodeOnPath(this.h1, receivedMulticast.getId(), this.h4);

        // Connect h1 to h2.
        this.h1.connect(this.h2);

        // Check that only half of the messages are transferred
        HashSet<String> sentIds = new HashSet<>();
        this.mc.reset();
        this.h1.update(true);
        while (this.mc.next()) {
            sentIds.add(this.mc.getLastMsg().getId());
            this.h1.update(true);
        }
        HashSet<String> expectedIds = new HashSet<>(2);
        expectedIds.add(messageToH4.getId());
        expectedIds.add(newMulticast.getId());
        assertEquals("Expected different message set to be sent.", sentIds, expectedIds);
    }

    /**
     * Checks that a host does not send out new messages to hosts which are already transferring.
     */
    public void testNoMessagesAreReceivedWhenAlreadyTransferring() {
        // Let h2 be transferring.
        this.h2.connect(this.h3);
        Message m1 = new Message(this.h2, this.h3, "M1", 1);
        this.h2.createNewMessage(m1);
        this.updateAllNodes();

        // Check the transfer started.
        this.mc.next();
        this.checkTransferStart(this.h2, this.h3, m1.getId());

        // Let h1 try to send a message to h2 now.
        Message m2 = new Message(this.h1, this.h2, "M2", 0);
        this.h1.createNewMessage(m2);
        this.h1.connect(this.h2);
        this.updateAllNodes();

        // Check that the new message was not sent.
        while(this.mc.next()) {
            Assert.assertNotEquals("Did not expect another transfer.", this.mc.TYPE_START, this.mc.getLastType());
        }

        // Finally, check that the original message will still be transferred.
        this.clock.advance(1);
        this.updateAllNodes();
        this.mc.next();
        Assert.assertEquals(
                "Original message should have been processed next.", m1.getId(), this.mc.getLastMsg().getId());
        Assert.assertEquals(
                "Original message should have been transferred.", this.mc.TYPE_RELAY, this.mc.getLastType());
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

    /**
     * Observes which messages are sent by {@link #h1} and checks they match the provided IDs.
     * @param idsInExpectedOrder The expected message IDs in expected order.
     */
	private void checkMessagesAreSentInOrder(String[] idsInExpectedOrder) {
        this.mc.reset();
        for (String expectedId : idsInExpectedOrder) {
            this.h1.update(true);
            do {
                this.mc.next();
            } while (!this.mc.TYPE_START.equals(this.mc.getLastType()));
            Assert.assertEquals("Expected different message.", expectedId, mc.getLastMsg().getId());
        }
    }

    /**
     * Creates a group using the provided number as address.
     * All provided hosts will be part of the group.
     * @param address The new group's address.
     * @param hosts The hosts in the new group.
     * @return The created group.
     */
	private static Group createGroup(int address, DTNHost... hosts) {
        Group group = Group.createGroup(address);
        for (DTNHost host : hosts) {
            group.addHost(host);
        }
        return group;
    }

    /**
     * Creates and initializes a {@link DatabaseApplication} for the provided {@link DTNHost}.
     * @param host Host to create the application for.
     * @return The created application.
     */
    private DatabaseApplication createDatabaseApplicationFor(DTNHost host) {
        DatabaseApplication app = new DatabaseApplication(this.ts);
        host.getRouter().addApplication(app);
        app.update(host);
        return app;
    }

    /**
     * Changes the meta data of the message with provided ID in the buffer of the provided host by adding the provided
     * node to path.
     * @param bufferToChange The host in which buffer the message should be changed.
     * @param messageId The message's ID.
     * @param node The host to add to path.
     */
    private static void addNodeOnPath(DTNHost bufferToChange, String messageId, DTNHost node) {
        for (Message m : bufferToChange.getMessageCollection()) {
            if (m.getId().equals(messageId)) {
                m.addNodeOnPath(node);
            }
        }
    }
}
