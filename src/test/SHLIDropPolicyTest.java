package test;

import buffermanagement.PassiveDropPolicy;
import buffermanagement.SHLIDropPolicy;
import core.Message;

public class SHLIDropPolicyTest extends AbstractDropPolicyTest {

	public SHLIDropPolicyTest() {
		super("SHLIDropPolicy");
	}
	
	@Override
	protected void setUp() throws Exception {
		ts.putSetting("Group.msgTtl", "100");
		ts.putSetting("Group1.bufferSize", "3");
		super.setUp();
	}
	
	/**
	 * Test the case when the buffer of the receiver is empty
	 */
	public void testBufferFree() {
		assertTrue(r0.getDropPolicy() instanceof SHLIDropPolicy);
		
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
	
	public void testDrop() {
		assertTrue(r0.getDropPolicy() instanceof SHLIDropPolicy);
		
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
		
		m1.setTtl(10);
		m2.setTtl(8);
		m3.setTtl(7);
		
		
		Message m4 = new Message(h0, h1, msgId4, 2);
		h0.createNewMessage(m4);
				
		assertTrue(mc.next());
		assertEquals(mc.getLastType(), mc.TYPE_DELETE);
		assertTrue(mc.getLastDropped());
		assertFalse(h0.getMessageCollection().contains(m2));
		
		assertTrue(mc.next());
		assertEquals(mc.getLastType(), mc.TYPE_DELETE);
		assertTrue(mc.getLastDropped());
		assertFalse(h0.getMessageCollection().contains(m3));
		
	}

}
