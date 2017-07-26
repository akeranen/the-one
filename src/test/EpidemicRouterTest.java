/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import applications.DatabaseApplication;
import core.DataMessage;
import core.DisasterData;
import core.SimClock;
import org.junit.Test;
import routing.ActiveRouter;
import routing.EpidemicRouter;
import routing.MessageRouter;
import core.DTNHost;
import core.Message;
import routing.util.DatabaseApplicationUtil;

import java.util.Arrays;

import java.util.List;

/**
 * Tests for EpidemicRouter and, due the simple nature of Epidemic router,
 * also ActiveRouter in general.
 */
public class EpidemicRouterTest extends AbstractRouterTest {

	private static int TTL = 300;
	private static final int THREE_HOURS = 10_800;

	/* Data base item sizes needed for tests. */
	private static final int DB_SIZE = 50;
	private static final int SMALL_SIZE_DIFFERENCE = 2;

	private static final String EXPECTED_DATA_MESSAGE = "Data message should have been sent.";

	@Override
	public void setUp() throws Exception {
		ts.putSetting(MessageRouter.MSG_TTL_S, ""+TTL);
		ts.putSetting(MessageRouter.B_SIZE_S, ""+BUFFER_SIZE);
		addDatabaseApplicationSettings();
		setRouterProto(new EpidemicRouter(ts));
		super.setUp();
	}

	private void addDatabaseApplicationSettings() {
        ts.putSetting(DatabaseApplication.UTILITY_THRESHOLD, "0.2");
        ts.putSetting(DatabaseApplication.SIZE_RANDOMIZER_SEED, "0");
        ts.putSetting(DatabaseApplication.DATABASE_SIZE_RANGE,
                String.format("%s,%s", Integer.toString(DB_SIZE), Integer.toString(DB_SIZE)));
        ts.putSetting(DatabaseApplication.MIN_INTERVAL_MAP_SENDING, "30");
        ts.putSetting(DatabaseApplication.ITEMS_PER_MESSAGE, "2");
    }

	/**
	 * Tests routing messages between three hosts
	 */
	@Test
	public void testRouter() {
		// nothing should have happened so far
		assertEquals(mc.TYPE_NONE, mc.getLastType());

		Message m1 = new Message(h1, h3, MSG_ID1, 1);
		h1.createNewMessage(m1);
		assertTrue(mc.next());
		assertEquals(mc.TYPE_CREATE, mc.getLastType());
		assertEquals(mc.getLastFrom(), h1);
		assertEquals(mc.getLastTo(), h3);

		// connect h1-h2-h3
		h1.connect(h2);
		h2.connect(h3);

		updateAllNodes();
		clock.advance(2);
		updateAllNodes();

		// should cause relay h1 -> h2
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());
		assertFalse(mc.getLastFirstDelivery());
		assertEquals(mc.getLastFrom(), h1);
		assertEquals(mc.getLastTo(), h2);

