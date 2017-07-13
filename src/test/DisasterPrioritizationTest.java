package test;

import core.BroadcastMessage;
import core.CBRConnection;
import core.Connection;
import core.Coord;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.Group;
import core.Message;
import core.MulticastMessage;
import core.SettingsError;
import core.SimClock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import routing.DisasterRouter;
import routing.PassiveRouter;
import routing.prioritizers.DisasterPrioritization;
import routing.prioritizers.DisasterPrioritizationStrategy;
import util.Tuple;

import java.util.ArrayList;

import static test.DisasterRouterTestUtils.RD_WINDOW_LENGTH;

/**
 * Contains tests for the {@link routing.prioritizers.DisasterPrioritization} class.
 *
 * Created by Britta Heymann on 25.05.2017.
 */
public class DisasterPrioritizationTest {
    /** The delivery predictability weight for the {@link DisasterPrioritization} used in most test. */
    private static final double DP_WEIGHT = 0.8;

    /* Some values needed in tests. */
    private static final double WEIGHT_GREATER_ONE = 1.1;
    private static final double MEDIUM_UTILITY = 0.15;

    /** The allowed delta when comparing doubles for equality. */
    private static final double DOUBLE_COMPARISON_DELTA = 0.0001;

    private static final String UNEXPECTED_COMPARISON_RESULT = "Expected different comparison result.";

    private TestUtils testUtils = new TestUtils(new ArrayList<>(), new ArrayList<>(), new TestSettings());
    private SimClock clock = SimClock.getInstance();

    private DTNHost host;
    private DisasterPrioritization prioritization;

    public DisasterPrioritizationTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    public void setUp() {
        TestSettings s = new TestSettings();
        DisasterRouterTestUtils.addDisasterRouterSettings(s);
        this.testUtils.setMessageRouterProto(new DisasterRouter(s));
        this.host = this.testUtils.createHost();
        this.prioritization = DisasterPrioritizationTest.createDisasterPrioritization(DP_WEIGHT, this.host);
    }

