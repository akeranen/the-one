package test;

import applications.DatabaseApplication;
import core.BroadcastMessage;
import core.Coord;
import core.DataMessage;
import core.DisasterData;
import core.Group;
import core.Message;
import core.MulticastMessage;
import core.SimClock;
import org.junit.Assert;
import org.junit.Test;
import routing.DisasterRouter;
import util.Tuple;

import java.util.Arrays;
import java.util.Collections;

/**
 * Contains tests for the {@link DisasterRouter} class.
 *
 * Created by Britta Heymann on 19.05.2017.
 */
// Suppress warnings about there being no tests in this class: Tests exist; they just don't need a @Test annotation
// because this class extends TestCase.
@SuppressWarnings({"squid:S2187"})
public class DisasterRouterTest extends AbstractRouterTest {
    /* Some time (span)s needed for tests. */
    private static final double SHORT_TIME_SPAN = 0.1;
    private static final double FIRST_MEETING_TIME = 4;
    private static final double SECOND_MEETING_TIME = 8;

    /* Some priority values needed for tests. */
    private static final int PRIORITY = 5;
    private static final int VERY_HIGH_PRIORITY = 10;
    private static final int HIGH_PRIORITY = 6;
    private static final int LOW_PRIORITY = 4;

    /** Assumed replications densitiy if nothing is known about a message. */
    private static final double DEFAULT_REPLICATIONS_DENSITY = 0.5;

    private static final String EXPECTED_DIFFERENT_DELIVERY_PREDICTABILITY =
            "Expected different delivery predictability.";
    private static final String EXPECTED_DIFFERENT_MESSAGE = "Expected different message.";

    /* The delta allowed on double comparisons. */
    private static final double DOUBLE_COMPARISON_DELTA = 0.0001;

    @Override
    public void setUp() throws Exception {
        DisasterRouterTestUtils.addDisasterRouterSettings(ts);

        setRouterProto(new DisasterRouter(ts));
        super.setUp();
    }

