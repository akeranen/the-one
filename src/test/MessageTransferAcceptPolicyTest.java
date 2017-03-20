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


    private static final String NOT_ACCEPT_S = "Sending should not have bin accepted.";
    private static final String ACCEPT_S = "Sending should have bin accepted.";

    private static final String POLICY_NS = "simplepolicy";

    private TestUtils utils;
    private TestSettings settings;

    private Message msg;
    private Message broadcast;
    private Message multicast;
    private DTNHost sender;
    private DTNHost recipient;
    private DTNHost recipient2;

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
        this.recipient2 = this.utils.createHost();

        this.msg = new Message(this.sender, recipient, "M", 100);
        this.broadcast = new BroadcastMessage(this.sender, "B", 50);
        Group g = Group.createGroup(0);
        g.addHost(sender);
        g.addHost(recipient);
        g.addHost(recipient2);
        this.multicast = new MulticastMessage(this.sender,g,"G",100);

        this.settings.putSetting(MessageTransferAcceptPolicy.MTA_POLICY_NS, POLICY_NS);
    }

    @Test
    public void testAcceptSendingReturnsTrueForMessageAdheringToSimplePolicies() {
        this.settings.setNameSpace(POLICY_NS);
        this.settings.putSetting(MessageTransferAcceptPolicy.TO_SPOLICY_S,
                Integer.toString(this.recipient.getAddress()));
        this.settings.putSetting(MessageTransferAcceptPolicy.FROM_SPOLICY_S,
                Integer.toString(MessageTransferAcceptPolicy.TO_ME_VALUE));
        this.settings.restoreNameSpace();

        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertTrue(
                ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.msg));
    }

    @Test
    public void testAcceptSendingReturnsFalseForMessageWithSenderOutsideRange() {
        this.settings.setNameSpace(POLICY_NS);
        this.settings.putSetting(MessageTransferAcceptPolicy.TO_SPOLICY_S,
                Integer.toString(this.recipient.getAddress()));
        this.settings.putSetting(MessageTransferAcceptPolicy.FROM_SPOLICY_S,
                Integer.toString(this.recipient.getAddress()));
        this.settings.restoreNameSpace();

        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertFalse(
                NOT_ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.msg));
    }

    @Test
    public void testAcceptSendingReturnsFalseForMessageWithRecipientOutsideRange() {
        this.settings.setNameSpace(POLICY_NS);
        this.settings.putSetting(MessageTransferAcceptPolicy.TO_SPOLICY_S,
                Integer.toString(MessageTransferAcceptPolicy.TO_ME_VALUE));
        this.settings.putSetting(MessageTransferAcceptPolicy.FROM_SPOLICY_S,
                Integer.toString(MessageTransferAcceptPolicy.TO_ME_VALUE));
        this.settings.restoreNameSpace();

        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertFalse(
                NOT_ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.msg));
    }

    @Test
    public void testAcceptSendingReturnsTrueForPartOfMulticastGroupInsideRange(){
        this.settings.setNameSpace(POLICY_NS);
        this.settings.putSetting(MessageTransferAcceptPolicy.TO_SPOLICY_S,
                Integer.toString(this.recipient.getAddress()));
        this.settings.putSetting(MessageTransferAcceptPolicy.FROM_SPOLICY_S,
                Integer.toString(MessageTransferAcceptPolicy.TO_ME_VALUE));
        this.settings.restoreNameSpace();

        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertTrue(
                ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.multicast));
    }

    @Test
    public void testAcceptSendingReturnsTrueForSenderOfMulticastInsideRange(){
        this.settings.setNameSpace(POLICY_NS);
        this.settings.putSetting(MessageTransferAcceptPolicy.FROM_SPOLICY_S,
                Integer.toString(MessageTransferAcceptPolicy.TO_ME_VALUE));
        this.settings.restoreNameSpace();

        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertTrue(
                ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.multicast));
    }

    @Test
    public void testAcceptSendingReturnsFalseForSenderOfMulticastOutsideRange(){
        this.settings.setNameSpace(POLICY_NS);
        this.settings.putSetting(MessageTransferAcceptPolicy.TO_SPOLICY_S,
                Integer.toString(this.recipient2.getAddress()));
        this.settings.putSetting(MessageTransferAcceptPolicy.FROM_SPOLICY_S,
                Integer.toString(this.recipient.getAddress()));
        this.settings.restoreNameSpace();

        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertFalse(
                NOT_ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.multicast));
    }

    @Test
    public void testAcceptSendingReturnsTrueForBroadcastWithSenderInsideRange() {
        this.settings.setNameSpace(POLICY_NS);
        this.settings.putSetting(MessageTransferAcceptPolicy.FROM_SPOLICY_S,
                Integer.toString(MessageTransferAcceptPolicy.TO_ME_VALUE));
        this.settings.restoreNameSpace();

        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertTrue(
                ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.broadcast));
    }

    @Test
    public void testAcceptSendingReturnsFalseForBroadcastWithSenderOutsideRange() {
        this.settings.setNameSpace(POLICY_NS);
        this.settings.putSetting(MessageTransferAcceptPolicy.FROM_SPOLICY_S,
                Integer.toString(this.recipient.getAddress()));
        this.settings.restoreNameSpace();

        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertFalse(
                NOT_ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.broadcast));
    }
}
