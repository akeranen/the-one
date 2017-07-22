package test;

import core.BroadcastMessage;
import core.DTNHost;
import core.Message;
import core.SettingsError;
import core.SimClock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import routing.DisasterRouter;
import routing.util.DisasterBufferComparator;

import java.util.ArrayList;

/**
 * Contains tests for the {@link routing.util.DisasterBufferComparator} class.
 *
 * Created by Britta Heymann on 22.07.2017.
 */
public class DisasterBufferComparatorTest {
    private static final double AGE_BELOW_THRESHOLD = DisasterRouterTestUtils.AGE_THRESHOLD - 0.1;
    private static final int HOP_COUNT_BELOW_THRESHOLD = DisasterRouterTestUtils.HOP_THRESHOLD - 1;

    /** The allowed delta when comparing doubles for equality. */
    private static final double DOUBLE_COMPARISON_DELTA = 0.0001;

    private static final String UNEXPECTED_COMPARISON_RESULT = "Expected different comparison result.";

    private TestUtils testUtils = new TestUtils(new ArrayList<>(), new ArrayList<>(), new TestSettings());
    private SimClock clock = SimClock.getInstance();

    private DTNHost host;
    private DTNHost neighbor;
    private DisasterBufferComparator comparator;

    public DisasterBufferComparatorTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    public void setUp() {
        TestSettings s = new TestSettings();
        DisasterRouterTestUtils.addDisasterRouterSettings(s);
        this.testUtils.setMessageRouterProto(new DisasterRouter(s));
        this.host = this.testUtils.createHost();
        this.neighbor = this.testUtils.createHost();
        this.comparator = new DisasterBufferComparator(this.host.getRouter());
    }

