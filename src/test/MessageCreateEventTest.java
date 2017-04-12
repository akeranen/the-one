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
        Message createdMessage = getMessage();
        assertEquals((createdMessage).getPriority(), -1);
        createdMessage = getMessageWithPriority(PRIORITY);
        assertEquals((createdMessage).getPriority(), PRIORITY);
    }

    @Override
    protected boolean isInstanceOfDesiredMessage(Object o) {
        return (o instanceof Message);
    }

    @Override
    protected ExternalEvent getInstanceOfMessageEvent(DTNHost creator, String messageID, int size, double time) {
        return new MessageCreateEvent(creator.getAddress(),0,messageID,size,50,time);
    }
    
    private Message getMessage(){
        return getMessageWithPriority(Message.INVALID_PRIORITY);
    }
    
    private Message getMessageWithPriority(int prio){
        MessageCreateEvent event = new MessageCreateEvent(this.creator.getAddress(),0,"messageId",100,50,30,prio);
        event.processEvent(this.world);
        this.messageChecker.next();
        return messageChecker.getLastMsg();
    }
}
