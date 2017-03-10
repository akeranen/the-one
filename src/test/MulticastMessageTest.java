package test;

import core.ConnectionListener;
import core.DTNHost;
import core.Group;
import core.Message;
import core.MessageListener;
import core.MulticastMessage;
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
    private TestUtils utils;

    private MulticastMessage msg;
    private DTNHost from;
    private Group group1;

    @Before
    public void setUp() throws Exception {
        this.utils = new TestUtils(
                new ArrayList<ConnectionListener>(),
                new ArrayList<MessageListener>(),
                new TestSettings());
        Group.clearGroups();
        group1 = Group.createGroup(GROUP_ADDRESS_1);
        msg = new MulticastMessage(from, group1, "M", 100);
    }

    @Test(expected = java.lang.UnsupportedOperationException.class)
    public void testGetToThrowsException() {
        this.msg.getTo();
    }

    @Test
    public void testIsFinalRecipientReturnsTrueForHostWithSameGroup() {
        DTNHost host = this.utils.createHost();
        host.joinGroup(Group.getGroup(GROUP_ADDRESS_1));
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
        host.joinGroup(Group.getOrCreateGroup(GROUP_ADDRESS_2));
        assertFalse(this.msg.isFinalRecipient(host));
    }

    @Test
    public void testCompletesDeliveryReturnsFalse() {
        DTNHost host = this.utils.createHost();
        assertFalse(this.msg.completesDelivery(host));
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
    public void testRecipientsToString() {
        assertEquals(
                "Recipients descriptions should have been different.",
                "Group "+GROUP_ADDRESS_1,
                this.msg.recipientsToString());
    }
}
