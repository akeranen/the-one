package test;

import core.BroadcastMessage;
import core.DTNHost;
import core.Message;
import input.BroadcastCreateEvent;
import input.ExternalEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Contains tests for the BroadcastCreateEvent class.
 *
 * Created by Britta Heymann on 15.02.2017.
 */
public class BroadcastCreateEventTest extends AbstractMessageCreateEventTest {

    @Override
    protected boolean isInstanceOfDesiredMessage(Object o) {
        return (o instanceof BroadcastMessage);
    }

    @Override
    protected ExternalEvent getInstanceOfMessageEvent(DTNHost creator, String messageID, int size, double time) {
        return new BroadcastCreateEvent(creator.getAddress(),messageID,size,50,time);
    }

    @Test
    public void testProcessEventCreatesBroadcastMessageWithCorrectProperties() {
        String messageId = "messageId";
        int size = 100;
        int responseSize = 50;

        BroadcastCreateEvent event = new BroadcastCreateEvent(
                this.creator.getAddress(), messageId, size, responseSize, 23);
        event.processEvent(this.world);
        this.messageChecker.next();

        Message createdMessage = this.messageChecker.getLastMsg();
        assertEquals(
                "Broadcast message should have been created with different response size.",
                responseSize,
                createdMessage.getResponseSize());
    }

    @Test
    public void testToString() {
        BroadcastCreateEvent event = new BroadcastCreateEvent(
                this.creator.getAddress(), "messageId", 100, 50, 34.2);
        assertEquals(
                "ToString should have printed a different value",
                "MSG @34.2 messageId [" + this.creator.getAddress() + "->everyone] size:100 CREATE",
                event.toString());
    }
}
