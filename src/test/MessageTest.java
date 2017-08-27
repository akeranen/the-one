/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import java.util.ArrayList;
import java.util.Collection;

import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimClock;
import core.World;
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

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        SimClock.reset();
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
    public void testGetHopCount(){
        DTNHost relayHost1 = this.utils.createHost();
        msg = new Message(from, to, "M", 100);
        from.createNewMessage(msg);
        transferMessage(msg.getId(), from, relayHost1);
        //Get hop count of message copy from host "relayHost1" because only that has the longer path
        int hopCount = getHopCountForMsgInCollection(msg.getId(), relayHost1.getRouter().getMessageCollection());
        final int expectedHopCountAfterRelay =1;
        assertEquals("The message has made one hop.", expectedHopCountAfterRelay, hopCount);

        DTNHost relayHost2 = this.utils.createHost();
        transferMessage(msg.getId(), relayHost1, relayHost2);
        //Get hop count of message copy from host "relayHost2" because only that has the longer path
        hopCount = getHopCountForMsgInCollection(msg.getId(), relayHost2.getRouter().getMessageCollection());
        final int expectedHopCountAfterDelivery =2;
        assertEquals("The message has made made two hops.", expectedHopCountAfterDelivery, hopCount);
    }

    @Test
    public void testGetHopCountWithoutMessagePath(){
        //Turn off storing the message path
        TestSettings ts = new TestSettings();
        ts.setNameSpace(World.OPTIMIZATION_SETTINGS_NS);
        ts.putSetting(Message.MSG_PATH_S, "false");
        Message.setStoreFullMsgPath(ts);

        testGetHopCount();
    }

    /**
     * Searches the collection for a message with the given id and returns its hop count
     * @param msgId The id of the message whose hop count is requested
     * @param collection The collection to search for the message in
     * @return The hop count of the message with the id or -1 if the message is not found.
     */
    private int getHopCountForMsgInCollection(String msgId, Collection<Message> collection){
        for (Message message : collection){
            if (message.getId().equals(msgId)){
                return message.getHopCount();
            }
        }
        //If we do not find the message at all return an error value
        return -1;
    }

    /**
     * Connects the given hosts and transfers the message with the given ID from one to the other.
     * @param msgId The id of the message to transfer
     * @param transferFrom The host the message is sent from
     * @param transferTo The host the message is sent to
     */
    private void transferMessage(String msgId, DTNHost transferFrom, DTNHost transferTo){
        transferFrom.connect(transferTo);
        transferFrom.sendMessage(msgId, transferTo);
        transferTo.messageTransferred(msgId, transferFrom);
    }
}
