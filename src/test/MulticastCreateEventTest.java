package test;

import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.MulticastMessage;
import core.UpdateListener;
import core.World;
import input.EventQueue;
import input.MulticastCreateEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *Contains tests for MulticastCreateEvent class
 *
 * Created by Marius Meyer on 10.03.17.
 */
public class MulticastCreateEventTest {
    private TestUtils utils;
    private MessageChecker messageChecker;

    private World world;
    private DTNHost creator;

    @Before
    public void setUp() throws Exception {
        this.messageChecker = new MessageChecker();

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

    private void resetDtnHostAddressGenerator() {
        DTNHost.reset();
    }


    @Test
    public void testProcessEventCreatesMulticastMessage() {
        MulticastCreateEvent event = new MulticastCreateEvent(
                this.creator.getAddress(),0, "messageId", 100, 50, 23);
        event.processEvent(this.world);
        assertTrue(this.messageChecker.next());
    }

    @Test
    public void testProcessEventCreatesMulticastMessageWithCorrectProperties() {
        String messageId = "messageId";
        int groupAddress = 1;
        int size = 100;
        int responseSize = 50;

        MulticastCreateEvent event = new MulticastCreateEvent(
                this.creator.getAddress(),groupAddress, messageId, size, responseSize, 23);
        event.processEvent(this.world);
        this.messageChecker.next();

        Message createdMessage = this.messageChecker.getLastMsg();
        assertEquals(
                "Multicast message should have been created with different host.",
                this.creator,
                createdMessage.getFrom());
        assertEquals(
                "Multicast message should have been created with different ID.",
                messageId,
                createdMessage.getId());
        assertEquals(
                "Multicast message should have been created with different size.",
                size,
                createdMessage.getSize());
        assertEquals(
                "Multicast message should have been created with different response size.",
                responseSize,
                createdMessage.getResponseSize());
        assertEquals(
                "Multicast message should have been created with different group address.",
                groupAddress,
                ((MulticastMessage)createdMessage).getGroup().getAddress());
    }

    @Test
    public void testGetTimeReturnsTimeProvidedOnConstruction() {
        double time = 34.2;
        MulticastCreateEvent event = new MulticastCreateEvent(
                this.creator.getAddress(),0, "messageId", 100, 50, time);
        assertEquals("Time should have been set differently.", time, event.getTime(), 0.000001);
    }

    @Test
    public void testToString() {
        MulticastCreateEvent event = new MulticastCreateEvent(
                this.creator.getAddress(),0, "messageId", 100, 50, 34.2);
        assertEquals(
                "ToString should have printed a different value",
                "MSG @34.2 messageId [" + this.creator.getAddress() + "->Group 0] size:100 CREATE",
                event.toString());
    }
}
