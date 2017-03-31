package test;

import core.Application;
import core.BroadcastMessage;
import core.ConnectionListener;
import core.DTNHost;
import core.Group;
import core.Message;
import core.MessageListener;
import core.MulticastMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import routing.MessageRouter;
import routing.PassiveRouter;
import routing.util.RoutingInfo;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * Contains tests for the message router class.
 *
 * Created by Britta Heymann on 15.02.2017.
 */
public class MessageRouterTest {

    private static final int DEFAULT_MESSAGE_SIZE = 100;

    private TestUtils utils;

    private Message msg;
    private Message broadcast;
    private Message multicast;
    private DTNHost recipient;
    private DTNHost sender;

    public MessageRouterTest(){
        //set up is done in methods annotated with @Before
    }

    @Before
    public void setUp() {
        Group.clearGroups();
        this.utils = new TestUtils(
                new ArrayList<ConnectionListener>(),
                new ArrayList<MessageListener>(),
                new TestSettings());
        // Use passive router as that is nearest to the original MessageRouter class
        this.utils.setMessageRouterProto(new PassiveRouter(new TestSettings()));
        this.recipient = this.utils.createHost();
        this.sender = this.utils.createHost();
        Group g = Group.createGroup(0);
        g.addHost(sender);

        this.msg = new Message(sender, recipient, "M", DEFAULT_MESSAGE_SIZE);
        this.broadcast = new BroadcastMessage(sender, "B", DEFAULT_MESSAGE_SIZE);
        this.multicast = new MulticastMessage(sender, g,"G",DEFAULT_MESSAGE_SIZE);
    }

    @Test
    public void testMessageTransferredForNonRecipientPutsMessageIntoBufferButNotIntoDelivered() {
        DTNHost nonRecipient = this.utils.createHost();
        MessageRouter nonRecipientRouter = nonRecipient.getRouter();

        checkInitialBufferIsEmpty(nonRecipientRouter);
        checkNoMessagesHaveBeenDeliveredAfterInitialization(nonRecipientRouter);
        nonRecipient.receiveMessage(this.msg, this.msg.getFrom());
        nonRecipient.messageTransferred(this.msg.getId(), this.msg.getFrom());

        assertEquals(
                "Message should have been put into buffer.", 1, nonRecipientRouter.getNrofMessages());
        assertEquals(
                "Message should not have been added to delivered messages",
                0,
                getNrOfDeliveredMessages(nonRecipientRouter));
    }

    @Test
    public void testMessageTransferredForNonRecipientDoesNotPutMessageIntoBufferIfAppDropsIt() {
        DTNHost nonRecipient = this.utils.createHost();
        MessageRouter nonRecipientRouter = nonRecipient.getRouter();
        nonRecipientRouter.addApplication(new DroppingApplication());

        checkInitialBufferIsEmpty(nonRecipientRouter);
        nonRecipient.receiveMessage(this.msg, this.msg.getFrom());
        nonRecipient.messageTransferred(this.msg.getId(), this.msg.getFrom());

        assertEquals(
                "Message should not have been put into buffer.",
                0,
                nonRecipientRouter.getNrofMessages());
    }

    @Test
    public void testMessageTransferredForSingleRecipientPutsMessageIntoDeliveredButNotIntoBuffer() {
        MessageRouter recipientRouter = this.recipient.getRouter();

        checkInitialBufferIsEmpty(recipientRouter);
        checkNoMessagesHaveBeenDeliveredAfterInitialization(recipientRouter);
        this.recipient.receiveMessage(this.msg, this.msg.getFrom());
        this.recipient.messageTransferred(this.msg.getId(), this.msg.getFrom());

        assertEquals(
                "Message should have been put into delivered messages.",
                1,
                getNrOfDeliveredMessages(recipientRouter));
        assertEquals(
                "Message should not have been put into buffer.",
                0,
                recipientRouter.getNrofMessages());
    }

    @Test
    public void testMessageTransferredForSingleRecipientDoesNotPutMessageIntoDeliveredForSecondTime() {
        MessageRouter recipientRouter = this.recipient.getRouter();

        checkNoMessagesHaveBeenDeliveredAfterInitialization(recipientRouter);
        this.recipient.receiveMessage(this.msg, this.msg.getFrom());
        this.recipient.messageTransferred(this.msg.getId(), this.msg.getFrom());
        this.recipient.receiveMessage(this.msg, this.msg.getFrom());
        this.recipient.messageTransferred(this.msg.getId(), this.msg.getFrom());

        assertEquals(
                "Message should not have been put into delivered messages twice.",
                1,
                getNrOfDeliveredMessages(recipientRouter));
    }