    /**
     * Tears down the fixture, for example, close a network connection.
     * This method is called after a test is executed.
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Group.clearGroups();
    }

    public void testEncounterValueIsComputedCorrectly() {
        this.clock.setTime(DisasterRouterTestUtils.EV_WINDOW_LENGTH);
        h1.connect(h2);
        this.updateAllNodes();

        DisasterRouter router = (DisasterRouter)h2.getRouter();
        Assert.assertEquals(
                "Expected different encounter value.",
                1 * DisasterRouterTestUtils.NEW_DATA_WEIGHT, router.getEncounterValue(), DOUBLE_COMPARISON_DELTA);
    }

    public void testEncounterValueIsUpdatedAtCorrectTime() {
        // Make sure an encounter exists.
        h1.connect(h2);
        DisasterRouter router = (DisasterRouter)h2.getRouter();

        // Check encounter value is not updated shortly before time window is up.
        this.clock.setTime(DisasterRouterTestUtils.EV_WINDOW_LENGTH - SHORT_TIME_SPAN);
        this.updateAllNodes();
        Assert.assertEquals(
                "Encounter value should not have been updated yet.",
                0, router.getEncounterValue(), DOUBLE_COMPARISON_DELTA);

        // Check encounter value is updated shortly after time window is up.
        this.clock.setTime(DisasterRouterTestUtils.EV_WINDOW_LENGTH + SHORT_TIME_SPAN);
        this.updateAllNodes();
        Assert.assertNotEquals(
                "Encounter value should have been updated.",
                0, router.getEncounterValue(), DOUBLE_COMPARISON_DELTA);
    }

    public void testEncounterValueManagersDifferBetweenHosts() {
        this.clock.setTime(DisasterRouterTestUtils.EV_WINDOW_LENGTH);
        h1.connect(h2);
        this.updateAllNodes();

        DisasterRouter router = (DisasterRouter)h3.getRouter();
        Assert.assertEquals(
                "Expected different encounter value.",
                0, router.getEncounterValue(), DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Tests that the replications density is computed correctly if two nodes with the same message meet each other.
     */
    public void testReplicationsDensitiesAreComputedCorrectly() {
        // Make sure it's the correct time.
        this.clock.setTime(DisasterRouterTestUtils.RD_WINDOW_LENGTH);

        // Give a message to two hosts.
        Message message = new Message(h3, h4, "M1", 0);
        h1.createNewMessage(message);
        h2.createNewMessage(message);

        // They should meet each other.
        h1.connect(h2);
        disconnect(h1);
        this.updateAllNodes();

        // Make sure the replications density was computed accordingly.
        DisasterRouter router = (DisasterRouter)h1.getRouter();
        Assert.assertEquals(
                "Expected different replications density.",
                1, router.getReplicationsDensity(message), DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Tests that the replications density for a message is added as soon as a host has this message in buffer.
     */
    public void testReplicationsDensityIsAddedWhenMessageIsAdded() {
        Message message = new Message(h3, h4, "M1", 0);
        h1.createNewMessage(message);

        DisasterRouter router = (DisasterRouter)h1.getRouter();
        Assert.assertEquals(
                "Expected different replications density.",
                DEFAULT_REPLICATIONS_DENSITY, router.getReplicationsDensity(message), DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Tests that removing a message from a host's buffer means it will also be removed from the host's replications
     * densities.
     */
    public void testReplicationsDensityIsRemovedWhenMessageIsRemoved() {
        Message message = new Message(h3, h4, "M1", 0);
        h1.createNewMessage(message);
        h1.deleteMessage(message.getId(), false);

        DisasterRouter router = (DisasterRouter)h1.getRouter();
        try {
            router.getReplicationsDensity(message);
            fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Expected different exception.", "Asked for a non-stored message!", e.getMessage());
        }
    }

    public void testReplicationsDensitiesAreUpdatedAtCorrectTime() {
        // Make two hosts aware of a message and let them meet each other.
        Message message = new Message(h3, h4, "M1", 0);
        h1.createNewMessage(message);
        h2.createNewMessage(message);
        h1.connect(h2);
        disconnect(h1);

        // Check replications density is not updated shortly before time window is up.
        DisasterRouter router = (DisasterRouter)h2.getRouter();
        this.clock.setTime(DisasterRouterTestUtils.RD_WINDOW_LENGTH - SHORT_TIME_SPAN);
        this.updateAllNodes();
        Assert.assertEquals(
                "Replications density should not have been updated yet.",
                DEFAULT_REPLICATIONS_DENSITY, router.getReplicationsDensity(message), DOUBLE_COMPARISON_DELTA);

        // Check replications density is updated shortly after time window is up.
        this.clock.setTime(DisasterRouterTestUtils.RD_WINDOW_LENGTH + SHORT_TIME_SPAN);
        this.updateAllNodes();
        Assert.assertNotEquals(
                "Replications density should have been updated.",
                DEFAULT_REPLICATIONS_DENSITY, router.getReplicationsDensity(message), DOUBLE_COMPARISON_DELTA);
    }

    public void testReplicationsDensityManagersDifferBetweenHosts() {
        // Create a message h1 knows about.
        Message message = new Message(h3, h4, "M1", 0);
        h1.createNewMessage(message);

        // h2 should not be able to get a replications density for the message.
        DisasterRouter router = (DisasterRouter)h2.getRouter();
        try {
            router.getReplicationsDensity(message);
            fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Expected different exception.", "Asked for a non-stored message!", e.getMessage());
        }
    }

    /**
     * Tests that delivery predictabilities are correctly computed by the router by checking them after two meetings
     * between three nodes.
     */
    public void testDeliveryPredictabilitesAreComputedCorrectly() {
        // Play short scenario with two meetings.
        this.clock.setTime(FIRST_MEETING_TIME);
        h1.connect(h2);
        this.clock.setTime(SECOND_MEETING_TIME);
        h2.connect(h3);

        // Get all routers.
        DisasterRouter router1 = (DisasterRouter)h1.getRouter();
        DisasterRouter router2 = (DisasterRouter)h2.getRouter();
        DisasterRouter router3 = (DisasterRouter)h3.getRouter();

        // Check delivery predictabilities are correct:
        Message messageToH1 = new Message(h4, h1, "M1", 0);
        Message messageToH2 = new Message(h4, h2, "M2", 0);
        Message messageToH3 = new Message(h4, h3, "M3", 0);

        // Check delivery predictabilies for h1.
        Assert.assertEquals(
                EXPECTED_DIFFERENT_DELIVERY_PREDICTABILITY,
                DisasterRouterTestUtils.SUMMAND, router1.getDeliveryPredictability(messageToH2),
                DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                EXPECTED_DIFFERENT_DELIVERY_PREDICTABILITY,
                0, router1.getDeliveryPredictability(messageToH3), DOUBLE_COMPARISON_DELTA);

        // Check delivery predictabilies for h2.
        double age = (SECOND_MEETING_TIME - FIRST_MEETING_TIME) / DisasterRouterTestUtils.SECONDS_IN_TIME_UNIT;
        double agedPredictability = DisasterRouterTestUtils.SUMMAND * Math.pow(DisasterRouterTestUtils.GAMMA, age);
        Assert.assertEquals(
                EXPECTED_DIFFERENT_DELIVERY_PREDICTABILITY,
                agedPredictability, router2.getDeliveryPredictability(messageToH1), DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                EXPECTED_DIFFERENT_DELIVERY_PREDICTABILITY,
                DisasterRouterTestUtils.SUMMAND, router2.getDeliveryPredictability(messageToH3),
                DOUBLE_COMPARISON_DELTA);

        // Check delivery predictabilies for h3.
        double transitivePredictability =
                DisasterRouterTestUtils.SUMMAND * DisasterRouterTestUtils.BETA * agedPredictability;
        Assert.assertEquals(
                EXPECTED_DIFFERENT_DELIVERY_PREDICTABILITY,
                transitivePredictability, router3.getDeliveryPredictability(messageToH1), DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                EXPECTED_DIFFERENT_DELIVERY_PREDICTABILITY,
                DisasterRouterTestUtils.SUMMAND, router3.getDeliveryPredictability(messageToH2),
                DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Checks that a host does not send out new messages when already transferring.
     */
    public void testNoMessagesAreSentWhenAlreadyTransferring() {
        h1.connect(h2);

        // Try to send a non-empty message.
        Message m1 = new Message(h1, h2, "M1", 1);
        h1.createNewMessage(m1);
        this.updateAllNodes();

        // Should have started transfer.
        this.mc.next();
        this.checkTransferStart(h1, h2, m1.getId());

        // Create a new message and update routers again.
        Message m2 = new Message(h1, h2, "M2", 0);
        h1.createNewMessage(m2);
        this.updateAllNodes();

        // Check that the new message was not sent.
        while (this.mc.next()) {
            Assert.assertNotEquals("Did not expect another transfer.", mc.TYPE_START, this.mc.getLastType());
        }

        // Finally, check that the original message will still be transferred.
        this.clock.advance(1);
        this.updateAllNodes();
        this.mc.next();
        Assert.assertEquals(
                "Original message should have been processed next.", m1.getId(), this.mc.getLastMsg().getId());
        Assert.assertEquals(
                "Original message should have been transferred.", this.mc.TYPE_RELAY, this.mc.getLastType());
    }

    /**
     * Checks that a host does not send out new messages to hosts which are already transferring.
     */
    public void testNoMessagesAreReceivedWhenAlreadyTransferring() {
        // Let h2 be transferring.
        h2.connect(h3);
        Message m1 = new Message(h2, h3, "M1", 1);
        h2.createNewMessage(m1);
        this.updateAllNodes();

        // Check the transfer started.
        this.mc.next();
        this.checkTransferStart(h2, h3, m1.getId());

        // Let h1 try to send a message to h2 now.
        Message m2 = new Message(h1, h2, "M2", 0);
        h1.createNewMessage(m2);
        h1.connect(h2);
        this.updateAllNodes();

        // Check that the new message was not sent.
        while(this.mc.next()) {
            Assert.assertNotEquals("Did not expect another transfer.", mc.TYPE_START, this.mc.getLastType());
        }

        // Finally, check that the original message will still be transferred.
        this.clock.advance(1);
        this.updateAllNodes();
        this.mc.next();
        Assert.assertEquals(
                "Original message should have been processed next.", m1.getId(), this.mc.getLastMsg().getId());
        Assert.assertEquals(
                "Original message should have been transferred.", this.mc.TYPE_RELAY, this.mc.getLastType());
    }

    /**
     * Tests that direct messages of all types but data message are sent first, both to and from the neighbor.
     */
    public void testDirectMessagesAreSentFirst() {
        // Create groups for multicasts.
        Group directGroup = Group.createGroup(0);
        directGroup.addHost(h1);
        directGroup.addHost(h2);
        Group indirectGroup = Group.createGroup(1);
        indirectGroup.addHost(h1);
        indirectGroup.addHost(h3);

        // Create other messages to sort.
        Message directMulticast = new MulticastMessage(h1, directGroup, "m1", 0, 1);
        Message indirectMulticast = new MulticastMessage(h1, indirectGroup, "m2", 0, HIGH_PRIORITY);
        Message broadcast = new BroadcastMessage(h2, "B1", 0);
        Message directMessage = new Message(h1, h2, "M1", 0, 0);
        Message indirectMessage = new Message(h1, h3, "M2", 0, VERY_HIGH_PRIORITY);
        Message lowPrioMessage = new Message(h1, h3, "M3", 0, 0);
        h1.createNewMessage(directMulticast);
        h1.createNewMessage(indirectMulticast);
        h2.createNewMessage(broadcast);
        h1.createNewMessage(directMessage);
        h1.createNewMessage(indirectMessage);
        h1.createNewMessage(lowPrioMessage);

        // Advance time to prevent that any message gets a head start in sorting due to being new.
        this.clock.advance(DisasterRouterTestUtils.HEAD_START_THRESHOLD + SHORT_TIME_SPAN);

        // Add high utility data for data message.
        DisasterData data = new DisasterData(DisasterData.DataType.MARKER, 0, SimClock.getTime(), h1.getLocation());
        DatabaseApplication app = new DatabaseApplication(ts);
        h1.getRouter().addApplication(app);
        app.update(h1);
        app.disasterDataCreated(h1, data);

        // Connect hosts.
        h1.connect(h2);

        // Make sure messages are sent in correct order.
        String[] expectedIdOrder = {
                directMulticast.getId(), directMessage.getId(), broadcast.getId(), indirectMessage.getId(),
                indirectMulticast.getId(), "D" + Arrays.asList(data).hashCode(), lowPrioMessage.getId()
        };
        this.mc.reset();
        for (String expectedId : expectedIdOrder) {
            this.updateAllNodes();
            do {
                this.mc.next();
            } while (!this.mc.TYPE_START.equals(this.mc.getLastType()));
            Assert.assertEquals(EXPECTED_DIFFERENT_MESSAGE, expectedId, mc.getLastMsg().getId());
        }
    }

    /**
     * Tests that direct messages are sorted in correct order when we send them out.
     */
    public void testDirectMessagesAreSentSortedByPriority() {
        // Connect to several hosts.
        h1.connect(h2);
        h1.connect(h3);

        // Create direct messages with different priorities to two different neighbors.
        Message broadcast = new BroadcastMessage(h1, "B1", 0, PRIORITY);
        Message lowPrio = new Message(h1, h2,"M1", 0, LOW_PRIORITY);
        Message highPrio = new Message(h1, h3, "M2", 0, HIGH_PRIORITY);
        h1.createNewMessage(broadcast);
        h1.createNewMessage(lowPrio);
        h1.createNewMessage(highPrio);

        // Make sure they are sent in correct order.
        Message[] expectedOrder = { highPrio, broadcast, broadcast, lowPrio };
        this.mc.reset();
        for (Message expectedMessage : expectedOrder) {
            h1.update(true);
            do {
                this.mc.next();
            } while (!this.mc.TYPE_START.equals(this.mc.getLastType()));
            Assert.assertEquals(EXPECTED_DIFFERENT_MESSAGE, expectedMessage.getId(), mc.getLastMsg().getId());
        }
    }

    /**
     * Tests that direct messages are received in correct order if neighbor wants to send us some.
     */
    public void testDirectMessagesAreReceivedSortedByPriority() {
        // Connect to a neighbor.
        h1.connect(h2);

        // Let it create direct messages to attached host.
        Message broadcast = new BroadcastMessage(h2, "B1", 0, PRIORITY);
        Message lowPrio = new Message(h2, h1,"M1", 0, LOW_PRIORITY);
        Message highPrio = new Message(h2, h1, "M2", 0, HIGH_PRIORITY);
        h2.createNewMessage(broadcast);
        h2.createNewMessage(lowPrio);
        h2.createNewMessage(highPrio);

        // Also give a message to h1 so it tries to find messages to receive.
        h1.createNewMessage(new Message(h1, h3, "M6", 1));

        // Make sure direct messages h2 --> h1 are sent in correct order.
        Message[] expectedOrder = { highPrio, broadcast, lowPrio };
        for (Message expectedMessage : expectedOrder) {
            this.mc.reset();

            // Let H1 update while connected to H2, but not the other way around (else we look at sending direct
            // messages, not receiving them!).
            h1.connect(h2);
            h1.update(true);
            disconnect(h2);
            h2.update(true);

            // Find started message and see if it is the correct one.
            while (!this.mc.TYPE_START.equals(this.mc.getLastType())) {
                this.mc.next();
            }
            Assert.assertEquals(EXPECTED_DIFFERENT_MESSAGE, expectedMessage.getId(), mc.getLastMsg().getId());

            // Don't send it again even if transfer was not completed.
            h2.deleteMessage(mc.getLastMsg().getId(), true);
        }
    }

    /**
     * Tests that messages are sorted by delivery, replications density and utility (data messages). In addition,
     * messages with high priority should be send first, sorted by priority, and very new text messages should be sent
     * before high priority text messages.
     */
    public void testNonDirectMessageSorting() {
        // Create messages to sort.
        DisasterData data = new DisasterData(DisasterData.DataType.MARKER, 0, 0, new Coord(0, 0));
        Message vipDataMessages =
                new DataMessage(h1, h3, "D1", Collections.singleton(new Tuple<>(data, 0D)), VERY_HIGH_PRIORITY);
        Message usefulDataMessages = new DataMessage(h1, h3, "D2", Collections.singleton(new Tuple<>(data, 1D)), 0);
        Message highDeliveryPredictabilityMessage = new Message(h1, h4, "M1", 0, 0);
        Message lowReplicationsDensityMessage = new Message(h1, h3, "M2", 0, 0);
        Message highReplicationsDensityMessage = new Message(h1, h3, "M3", 0, 0);
        Message vipMessage = new Message(h1, h3, "M6", 0, HIGH_PRIORITY);
        this.clock.setTime(DisasterRouterTestUtils.HEAD_START_THRESHOLD + SHORT_TIME_SPAN);
        Message newMessage = new Message(h1, h3, "M4", 0, 0);
        this.clock.advance(SHORT_TIME_SPAN);
        Message newestMessage = new Message(h1, h3, "M5", 0, 0);

        // Make h1 know all of them.
        Message[] allMessages = {
                vipDataMessages, usefulDataMessages, highDeliveryPredictabilityMessage, lowReplicationsDensityMessage,
                highReplicationsDensityMessage, newMessage, newestMessage, vipMessage
        };
        for (Message m : allMessages) {
            h1.createNewMessage(m);
        }

        // Increase delivery predictability for message M1 by letting h2 meet its final recipient, h4.
        h2.connect(h4);
        disconnect(h4);

        // Increase replications density for M3 by giving it to h5, then letting h1 notice that h5 has it.
        h5.createNewMessage(highReplicationsDensityMessage);
        h1.connect(h5);
        disconnect(h5);
        this.updateAllNodes();

        // Connect h1 to h2.
        h1.connect(h2);

        // Check order of messages.
        Message[] expectedOrder = {
                vipDataMessages, newestMessage, newMessage, vipMessage, usefulDataMessages,
                highDeliveryPredictabilityMessage, lowReplicationsDensityMessage, highReplicationsDensityMessage
        };
        this.mc.reset();
        for (Message expectedMessage : expectedOrder) {
            h1.update(true);
            do {
                this.mc.next();
            } while (!this.mc.TYPE_START.equals(this.mc.getLastType()));
            Assert.assertEquals(EXPECTED_DIFFERENT_MESSAGE, expectedMessage.getId(), mc.getLastMsg().getId());
        }
    }
}
