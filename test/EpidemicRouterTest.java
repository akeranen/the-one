/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package test;

import routing.EpidemicRouter;
import routing.MessageRouter;
import core.DTNHost;
import core.Message;

/**
 * Tests for EpidemicRouter and, due the simple nature of Epidemic router,
 * also ActiveRouter in general. 
 */
public class EpidemicRouterTest extends AbstractRouterTest {

	private static int TTL = 300;
	
	@Override
	public void setUp() throws Exception {
		ts.putSetting(MessageRouter.MSG_TTL_S, ""+TTL);
		ts.putSetting(MessageRouter.B_SIZE_S, ""+BUFFER_SIZE);
		setRouterProto(new EpidemicRouter(ts));
		super.setUp();
	}
	
	/**
	 * Tests routing messages between three hosts
	 */
	public void testRouter() {
		// nothing should have happened so far
		assertEquals(mc.TYPE_NONE, mc.getLastType()); 
		
		Message m1 = new Message(h1, h3, msgId1, 1);
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
		Message m2 = new Message(h3,h1, msgId2, 1);
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
		assertEquals(msgId2, mc.getLastMsg().getId());
		assertFalse(mc.next());
		
		clock.advance(10);
		updateAllNodes();	// forwarding msg2 h3->h2
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());
		assertEquals(h2, mc.getLastTo());
		assertEquals(h3, mc.getLastFrom());
		assertEquals(msgId2, mc.getLastMsg().getId());
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
		assertEquals(msgId2, mc.getLastMsg().getId());
		assertTrue(mc.getLastFirstDelivery()); // message delivered to recipient
		assertFalse(mc.next());
	}
	
	/**
	 * Checks that delivering many messages in a row works
	 */
	public void testManyMessages() {
		
		Message m1 = new Message(h1,h2, msgId1, 1);
		h1.createNewMessage(m1);
		Message m2 = new Message(h1,h2, msgId2, 1);
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
	public void testDeliverableMessageExchange() {
		Message m1 = new Message(h1,h3, "Dummy1", 1);
		h1.createNewMessage(m1);
		Message m2 = new Message(h1,h3, "A_Dummy2", 1);
		h1.createNewMessage(m2);
		Message m3 = new Message(h1,h2, msgId1, 1);
		h1.createNewMessage(m3);
		
		Message m4 = new Message(h2,h3, "Dummy3", 1);
		h2.createNewMessage(m4);
		Message m5 = new Message(h2,h1, msgId2, 1);
		h2.createNewMessage(m5);
		Message m6 = new Message(h2,h3, "Dummy4", 1);
		h2.createNewMessage(m6);
		
		checkCreates(6);
				
		h1.connect(h2);
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType()); // starts h1->h2 msgId1
		assertEquals(h2, mc.getLastTo());
		assertEquals(msgId1, mc.getLastMsg().getId());
		
		clock.advance(10);
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType()); // finished delivery

		// should also start delivery of msgId2 from h2 to h1
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(msgId2, mc.getLastMsg().getId());
		assertEquals(h1, mc.getLastTo());
		
	}
	
	/**
	 * Tests aborting transfer when connections is disconnected during the
	 * transfer
	 */
	public void testMessageRelayAbort() {
		Message m1 = new Message(h1,h2, msgId1, BUFFER_SIZE);
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
	 * try disconnecting on the same update interval when a transfer should 
	 * be finished -> should not cause abort (anymore)
	 */
	public void testAbortWhenReady() {
		Message m1 = new Message(h2, h1, msgId2, 1);
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
	public void testDifferentOrdering() {	
		h1.connect(h2);
		Message m1 = new Message(h1,h2, msgId1, 1);
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
		Message m2 = new Message(h2,h1, msgId2, 1);
		h2.createNewMessage(m2);
		h2.update(true); // h2 and h1 are connected but this shouldn't start relay
		
		assertTrue(mc.next());
		assertEquals(mc.TYPE_CREATE, mc.getLastType());
		assertFalse(mc.next()); // shouldn't start transfer (prev not finalized)
		
		h1.update(true); // finalize the transfer of MSG2
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());
		assertEquals(h2, mc.getLastTo());
		
		// last transfer is finalized -> update should start transfer of msgId2
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(h1, mc.getLastTo());
		assertEquals(msgId2, mc.getLastMsg().getId());

		assertFalse(mc.next());
	}
	
	/**
	 * Tests if rejecting already delivered message(s) work
	 */
	public void testDoubleDelivery() {
		Message m1 = new Message(h1,h2, msgId1, 1);
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
	public void testQueueManagement() {
		Message m1 = new Message(h1,h3, "dummy", BUFFER_SIZE-1);
		h1.createNewMessage(m1);
		assertEquals(1, h1.getNrofMessages());
		Message m2 = new Message(h1,h3, msgId1, BUFFER_SIZE/3);
		h1.createNewMessage(m2);
		assertEquals(1, h1.getNrofMessages()); // message should replace dummy
		assertEquals(msgId1, h1.getMessageCollection().iterator().next().getId());
		
		mc.reset();
		
		clock.advance(10);
		Message m3 = new Message(h1,h3, msgId2, BUFFER_SIZE/3);
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
		// h1 should drop first msgId1 and then msgId2 (older first)
		assertTrue(mc.next());
		assertEquals(mc.TYPE_DELETE, mc.getLastType());
		assertEquals(h1, mc.getLastFrom());
		assertEquals(msgId1, mc.getLastMsg().getId());
		assertTrue(mc.getLastDropped());
		assertTrue(mc.next());
		assertEquals(mc.TYPE_DELETE, mc.getLastType());
		assertEquals(msgId2, mc.getLastMsg().getId());

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
	public void testNewMessageToFullBufferWhileTransferring() {
		int m3Size = BUFFER_SIZE-1;
		int m1Size = BUFFER_SIZE/2;
		
		Message m1 = new Message(h1,h3, msgId1, m1Size);
		h1.createNewMessage(m1);
		Message m2 = new Message(h1,h4, msgId2, BUFFER_SIZE/2);
		h1.createNewMessage(m2);
		checkCreates(2);
		
		h3.connect(h1);
		updateAllNodes(); // transfer of msgId1 should start
		
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType()); 
		clock.advance(1);
		updateAllNodes(); // should still be transferring
		assertFalse(mc.next());
		
		// creating a new message should cause dropping the msgId2 but
		// not msgId1 (which is being transferred) -> buffer should become 
		// "over full"
		Message m3 = new Message(h1,h4, msgId3, m3Size);
		h1.createNewMessage(m3);
		
		assertTrue(mc.next());
		assertEquals(mc.TYPE_DELETE, mc.getLastType());
		assertTrue(mc.getLastDropped());
		assertEquals(msgId2, mc.getLastMsg().getId());
		
		assertTrue(mc.next());
		assertEquals(mc.TYPE_CREATE, mc.getLastType());
		assertEquals(msgId3, mc.getLastMsg().getId());
		assertTrue(h1.getBufferOccupancy() > 100); // buffer occupancy > 100%
		
		assertFalse(mc.next());
		
		clock.advance((m1Size/TRANSMIT_SPEED) + 1);
		updateAllNodes(); // now transmission should be done
		
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());
		assertEquals(msgId1, mc.getLastMsg().getId());
		
		assertTrue(mc.next()); // now should drop the transferred message
		assertEquals(mc.TYPE_DELETE, mc.getLastType());
		assertTrue(mc.getLastDropped());
		assertEquals(msgId1, mc.getLastMsg().getId());
		
		 // buffer occupancy should drop back under 100 %
		assertTrue(h1.getBufferOccupancy() < 100);
		
		// should start transferring msgId3 
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(msgId3, mc.getLastMsg().getId());
		
		assertFalse(mc.next());
	}
	
	public void testTtlExpiry() {
		final int TIME_STEP = 10;
		Message m1 = new Message(h1,h3, msgId1, 1);
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
		Message m2 = new Message(h4,h3, msgId2, 1);
		h4.createNewMessage(m2);
		checkCreates(1);
		
		clock.advance(61);
		updateAllNodes();
		
		// h1 and h2 should delete the expired message
		assertTrue(mc.next());
		assertEquals(mc.TYPE_DELETE, mc.getLastType());
		assertEquals(h1, mc.getLastFrom());
		assertEquals(msgId1, mc.getLastMsg().getId());
		
		assertTrue(mc.next());
		assertEquals(mc.TYPE_DELETE, mc.getLastType());
		assertEquals(h2, mc.getLastFrom());
		assertEquals(msgId1, mc.getLastMsg().getId());
		
		assertFalse(mc.next()); // h4 shouldn't remove the msg just yet
		
		clock.advance((TTL-1)*60 -61);
		updateAllNodes();
		assertFalse(mc.next()); // not yet either
		clock.advance(61);

		updateAllNodes(); // but now it's time for h4 to remove the msg
		
		assertTrue(mc.next());
		assertEquals(mc.TYPE_DELETE, mc.getLastType());
		assertEquals(h4, mc.getLastFrom());
		assertEquals(msgId2, mc.getLastMsg().getId());
	
		assertFalse(mc.next());
	}
	
	public void testResponse() {
		Message m1 = new Message(h1,h3, msgId1, 1);
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
		assertEquals(routing.ActiveRouter.RESPONSE_PREFIX + msgId1, 
				mc.getLastMsg().getId());
		assertEquals(h3, mc.getLastFrom());
		assertEquals(h1, mc.getLastTo());

		// .. and starting its transfer
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(routing.ActiveRouter.RESPONSE_PREFIX + msgId1, 
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
	
	public void testFifoSendingQ() throws Exception {
		ts.putSetting(MessageRouter.SEND_QUEUE_MODE_S, 
				""+MessageRouter.Q_MODE_FIFO);
		this.setUp();

		String expectedIds = "1 2 3 4 5 ";
		
		assertEquals(expectedIds, runMessageExchange(true));
		assertEquals(expectedIds, runMessageExchange(false));
	}
	
	public void testRandomSendingQ() throws Exception {
		ts.putSetting(MessageRouter.SEND_QUEUE_MODE_S, 
				""+MessageRouter.Q_MODE_RANDOM);
		this.setUp();
		
		String orderedIds = "1 2 3 4 5 ";
		
		assertNotSame(orderedIds, runMessageExchange(true));
		assertNotSame(orderedIds, runMessageExchange(false));
	}
}