    @After
    public void cleanUp() {
        DTNHost.reset();
        SimClock.reset();
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsOnMissingAgeThreshold() {
        TestSettings s = new TestSettings();
        s.setNameSpace(DisasterBufferComparator.DISASTER_BUFFER_NS);
        s.putSetting(DisasterBufferComparator.HOP_THRESHOLD_S, Integer.toString(DisasterRouterTestUtils.HOP_THRESHOLD));
        s.restoreNameSpace();
        new DisasterBufferComparator(this.host.getRouter());
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsOnMissingHopThreshold() {
        TestSettings s = new TestSettings();
        s.setNameSpace(DisasterBufferComparator.DISASTER_BUFFER_NS);
        s.putSetting(DisasterBufferComparator.AGE_THRESHOLD_S, Double.toString(DisasterRouterTestUtils.AGE_THRESHOLD));
        new DisasterBufferComparator(this.host.getRouter());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsOnMissingRouter() {
        new DisasterBufferComparator(null);
    }

    @Test
    public void testGetAgeThreshold() {
        Assert.assertEquals("Expected different age threshold.",
                DisasterRouterTestUtils.AGE_THRESHOLD, this.comparator.getAgeThreshold(), DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetHopThreshold() {
        Assert.assertEquals("Expected different hop threshold.",
                DisasterRouterTestUtils.HOP_THRESHOLD, this.comparator.getHopThreshold(), DOUBLE_COMPARISON_DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCopyConstructorThrowsOnMissingRouter() {
        new DisasterBufferComparator(null);
    }

    @Test
    public void testCopyConstructorCopiesThresholds() {
        DisasterBufferComparator copy =
                new DisasterBufferComparator(this.comparator, (DisasterRouter)this.host.getRouter());
        Assert.assertEquals("Expected different age threshold.",
                this.comparator.getAgeThreshold(), copy.getAgeThreshold(),
                DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals("Expected different hop threshold.",
                this.comparator.getHopThreshold(), copy.getHopThreshold());
    }

    /**
     * Checks that if two messages are given:
     * - M1 with age in buffer 0 and hop count {@link #HOP_COUNT_BELOW_THRESHOLD} and
     * - M2 with age in buffer {@link #AGE_BELOW_THRESHOLD} and hop count 0,
     * the one with the higher hop count (= M1) should be deleted first.
     */
    @Test
    public void testHighRankMessagesAreFirstOrderedByHopCount() {
        this.clock.setTime(AGE_BELOW_THRESHOLD);

        // Create two messages to a neighbor.
        Message messageNewInBuffer = new Message(this.host, this.neighbor, "M1", 0);
        Message messageWithLowHopCount = new Message(this.host, this.neighbor, "M2", 0);
        this.host.createNewMessage(messageNewInBuffer);
        this.host.createNewMessage(messageWithLowHopCount);

        // Increase age in buffer for one message, hop count for the other one.
        this.increaseHopCount(messageNewInBuffer, HOP_COUNT_BELOW_THRESHOLD);
        messageWithLowHopCount.setReceiveTime(0);

        // Check comparison values: Message with higher hop count should be deleted first.
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.comparator.compare(messageNewInBuffer, messageWithLowHopCount) < 0);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.comparator.compare(messageWithLowHopCount, messageNewInBuffer) > 0);
    }

    /**
     * Checks that if two messages are given:
     * - M1 with age in buffer 1 and hop count 0 and
     * - M2 with age in buffer 0 and hop count 0,
     * the one with the higher age (= M1) should be deleted first.
     */
    @Test
    public void testHighRankMessagesWithSameHopCountAreOrderedByAge() {
        this.clock.setTime(AGE_BELOW_THRESHOLD);

        // Create two messages to a neighbor.
        Message oldMessage = new Message(this.host, this.neighbor, "M1", 0);
        Message newMessage = new Message(this.host, this.neighbor, "M2", 0);
        this.host.createNewMessage(oldMessage);
        this.host.createNewMessage(newMessage);

        // Increase age for one message.
        oldMessage.setReceiveTime(AGE_BELOW_THRESHOLD - 1);

        // Check comparison values: Message with higher age should be deleted first.
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.comparator.compare(oldMessage, newMessage) < 0);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.comparator.compare(newMessage, oldMessage) > 0);
    }

    /**
     * Checks that if two messages are given:
     * - M1 with age in buffer {@link DisasterRouterTestUtils#AGE_THRESHOLD} and hop count 0 and
     * - M2 with age in buffer 0 and hop count 1,
     * the one with age above the age threshold (= M1) should be deleted first.
     */
    @Test
    public void testHighAgeLeadsToFasterDeletion() {
        this.clock.setTime(DisasterRouterTestUtils.AGE_THRESHOLD);

        // Create two messages to a neighbor.
        Message oldMessage = new Message(this.host, this.neighbor, "M1", 0);
        Message newMessage = new Message(this.host, this.neighbor, "M2", 0);
        this.host.createNewMessage(oldMessage);
        this.host.createNewMessage(newMessage);

        // Increase age for one message and hop count for the other.
        oldMessage.setReceiveTime(0);
        this.increaseHopCount(newMessage, 1);

        // Check comparison values: Message with higher age should be deleted first.
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.comparator.compare(oldMessage, newMessage) < 0);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.comparator.compare(newMessage, oldMessage) > 0);
    }

    /**
     * Checks that if two low rank messages are given:
     * - M1 with age in buffer 0, hop count {@link DisasterRouterTestUtils#HOP_THRESHOLD} and delivery predictability 0,
     *   and
     * - M2 with age in buffer {@link DisasterRouterTestUtils#AGE_THRESHOLD}, hop count
     *   {@link DisasterRouterTestUtils#HOP_THRESHOLD} and delivery predictability
     *   {@link DisasterRouterTestUtils#SUMMAND},
     * the one with less delivery predictability (= M1) should be deleted first.
     */
    @Test
    public void testLowRankMessagesAreSortedByDeliveryPredictability() {
        this.clock.setTime(DisasterRouterTestUtils.AGE_THRESHOLD);

        // Make sure the hosts knows a neighbor.
        this.host.forceConnection(this.neighbor, null, true);

        // Create two messages to different neighbors, one of them known.
        Message messageToUnknownHost = new Message(this.host, this.testUtils.createHost(), "M1", 0);
        Message messageToKnownNeighbor = new Message(this.host, this.neighbor, "M2", 0);
        this.host.createNewMessage(messageToUnknownHost);
        this.host.createNewMessage(messageToKnownNeighbor);

        // Set the hop counts s.t. both messages are low rank.
        this.increaseHopCount(messageToKnownNeighbor, DisasterRouterTestUtils.HOP_THRESHOLD);
        this.increaseHopCount(messageToUnknownHost, DisasterRouterTestUtils.HOP_THRESHOLD);

        // The message to the known neighbor should have been in buffer for a longer time.
        messageToKnownNeighbor.setReceiveTime(0);

        // Check comparison values: Message to unknown host should be deleted first.
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.comparator.compare(messageToUnknownHost, messageToKnownNeighbor) < 0);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.comparator.compare(messageToKnownNeighbor, messageToUnknownHost) > 0);
    }

