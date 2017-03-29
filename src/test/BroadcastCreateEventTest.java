package test;

import core.*;

import input.BroadcastCreateEvent;
import input.EventQueue;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import test.MessageChecker;
import test.TestUtils;
import test.TestSettings;

import static org.junit.Assert.*;

/**
 * Contains tests for the BroadcastCreateEvent class.
 *
 * Created by Britta Heymann on 15.02.2017.
 */
public class BroadcastCreateEventTest {
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
    public void testProcessEventCreatesBroadcastMessage() {
        BroadcastCreateEvent event = new BroadcastCreateEvent(
                this.creator.getAddress(), "messageId", 100, 50, 23);
        event.processEvent(this.world);
        assertTrue(this.messageChecker.next());
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
                "Broadcast message should have been created with different host.",
                this.creator,
                createdMessage.getFrom());
        assertEquals(
                "Broadcast message should have been created with different ID.",
                messageId,
                createdMessage.getId());
        assertEquals(
                "Broadcast message should have been created with different size.",
                size,
                createdMessage.getSize());
        assertEquals(
                "Broadcast message should have been created with different response size.",
                responseSize,
                createdMessage.getResponseSize());
    }

    @Test
    public void testGetTimeReturnsTimeProvidedOnConstruction() {
        double time = 34.2;
        BroadcastCreateEvent event = new BroadcastCreateEvent(
                this.creator.getAddress(), "messageId", 100, 50, time);
        assertEquals("Time should have been set differently.", time, event.getTime(), 0.000001);
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
