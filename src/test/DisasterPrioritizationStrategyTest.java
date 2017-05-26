package test;

import core.Connection;
import core.Coord;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.Message;
import core.SettingsError;
import core.SimClock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import routing.DisasterRouter;
import routing.MessagePrioritizationStrategy;
import routing.PassiveRouter;
import routing.prioritizers.DisasterPrioritization;
import routing.prioritizers.DisasterPrioritizationStrategy;
import util.Tuple;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains tests for the {@link routing.prioritizers.DisasterPrioritizationStrategy} class.
 *
 * Created by Britta Heymann on 26.05.2017.
 */
public class DisasterPrioritizationStrategyTest {
    private static final double SHORT_TIME_SPAN = 0.1;
    private static final double LOW_UTILITY = 0.1;

    /** The allowed delta when comparing doubles for equality. */
    private static final double DOUBLE_COMPARISON_DELTA = 0.0001;

    private TestUtils testUtils = new TestUtils(new ArrayList<>(), new ArrayList<>(), new TestSettings());
    private TestSettings settings = new TestSettings();
    private SimClock clock = SimClock.getInstance();

    private DTNHost host;
    private DisasterPrioritizationStrategy prioritization;

    public DisasterPrioritizationStrategyTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    public void setUp() {
        DisasterRouterTestUtils.addDisasterRouterSettings(this.settings);
        this.testUtils.setMessageRouterProto(new DisasterRouter(this.settings));
        this.host = this.testUtils.createHost();

        this.prioritization = new DisasterPrioritizationStrategy(this.settings, this.host.getRouter());
        this.prioritization.setAttachedHost(this.host);
    }

