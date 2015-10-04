/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package test;

import routing.MessageRouter;
import routing.ProphetRouter;
import core.Message;

/**
 * Tests for PRoPHET router
 */
public class ProphetRouterTest extends AbstractRouterTest {

	private static int SECONDS_IN_TIME_UNIT = 60;
	
	@Override
	public void setUp() throws Exception {
		ts.setNameSpace(null);
		ts.putSetting(MessageRouter.B_SIZE_S, ""+BUFFER_SIZE);
		ts.putSetting(ProphetRouter.PROPHET_NS + "." + 
				ProphetRouter.SECONDS_IN_UNIT_S , SECONDS_IN_TIME_UNIT+"");
		setRouterProto(new ProphetRouter(ts));
		super.setUp();
	}
	
	/**
	 * Tests normal routing
	 */
	public void testRouting() {
		Message m1 = new Message(h1,h2, msgId2, 1);
		h1.createNewMessage(m1);
		Message m2 = new Message(h1,h3, msgId3, 1);
		h1.createNewMessage(m2);
		Message m3 = new Message(h1,h4, msgId4, 1);
		h1.createNewMessage(m3);
		Message m4 = new Message(h1,h6, "dummy", 1); // this message should not be fwded
		h1.createNewMessage(m4);
		Message m5 = new Message(h1,h5, msgId5, 1);
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

		// first h1 should transfer msgId2 to h3 (final recipient)
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(msgId3, mc.getLastMsg().getId());
		assertEquals(h1, mc.getLastFrom());
		assertFalse(mc.next());
		
		clock.advance(10);
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType()); // finished transfer
		assertEquals(msgId3, mc.getLastMsg().getId());
		assertTrue(mc.getLastFirstDelivery());
		
		// h1 should next transfer msgId5 to h3 because h3 is connected to h5
		assertTrue(mc.next());
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(msgId5, mc.getLastMsg().getId());
		assertEquals(h1, mc.getLastFrom());
		assertFalse(mc.next());
		
		clock.advance(10);
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType()); // finished transfer
		assertEquals(msgId5, mc.getLastMsg().getId());
		assertTrue(mc.next());
		
		// next h1 should transfer msgId4 since h3 knows h4 trough h5
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(msgId4, mc.getLastMsg().getId());
		assertEquals(h1, mc.getLastFrom());
		assertFalse(mc.next());
		
		doRelay(); // relaying should be tested by now..
		assertTrue(mc.next());
		
		// now h3 should transfer msgId5 to h5
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(msgId5, mc.getLastMsg().getId());
		assertEquals(h3, mc.getLastFrom());

		doRelay(); // id5 delivered to h5
		assertTrue(mc.getLastFirstDelivery());
		assertTrue(mc.next());
		
		// next h3 should transfer id4 to h5
		assertEquals(mc.TYPE_START, mc.getLastType());
		assertEquals(msgId4, mc.getLastMsg().getId());
		assertEquals(h3, mc.getLastFrom());
		
		doRelay();
		
		// now no new transfers should be started
		assertFalse(mc.next());
				
	}
	
	private void doRelay() {
		clock.advance(10);
		updateAllNodes();
		assertTrue(mc.next());
		assertEquals(mc.TYPE_RELAY, mc.getLastType());		
	}
	
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