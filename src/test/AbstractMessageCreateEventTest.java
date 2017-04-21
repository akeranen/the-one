package test;

import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.UpdateListener;
import core.World;
import input.EventQueue;
import input.ExternalEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Abstract test class providing the necessary setup to test MessageCreateEvents
 *
 * Created by Marius Meyer on 17.03.17.
 */
public abstract class AbstractMessageCreateEventTest {
    public static final int PRIORITY = 5;
    protected TestUtils utils;
    protected MessageChecker messageChecker = new MessageChecker();

    protected World world;
    protected DTNHost creator;

    @Before
    public void setUp() throws Exception {
        List<MessageListener> messageListeners = new ArrayList<>(1);
        messageListeners.add(this.messageChecker);

        this.utils = new TestUtils(new ArrayList<ConnectionListener>(), messageListeners, new TestSettings());

        this.resetDtnHostAddressGenerator();
        this.creator = this.utils.createHost();

        List<DTNHost> hosts = new ArrayList<>(1);
        hosts.add(this.creator);

        this.world = new World(
                hosts,
                100,
                50,
                1,
                new ArrayList<UpdateListener>(),
                false,
                new ArrayList<EventQueue>());
    }

    @Test
    public void testProcessEventCreatesDesiredMessage() {
        ExternalEvent event = getInstanceOfMessageEvent(creator,"message",100,0);
        event.processEvent(this.world);
        assertTrue(this.messageChecker.next() && isInstanceOfDesiredMessage(this.messageChecker.getLastMsg()));
    }

    @Test
    public void testProcessEventCreatesMessageWithCorrectProperties() {
        String messageId = "messageId";
        int size = 100;

        ExternalEvent event = getInstanceOfMessageEvent(creator,messageId,size,0);
        event.processEvent(this.world);
        this.messageChecker.next();

        Message createdMessage = this.messageChecker.getLastMsg();
        assertEquals(
                "Message should have been created with different host.",
                this.creator,
                createdMessage.getFrom());
        assertEquals(
                "Message should have been created with different ID.",
                messageId,
                createdMessage.getId());
        assertEquals(
                "Message should have been created with different size.",
                size,
                createdMessage.getSize());
    }

    @Test
    public void testGetTimeReturnsTimeProvidedOnConstruction() {
        double time = 34.2;
        ExternalEvent event = getInstanceOfMessageEvent(creator,"messageID",100,time);
        assertEquals("Time should have been set differently.", time, event.getTime(), 0.000001);
    }

    private void resetDtnHostAddressGenerator() {
        DTNHost.reset();
    }

    /**
     * Checks, if the given object is an instance of the desired message
     * @param o the object to check
     * @return true, if the object is an instance of the message
     */
    protected abstract boolean isInstanceOfDesiredMessage(Object o);

    /**
     * Returns an instance of the message event, that should be tested
     *
     * @param creator The host, that should create the message
     * @param messageID The id of the created message
     * @param size the size of the created message
     * @return the message event as an external event
     */
    protected abstract ExternalEvent getInstanceOfMessageEvent(DTNHost creator, String messageID,int size, double time);
}
