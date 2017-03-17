package test;

import core.DTNHost;
import core.Group;
import core.Message;
import core.MulticastMessage;
import input.ExternalEvent;
import input.MulticastCreateEvent;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *Contains tests for MulticastCreateEvent class
 *
 * Created by Marius Meyer on 10.03.17.
 */
public class MulticastCreateEventTest extends AbstractMessageCreateEventTest {

    @Before
    public void createGroup(){
        Group.clearGroups();
        Group g = Group.createGroup(0);
        g.addHost(creator);
    }

    @Test
    public void testProcessEventCreatesMulticastMessageWithCorrectProperties() {
        String messageId = "messageId";
        int groupAddress = 0;
        int size = 100;

        MulticastCreateEvent event = new MulticastCreateEvent(
                this.creator.getAddress(),groupAddress, messageId, size, 23);
        event.processEvent(this.world);
        this.messageChecker.next();

        Message createdMessage = this.messageChecker.getLastMsg();
        assertEquals(
                "Multicast message should have been created with different group address.",
                groupAddress,
                ((MulticastMessage)createdMessage).getGroup().getAddress());
    }

    @Test
    public void testToString() {
        MulticastCreateEvent event = new MulticastCreateEvent(
                this.creator.getAddress(),0, "messageId", 100, 34.2);
        assertEquals(
                "ToString should have printed a different value",
                "MSG @34.2 messageId [" + this.creator.getAddress() + "->Group 0] size:100 CREATE",
                event.toString());
    }

    @Override
    protected boolean isInstanceOfDesiredMessage(Object o) {
        return (o instanceof MulticastMessage);
    }

    @Override
    protected ExternalEvent getInstanceOfMessageEvent(DTNHost creator, String messageID, int size, double time) {
        return new MulticastCreateEvent(creator.getAddress(),0,messageID,size,time);
    }
}