    @After
    public void cleanUp() {
        DTNHost.reset();
        SimClock.reset();
        Group.clearGroups();
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsOnMissingWeight() {
        new DisasterPrioritization(new TestSettings(), (DisasterRouter)this.host.getRouter());
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsOnNegativeWeight() {
        DisasterPrioritizationTest.createDisasterPrioritization(-1, this.host);
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsOnWeightGreaterOne() {
        DisasterPrioritizationTest.createDisasterPrioritization(WEIGHT_GREATER_ONE, this.host);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsOnMissingRouter() {
        TestSettings settings = new TestSettings(DisasterPrioritizationStrategy.DISASTER_PRIORITIZATION_NS);
        settings.putSetting(DisasterPrioritization.DELIVERY_PREDICTABILITY_WEIGHT, Double.toString(DP_WEIGHT));
        new DisasterPrioritization(settings, null);
    }

    @Test
    public void testGetDeliveryPredictabilityWeight() {
        Assert.assertEquals(
                "Expected different weight.",
                DP_WEIGHT, this.prioritization.getDeliveryPredictabilityWeight(), DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetReplicationsDensityWeight() {
        Assert.assertEquals(
                "Expected different weight.",
                1 - DP_WEIGHT, this.prioritization.getReplicationsDensityWeight(), DOUBLE_COMPARISON_DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCopyConstructorThrowsOnMissingRouter() {
        new DisasterPrioritization(this.prioritization, null);
    }

    @Test
    public void testCopyConstructorCopiesWeights() {
        DisasterPrioritization copy =
                new DisasterPrioritization(this.prioritization, (DisasterRouter)this.host.getRouter());
        Assert.assertEquals(
                "Expected different delivery predictability weight.",
                this.prioritization.getDeliveryPredictabilityWeight(), copy.getDeliveryPredictabilityWeight(),
                DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                "Expected different replications densitiy weight.",
                this.prioritization.getReplicationsDensityWeight(), copy.getReplicationsDensityWeight(),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetHostThrowsOnMissingHost() {
        this.prioritization.setAttachedHost(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompareThrowsForFirstMessageBroadcastMessage() {
        Message broadcast = new BroadcastMessage(this.host, "M1", 0);
        Message oneToOne = new Message(this.host, this.testUtils.createHost(), "M2", 0);
        Connection connection = DisasterPrioritizationTest.createConnection(this.host, this.testUtils.createHost());

        this.prioritization.compare(new Tuple<>(broadcast, connection), new Tuple<>(oneToOne, connection));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompareThrowsForSecondMessageBroadcastMessage() {
        Message broadcast = new BroadcastMessage(this.host, "M1", 0);
        Message oneToOne = new Message(this.host, this.testUtils.createHost(), "M2", 0);
        Connection connection = DisasterPrioritizationTest.createConnection(this.host, this.testUtils.createHost());

        this.prioritization.compare(new Tuple<>(oneToOne, connection), new Tuple<>(broadcast, connection));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompareThrowsForConnectionToNonDisasterHost() {
        this.testUtils.setMessageRouterProto(new PassiveRouter(new TestSettings()));
        DTNHost nonDisasterHost = this.testUtils.createHost();

        Message m = new Message(this.host, nonDisasterHost, "M1", 0);

        this.prioritization.compare(this.messageToHost(m, nonDisasterHost), this.messageToHost(m, nonDisasterHost));
    }

    @Test
    public void testLowReplicationsDensityLeadsToEarlierSending() {
        // Create two messages to a neighbor.
        DTNHost neighbor = this.testUtils.createHost();
        Message knownMessage = new Message(this.host, neighbor, "M1", 0);
        Message unknownMessage = new Message(this.host, neighbor, "M2", 0);

        // One should be known by the neighbor.
        neighbor.createNewMessage(knownMessage);

        // Update replications densities.
        this.host.createNewMessage(knownMessage);
        this.host.createNewMessage(unknownMessage);
        this.host.forceConnection(neighbor, null, true);
        this.clock.setTime(RD_WINDOW_LENGTH);
        this.host.update(true);

        // Check comparison values.
        Tuple<Message, Connection> knownMessageToHost = this.messageToHost(knownMessage, neighbor);
        Tuple<Message, Connection> unknownMessageToHost = this.messageToHost(unknownMessage, neighbor);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.prioritization.compare(knownMessageToHost, unknownMessageToHost) > 0);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.prioritization.compare(unknownMessageToHost, knownMessageToHost) < 0);
    }

    @Test
    public void testHighDeliveryPredictabilityLeadsToEarlierSending() {
        // Create some hosts.
        DTNHost neighbor = this.testUtils.createHost();
        DTNHost knownRecipient = this.testUtils.createHost();
        DTNHost unknownRecipient = this.testUtils.createHost();

        // Let two of them know each other.
        neighbor.forceConnection(knownRecipient, null, true);
        neighbor.update(true);

        // Create two messages, one of them to the known host.
        Message messageToKnownHost = new Message(this.host, knownRecipient, "M1", 0);
        Message messageToUnknownHost = new Message(this.host, unknownRecipient, "M2", 0);
        this.host.createNewMessage(messageToKnownHost);
        this.host.createNewMessage(messageToUnknownHost);

        // Check comparison values.
        Tuple<Message, Connection> highDeliveryPredictabilityMessage = this.messageToHost(messageToKnownHost, neighbor);
        Tuple<Message, Connection> lowDeliveryPredictabilityMessage =
                this.messageToHost(messageToUnknownHost, neighbor);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.prioritization.compare(lowDeliveryPredictabilityMessage, highDeliveryPredictabilityMessage) > 0);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.prioritization.compare(highDeliveryPredictabilityMessage, lowDeliveryPredictabilityMessage) < 0);
    }

    /**
     * Checks that the return value of {@link DisasterPrioritization#compare(Tuple, Tuple)} may change if the
     * underlying values change.
     * This test is interesting because we cache some values and have to make sure that the cache gets invalidated.
     */
    @Test
    public void testComparisonReactsToNewInformation() {
        // Create some hosts.
        DTNHost neighbor = this.testUtils.createHost();
        DTNHost knownRecipient = this.testUtils.createHost();
        DTNHost unknownRecipient = this.testUtils.createHost();

        // Let two of them know each other.
        neighbor.forceConnection(knownRecipient, null, true);
        neighbor.update(true);

        // Create two messages, one of them to the known host.
        Message message1 = new Message(this.host, knownRecipient, "M1", 0);
        Message message2 = new Message(this.host, unknownRecipient, "M2", 0);
        this.host.createNewMessage(message1);
        this.host.createNewMessage(message2);

        // Check comparison values.
        Tuple<Message, Connection> tuple1 = this.messageToHost(message1, neighbor);
        Tuple<Message, Connection> tuple2 = this.messageToHost(message2, neighbor);
        Assert.assertTrue(UNEXPECTED_COMPARISON_RESULT, this.prioritization.compare(tuple2, tuple1) > 0);

        // Increase simulator time.
        this.clock.advance(DisasterRouterTestUtils.RD_WINDOW_LENGTH);

        // Then make sure that the message to the known host is seen as being spread widely.
        DTNHost otherHost = this.testUtils.createHost();
        otherHost.createNewMessage(message1);
        this.host.forceConnection(otherHost, null, true);
        this.host.update(true);

        // And also let the neighbor meet the currently unknown host.
        neighbor.forceConnection(unknownRecipient, null, true);

        // The comparison return value should now have flipped.
        Assert.assertTrue(UNEXPECTED_COMPARISON_RESULT, this.prioritization.compare(tuple1, tuple2) > 0);
    }

    @Test
    public void testComparisonUsesHigherDeliveryPredictabilityForMulticastMessages() {
        // Create some hosts.
        DTNHost neighbor = this.testUtils.createHost();
        DTNHost knownRecipient = this.testUtils.createHost();
        DTNHost unknownRecipient = this.testUtils.createHost();

        // Create a group including the recipients.
        Group group = Group.createGroup(0);
        group.addHost(knownRecipient);
        group.addHost(unknownRecipient);
        group.addHost(this.host);

        // Let two of them know each other.
        neighbor.forceConnection(knownRecipient, null, true);
        neighbor.update(true);

        // Create two messages, one of them a multicast message to the group including the known host.
        Message messageToGroup = new MulticastMessage(this.host, group, "M1", 0);
        Message messageToUnknownHost = new Message(this.host, unknownRecipient, "M2", 0);
        this.host.createNewMessage(messageToGroup);
        this.host.createNewMessage(messageToUnknownHost);

        // Check comparison values.
        Tuple<Message, Connection> groupMessageToNeighbor = this.messageToHost(messageToGroup, neighbor);
        Tuple<Message, Connection> unknownHostMessageToNeighbor =
                this.messageToHost(messageToUnknownHost, neighbor);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.prioritization.compare(unknownHostMessageToNeighbor, groupMessageToNeighbor) > 0);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.prioritization.compare(groupMessageToNeighbor, unknownHostMessageToNeighbor) < 0);
    }

    /**
     * Checks that two messages M1 and M2 where M1 has the higher delivery predictability, but M2 has the lower
     * replications density, might be ordered differently depending on the delivery predictability weight the disaster
     * prioritization has.
     */
    @Test
    public void testImportanceIsWeightDependent() {
        // Create two prioritizers, one only sorting by delivery predictability, one only by replications density.
        DisasterPrioritization deliveryPredictabilityPrio =
                DisasterPrioritizationTest.createDisasterPrioritization(1, this.host);
        DisasterPrioritization replicationsDensityPrio =
                DisasterPrioritizationTest.createDisasterPrioritization(0, this.host);

        // Create some hosts.
        DTNHost neighbor = this.testUtils.createHost();
        DTNHost knownRecipient = this.testUtils.createHost();
        DTNHost unknownRecipient = this.testUtils.createHost();

        // Make sure two of them know each other.
        neighbor.forceConnection(knownRecipient, null, true);
        neighbor.update(true);

        // Create two messages, one of them to the known host.
        Message messageToKnownHost = new Message(this.host, knownRecipient, "M1", 0);
        Message messageToUnknownHost = new Message(this.host, unknownRecipient, "M2", 0);
        this.host.createNewMessage(messageToKnownHost);
        this.host.createNewMessage(messageToUnknownHost);

        // The one to the known host should also be known by many nodes in the host's region.
        unknownRecipient.createNewMessage(messageToKnownHost);
        this.host.forceConnection(unknownRecipient, null, true);
        this.host.forceConnection(unknownRecipient, null, false);
        this.clock.setTime(RD_WINDOW_LENGTH);
        this.host.update(true);

        // Check comparison values.
        Tuple<Message, Connection> tupleWithMessageToKnownHost = this.messageToHost(messageToKnownHost, neighbor);
        Tuple<Message, Connection> tupleWithMessageToUnknownHost =
                this.messageToHost(messageToUnknownHost, neighbor);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                deliveryPredictabilityPrio.compare(tupleWithMessageToUnknownHost, tupleWithMessageToKnownHost) > 0);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                deliveryPredictabilityPrio.compare(tupleWithMessageToKnownHost, tupleWithMessageToUnknownHost) < 0);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                replicationsDensityPrio.compare(tupleWithMessageToUnknownHost, tupleWithMessageToKnownHost) < 0);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                replicationsDensityPrio.compare(tupleWithMessageToKnownHost, tupleWithMessageToUnknownHost) > 0);
    }

    @Test
    public void testDataMessagesUseUtilityForSorting() {
        // Create three messages to a neighbor, one of them a data message.
        DTNHost neighbor = this.testUtils.createHost();
        Message knownMessage = new Message(this.host, neighbor, "M1", 0);
        Message unknownMessage = new Message(this.host, neighbor, "M2", 0);
        DisasterData data = new DisasterData(DisasterData.DataType.MARKER, 0, 0, new Coord(0, 0));
        Message dataMessage = new DataMessage(this.host, neighbor, "D1", data, MEDIUM_UTILITY, 0);

        // One of the non-data messages should be known by the neighbor s.t. they have different priority values.
        neighbor.createNewMessage(knownMessage);

        // Update replications densities.
        this.host.createNewMessage(knownMessage);
        this.host.createNewMessage(unknownMessage);
        this.host.forceConnection(neighbor, null, true);
        this.clock.setTime(RD_WINDOW_LENGTH);
        this.host.update(true);

        // Check comparison values wrt to data message.
        Tuple<Message, Connection> lowPriorityMessage = this.messageToHost(knownMessage, neighbor);
        Tuple<Message, Connection> highPriorityMessage = this.messageToHost(unknownMessage, neighbor);
        Tuple<Message, Connection> dataMessageToHost = this.messageToHost(dataMessage, neighbor);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.prioritization.compare(lowPriorityMessage, dataMessageToHost) > 0);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.prioritization.compare(dataMessageToHost, lowPriorityMessage) < 0);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.prioritization.compare(dataMessageToHost, highPriorityMessage) > 0);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.prioritization.compare(highPriorityMessage, dataMessageToHost) < 0);
    }

