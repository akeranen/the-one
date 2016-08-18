/*
 * Copyright 2016 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

package test;

import buffermanagement.FIFODropPolicy;
import core.Message;

/**
 * Tests the basic functionality of FIFODropPolicy implementation.
 */
public class FIFODropPolicyTest extends AbstractDropPolicyTest {

	public FIFODropPolicyTest() {
		super("FIFODropPolicy");
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
		assertTrue(r0.getDropPolicy() instanceof FIFODropPolicy);
		
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
	 * Test the drop logic.
	 */
	public void testDrop() {
		
		assertTrue(r0.getDropPolicy() instanceof FIFODropPolicy);
		
		Message m1 = new Message(h0, h1, msgId1, 1);
		h0.createNewMessage(m1);
		checkCreates(1);
		advanceWorld(1);
		
		Message m2 = new Message(h0, h1, msgId2, 1);
		h0.createNewMessage(m2);
		checkCreates(1);
		advanceWorld(1);
		
		Message m3 = new Message(h0, h1, msgId3, 1);
		h0.createNewMessage(m3);
		checkCreates(1);
		advanceWorld(1);
		
		
		Message m4 = new Message(h0, h1, msgId4, 2);
		h0.createNewMessage(m4);
				
		assertTrue(mc.next());
		assertEquals(mc.getLastType(), mc.TYPE_DELETE);
		assertTrue(mc.getLastDropped());
		assertFalse(h0.getMessageCollection().contains(m1));
		
		assertTrue(mc.next());
		assertEquals(mc.getLastType(), mc.TYPE_DELETE);
		assertTrue(mc.getLastDropped());
		assertFalse(h0.getMessageCollection().contains(m2));
		
		
		
	}
	
}
