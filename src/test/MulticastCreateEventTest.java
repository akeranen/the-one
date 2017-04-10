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
    public void testProcessEventCreatesMulticastMessageWithCorrectGroupAddress() {
        int groupAddress = 0;
        Message createdMessage = getMessage(groupAddress);
        assertEquals(
                "Multicast message should have been created with different group address.",
                groupAddress,
                ((MulticastMessage)createdMessage).getGroup().getAddress());
    }
    
    @Test
    public void testPriorities(){
        int groupAddress = 0;
        Message createdMessage = getMessage(groupAddress);
        assertEquals(((MulticastMessage)createdMessage).getPriority(), -1);
        createdMessage = getMessageWithPriority(groupAddress, PRIORITY);
        assertEquals(((MulticastMessage)createdMessage).getPriority(), PRIORITY);
    }

    @Test
    public void testToString() {
        MulticastCreateEvent event = new MulticastCreateEvent(
                this.creator.getAddress(),0, "messageId", 100, 34.2);
        assertEquals(
                "ToString should have printed a different value",
                "MSG @34.2 messageId [" + this.creator.getAddress() + "->"
                        +Group.getGroup(0)+"] size:100 CREATE",
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
    
    private Message getMessage(int groupAddress){
        return getMessageWithPriority(groupAddress, Message.INVALID_PRIORITY);
    }
    
    private Message getMessageWithPriority(int groupAddress, int prio){
        MulticastCreateEvent event = new MulticastCreateEvent(
                this.creator.getAddress(),groupAddress, "messageId", 100, 23, prio);
        event.processEvent(this.world);
        this.messageChecker.next();
        return this.messageChecker.getLastMsg();
    }
}