    @After
    public void cleanUp() {
        DTNHost.reset();
        SimClock.reset();
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsOnMissingHeadStartThreshold() {
        new DisasterPrioritizationStrategy(new TestSettings(), this.host.getRouter());
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsOnNegativeHeadStartThreshold() {
        this.settings.putSetting(DisasterPrioritizationStrategy.HEAD_START_THRESHOLD_S, Integer.toString(-1));
        new DisasterPrioritizationStrategy(this.settings, this.host.getRouter());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsOnMissingRouter() {
        new DisasterPrioritizationStrategy(this.settings, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsOnNonDisasterRouter() {
        this.testUtils.setMessageRouterProto(new PassiveRouter(this.settings));
        new DisasterPrioritizationStrategy(this.settings, this.testUtils.createHost().getRouter());
    }

    @Test
    public void testGetHeadStartThreshold() {
        Assert.assertEquals(
                "Expected different threshold.",
                DisasterRouterTestUtils.HEAD_START_THRESHOLD, this.prioritization.getHeadStartThreshold(),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplicateThrowsOnMissingRouter() {
        this.prioritization.replicate(null);
    }

    @Test
    public void testReplicateCopiesHeadStartThreshold() {
        MessagePrioritizationStrategy copy = this.prioritization.replicate(this.testUtils.createHost().getRouter());
        Assert.assertTrue(
                "Expected replicated message prioritization strategy to be of different type.",
                copy instanceof DisasterPrioritizationStrategy);
        Assert.assertEquals(
                "Expected different threshold.",
                this.prioritization.getHeadStartThreshold(),
                ((DisasterPrioritizationStrategy)copy).getHeadStartThreshold(),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetAttachedHostThrowsOnMissingHost() {
        this.prioritization.setAttachedHost(null);
    }

    /**
     * Checks that when only sorting non-data messages, new messages are sorted first.
     */
    @Test
    public void testHeadStartsSortedBeforeAllNonDataMessages() {
        // Create neighbor to send messages to and a host that is known by that neighbor.
        DTNHost neighbor = this.testUtils.createHost();
        DTNHost knownHost = this.testUtils.createHost();
        neighbor.forceConnection(knownHost, null, true);

        // Create a message to that known host.
        Message highDeliveryPredMessage = new Message(this.host, knownHost, "M1", 0);
        this.host.createNewMessage(highDeliveryPredMessage);
        Tuple<Message, Connection> highDeliveryPredToNeighbor = this.messageToHost(highDeliveryPredMessage, neighbor);

        // Make sure it is not considered as new anymore.
        this.clock.advance(DisasterRouterTestUtils.HEAD_START_THRESHOLD + SHORT_TIME_SPAN);

        // Then create a new message to an unknown host.
        Message newlyCreatedMessage = new Message(this.host, this.testUtils.createHost(), "M2", 0);
        this.host.createNewMessage(newlyCreatedMessage);
        Tuple<Message, Connection> newMessageToNeighbor = this.messageToHost(newlyCreatedMessage, neighbor);

        // Make sure the newly created message would be sent after the other one if no headstarts are considered.
        this.checkOrderWithoutHeadStarts(highDeliveryPredToNeighbor, newMessageToNeighbor);

        // Make sure it is the other way around when using head starts.
        this.checkOrder(newMessageToNeighbor, highDeliveryPredToNeighbor);
    }

    /**
     * Tests that even new data messages do not get a head start.
     */
    @Test
    public void testDataMessagesDoNotGetHeadStarts() {
        // Create neighbor to send messages to and a host that is known by that neighbor.
        DTNHost neighbor = this.testUtils.createHost();
        DTNHost knownHost = this.testUtils.createHost();
        neighbor.forceConnection(knownHost, null, true);

        // Create a message to that known host.
        Message highDeliveryPredMessage = new Message(this.host, knownHost, "M1", 0);
        this.host.createNewMessage(highDeliveryPredMessage);
        Tuple<Message, Connection> highDeliveryPredToNeighbor = this.messageToHost(highDeliveryPredMessage, neighbor);

        // Make sure it is not considered as new anymore.
        this.clock.advance(DisasterRouterTestUtils.HEAD_START_THRESHOLD + SHORT_TIME_SPAN);

        // Then create a new data message to an unknown host.
        DisasterData data =
                new DisasterData(DisasterData.DataType.MARKER, 0, SimClock.getTime(), new Coord(0, 0));
        Message newlyCreatedMessage = new DataMessage(this.host, this.testUtils.createHost(), "M2", data, 0, 0);
        this.host.createNewMessage(newlyCreatedMessage);
        Tuple<Message, Connection> newMessageToNeighbor = this.messageToHost(newlyCreatedMessage, neighbor);

        // Make sure it is not being considered a head start message.
        this.checkOrder(highDeliveryPredToNeighbor, newMessageToNeighbor);
    }

    /**
     * Checks that when sorting one new message and data messages otherwise, new messages are sorted last.
     */
    @Test
    public void testHeadStartsSortedAfterAllDataMessages() {
        // Create neighbor.
        DTNHost neighbor = this.testUtils.createHost();

        // Create a normal message...
        Message message = new Message(this.host, this.testUtils.createHost(), "M1", 0);
        Tuple<Message, Connection> messageToNeighbor = this.messageToHost(message, neighbor);
        this.host.createNewMessage(message);

        // ...and a useless data message.
        DisasterData data = new DisasterData(DisasterData.DataType.MARKER, 0, SimClock.getTime(), new Coord(0, 0));
        Message dataMessage = new DataMessage(this.host, this.testUtils.createHost(), "D1", data, 0, 1);
        Tuple<Message, Connection> dataMessageToNeighbor = this.messageToHost(dataMessage, neighbor);
        this.host.createNewMessage(dataMessage);

        // Make sure the data message would be sent after the other one if no head starts are considered.
        this.checkOrderWithoutHeadStarts(messageToNeighbor, dataMessageToNeighbor);

        // Make sure it is the other way around when using head starts.
        this.checkOrder(dataMessageToNeighbor, messageToNeighbor);
    }

    /**
     * Makes sure that a head start message is inserted exactly before the first non-data message.
     */
    @Test
    public void testHeadStartSortedBetweenDataMessagesAndOthers() {
        DTNHost neighbor = this.testUtils.createHost();
        DTNHost knownHost = this.testUtils.createHost();
        neighbor.forceConnection(knownHost, null, true);

        // Create two data messages and three other messages:
        // One data message (D1) has high utility, the other one (D2) quite low.
        DisasterData data = new DisasterData(DisasterData.DataType.MARKER, 0, 0, new Coord(0, 0));
        Message highUtilityData = new DataMessage(this.host, this.testUtils.createHost(), "D1", data, 1, 0);
        Message lowUtilityData = new DataMessage(this.host, this.testUtils.createHost(), "D2", data, LOW_UTILITY, 0);

        // One of the other messages (M1) has high delivery predictability, the other (M2) none.
        Message messageToKnownHost = new Message(this.host, knownHost, "M1", 0);
        Message messageToUnknownHost = new Message(this.host, this.testUtils.createHost(), "M2", 0);
        this.host.createNewMessage(messageToKnownHost);
        this.host.createNewMessage(messageToUnknownHost);

        // Finally, the last message (M3) is the only head start message.
        this.clock.advance(DisasterRouterTestUtils.HEAD_START_THRESHOLD + SHORT_TIME_SPAN);
        Message headStart = new Message(this.host, this.testUtils.createHost(), "M3", 0);
        this.host.createNewMessage(headStart);

        // Create an unordered list of the messages.
        List<Tuple<Message, Connection>> unorderedMessages = new ArrayList<>();
        unorderedMessages.add(this.messageToHost(lowUtilityData, neighbor));
        unorderedMessages.add(this.messageToHost(highUtilityData, neighbor));
        unorderedMessages.add(this.messageToHost(messageToUnknownHost, neighbor));
        unorderedMessages.add(this.messageToHost(messageToKnownHost, neighbor));
        unorderedMessages.add(this.messageToHost(headStart, neighbor));

        // Therefore, the order without head starts should be:
        // D1, M1, D2, M2, M3
        DisasterPrioritization nonHeadStartPrioritizer =
                new DisasterPrioritization(this.settings, (DisasterRouter)this.host.getRouter());
        nonHeadStartPrioritizer.setAttachedHost(this.host);
        String[] expectedIdOrder = new String[] {"D1", "M1", "D2", "M2", "M3"};
        String[] orderWithoutHeadStarts = unorderedMessages.stream()
                .sorted(nonHeadStartPrioritizer)
                .map(t -> t.getKey().getId())
                .toArray(String[]::new);
        Assert.assertArrayEquals("Expected different order.", expectedIdOrder, orderWithoutHeadStarts);

        // And with head starts:
        // D1, M3, M1, D2, M2
        expectedIdOrder = new String[] {"D1", "M3", "M1", "D2", "M2"};
        String[] order = this.prioritization.sortMessages(unorderedMessages)
                .stream().map(t -> t.getKey().getId()).toArray(String[]::new);
        Assert.assertArrayEquals("Expected different order.", expectedIdOrder, order);
    }

    /**
     * Checks that multiple head start messages are sorted by creation, newer ones first.
     */
    @Test
    public void testHeadStartsAreSortedByCreation() {
        DTNHost neighbor = this.testUtils.createHost();
        DTNHost knownHost = this.testUtils.createHost();

        // Create two messages.
        Message olderMessage = new Message(this.host, knownHost, "M1", 0);
        Tuple<Message, Connection> olderMessageToNeighbor = this.messageToHost(olderMessage, neighbor);
        this.host.createNewMessage(olderMessage);

        this.clock.advance(SHORT_TIME_SPAN);
        Message newMessage = new Message(this.host, this.testUtils.createHost(), "M2", 0);
        Tuple<Message, Connection> newMessageToNeighbor = this.messageToHost(newMessage, neighbor);
        this.host.createNewMessage(newMessage);

        // Make sure older message has a higher delivery predictability.
        neighbor.forceConnection(knownHost, null, true);

        // It should therefore be sent first if head starts are not considered.
        this.checkOrderWithoutHeadStarts(olderMessageToNeighbor, newMessageToNeighbor);

        // But if they are considered, the newer message is more important.
        this.checkOrder(newMessageToNeighbor, olderMessageToNeighbor);
    }

    /**
     * Checks the order of messages if {@link DisasterPrioritizationStrategy} is used.
     * @param expectedFirstMessage Message expected to be sent first.
     * @param expectedSecondMessage Message expected to be sent second.
     */
    private void checkOrder(
            Tuple<Message, Connection> expectedFirstMessage, Tuple<Message, Connection> expectedSecondMessage) {
        // Create unordered message list.
        List<Tuple<Message, Connection>> unorderedMessages = new ArrayList<>();
        unorderedMessages.add(expectedSecondMessage);
        unorderedMessages.add(expectedFirstMessage);

        // Check ordering.
        List<Tuple<Message, Connection>> orderedMessages = this.prioritization.sortMessages(unorderedMessages);
        Assert.assertEquals("Expected different message first.", expectedFirstMessage, orderedMessages.get(0));
        Assert.assertEquals("Expected different message second.", expectedSecondMessage, orderedMessages.get(1));
    }

    /**
     * Checks the order of the messages if no head starts were considered.
     * @param expectedFirstMessage Message expected to be sent first.
     * @param expectedSecondMessage Message expected to be sent second.
     */
    private void checkOrderWithoutHeadStarts(
            Tuple<Message, Connection> expectedFirstMessage, Tuple<Message, Connection> expectedSecondMessage) {
        DisasterPrioritization nonHeadStartPrioritizer =
                new DisasterPrioritization(this.settings, (DisasterRouter)this.host.getRouter());
        nonHeadStartPrioritizer.setAttachedHost(this.host);
        Assert.assertTrue(
                "Other message should be more important.",
                nonHeadStartPrioritizer.compare(expectedFirstMessage, expectedSecondMessage) < 0);
    }

    /**
     * Translates a message to a message-connection tuple where the connection is one that is built up from
     * {@link #host} to the provided host.
     * @param message The message to use.
     * @param to The host to connect to.
     * @return The message-connection tuple.
     */
    private Tuple<Message, Connection> messageToHost(Message message, DTNHost to) {
        return new Tuple<>(
                message,
                DisasterPrioritizationTest.createConnection(this.host, to));
    }
}