    /**
     * Checks that a low rank broadcast message with a replications density of ~0.33 is sorted between two low rank
     * one to one messages with replications densities ~0.68 and 0.
     */
    @Test
    public void testLowRankBroadcastsAreSortedByReplicationsDensity() {
        this.clock.setTime(DisasterRouterTestUtils.AGE_THRESHOLD);

        // Make sure the hosts knows a neighbor.
        this.host.forceConnection(this.neighbor, null, true);

        // Create three messages, one of them to a known host, one of them to an unknown host, and one broadcast.
        Message messageToUnknownHost = new Message(this.host, this.testUtils.createHost(), "M1", 0);
        Message messageToKnownNeighbor = new Message(this.host, this.neighbor, "M2", 0);
        Message broadcast = new BroadcastMessage(this.host, "B1", 0);
        this.host.createNewMessage(messageToUnknownHost);
        this.host.createNewMessage(messageToKnownNeighbor);
        this.host.createNewMessage(broadcast);

        // Update replications density of broadcast message: 1 in 3 should have it.
        DTNHost hostWithBroadcast = this.testUtils.createHost();
        hostWithBroadcast.createNewMessage(broadcast);
        this.host.forceConnection(this.testUtils.createHost(), null, true);
        this.host.forceConnection(hostWithBroadcast, null, true);
        this.host.forceConnection(this.neighbor, null, true);
        this.clock.advance(DisasterRouterTestUtils.RD_WINDOW_LENGTH);
        this.host.update(true);

        // Set age in buffer s.t. all messages are low rank.
        messageToKnownNeighbor.setReceiveTime(0);
        messageToUnknownHost.setReceiveTime(0);
        broadcast.setReceiveTime(0);

        // Check comparison values wrt to broadcast: As broadcast has a replications density of ~0.33, it should
        // be deleted before the message to the known host, but after the message to the unknown host.
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.comparator.compare(broadcast, messageToKnownNeighbor) < 0);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.comparator.compare(messageToKnownNeighbor, broadcast) > 0);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.comparator.compare(messageToUnknownHost, broadcast) < 0);
        Assert.assertTrue(
                UNEXPECTED_COMPARISON_RESULT,
                this.comparator.compare(broadcast, messageToUnknownHost) > 0);
    }

    /**
     * Checks that the return value of {@link DisasterBufferComparator#compare(Message, Message)} may change if the
     * underlying values change.
     * This test is interesting because we cache some values and have to make sure that the cache gets invalidated.
     */
    @Test
    public void testComparisonReactsToNewInformation() {
        this.clock.setTime(DisasterRouterTestUtils.AGE_THRESHOLD);

        // Make sure the host knows a neighbor.
        this.host.forceConnection(this.neighbor, null, true);

        // Create two messages to different neighbors, one of them known.
        DTNHost unknownHost = this.testUtils.createHost();
        Message m1 = new Message(this.host, unknownHost, "M1", 0);
        Message m2 = new Message(this.host, this.neighbor, "M2", 0);
        this.host.createNewMessage(m1);
        this.host.createNewMessage(m2);

        // Set the hop counts s.t. both messages are low rank.
        this.increaseHopCount(m2, DisasterRouterTestUtils.HOP_THRESHOLD);
        this.increaseHopCount(m1, DisasterRouterTestUtils.HOP_THRESHOLD);

        // Check comparison value.
        Assert.assertTrue(UNEXPECTED_COMPARISON_RESULT, this.comparator.compare(m1, m2) < 0);

        // Increase simulator time and update delivery predictabilities.
        this.clock.advance(DisasterRouterTestUtils.DP_WINDOW_LENGTH);
        this.host.update(true);

        // Then meet the up to now unknown host.
        this.host.forceConnection(unknownHost, null, true);

        // The comparison return value should now have flipped.
        Assert.assertTrue(UNEXPECTED_COMPARISON_RESULT, this.comparator.compare(m1, m2) > 0);
    }

    /**
     * Increases the provided message's hop count by the provided value.
     * @param m Message to increase the hop count for.
     * @param increase The number of hops to add.
     */
    private void increaseHopCount(Message m, int increase) {
        for (int i = 0; i < increase; i++) {
            m.addNodeOnPath(this.testUtils.createHost());
        }
    }
}