    @Test
    public void testMessageTransferredForBroadcastMessagePutsMessageIntoDeliveredAndIntoBuffer() {
        DTNHost host = this.utils.createHost();
        MessageRouter router = host.getRouter();

        checkInitialBufferIsEmpty(router);
        checkNoMessagesHaveBeenDeliveredAfterInitialization(router);
        host.receiveMessage(this.broadcast, this.broadcast.getFrom());
        host.messageTransferred(this.broadcast.getId(), this.broadcast.getFrom());

        assertEquals(
                "Broadcast should have been put into buffer.", 1, router.getNrofMessages());
        assertEquals(
                "Broadcast should have been added to delivered messages",
                1,
                getNrOfDeliveredMessages(router));
    }

    @Test
    public void testMessageTransferredForBroadcastMessagesDoesNotPutMessageIntoDeliveredForSecondTime() {
        DTNHost host = this.utils.createHost();
        MessageRouter router = host.getRouter();

        checkNoMessagesHaveBeenDeliveredAfterInitialization(router);
        host.receiveMessage(this.broadcast, this.broadcast.getFrom());
        host.messageTransferred(this.broadcast.getId(), this.broadcast.getFrom());
        host.receiveMessage(this.broadcast, this.broadcast.getFrom());
        host.messageTransferred(this.broadcast.getId(), this.broadcast.getFrom());

        assertEquals(
                "Broadcast should not have been added to delivered messages twice.",
                1,
                getNrOfDeliveredMessages(router));
    }

    @Test
    public void testMessageTransferredForBroadcastMessageDoesNotPutMessageIntoBufferIfAppDropsIt() {
        DTNHost host = this.utils.createHost();
        MessageRouter router = host.getRouter();
        router.addApplication(new DroppingApplication());

        checkInitialBufferIsEmpty(router);
        router.receiveMessage(this.broadcast, this.broadcast.getFrom());
        router.messageTransferred(this.broadcast.getId(), this.broadcast.getFrom());

        assertEquals(
                "Broadcast should not have been put into buffer.",
                0,
                router.getNrofMessages());
    }

    @Test
    public void testSenderOfBroadcastRecognizesNoFirstDeliveryForOwnMessage(){
        MessageRouter router = sender.getRouter();
        sender.createNewMessage(this.broadcast);
        Assert.assertTrue("Broadcast should be set as delivered message after creation",
                router.isDeliveredMessage(this.broadcast));

    }

    @Test
    public void testSenderOfMulticastRecognizesNoFirstDeliveryForOwnMessage(){
        MessageRouter router = sender.getRouter();
        sender.createNewMessage(this.multicast);
        Assert.assertTrue("Multicast should be set as delivered message after creation",
                router.isDeliveredMessage(this.multicast));
    }

    @Test
    public void testSenderOfUnicastRecognizesFirstDeliveryForOwnMessage(){
        MessageRouter router = sender.getRouter();
        Message senderToSenderMsg = new Message(sender,sender,"S to S",DEFAULT_MESSAGE_SIZE);
        sender.createNewMessage(senderToSenderMsg);
        Assert.assertFalse("Unicast should not be set as delivered message after creation",
                router.isDeliveredMessage(this.multicast));
    }

    /**
     * Asserts that the router's buffer is empty.
     * @param router The MessageRouter to look at.
     */
    private static void checkInitialBufferIsEmpty(MessageRouter router) {
        assertEquals(
                "Initial number of messages should have been different.",
                0,
                router.getNrofMessages());
    }

    /**
     * Asserts that the number of delivered messages to the router is 0.
     * @param router The MessageRouter to look at.
     */
    private static void checkNoMessagesHaveBeenDeliveredAfterInitialization(MessageRouter router) {
        assertEquals(
                "Initial number of delivered messages should have been different.",
                0,
                getNrOfDeliveredMessages(router));
    }

    /**
     * Determines the number of delivered messages to the router.
     * @param router The router to look at.
     * @return The number of delivered messages to the router.
     */
    private static int getNrOfDeliveredMessages(MessageRouter router) {
        RoutingInfo info = router.getRoutingInfo();
        for(RoutingInfo additionalInfo : info.getMoreInfo()) {
            if(additionalInfo.toString().contains("delivered message(s)")) {
                String number = additionalInfo.toString().split(" ")[0];
                return Integer.parseInt(number);
            }
        }
        throw new UnsupportedOperationException("No information about delivered messages could be found.");
    }

    /**
     * An application that simply drops every message.
     */
    private static class DroppingApplication extends Application {
        @Override
        public Message handle(Message msg, DTNHost host) {
            return null;
        }

        @Override
        public void update(DTNHost host) {
            //just a dummy application, so no functionality needed here
        }

        @Override
        public Application replicate() {
            return null;
        }
    }
}
