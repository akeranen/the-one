package test;

import core.BroadcastMessage;
import core.Coord;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.Group;
import core.Message;
import core.MulticastMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import routing.util.MessageTransferAcceptPolicy;
import util.Tuple;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Contains tests for the MessageTransferAcceptPolicy class.
 *
 * Created by Britta Heymann on 16.02.2017.
 */
public class MessageTransferAcceptPolicyTest {
    private static final String UNEXPECTED_ACCEPT_S = "Sending should not have been accepted.";
    private static final String EXPECTED_ACCEPT_S = "Sending should have been accepted.";

    /**
     * Use a invalid address value to test an empty TO_SPOLICY_S in multicasts. When this value is
     * left empty, it would put in the TO_ME_VALUE by default.
     */
    private static final int INVALID_VALUE = -2;

    private static final String POLICY_NS = "simplepolicy";

    private TestSettings settings;

    private Message msg;
    private Message broadcast;
    private Message multicast;
    private Message dataMessage;
    private DTNHost sender;
    private DTNHost recipient;
    private DTNHost recipient2;

    public MessageTransferAcceptPolicyTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    public void init() {
        DTNHost.reset();
        this.settings = new TestSettings();
        TestUtils utils = new TestUtils(
                new ArrayList<>(),
                new ArrayList<>(),
                this.settings);

        this.sender = utils.createHost();
        this.recipient = utils.createHost();
        this.recipient2 = utils.createHost();

        // Create all messages.
        this.msg = new Message(this.sender, recipient, "M", 0);
        this.broadcast = new BroadcastMessage(this.sender, "B", 0);
        Group g = Group.createGroup(0);
        g.addHost(sender);
        g.addHost(recipient);
        g.addHost(recipient2);
        this.multicast = new MulticastMessage(this.sender,g,"G",0);
        DisasterData data = new DisasterData(DisasterData.DataType.MARKER, 0, 0, new Coord(0, 0));
        this.dataMessage = new DataMessage(
                this.sender, this.recipient, "D", Collections.singleton(new Tuple<>(data, 1D)), 0);

        // Prepare settings for simple policy.
        this.settings.putSetting(MessageTransferAcceptPolicy.MTA_POLICY_NS, POLICY_NS);
        this.setAcceptableSenderAddress(this.sender.getAddress());
        this.setAcceptableRecipientAddress(this.recipient.getAddress());
    }

    @After
    public void tearDown() {
        Group.clearGroups();
    }

    @Test
    public void testAcceptSendingReturnsTrueForMessageAdheringToSimplePolicies() {
        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertTrue(
                EXPECTED_ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.msg));
    }

    @Test
    public void testAcceptSendingReturnsFalseForMessageWithSenderOutsideRange() {
        this.setAcceptableSenderAddress(this.recipient.getAddress());
        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertFalse(
                UNEXPECTED_ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.msg));
    }

    @Test
    public void testAcceptSendingReturnsFalseForMessageWithRecipientOutsideRange() {
        this.setAcceptableRecipientAddress(this.sender.getAddress());
        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertFalse(
                UNEXPECTED_ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.msg));
    }

    @Test
    public void testAcceptSendingReturnsTrueForPartOfMulticastGroupInsideRange(){
        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertTrue(
                EXPECTED_ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.multicast));
    }

    @Test
    public void testAcceptSendingReturnsFalseForAllOfMulticastGroupOutsideRange(){
        this.setAcceptableRecipientAddress(INVALID_VALUE);
        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertFalse(
                UNEXPECTED_ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.multicast));
    }

    @Test
    public void testAcceptSendingReturnsTrueForSenderOfMulticastInsideRange(){
        this.setAcceptableRecipientAddress(MessageTransferAcceptPolicy.TO_ME_VALUE);
        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertTrue(
                EXPECTED_ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.multicast));
    }

    @Test
    public void testAcceptSendingReturnsFalseForSenderOfMulticastOutsideRange(){
        this.setAcceptableSenderAddress(this.recipient.getAddress());
        this.setAcceptableRecipientAddress(this.recipient2.getAddress());
        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertFalse(
                UNEXPECTED_ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.multicast));
    }

    @Test
    public void testAcceptSendingReturnsTrueForBroadcastWithSenderInsideRange() {
        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertTrue(
                EXPECTED_ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.broadcast));
    }

    @Test
    public void testAcceptSendingReturnsFalseForBroadcastWithSenderOutsideRange() {
        this.setAcceptableSenderAddress(this.recipient.getAddress());
        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertFalse(
                UNEXPECTED_ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.broadcast));
    }

    @Test
    public void testAcceptSendingReturnsTrueForDataMessageAdheringToSimplePolicies() {
        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertTrue(
                EXPECTED_ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.dataMessage));
    }

    @Test
    public void testAcceptSendingReturnsFalseForDataMessageWithSenderOutsideRange() {
        this.setAcceptableSenderAddress(this.recipient.getAddress());
        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertFalse(
                UNEXPECTED_ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.dataMessage));
    }

    @Test
    public void testAcceptSendingReturnsFalseForDataMessageWithRecipientOutsideRange() {
        this.setAcceptableRecipientAddress(this.sender.getAddress());
        MessageTransferAcceptPolicy policy = new MessageTransferAcceptPolicy(this.settings);

        assertFalse(
                UNEXPECTED_ACCEPT_S,
                policy.acceptSending(this.sender, this.recipient, null, this.dataMessage));
    }

    /**
     * Sets a sender to the settings that should get accepted by {@link MessageTransferAcceptPolicy}.
     * @param address The sender's address.
     */
    private void setAcceptableSenderAddress(int address) {
        this.settings.setNameSpace(POLICY_NS);
        this.settings.putSetting(MessageTransferAcceptPolicy.FROM_SPOLICY_S, Integer.toString(address));
        this.settings.restoreNameSpace();
    }

    /**
     * Sets a recipient to the settings that should get accepted by {@link MessageTransferAcceptPolicy}.
     * @param address The recipient's address.
     */
    private void setAcceptableRecipientAddress(int address) {
        this.settings.setNameSpace(POLICY_NS);
        this.settings.putSetting(MessageTransferAcceptPolicy.TO_SPOLICY_S, Integer.toString(address));
        this.settings.restoreNameSpace();
    }
}
