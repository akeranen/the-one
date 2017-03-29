package test;

import core.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import routing.EpidemicRouter;
import routing.PassiveRouter;
import routing.util.MessageTransferAcceptPolicy;

import test.TestUtils;
import test.TestSettings;

import java.util.ArrayList;

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
    private DTNHost sender;
    private DTNHost recipient;

    @Before
    public void init() {
        this.settings = new TestSettings();
        this.utils = new TestUtils(
                new ArrayList<ConnectionListener>(),
                new ArrayList<MessageListener>(),
                this.settings);

        this.sender = this.utils.createHost();
        this.recipient = this.utils.createHost();

        this.msg = new Message(this.sender, recipient, "M", 100);
        this.broadcast = new BroadcastMessage(this.sender, "B", 50);

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
                "Sending should have been accepted.",
                policy.acceptSending(this.sender, this.recipient, null, this.msg));
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
                "Sending should not have been accepted.",
                policy.acceptSending(this.sender, this.recipient, null, this.msg));
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
                "Sending should not have been accepted.",
                policy.acceptSending(this.sender, this.recipient, null, this.msg));
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
