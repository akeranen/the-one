package test;

import core.BroadcastMessage;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.SettingsError;
import core.SimClock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import routing.prioritizers.DisasterPrioritization;
import routing.util.DeliveryPredictabilityStorage;
import routing.util.ReplicationsDensityManager;
import util.Tuple;

import java.util.ArrayList;

/**
 * Contains tests for the {@link routing.prioritizers.DisasterPrioritization} class.
 *
 * Created by Britta Heymann on 25.05.2017.
 */
public class DisasterPrioritizationTest {
    /** The default window length for the used {@link ReplicationsDensityManager}. */
    private static final double WINDOW_LENGTH = 21.3;

    /** The delivery predictability weight for the {@link DisasterPrioritization} used in most test. */
    private static final double DP_WEIGHT = 0.8;

    private static final double WEIGHT_GREATER_ONE = 1.1;

    /** The allowed delta when comparing doubles for equality. */
    private static final double DOUBLE_COMPARISON_DELTA = 0.0001;

    private TestUtils testUtils = new TestUtils(new ArrayList<>(), new ArrayList<>(), new TestSettings());
    private SimClock clock = SimClock.getInstance();

    private DTNHost host = this.testUtils.createHost();

    DeliveryPredictabilityStorage dpStorage =
            DeliveryPredictabilityStorageTest.createDeliveryPredictabilityStorage(this.host);
    ReplicationsDensityManager rdManager =
            ReplicationsDensityManagerTest.createReplicationsDensityManager(WINDOW_LENGTH);
    private DisasterPrioritization prioritization =
            DisasterPrioritizationTest.createDisasterPrioritization(DP_WEIGHT, this.dpStorage, this.rdManager);

    @After
    public void cleanUp() {
        DTNHost.reset();
        SimClock.reset();
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsOnMissingWeight() {
        new DisasterPrioritization(new TestSettings(), this.dpStorage, this.rdManager);
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
    public void testConstructorThrowsOnMissingDeliveryPredictabilies() {
        DisasterPrioritizationTest.createDisasterPrioritization(DP_WEIGHT, null, this.rdManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsOnMissingReplicationsDensities() {
        DisasterPrioritizationTest.createDisasterPrioritization(DP_WEIGHT, this.dpStorage, null);
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
    public void testCopyConstructorThrowsOnMissingDeliveryPredictabilities() {
        new DisasterPrioritization(this.prioritization, null, this.rdManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCopyConstructorThrowsOnMissingReplicationsDensities() {
        new DisasterPrioritization(this.prioritization, this.dpStorage, null);
    }

    @Test
    public void testCopyConstructorCopiesWeights() {
        DisasterPrioritization copy = new DisasterPrioritization(this.prioritization, this.dpStorage, this.rdManager);
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

    @Test
    public void testLowReplicationsDensityLeadsToEarlierSending() {
        // Create two messages.
        Message knownMessage = new Message(this.host, this.testUtils.createHost(), "M1", 0);
        Message unknownMessage = new Message(this.host, this.testUtils.createHost(), "M2", 0);

        // One should be known by a neighbor.
        DTNHost neighbor = this.testUtils.createHost();
        neighbor.createNewMessage(knownMessage);

        // Update replications densities.
        this.rdManager.addMessage(knownMessage.getId());
        this.rdManager.addMessage(unknownMessage.getId());
        this.rdManager.addEncounter(neighbor);
        this.clock.setTime(WINDOW_LENGTH);
        this.rdManager.update();

        // Check comparison values.
        Assert.assertTrue(
                "Expected different comparison result.",
                this.prioritization.compare(this.toTuple(knownMessage), this.toTuple(unknownMessage)) > 0);
        Assert.assertTrue(
                "Expected different comparison result.",
                this.prioritization.compare(this.toTuple(unknownMessage), this.toTuple(knownMessage)) < 0);

    }

    /**
     * Creates an instance of the {@link DisasterPrioritization} class attached to the provided host using the provided
     * weight.
     * @param weight Weight for delivery predictability.
     * @param host The host this prioritization is attached to.
     * @return The created {@link DisasterPrioritization} instance.
     */
    private static DisasterPrioritization createDisasterPrioritization(double weight, DTNHost host) {
        DeliveryPredictabilityStorage dpStorage =
                DeliveryPredictabilityStorageTest.createDeliveryPredictabilityStorage(host);
        ReplicationsDensityManager rdManager =
                ReplicationsDensityManagerTest.createReplicationsDensityManager(WINDOW_LENGTH);
        return DisasterPrioritizationTest.createDisasterPrioritization(weight, dpStorage, rdManager);
    }

    /**
     * Creates an instance of the {@link DisasterPrioritization} class using the provided parameters.
     * @param weight Weight for delivery predictability.
     * @param deliveryPredictabilities Storage of delivery predictabilities to use.
     * @param replicationsDensities Replications densities to use.
     * @return The created {@link DisasterPrioritization} instance.
     */
    private static DisasterPrioritization createDisasterPrioritization(
            double weight,
            DeliveryPredictabilityStorage deliveryPredictabilities,
            ReplicationsDensityManager replicationsDensities) {
        TestSettings settings = new TestSettings();
        settings.putSetting(DisasterPrioritization.DELIVERY_PREDICTABILITY_WEIGHT, Double.toString(weight));
        return new DisasterPrioritization(settings, deliveryPredictabilities, replicationsDensities);
    }

    private Tuple<Message, Connection> toTuple(Message message) {
        return new Tuple<>(
                message,
                DisasterPrioritizationTest.createConnection(this.testUtils.createHost(), this.testUtils.createHost()));
    }

    /**
     * Connects two hosts and returns their connection.
     * @param from Host initiating connection.
     * @param to Host to connect to.
     * @return The new connection.
     */
    private static Connection createConnection(DTNHost from, DTNHost to) {
        from.forceConnection(to, null, true);
        for (Connection con : from.getConnections()) {
            if (con.getOtherNode(from).equals(to)) {
                return con;
            }
        }
        throw new IllegalStateException("This should never happen!");
    }
}