		// should cause relay h2 -> h3
		clock.advance(1);
		updateAllNodes();

		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());
		assertTrue(mc.getLastFirstDelivery());
		assertEquals(mc.getLastFrom(), h2);
		assertEquals(mc.getLastTo(), h3);
		// message delivered to recipient
		assertFalse(mc.next());


		// disconnect all nodes and try to other direction
		disconnect(h2);
		// create message while not connected
		Message m2 = new Message(h3,h1, MSG_ID2, 1);
		h3.createNewMessage(m2);
		assertTrue(mc.next());
		assertEquals(mc.TYPE_CREATE, mc.getLastType());

		h1.connect(h2);	// reconnect h1-h2
		clock.advance(10);
		updateAllNodes();
		assertFalse(mc.next()); // nothing should have happened to messages

		h2.connect(h3);
		updateAllNodes(); // now h3 should start forwarding msg to h2
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(h2, mc.getLastTo());
		assertEquals(h3, mc.getLastFrom());
		assertEquals(MSG_ID2, mc.getLastMsg().getId());
		assertFalse(mc.next());

		clock.advance(10);
		updateAllNodes();	// forwarding msg2 h3->h2
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());
		assertEquals(h2, mc.getLastTo());
		assertEquals(h3, mc.getLastFrom());
		assertEquals(MSG_ID2, mc.getLastMsg().getId());
		assertFalse(mc.next());

		updateAllNodes();	// forwarding msg2 h2->h1
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertFalse(mc.next());

		clock.advance(10);
		updateAllNodes();	// forwarding msg2 h2->h1
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());
		assertEquals(h1, mc.getLastTo());
		assertEquals(h2, mc.getLastFrom());
		assertEquals(MSG_ID2, mc.getLastMsg().getId());
		assertTrue(mc.getLastFirstDelivery()); // message delivered to recipient
		assertFalse(mc.next());
	}

	/**
	 * Checks that delivering many messages in a row works
	 */
	@Test
	public void testManyMessages() {

		Message m1 = new Message(h1,h2, MSG_ID1, 1);
		h1.createNewMessage(m1);
		Message m2 = new Message(h1,h2, MSG_ID2, 1);
		h1.createNewMessage(m2);
		mc.reset();

		h1.connect(h2);
		updateAllNodes(); // h1 should start transfer
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertFalse(mc.next());

		clock.advance(10);
		updateAllNodes();

		/* h1 should have delivered the msg & start next transfer */
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());
		assertEquals(h2, mc.getLastTo());
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertFalse(mc.next());	// nothing more happened so far

		clock.advance(10);
		updateAllNodes(); // h1 should finish relaying the msg
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());
		assertEquals(h2, mc.getLastTo());

		assertFalse(mc.next());
	}

	/**
	 * Tests that messages that can be delivered right a way are delivered first
	 */
	@Test
	public void testDeliverableMessageExchange() {
		Message m1 = new Message(h1,h3, "Dummy1", 1);
		h1.createNewMessage(m1);
		Message m2 = new Message(h1,h3, "A_Dummy2", 1);
		h1.createNewMessage(m2);
		Message m3 = new Message(h1,h2, MSG_ID1, 1);
		h1.createNewMessage(m3);

		Message m4 = new Message(h2,h3, "Dummy3", 1);
		h2.createNewMessage(m4);
		Message m5 = new Message(h2,h1, MSG_ID2, 1);
		h2.createNewMessage(m5);
		Message m6 = new Message(h2,h3, "Dummy4", 1);
		h2.createNewMessage(m6);

		checkCreates(6);

		h1.connect(h2);
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType()); // starts h1->h2 MSG_ID1
		assertEquals(h2, mc.getLastTo());
		assertEquals(MSG_ID1, mc.getLastMsg().getId());

		clock.advance(10);
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType()); // finished delivery

		// should also start delivery of MSG_ID2 from h2 to h1
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(MSG_ID2, mc.getLastMsg().getId());
		assertEquals(h1, mc.getLastTo());

	}

	/**
	 * Tests aborting transfer when connections is disconnected during the
	 * transfer
	 */
	@Test
	public void testMessageRelayAbort() {
		Message m1 = new Message(h1,h2, MSG_ID1, BUFFER_SIZE);
		h1.createNewMessage(m1);
		checkCreates(1);

		h1.connect(h2);
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertFalse(mc.next());
		clock.advance(1);
		updateAllNodes();
		assertFalse(mc.next());

		h2.setLocation(farAway);
		updateAllNodes(); // disconnect, still transferring

		assertTrue(mc.next());
		assertEquals(mc.TYPE_ABORT, mc.getLastType());
		assertEquals(h1, mc.getLastFrom());
		assertFalse(mc.next());
	}

    /**
     * Checks that {@link EpidemicRouter} can deal with {@link DataMessage}s.
     */
	@Test
    public void testDataMessagesAreDelivered() {
        // Add relevant data to h1.
        DisasterData data = new DisasterData(DisasterData.DataType.MARKER, 0, SimClock.getTime(), h1.getLocation());
        this.setUpAsDataCarrier(h1, data);

        // Check that it is sent.
        h1.connect(h2);
        updateAllNodes();
        assertTrue("A message should have been sent.", mc.next());
        assertTrue(EXPECTED_DATA_MESSAGE, mc.getLastMsg() instanceof DataMessage);
        assertEquals("Message should have had neighbor as receiver.", h2, mc.getLastMsg().getTo());
        assertEquals("Message transfer should just have started.", mc.TYPE_START, mc.getLastType());
    }

    /**
     * Checks that there is a (pseudo)random component in how data messages are ordered in the sending queue, i. e. they
     * might get ordered differently for different hosts.
     */
    @Test
    public void testDataMessageOrderIsDynamic() {
        // Make sure h1 has several normal messages and one data message.
        DisasterData data = new DisasterData(DisasterData.DataType.MARKER, 0, SimClock.getTime(), h1.getLocation());
        Message m1 = new Message(h1, h5, "M1", 0);
        Message m2 = new Message(h1, h5, "M2", 0);
        this.setUpAsDataCarrier(h1, data);
        h1.createNewMessage(m1);
        h1.createNewMessage(m2);

        // Make h1 meet several nodes and remember order of messages.
        DTNHost[] hostsToMeet = { h2, h3 };
        // We expect all messages in buffer and a data messages.
        int numMessages = h1.getNrofMessages() + 1;
        String[][] messageOrder = new String[hostsToMeet.length][numMessages];
        for (int hostIdx = 0; hostIdx < hostsToMeet.length; hostIdx++) {
            h1.connect(hostsToMeet[hostIdx]);
            messageOrder[hostIdx] = new String[numMessages];
            // For each node: Check MessageChecker iteratively until all messages have been sent.
            mc.reset();
            for (int i = 0; i < numMessages; i++) {
                this.updateAllNodes();
                do {
                    mc.next();
                } while (!mc.getLastType().equals(mc.TYPE_START));
                // And remember their order.
                messageOrder[hostIdx][i] = mc.getLastMsg().getId();

            }
        }

        // Check order was different depending on neighbor.
        assertFalse("Message order should be different.", Arrays.equals(messageOrder[0], messageOrder[1]));
    }

    /**
     * Checks that the data messages which get sent are based on data in the host's database.
     */
    @Test
    public void testDataMessagesAdaptToDatabase() {
        // Start off with one data object.
        DisasterData data = new DisasterData(DisasterData.DataType.MARKER, 1, SimClock.getTime(), h1.getLocation());
        this.setUpAsDataCarrier(h1, data);

        // Check it gets sent.
        h1.connect(h2);
        updateAllNodes();
        mc.next();
        assertTrue(EXPECTED_DATA_MESSAGE, mc.getLastMsg() instanceof DataMessage);
        DataMessage message = (DataMessage)mc.getLastMsg();
        assertEquals("Expected only one data item to be sent.", 1, message.getData().size());
        assertEquals("Expected other data item to be sent.", data, message.getData().get(0));

        // Add another, large one to replace the original object. It is more useful so it stays in the database.
        this.clock.advance(THREE_HOURS);
        DisasterData newData = new DisasterData(
                DisasterData.DataType.SKILL, DB_SIZE - SMALL_SIZE_DIFFERENCE, SimClock.getTime(), h1.getLocation());
        this.setUpAsDataCarrier(h1, newData);

        // Now, only the second object should get sent.
        h1.forceConnection(h2, null, false);
        h1.forceConnection(h3, null, true);
        updateAllNodes();
        do {
            mc.next();
        } while (!mc.getLastType().equals(mc.TYPE_START));
        assertTrue(EXPECTED_DATA_MESSAGE, mc.getLastMsg() instanceof DataMessage);
        message = (DataMessage)mc.getLastMsg();
        assertEquals("Expected only one data item to be sent.", 1, message.getData().size());
        assertEquals("Expected other data item to be sent.", newData, message.getData().get(0));
        assertFalse("Only one message should have been sent.", mc.next());
    }

	/**
	 * try disconnecting on the same update interval when a transfer should
	 * be finished -> should not cause abort (anymore)
	 */
	@Test
	public void testAbortWhenReady() {
		Message m1 = new Message(h2, h1, MSG_ID2, 1);
		h2.createNewMessage(m1);
		checkCreates(1);

		h2.connect(h1);
		updateAllNodes(); // should start transfer

		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertFalse(mc.next());

		clock.advance(10);
		h2.setLocation(farAway);
		// transfer should have been finished even if nodes disconnected
		updateAllNodes();

		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());
		assertEquals(h2, mc.getLastFrom());
		assertFalse(mc.next());
	}

	/**
	 * Test unexpected ordering of finalizations and message transfers.
	 */
	@Test
	public void testDifferentOrdering() {
		h1.connect(h2);
		Message m1 = new Message(h1,h2, MSG_ID1, 1);
		h1.createNewMessage(m1);
		assertTrue(mc.next());
		assertEquals(mc.TYPE_CREATE, mc.getLastType());
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(mc.getLastFrom(),h1);
		assertEquals(mc.getLastTo(),h2);

		clock.advance(10);
		// h1 has transferred msg but not finalized transfer when h2 starts
		Message m2 = new Message(h2,h1, MSG_ID2, 1);
		h2.createNewMessage(m2);
		h2.update(true); // h2 and h1 are connected but this shouldn't start relay

		assertTrue(mc.next());
		assertEquals(mc.TYPE_CREATE, mc.getLastType());
		assertFalse(mc.next()); // shouldn't start transfer (prev not finalized)

		h1.update(true); // finalize the transfer of MSG2
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());
		assertEquals(h2, mc.getLastTo());

		// last transfer is finalized -> update should start transfer of MSG_ID2
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(h1, mc.getLastTo());
		assertEquals(MSG_ID2, mc.getLastMsg().getId());

		assertFalse(mc.next());
	}

	/**
	 * Tests if rejecting already delivered message(s) work
	 */
	@Test
	public void testDoubleDelivery() {
		Message m1 = new Message(h1,h2, MSG_ID1, 1);
		h1.createNewMessage(m1);

		h1.connect(h2);
		updateAllNodes(); // starts transfer h1 -> h2
		clock.advance(10);
		mc.reset();	// discard create & start
		updateAllNodes(); // msg delivered h1 -> h2
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());
		assertEquals(h2, mc.getLastTo());

		h1.connect(h3);
		updateAllNodes();	// start transfer h1 -> h3
		assertTrue(mc.next());
		clock.advance(10);
		updateAllNodes(); // msg transferred h1 -> h3
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());
		assertEquals(h3, mc.getLastTo());

		h3.connect(h2);
		updateAllNodes(); // should not start a new transfer (h1 -> h2)
		clock.advance(10);
		updateAllNodes(); // still shouldn't do anything

		assertFalse(mc.next());
	}

	/**
	 * Tests if the FIFO queue management works
	 */
	@Test
	public void testQueueManagement() {
		Message m1 = new Message(h1,h3, "dummy", BUFFER_SIZE-1);
		h1.createNewMessage(m1);
		assertEquals(1, h1.getNrofMessages());
		Message m2 = new Message(h1,h3, MSG_ID1, BUFFER_SIZE/3);
		h1.createNewMessage(m2);
		assertEquals(1, h1.getNrofMessages()); // message should replace dummy
		assertEquals(MSG_ID1, h1.getMessageCollection().iterator().next().getId());

		mc.reset();

		clock.advance(10);
		Message m3 = new Message(h1,h3, MSG_ID2, BUFFER_SIZE/3);
		h1.createNewMessage(m3);
		clock.advance(10);
		Message m4 = new Message(h1,h3, "newestMsg", BUFFER_SIZE/3);
		h1.createNewMessage(m4);

		clock.advance(10);
		Message m5 = new Message(h2,h3, "MSG_from_h2", BUFFER_SIZE/2);
		h2.createNewMessage(m5);

		checkCreates(3); // remove 3 creates from mc

		h2.connect(h1);	// h2 starts transfer of message -> h1 makes room
		h2.update(true);
		// h1 should drop first MSG_ID1 and then MSG_ID2 (older first)
		assertTrue(mc.next());
		assertEquals(mc.TYPE_DELETE, mc.getLastType());
		assertEquals(h1, mc.getLastFrom());
		assertEquals(MSG_ID1, mc.getLastMsg().getId());
		assertTrue(mc.getLastDropped());
		assertTrue(mc.next());
		assertEquals(mc.TYPE_DELETE, mc.getLastType());
		assertEquals(MSG_ID2, mc.getLastMsg().getId());

		assertEquals(1, h1.getNrofMessages());

		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType()); // h2 should start
		assertEquals(h2, mc.getLastFrom());
		assertFalse(mc.next());

		clock.advance(10);
		updateAllNodes(); // h2 should finish transfer
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());
		assertEquals(h1, mc.getLastTo());
		assertFalse(mc.next());
	}

	/**
	 * Tests creating a new message when the message buffer is full and the
	 * message that should be removed is the message being sent
	 */
	@Test
	public void testNewMessageToFullBufferWhileTransferring() {
		int m3Size = BUFFER_SIZE-1;
		int m1Size = BUFFER_SIZE/2;

		Message m1 = new Message(h1,h3, MSG_ID1, m1Size);
		h1.createNewMessage(m1);
		Message m2 = new Message(h1,h4, MSG_ID2, BUFFER_SIZE/2);
		h1.createNewMessage(m2);
		checkCreates(2);

		h3.connect(h1);
		updateAllNodes(); // transfer of MSG_ID1 should start

		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		clock.advance(1);
		updateAllNodes(); // should still be transferring
		assertFalse(mc.next());

		// creating a new message should cause dropping the MSG_ID2 but
		// not MSG_ID1 (which is being transferred) -> buffer should become
		// "over full"
		Message m3 = new Message(h1,h4, MSG_ID3, m3Size);
		h1.createNewMessage(m3);

		assertTrue(mc.next());
		assertEquals(mc.TYPE_DELETE, mc.getLastType());
		assertTrue(mc.getLastDropped());
		assertEquals(MSG_ID2, mc.getLastMsg().getId());

		assertTrue(mc.next());
		assertEquals(mc.TYPE_CREATE, mc.getLastType());
		assertEquals(MSG_ID3, mc.getLastMsg().getId());
		assertTrue(h1.getBufferOccupancy() > 100); // buffer occupancy > 100%

		assertFalse(mc.next());

		clock.advance((m1Size/TRANSMIT_SPEED) + 1);
		updateAllNodes(); // now transmission should be done

		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());
		assertEquals(MSG_ID1, mc.getLastMsg().getId());

		assertTrue(mc.next()); // now should drop the transferred message
		assertEquals(mc.TYPE_DELETE, mc.getLastType());
		assertTrue(mc.getLastDropped());
		assertEquals(MSG_ID1, mc.getLastMsg().getId());

		 // buffer occupancy should drop back under 100 %
		assertTrue(h1.getBufferOccupancy() < 100);

		// should start transferring MSG_ID3
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(MSG_ID3, mc.getLastMsg().getId());

		assertFalse(mc.next());
	}

	@Test
	public void testTtlExpiry() {
		final int TIME_STEP = 10;
		Message m1 = new Message(h1,h3, MSG_ID1, 1);
		h1.createNewMessage(m1);
		checkCreates(1);

		clock.advance(TIME_STEP);
		updateAllNodes();
		assertFalse(mc.next());

		// relay msg1 from h1 to h2
		h1.connect(h2);
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		clock.advance(TIME_STEP);
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());

		assertFalse(mc.next());

		clock.advance((TTL-1)*60 - TIME_STEP*2);
		updateAllNodes();
		assertFalse(mc.next());
		Message m2 = new Message(h4,h3, MSG_ID2, 1);
		h4.createNewMessage(m2);
		checkCreates(1);

		clock.advance(61);
		updateAllNodes();

		// h1 and h2 should delete the expired message
		assertTrue(mc.next());
		assertEquals(mc.TYPE_DELETE, mc.getLastType());
		assertEquals(h1, mc.getLastFrom());
		assertEquals(MSG_ID1, mc.getLastMsg().getId());

		assertTrue(mc.next());
		assertEquals(mc.TYPE_DELETE, mc.getLastType());
		assertEquals(h2, mc.getLastFrom());
		assertEquals(MSG_ID1, mc.getLastMsg().getId());

		assertFalse(mc.next()); // h4 shouldn't remove the msg just yet

		clock.advance((TTL-1)*60 -61);
		updateAllNodes();
		assertFalse(mc.next()); // not yet either
		clock.advance(61);

		updateAllNodes(); // but now it's time for h4 to remove the msg

		assertTrue(mc.next());
		assertEquals(mc.TYPE_DELETE, mc.getLastType());
		assertEquals(h4, mc.getLastFrom());
		assertEquals(MSG_ID2, mc.getLastMsg().getId());

		assertFalse(mc.next());
	}

	@Test
	public void testResponse() {
		Message m1 = new Message(h1,h3, MSG_ID1, 1);
		m1.setResponseSize(1);
		h1.createNewMessage(m1);
		h1.connect(h2);
		updateAllNodes();
		clock.advance(10);
		updateAllNodes();
		h2.connect(h3);
		updateAllNodes();

		// started h2->h3 relay
		mc.reset();

		clock.advance(10);
		updateAllNodes();

		// finished relay
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());

		// h3 should reply by creating a reply message..
		assertTrue(mc.next());
		assertEquals(mc.TYPE_CREATE, mc.getLastType());
		assertEquals(routing.ActiveRouter.RESPONSE_PREFIX + MSG_ID1,
				mc.getLastMsg().getId());
		assertEquals(h3, mc.getLastFrom());
		assertEquals(h1, mc.getLastTo());

		// .. and starting its transfer
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(routing.ActiveRouter.RESPONSE_PREFIX + MSG_ID1,
				mc.getLastMsg().getId());
		assertEquals(h3, mc.getLastFrom());
		assertEquals(h2, mc.getLastTo());

		assertFalse(mc.next());
	}

	private void newMessage(String id, DTNHost from, DTNHost to) {
		Message m = new Message(from, to, id, 1);
		from.createNewMessage(m);
	}

	/**
	 * Runs a message exchange between node h1 and another node
	 * @param withDestination If true, the other node is the final destination
	 *        of the messages, if false, the other node is not the final dst
	 * @return The order of the messages in a space delimited string
	 */
	private String runMessageExchange(boolean withDestination) {
		String msgIds = "";
		int nrof = 5;
		DTNHost dst = h4;
		DTNHost other = h2;

		clock.setTime(0.0);
		newMessage("1", h1, dst);
		clock.advance(2.5);
		newMessage("2", h1, dst);
		clock.advance(3.5);
		newMessage("3", h1, dst);
		clock.advance(1.5);
		newMessage("3", h1, dst);
		clock.advance(2.0);
		newMessage("4", h1, dst);
		clock.advance(2.5);;
		newMessage("5", h1, dst);

		if (withDestination) {
			h1.connect(dst);
		}
		else {
			h1.connect(other);
		}

		mc.reset();
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());

		for (int i=0; i < nrof; i++) {
			msgIds += mc.getLastMsg().getId()+ " ";
			clock.advance(10);
			updateAllNodes();
			assertTrue("index " + i, mc.next());
			assertEquals(mc.TYPE_RELAY, mc.getLastType());
			if (i < nrof - 1) {
				assertTrue("index " + i, mc.next());
				assertEquals(mc.TYPE_START, mc.getLastType());
			}
		}

		assertFalse(mc.next());

		return msgIds;
	}

	@Test
	public void testFifoSendingQ() throws Exception {
		ts.putSetting(MessageRouter.SEND_QUEUE_MODE_S,
				""+MessageRouter.Q_MODE_FIFO);
		this.setUp();

		String expectedIds = "1 2 3 4 5 ";

		assertEquals(expectedIds, runMessageExchange(true));
		assertEquals(expectedIds, runMessageExchange(false));
	}

	@Test
	public void testRandomSendingQ() throws Exception {
		ts.putSetting(MessageRouter.SEND_QUEUE_MODE_S,
				""+MessageRouter.Q_MODE_RANDOM);
		this.setUp();

		String orderedIds = "1 2 3 4 5 ";

		assertNotSame(orderedIds, runMessageExchange(true));
		assertNotSame(orderedIds, runMessageExchange(false));
	}

    /**
     * This test checks whether the message ordering interval works.
     * The router is configured to sent high priority messages first.
     * The router is given a message with higher priority after the messages had already
     * been ordered. The message should not be sent before the message ordering interval has passed,
     * as that would mean the messages got reordered.
     * Once we reordered the messages, the high priority message should be sent before other messages.
     *
     * @throws Exception In case something in the settings does not work
     */
	@Test
    public void testMessageOrderingInterval() throws Exception {
	    final double messageOrderingInterval=5;
	    ts.putSetting(ActiveRouter.MESSAGE_ORDERING_INTERVAL_S, Double.toString(messageOrderingInterval));
	    ts.putSetting(MessageRouter.SEND_QUEUE_MODE_S, ""+MessageRouter.Q_MODE_PRIO);
	    this.setUp();

        ActiveRouter sendingRouter = (ActiveRouter)h0.getRouter();

        Message m1 = new Message(h0, h1, MSG_ID1, 0, 1);
        h0.createNewMessage(m1);
        Message m2 = new Message(h0, h1, MSG_ID2, 0, 1);
        h0.createNewMessage(m2);
        List<String> lowPrioMessages = Arrays.asList(MSG_ID1, MSG_ID2);

        //Connect h0 to another host
        h0.connect(h2);

        //We should order messages for the first time now
        updateAllNodes();

        //Here's a new message with higher prio. It should be first to send after reordering
        Message m3 = new Message(h0, h1, MSG_ID3, 0, 2);
        h0.createNewMessage(m3);

        //Advance time enough for a message to be sent but not enough to reorder messages
        final double smallTimeDifference = 0.1;
        clock.advance(messageOrderingInterval - smallTimeDifference);
        updateAllNodes();
        boolean isSendingLowPrioMsg = false;
        for (String msg : lowPrioMessages){
            if (sendingRouter.isSending(msg)){
                isSendingLowPrioMsg = true;
            }
        }
        assertTrue("We should not have reordered the messages yet.", isSendingLowPrioMsg);

        //Now advance time enough to reorder
        clock.advance(smallTimeDifference);
        updateAllNodes();
        assertTrue("We should send the message with higher priority now.", sendingRouter.isSending(MSG_ID3));
    }

    @Test
    public void testGetMessageOrderingInterval() throws Exception {
        final double messageOrderingInterval=3;
        ts.putSetting(ActiveRouter.MESSAGE_ORDERING_INTERVAL_S, Double.toString(messageOrderingInterval));
        this.setUp();

        assertEquals("Message ordering interval was not set correctly.", messageOrderingInterval,
                ((EpidemicRouter)routerProto).getMessageOrderingInterval());
    }

    @Test
    public void testDefaultMessageOrderingInterval() throws Exception{
	    ts = new TestSettings();
	    this.setUp();
        assertEquals("Default message ordering interval should be 0.0.", 0.0,
                ((EpidemicRouter)routerProto).getMessageOrderingInterval());
    }

    /**
     * Similar test like {@link #testMessageOrderingInterval()} just for messages that are final deliveries.
     * @throws Exception
     */
    @Test
    public void testInitialMessageOrderingIntervalForConnected() throws Exception{
        final double messageOrderingInterval=3;
        ts.putSetting(ActiveRouter.MESSAGE_ORDERING_INTERVAL_S, Double.toString(messageOrderingInterval));
        ts.putSetting(MessageRouter.SEND_QUEUE_MODE_S, ""+MessageRouter.Q_MODE_PRIO);
        this.setUp();

        ActiveRouter sendingRouter = (ActiveRouter)h0.getRouter();

        Message m1 = new Message(h0, h1, MSG_ID1, 0, 1);
        h0.createNewMessage(m1);
        //This message is not for connected, but has a higher priority.
        //If we have cached messages for connected hosts those should still be sent before this one
        Message m2NotForConnected = new Message(h0, h2, MSG_ID2, 0, 3);
        h0.createNewMessage(m2NotForConnected);

        //Connect h0 to the host for whom the messages are destined
        h0.connect(h1);

        //We should order messages for the first time now
        updateAllNodes();
        assertTrue("We should be sending the only message for connected first.", sendingRouter.isSending(MSG_ID1));

        //Here's a new message with higher prio. It should be first to send after reordering
        Message m3 = new Message(h0, h1, MSG_ID3, 0, 2);
        h0.createNewMessage(m3);
        Message m4 = new Message(h0, h1, MSG_ID4, 0, 1);
        h0.createNewMessage(m4);

        //Advance time enough for a message to be sent but not enough to reorder messages
        final double smallTimeDifference = 0.1;
        clock.advance(messageOrderingInterval - smallTimeDifference);
        updateAllNodes();

        assertTrue("We should not have reordered the messages yet, so we should be sending non-direct messages.",
                sendingRouter.isSending(MSG_ID2));

        //Now advance time enough to reorder
        clock.advance(smallTimeDifference);
        updateAllNodes();
        assertTrue("We should send the message with higher priority now.", sendingRouter.isSending(MSG_ID3));

    }

    /**
     * Makes sure the provided host has a {@link DatabaseApplication} and adds the provided data item to its stored
     * data.
     * @param host Host to add data item to.
     * @param data The data item to add.
     */
    private void setUpAsDataCarrier(DTNHost host, DisasterData data) {
        DatabaseApplication app = DatabaseApplicationUtil.findDatabaseApplication(host.getRouter());
        if (app == null) {
            app = new DatabaseApplication(ts);
            host.getRouter().addApplication(app);
        }
        app.update(host);
        app.disasterDataCreated(host, data);
    }
}
