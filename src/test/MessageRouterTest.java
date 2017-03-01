package test;

import core.*;
import org.junit.Before;
import org.junit.Test;
import routing.MessageRouter;
import routing.PassiveRouter;

import routing.util.RoutingInfo;

import test.TestUtils;
import test.TestSettings;

import java.util.ArrayList;


import static org.junit.Assert.*;

/**
 * Contains tests for the message router class.
 *
 * Created by Britta Heymann on 15.02.2017.
 */
public class MessageRouterTest {
    private TestUtils utils;

    private Message msg;
    private Message broadcast;
    private DTNHost recipient;

    @Before
    public void setUp() {
        this.utils = new TestUtils(
                new ArrayList<ConnectionListener>(),
                new ArrayList<MessageListener>(),
                new TestSettings());
        // Use passive router as that is nearest to the original MessageRouter class
        this.utils.setMessageRouterProto(new PassiveRouter(new TestSettings()));
        this.recipient = this.utils.createHost();

        this.msg = new Message(this.utils.createHost(), recipient, "M", 100);
        this.broadcast = new BroadcastMessage(this.utils.createHost(), "B", 50);
    }

    @Test
    public void testMessageTransferredForNonRecipientPutsMessageIntoBufferButNotIntoDelivered() {
        DTNHost nonRecipient = this.utils.createHost();
        MessageRouter nonRecipientRouter = nonRecipient.getRouter();

        this.checkInitialBufferIsEmpty(nonRecipientRouter);
        this.checkNoMessagesHaveBeenDeliveredAfterInitialization(nonRecipientRouter);
        nonRecipient.receiveMessage(this.msg, this.msg.getFrom());
        nonRecipient.messageTransferred(this.msg.getId(), this.msg.getFrom());

        assertEquals(
                "Message should have been put into buffer.", 1, nonRecipientRouter.getNrofMessages());
        assertEquals(
                "Message should not have been added to delivered messages",
                0,
                this.getNrOfDeliveredMessages(nonRecipientRouter));
    }

    @Test
    public void testMessageTransferredForNonRecipientDoesNotPutMessageIntoBufferIfAppDropsIt() {
        DTNHost nonRecipient = this.utils.createHost();
        MessageRouter nonRecipientRouter = nonRecipient.getRouter();
        nonRecipientRouter.addApplication(new DroppingApplication());

        this.checkInitialBufferIsEmpty(nonRecipientRouter);
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

        this.checkInitialBufferIsEmpty(recipientRouter);
        this.checkNoMessagesHaveBeenDeliveredAfterInitialization(recipientRouter);
        this.recipient.receiveMessage(this.msg, this.msg.getFrom());
        this.recipient.messageTransferred(this.msg.getId(), this.msg.getFrom());

        assertEquals(
                "Message should have been put into delivered messages.",
                1,
                this.getNrOfDeliveredMessages(recipientRouter));
        assertEquals(
                "Message should not have been put into buffer.",
                0,
                recipientRouter.getNrofMessages());
    }

    @Test
    public void testMessageTransferredForSingleRecipientDoesNotPutMessageIntoDeliveredForSecondTime() {
        MessageRouter recipientRouter = this.recipient.getRouter();

        this.checkNoMessagesHaveBeenDeliveredAfterInitialization(recipientRouter);
        this.recipient.receiveMessage(this.msg, this.msg.getFrom());
        this.recipient.messageTransferred(this.msg.getId(), this.msg.getFrom());
        this.recipient.receiveMessage(this.msg, this.msg.getFrom());
        this.recipient.messageTransferred(this.msg.getId(), this.msg.getFrom());

        assertEquals(
                "Message should not have been put into delivered messages twice.",
                1,
                this.getNrOfDeliveredMessages(recipientRouter));
    }

    @Test
    public void testMessageTransferredForBroadcastMessagePutsMessageIntoDeliveredAndIntoBuffer() {
        DTNHost host = this.utils.createHost();
        MessageRouter router = host.getRouter();

        this.checkInitialBufferIsEmpty(router);
        this.checkNoMessagesHaveBeenDeliveredAfterInitialization(router);
        host.receiveMessage(this.broadcast, this.broadcast.getFrom());
        host.messageTransferred(this.broadcast.getId(), this.broadcast.getFrom());

        assertEquals(
                "Broadcast should have been put into buffer.", 1, router.getNrofMessages());
        assertEquals(
                "Broadcast should have been added to delivered messages",
                1,
                this.getNrOfDeliveredMessages(router));
    }

    @Test
    public void testMessageTransferredForBroadcastMessagesDoesNotPutMessageIntoDeliveredForSecondTime() {
        DTNHost host = this.utils.createHost();
        MessageRouter router = host.getRouter();

        this.checkNoMessagesHaveBeenDeliveredAfterInitialization(router);
        host.receiveMessage(this.broadcast, this.broadcast.getFrom());
        host.messageTransferred(this.broadcast.getId(), this.broadcast.getFrom());
        host.receiveMessage(this.broadcast, this.broadcast.getFrom());
        host.messageTransferred(this.broadcast.getId(), this.broadcast.getFrom());

        assertEquals(
                "Broadcast should not have been added to delivered messages twice.",
                1,
                this.getNrOfDeliveredMessages(router));
    }

    @Test
    public void testMessageTransferredForBroadcastMessageDoesNotPutMessageIntoBufferIfAppDropsIt() {
        DTNHost host = this.utils.createHost();
        MessageRouter router = host.getRouter();
        router.addApplication(new DroppingApplication());

        this.checkInitialBufferIsEmpty(router);
        router.receiveMessage(this.broadcast, this.broadcast.getFrom());
        router.messageTransferred(this.broadcast.getId(), this.broadcast.getFrom());

        assertEquals(
                "Broadcast should not have been put into buffer.",
                0,
                router.getNrofMessages());
    }

    /**
     * Asserts that the router's buffer is empty.
     * @param router The MessageRouter to look at.
     */
    private void checkInitialBufferIsEmpty(MessageRouter router) {
        assertEquals(
                "Initial number of messages should have been different.",
                0,
                router.getNrofMessages());
    }

    /**
     * Asserts that the number of delivered messages to the router is 0.
     * @param router The MessageRouter to look at.
     */
    private void checkNoMessagesHaveBeenDeliveredAfterInitialization(MessageRouter router) {
        assertEquals(
                "Initial number of delivered messages should have been different.",
                0,
                this.getNrOfDeliveredMessages(router));
    }

    /**
     * Determines the number of delivered messages to the router.
     * @param router The router to look at.
     * @return The number of delivered messages to the router.
     */
    private int getNrOfDeliveredMessages(MessageRouter router) {
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
    private class DroppingApplication extends Application {
        @Override
        public Message handle(Message msg, DTNHost host) {
            return null;
        }

        @Override
        public void update(DTNHost host) {

        }

        @Override
        public Application replicate() {
            return null;
        }
    }
}
