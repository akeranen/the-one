package test;

import core.DTNHost;
import core.Message;
import input.ExternalEvent;
import input.MessageCreateEvent;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Contains tests for MessageCreateEvent class
 */
public class MessageCreateEventTest extends AbstractMessageCreateEventTest {
    
    @Test
    public void testPriorities(){
        ExternalEvent event = getInstanceOfMessageEvent(this.creator,"messageId",100,23);
        event.processEvent(this.world);
        this.messageChecker.next();
        Message createdMessage = this.messageChecker.getLastMsg();
        assertEquals(createdMessage.getPriority(), 0);
    }

    @Override
    protected boolean isInstanceOfDesiredMessage(Object o) {
        return (o instanceof Message);
    }

    @Override
    protected ExternalEvent getInstanceOfMessageEvent(DTNHost creator, String messageID, int size, double time) {
        return new MessageCreateEvent(creator.getAddress(),0,messageID,size,50,time);
    }
}
