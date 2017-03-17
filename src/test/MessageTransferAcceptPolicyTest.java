package test;

import core.BroadcastMessage;
import core.ConnectionListener;
import core.DTNHost;
import core.Group;
import core.Message;
import core.MessageListener;
import core.MulticastMessage;
import org.junit.Before;
import org.junit.Test;
import routing.util.MessageTransferAcceptPolicy;

import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Contains tests for the MessageTransferAcceptPolicy class.
 *
 * Created by Britta Heymann on 16.02.2017.
 */
public class MessageTransferAcceptPolicyTest {
    private TestUtils utils;
    private TestSettings settings;

    private Message msg;
    private Message broadcast;
    private Message multicast;
    private DTNHost sender;
    private DTNHost recipient;

    @Before
    public void init() {
        Group.clearGroups();
        DTNHost.reset();
        this.settings = new TestSettings();
        this.utils = new TestUtils(
                new ArrayList<ConnectionListener>(),
                new ArrayList<MessageListener>(),
                this.settings);

        this.sender = this.utils.createHost();
        this.recipient = this.utils.createHost();

        this.msg = new Message(this.sender, recipient, "M", 100);
        this.broadcast = new BroadcastMessage(this.sender, "B", 50);
        Group g = Group.createGroup(0);
        g.addHost(sender);
        g.addHost(recipient);
        this.multicast = new MulticastMessage(this.sender,g,"G",100);

        this.settings.putSetting(MessageTransferAcceptPolicy.MTA_POLICY_NS, "simplepolicy");
    }

    @Test
    public void testAcceptSendingReturnsTrueForMessageAdheringToSimplePolicies() {
        this.settings.setNameSpace("simplepolicy");
        this.settings.putSetting(MessageTransferAcceptPolicy.TO_SPOLICY_S,
                Integer.toString(this.recipient.getAddress()));
        this.settings.putSetting(MessageTransferAcceptPolicy.FROM_SPOLICY_S,
                Integer.toString(MessageTransferAcceptPolicy.TO_ME_VALUE));
        this.settings.restoreNameSpace();

        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertTrue(
                "Message: Sending should have been accepted.",
                policy.acceptSending(this.sender, this.recipient, null, this.msg));
        assertTrue(
                "Multicast: Sending should have been accepted.",
                policy.acceptSending(this.sender, this.recipient, null, this.multicast));
    }

    @Test
    public void testAcceptSendingReturnsFalseForMessageWithSenderOutsideRange() {
        this.settings.setNameSpace("simplepolicy");
        this.settings.putSetting(MessageTransferAcceptPolicy.TO_SPOLICY_S,
                Integer.toString(this.recipient.getAddress()));
        this.settings.putSetting(MessageTransferAcceptPolicy.FROM_SPOLICY_S,
                Integer.toString(this.recipient.getAddress()));
        this.settings.restoreNameSpace();

        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertFalse(
                "Message: Sending should not have been accepted.",
                policy.acceptSending(this.sender, this.recipient, null, this.msg));
        assertFalse(
                "Multicast: Sending should not have been accepted.",
                policy.acceptSending(this.sender, this.recipient, null, this.multicast));
    }

    @Test
    public void testAcceptSendingReturnsFalseForMessageWithRecipientOutsideRange() {
        this.settings.setNameSpace("simplepolicy");
        this.settings.putSetting(MessageTransferAcceptPolicy.TO_SPOLICY_S,
                Integer.toString(MessageTransferAcceptPolicy.TO_ME_VALUE));
        this.settings.putSetting(MessageTransferAcceptPolicy.FROM_SPOLICY_S,
                Integer.toString(MessageTransferAcceptPolicy.TO_ME_VALUE));
        this.settings.restoreNameSpace();

        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertFalse(
                "Message: Sending should not have been accepted.",
                policy.acceptSending(this.sender, this.recipient, null, this.msg));
        assertFalse(
                "Multicast: Sending should not have been accepted.",
                policy.acceptSending(this.sender, this.recipient, null, this.multicast));
    }

    @Test
    public void testAcceptSendingReturnsTrueForBroadcastWithSenderInsideRange() {
        this.settings.setNameSpace("simplepolicy");
        this.settings.putSetting(MessageTransferAcceptPolicy.FROM_SPOLICY_S,
                Integer.toString(MessageTransferAcceptPolicy.TO_ME_VALUE));
        this.settings.restoreNameSpace();

        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertTrue(
                "Sending should have been accepted.",
                policy.acceptSending(this.sender, this.recipient, null, this.broadcast));
    }

    @Test
    public void testAcceptSendingReturnsFalseForBroadcastWithSenderOutsideRange() {
        this.settings.setNameSpace("simplepolicy");
        this.settings.putSetting(MessageTransferAcceptPolicy.FROM_SPOLICY_S,
                Integer.toString(this.recipient.getAddress()));
        this.settings.restoreNameSpace();

        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertFalse(
                "Sending should not have been accepted.",
                policy.acceptSending(this.sender, this.recipient, null, this.broadcast));
    }
}
