/*
 * Copyright 2016 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

package test;

import buffermanagement.MOFODropPolicy;
import core.Message;

/**
 * Test cases for the MOFO (Most Forwarded) drop policy implementation.
 * @author michael
 *
 */
public class MOFODropPolicyTest extends AbstractDropPolicyTest {

	public MOFODropPolicyTest() {
		super("MOFODropPolicy");
	}
	
	@Override
	protected void setUp() throws Exception {
		ts.putSetting("Group1.bufferSize", "3");
		super.setUp();
	}
	
	/**
	 * Test the case when the buffer of the receiver is empty
	 */
	public void testBufferFree() {
		assertTrue(r0.getDropPolicy() instanceof MOFODropPolicy);
		
		Message m1 = new Message(h0, h3, msgId1, 1);
		h0.createNewMessage(m1);
		checkCreates(1);
		advanceWorld(1);
		
		h0.connect(h1);
		advanceWorld(1);
		
		assertTrue(mc.next());
		assertEquals(mc.getLastType(), mc.TYPE_START);
		advanceWorld(1);
		assertTrue(mc.next());
		assertEquals(mc.getLastType(), mc.TYPE_RELAY);
		assertEquals(mc.getLastFrom(), h0);
		assertEquals(mc.getLastTo(), h1);
		assertFalse(mc.getLastFirstDelivery());
	}
	
	/**
	 * Test the message forward counter.
	 */
	public void testForwardCounter() {
		Message m1 = new Message(h0, h1, msgId1, 1);
		h0.createNewMessage(m1);
		checkCreates(1);
		updateAllNodes();
		
		h0.connect(h2);
		advanceWorld(1);
		
		assertTrue(mc.next());
		assertEquals(mc.getLastType(), mc.TYPE_START);
        assertEquals(mc.getLastMsg().getId(), msgId1);
        assertEquals(mc.getLastFrom(), h0);
        assertEquals(mc.getLastTo(), h2);
		
		advanceWorld(1);
        assertTrue(mc.next());
        assertEquals(mc.getLastType(), mc.TYPE_RELAY);
        assertEquals(mc.getLastFrom(), h0);
        assertEquals(mc.getLastTo(), h2);
        assertFalse(mc.getLastFirstDelivery());
        
        assertEquals(m1.getForwardCount(), 1);
        assertEquals(((Message)h2.getMessageCollection().toArray()[0]).getForwardCount(), 0);
        
        disconnect(h0);
        h0.connect(h1);
        
        advanceWorld(1);
		assertTrue(mc.next());
		assertEquals(mc.getLastType(), mc.TYPE_START);
        assertEquals(mc.getLastMsg().getId(), msgId1);
        assertEquals(mc.getLastFrom(), h0);
        assertEquals(mc.getLastTo(), h1);
		
		advanceWorld(1);
        
        assertTrue(mc.next());
        assertEquals(mc.getLastType(), mc.TYPE_RELAY);
        assertEquals(mc.getLastFrom(), h0);
        assertEquals(mc.getLastTo(), h1);
        assertTrue(mc.getLastFirstDelivery());
        
        assertEquals(m1.getForwardCount(), 2);
        assertEquals(((Message)h2.getMessageCollection().toArray()[0]).getForwardCount(), 0);
        
	}
	
	
	/**
	 * Test the basic logic of the drop policy.
	 */
	public void testDrop() {
		assertTrue(r0.getDropPolicy() instanceof MOFODropPolicy);
		
		Message m1 = new Message(h0, h1, msgId1, 1);
		m1.setForwardCount(2);
		h0.createNewMessage(m1);
		checkCreates(1);
		advanceWorld(1);
		
		Message m2 = new Message(h0, h1, msgId2, 1);
		m2.setForwardCount(1);
		h0.createNewMessage(m2);
		checkCreates(1);
		advanceWorld(1);
		
		Message m3 = new Message(h0, h1, msgId3, 1);
		m3.setForwardCount(3);
		h0.createNewMessage(m3);
		checkCreates(1);
		advanceWorld(1);
		
		
		Message m4 = new Message(h0, h1, msgId4, 2);
		h0.createNewMessage(m4);
				
		assertTrue(mc.next());
		assertEquals(mc.getLastType(), mc.TYPE_DELETE);
		assertTrue(mc.getLastDropped());
		assertFalse(h0.getMessageCollection().contains(m3));
		
		assertTrue(mc.next());
		assertEquals(mc.getLastType(), mc.TYPE_DELETE);
		assertTrue(mc.getLastDropped());
		assertFalse(h0.getMessageCollection().contains(m1));
		
	}

}
