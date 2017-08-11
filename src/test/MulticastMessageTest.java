package test;

import core.ConnectionListener;
import core.DTNHost;
import core.Group;
import core.Message;
import core.MessageListener;
import core.MulticastMessage;
import core.SimError;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Contains tests for the MulticastMessage class
 *
 * Created by Marius Meyer on 10.03.17.
 */
public class MulticastMessageTest {

    private static final int GROUP_ADDRESS_1 = 0;
    private static final int GROUP_ADDRESS_2 = 1;
    private static final int PRIORITY = 7;
    private static final int MESSAGE_SIZE = 100;

    private static final int TWO_RECIPIENTS = 2;

    private TestUtils utils = new TestUtils(new ArrayList<ConnectionListener>(), new ArrayList<MessageListener>(),
            new TestSettings());

    private MulticastMessage msg;
    private MulticastMessage msgPrio;
    private Group group1;
    private DTNHost from = this.utils.createHost();

    public MulticastMessageTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    public void setUp()  {
        Group.clearGroups();
        this.group1 = Group.createGroup(GROUP_ADDRESS_1);
        this.group1.addHost(from);
        msg = new MulticastMessage(from, this.group1, "M", MESSAGE_SIZE);
        msgPrio = new MulticastMessage(from, this.group1, "N", MESSAGE_SIZE, PRIORITY);
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

    /**
     * Checks that {@link MulticastMessage#completesDelivery(DTNHost)} returns true if all group members have been
     * reached before.
     */
    @Test
    public void testCompletesDeliveryReturnsTrueIfAllGroupMembersHaveBeenReached() {
        // Use message to group with two members.
        DTNHost member2 = this.utils.createHost();
        this.group1.addHost(member2);

        // Send message to all group members.
        this.from.receiveMessage(this.msg, this.utils.createHost());
        member2.receiveMessage(this.msg, this.utils.createHost());

        assertTrue(
                "Expected delivery to be complete for any host.", this.msg.completesDelivery(this.utils.createHost()));
    }

    /**
     * Checks that {@link MulticastMessage#completesDelivery(DTNHost)} returns true for the last group member to be
     * reached.
     */
    @Test
    public void testCompletesDeliveryReturnsTrueForLastUnreachedGroupMember() {
        // Use message to group with three members.
        DTNHost member2 = this.utils.createHost();
        DTNHost member3 = this.utils.createHost();
        this.group1.addHost(member2);
        this.group1.addHost(member3);

        // Send message to all group members but one.
        this.from.receiveMessage(this.msg, this.utils.createHost());
        member2.receiveMessage(this.msg, this.utils.createHost());

        assertTrue("Expected delivery to be complete with third host.", this.msg.completesDelivery(member3));
    }

    /**
     * Checks that {@link MulticastMessage#completesDelivery(DTNHost)} returns false if a group member is reached, but
     * there are still members missing.
     */
    @Test
    public void testCompletesDeliveryReturnsFalseForUnreachedGroupMemberIfMoreAreMissing() {
        // Use message to group with three members.
        DTNHost member2 = this.utils.createHost();
        DTNHost member3 = this.utils.createHost();
        this.group1.addHost(member2);
        this.group1.addHost(member3);

        // Send message to a single group member.
        this.from.receiveMessage(this.msg, this.utils.createHost());

        assertFalse("Expected delivery to not be complete with second host.", this.msg.completesDelivery(member2));
    }

    /**
     * Checks that {@link MulticastMessage#completesDelivery(DTNHost)} returns false if only one member is missing and
     * the argument is a {@link DTNHost} not in the group.
     */
    @Test
    public void testCompletesDeliveryReturnsFalseForAlmostCompletedDeliveryToNonGroupMember() {
        // Use message to group with three members.
        DTNHost member2 = this.utils.createHost();
        DTNHost member3 = this.utils.createHost();
        this.group1.addHost(member2);
        this.group1.addHost(member3);

        // Send message to all group members but one.
        this.from.receiveMessage(this.msg, this.utils.createHost());
        member2.receiveMessage(this.msg, this.utils.createHost());

        assertFalse(
                "Expected delivery to not be complete with arbitrary third host.",
                this.msg.completesDelivery(this.utils.createHost()));
    }

    /**
     * Checks that {@link MulticastMessage#getRemainingRecipients()} returns the expected ones after several hops
     * being delivered to some recipients.
     */
    @Test
    public void testRemainingRecipients() {
        // Use message to group with three members.
        DTNHost member2 = this.utils.createHost();
        DTNHost member3 = this.utils.createHost();
        this.group1.addHost(member2);
        this.group1.addHost(member3);

        // At the beginning, the message should be delivered to its creator, but nobody else.
        this.from.createNewMessage(this.msg);
        MulticastMessage m = (MulticastMessage)findMessageInHostBuffer(this.msg.getId(), this.from);
        Collection<Integer> originalRemainingRecipients = m.getRemainingRecipients();
        assertEquals("Expected two remaining recipients.", TWO_RECIPIENTS, originalRemainingRecipients.size());
        assertTrue(
                "Member 2 should be in remaining recipients.",
                originalRemainingRecipients.contains(member2.getAddress()));
        assertTrue(
                "Member 3 should be in remaining recipients.",
                originalRemainingRecipients.contains(member3.getAddress()));

        // Deliver to a host not in group. The remaining recipients should not change.
        DTNHost nonMember = this.utils.createHost();
        this.from.sendMessage(this.msg.getId(), nonMember);
        nonMember.messageTransferred(this.msg.getId(), this.from);
        MulticastMessage m2 = (MulticastMessage)findMessageInHostBuffer(this.msg.getId(), nonMember);
        assertArrayEquals("Remaining recipients should not have changed for first hop.",
                originalRemainingRecipients.toArray(), m2.getRemainingRecipients().toArray());
        assertArrayEquals("Remaining recipients should not have changed for creator.",
                originalRemainingRecipients.toArray(), m.getRemainingRecipients().toArray());

        // Deliver to a host in group. The remaining recipients for that host should now only include the last host
        // in group.
        nonMember.sendMessage(this.msg.getId(), member2);
        member2.messageTransferred(this.msg.getId(), nonMember);
        MulticastMessage m3 = (MulticastMessage)findMessageInHostBuffer(this.msg.getId(), member2);
        assertArrayEquals("Remaining recipients should not have changed for creator.",
                originalRemainingRecipients.toArray(), m.getRemainingRecipients().toArray());
        assertArrayEquals("Remaining recipients should not have changed for first hop.",
                originalRemainingRecipients.toArray(), m2.getRemainingRecipients().toArray());
        assertEquals("Expected one remaining recipient for group member.", 1, m3.getRemainingRecipients().size());
        assertTrue("Expected third member to still be set as recipient.",
                m3.getRemainingRecipients().contains(member3.getAddress()));
    }

    @Test(expected = SimError.class)
    public void testSenderNotInDestinationGroupThrowsError() {
        Group group2 = Group.createGroup(GROUP_ADDRESS_2);
        new MulticastMessage(from, group2, "M", MESSAGE_SIZE);
    }

    @Test
    public void testGetTypeReturnsMulticast() {
        assertEquals(Message.MessageType.MULTICAST, this.msg.getType());
    }

    @Test
    public void testReplicateDoesNotChangeType() {
        Message replicate = this.msg.replicate();
        assertTrue("Replicated message should have been of type MulticastMessage.",
                replicate instanceof MulticastMessage);
    }

    @Test
    public void testReplicateDoesNotChangeDestinationGroup() {
        MulticastMessage replicate = (MulticastMessage) this.msg.replicate();
        assertEquals("Replicated message should be sent to same group.", replicate.getGroup().getAddress(),
                GROUP_ADDRESS_1);
    }

    @Test
    public void testGetGroupReturnsTheCorrectGroup() {
        MulticastMessage message = new MulticastMessage(from, Group.getGroup(GROUP_ADDRESS_1), "M", MESSAGE_SIZE);
        assertEquals("Destination group should be Group " + GROUP_ADDRESS_1, Group.getGroup(GROUP_ADDRESS_1),
                message.getGroup());
    }

    @Test
    public void testRecipientsToString() {
        assertEquals("Recipients descriptions should have been different.", this.msg.getGroup().toString(),
                this.msg.recipientsToString());
    }

    @Test
    public void testPriority() {
        assertEquals(msg.getPriority(), Message.INVALID_PRIORITY);
        assertEquals(msgPrio.getPriority(), PRIORITY);
    }

    /**
     * Looks for a message with a certain ID in a host's buffer.
     * @param id ID to look for.
     * @param host The host to search the buffer for.
     * @return The message.
     */
    private static Message findMessageInHostBuffer(String id, DTNHost host) {
        for (Message m : host.getMessageCollection()) {
            if (m.getId().equals(id)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Cannot find message with id " + id + " in buffer of host " + host);
    }
}
