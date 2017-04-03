package test;

import core.ConnectionListener;
import core.DTNHost;
import core.Group;
import core.Message;
import core.MessageListener;
import core.MulticastMessage;
import core.NetworkInterface;
import core.SimError;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Contains tests for the MulticastMessage class
 *
 * Created by Marius Meyer on 10.03.17.
 */
public class MulticastMessageTest {

    private final static int GROUP_ADDRESS_1 = 0;
    private final static int GROUP_ADDRESS_2 = 1;
    private TestUtils utils = new TestUtils(
            new ArrayList<ConnectionListener>(),
            new ArrayList<MessageListener>(),
            new TestSettings());

    private MulticastMessage msg, msgPrio;
    private DTNHost from = new TestDTNHost(new ArrayList<NetworkInterface>(),null,null);

    @Before
    public void setUp() throws Exception {
        Group.clearGroups();
        Group group1 = Group.createGroup(GROUP_ADDRESS_1);
        group1.addHost(from);
        msg = new MulticastMessage(from, group1, "M", 100);
        msgPrio = new MulticastMessage(from, group1, "N", 100, 7);
    }

    @Test(expected = java.lang.UnsupportedOperationException.class)
    public void testGetToThrowsException() {
        this.msg.getTo();
    }

    @Test
    public void testIsFinalRecipientReturnsTrueForHostWithSameGroup() {
        DTNHost host = this.utils.createHost();
        Group.getGroup(GROUP_ADDRESS_1).addHost(host);
        assertTrue(this.msg.isFinalRecipient(host));
    }

    @Test
    public void testIsFinalRecipientReturnsFalseForHostNotInGroup() {
        DTNHost host = this.utils.createHost();
        assertFalse(this.msg.isFinalRecipient(host));
    }

    @Test
    public void testIsFinalRecipientReturnsFalseForHostWithOtherGroup() {
        DTNHost host = this.utils.createHost();
        Group.getOrCreateGroup(GROUP_ADDRESS_2).addHost(host);
        assertFalse(this.msg.isFinalRecipient(host));
    }

    @Test
    public void testCompletesDeliveryReturnsFalse() {
        DTNHost host = this.utils.createHost();
        assertFalse(this.msg.completesDelivery(host));
    }

    @Test(expected = SimError.class)
    public void testSenderNotInDestinationGroupThrowsError(){
        Group group2 = Group.createGroup(GROUP_ADDRESS_2);
        MulticastMessage wrongMsg = new MulticastMessage(from,group2,"M",100);
    }

    @Test
    public void testGetTypeReturnsMulticast() {
        assertEquals(Message.MessageType.MULTICAST, this.msg.getType());
    }

    @Test
    public void testReplicateDoesNotChangeType() {
        Message replicate = this.msg.replicate();
        assertTrue(
                "Replicated message should have been of type MulticastMessage.",
                replicate instanceof MulticastMessage);
    }

    @Test
    public void testReplicateDoesNotChangeDestinationGroup() {
        MulticastMessage replicate = (MulticastMessage) this.msg.replicate();
        assertEquals(
                "Replicated message should be sent to same group.",replicate.getGroup().getAddress(),GROUP_ADDRESS_1);
    }

    @Test
    public void testGetGroupReturnsTheCorrectGroup(){
        MulticastMessage msg = new MulticastMessage(from,Group.getGroup(GROUP_ADDRESS_1),"M",100);
        assertEquals("Destination group should be Group "+GROUP_ADDRESS_1,
                Group.getGroup(GROUP_ADDRESS_1),msg.getGroup());
    }

    @Test
    public void testRecipientsToString() {
        assertEquals(
                "Recipients descriptions should have been different.",
                this.msg.getGroup().toString(),
                this.msg.recipientsToString());
    }
    
    @Test
	public void testPriority(){
		assertEquals(msg.getPriority(), -1);
		assertEquals(msgPrio.getPriority(), 7);
	}
}
