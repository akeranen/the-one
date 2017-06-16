/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import java.util.ArrayList;

import core.*;
import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import test.TestUtils;
import test.TestSettings;

import static org.junit.Assert.assertEquals;

public class MessageTest extends TestCase {

    private TestUtils utils;

    private Message msg, msgPrio;
    private DTNHost from;
    private DTNHost to;
    private SimClock sc;
    private int priority = 5;

    @Before
    public void setUp() throws Exception {
        sc = SimClock.getInstance();
        sc.setTime(10);

        this.utils = new TestUtils(new ArrayList<ConnectionListener>(), new ArrayList<MessageListener>(),
                new TestSettings());
        this.to = this.utils.createHost();
        this.from = this.utils.createHost();

        msg = new Message(from, to, "M", 100);
        msgPrio = new Message(from, to, "N", 100, priority);
        msg.setTtl(10);
    }

    @Test
    public void testGetTtl() {
        assertEquals(10, msg.getTtl());

        sc.advance(50);
        assertEquals(9, msg.getTtl());

        sc.advance(120);
        assertEquals(7, msg.getTtl());

        sc.advance(180);
        assertEquals(4, msg.getTtl());

        sc.advance(240);
        assertEquals(0, msg.getTtl());

    }

    @Test
    public void testAddProperty() {
        String value1 = "value1";
        String value2 = "value2";
        msg.addProperty("foo", value1);
        msg.addProperty("bar", value2);

        assertEquals(value1, msg.getProperty("foo"));
        assertEquals(value2, msg.getProperty("bar"));
    }

    @Test
    public void testGetTo() {
        assertEquals(this.to, this.msg.getTo());
    }

    @Test
    public void testIsFinalRecipientReturnsTrueForSingleRecipient() {
        assertTrue(this.msg.isFinalRecipient(this.to));
    }

    @Test
    public void testIsFinalRecipientReturnsFalseForHostDifferentFromRecipient() {
        DTNHost otherHost = this.utils.createHost();
        assertFalse(this.msg.isFinalRecipient(otherHost));
    }

    @Test
    public void testCompletesDeliveryReturnsTrueForSingleRecipient() {
        assertTrue(this.msg.completesDelivery(this.to));
    }

    @Test
    public void testCompletesDeliveryReturnsFalseForHostDifferentFromRecipient() {
        DTNHost otherHost = this.utils.createHost();
        assertFalse(this.msg.completesDelivery(otherHost));
    }

    @Test
    public void testGetTypeReturnsOneToOne() {
        assertEquals(Message.MessageType.ONE_TO_ONE, this.msg.getType());
    }

    @Test
    public void testRecipientsToString() {
        assertEquals("Recipient description should have been different.", this.to.toString(),
                this.msg.recipientsToString());
    }

    @Test
    public void testPriority() {
        assertEquals(msg.getPriority(), Message.INVALID_PRIORITY);
        assertEquals(msgPrio.getPriority(), priority);
    }

    /**
     * Tests whether the default value for {@link Message#storeFullMsgPath} is true and
     * the message path includes nodes
     */
    @Test
    public void testDefaultBehaviorIsStoringFullMessagePath(){
        Message.setStoreFullMsgPath(new TestSettings());
        msg = new Message(from, to, "M", 100);
        from.createNewMessage(msg);
        assertEquals("We should store the full path of the message.", 1, msg.getHops().size());
        assertEquals("The message path should include the creator as first node on the path.",
                msg.getFrom(), msg.getHops().get(0));
        from.connect(to);
        from.sendMessage(msg.getId(),to);
        to.messageTransferred(msg.getId(), from);
        //Get message copy host "to" received because only that has the longer path
        for (Message msgTo : to.getRouter().getMessageCollection() ){
            if (msgTo.getId().equals(msg.getId())){
                assertEquals("The message path should include creator and receiver.", 2, msgTo.getHops().size());
            }
        }
    }

    /**
     * Tests whether the setting {@link Message#storeFullMsgPath} can
     * be set to false (true is default)
     */
    @Test
    public void testSetStoreFullMsgPath(){
        TestSettings ts = new TestSettings();
        ts.setNameSpace(World.OPTIMIZATION_SETTINGS_NS);
        ts.putSetting(Message.MSG_PATH_S, "false");
        Message.setStoreFullMsgPath(ts);
        msg = new Message(from, to, "M", 100);
        from.createNewMessage(msg);
        assertTrue("The message path should be empty", msg.getHops().isEmpty());
    }

    @Test
    public void testGetHopCountWithMessagePath(){
        DTNHost relayHost = this.utils.createHost();
        msg = new Message(from, to, "M", 100);
        from.createNewMessage(msg);
        from.connect(relayHost);
        from.sendMessage(msg.getId(),relayHost);
        relayHost.messageTransferred(msg.getId(), from);
        //Get message copy host "relayHost" received because only that has the longer path
        for (Message msgRelay : relayHost.getRouter().getMessageCollection() ){
            if (msgRelay.getId().equals(msg.getId())){
                assertEquals("The message has made one hop.", 1, msgRelay.getHopCount());
            }
        }
        relayHost.connect(to);
        relayHost.sendMessage(msg.getId(),to);
        to.messageTransferred(msg.getId(), relayHost);
        //Get message copy host "to" received because only that has the longer path
        for (Message msgTo : to.getRouter().getMessageCollection() ){
            if (msgTo.getId().equals(msg.getId())){
                assertEquals("The message has traversed two nodes.", 2, msgTo.getHopCount());
            }
        }
    }

    @Test
    public void testGetHopCountWithoutMessagePath(){
        //Turn off storing the message path
        TestSettings ts = new TestSettings();
        ts.setNameSpace(World.OPTIMIZATION_SETTINGS_NS);
        ts.putSetting(Message.MSG_PATH_S, "false");
        Message.setStoreFullMsgPath(ts);
        //Now transfer messages check whether hop counts are correct
        DTNHost relayHost = this.utils.createHost();
        msg = new Message(from, to, "M", 100);
        from.createNewMessage(msg);
        from.connect(relayHost);
        from.sendMessage(msg.getId(),relayHost);
        relayHost.messageTransferred(msg.getId(), from);
        //Get message copy host "relayHost" received because only that has the longer path
        for (Message msgRelay : relayHost.getRouter().getMessageCollection() ){
            if (msgRelay.getId().equals(msg.getId())){
                assertEquals("The message has made one hop.", 1, msgRelay.getHopCount());
            }
        }
        relayHost.connect(to);
        relayHost.sendMessage(msg.getId(),to);
        to.messageTransferred(msg.getId(), relayHost);
        //Get message copy host "to" received because only that has the longer path
        for (Message msgTo : to.getRouter().getMessageCollection() ){
            if (msgTo.getId().equals(msg.getId())){
                assertEquals("The message has traversed two nodes.", 2, msgTo.getHopCount());
            }
        }
    }
}
