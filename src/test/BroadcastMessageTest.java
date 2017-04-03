package test;

import java.util.ArrayList;

import core.*;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import test.TestUtils;
import test.TestSettings;

/**
 * Contains tests for BroadcastMessage.
 *
 * Created by Britta Heymann on 15.02.2017.
 */
public class BroadcastMessageTest {
    private TestUtils utils;

    private BroadcastMessage msg, msgPrio;
    private DTNHost from;

    @Before
    public void setUp() throws Exception {
        this.utils = new TestUtils(
                new ArrayList<ConnectionListener>(),
                new ArrayList<MessageListener>(),
                new TestSettings());

        msg = new BroadcastMessage(from, "M", 100);
        msgPrio = new BroadcastMessage(from, "N", 100, 3);
    }

    @Test(expected = java.lang.UnsupportedOperationException.class)
    public void testGetToThrowsException() {
        this.msg.getTo();
    }

    @Test
    public void testIsFinalRecipientReturnsTrueForArbitraryHost() {
        DTNHost host = this.utils.createHost();
        assertTrue(this.msg.isFinalRecipient(host));
    }

    @Test
    public void testCompletesDeliveryReturnsFalse() {
        DTNHost host = this.utils.createHost();
        assertFalse(this.msg.completesDelivery(host));
    }

    @Test
    public void testGetTypeReturnsBroadcast() {
        assertEquals(Message.MessageType.BROADCAST, this.msg.getType());
    }

    @Test
    public void testReplicateDoesNotChangeType() {
        Message replicate = this.msg.replicate();
        assertTrue(
                "Replicated message should have been of type BroadcastMessage.",
                replicate instanceof BroadcastMessage);
    }

    @Test
    public void testRecipientsToString() {
        assertEquals(
                "Recipients descriptions should have been different.",
                "everyone",
                this.msg.recipientsToString());
    }
    
    @Test
   	public void testPriority(){
   		assertEquals(msg.getPriority(), -1);
   		assertEquals(msgPrio.getPriority(), 3);
   	}
}