    /**
     * Creates an instance of the {@link DisasterPrioritization} class using the provided parameters.
     * @param weight Weight for delivery predictability.
     * @param attachedHost Host prioritizing the messages.
     * @return The created {@link DisasterPrioritization} instance.
     */
    private static DisasterPrioritization createDisasterPrioritization(double weight, DTNHost attachedHost) {
        TestSettings settings = new TestSettings(DisasterPrioritizationStrategy.DISASTER_PRIORITIZATION_NS);
        settings.putSetting(DisasterPrioritization.DELIVERY_PREDICTABILITY_WEIGHT, Double.toString(weight));

        DisasterPrioritization prio =  new DisasterPrioritization(settings, (DisasterRouter)attachedHost.getRouter());
        prio.setAttachedHost(attachedHost);
        return prio;
    }

    /**
     * Translates a message to a message-connection tuple where the connection is one that is built between
     * {@link #host} and the provided host.
     * @param message The message to use.
     * @param host The host to connect to.
     * @return The message-connection tuple.
     */
    private Tuple<Message, Connection> messageToHost(Message message, DTNHost host) {
        return new Tuple<>(
                message,
                DisasterPrioritizationTest.createConnection(this.host, host));
    }

    /**
     * Creates a {@link Connection} from the first provided host to the second provided host.
     * @return The created connection object.
     */
    public static Connection createConnection(DTNHost from, DTNHost to) {
        return new CBRConnection(from, from.getInterfaces().get(0), to, to.getInterfaces().get(0), 1);
    }
}
