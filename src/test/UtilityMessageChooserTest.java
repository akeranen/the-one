package test;

import applications.DatabaseApplication;
import core.CBRConnection;
import core.Connection;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.Message;
import core.SettingsError;
import core.SimClock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import routing.DisasterRouter;
import routing.EpidemicRouter;
import routing.MessageRouter;
import routing.choosers.UtilityMessageChooser;
import routing.util.DatabaseApplicationUtil;
import util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Contains tests for the {@link routing.choosers.UtilityMessageChooser} class.
 *
 * Created by Britta Heymann on 29.06.2017.
 */
public class UtilityMessageChooserTest {
    private static final double NEGATIVE_VALUE = -0.1;
    private static final double OPPOSITE_OF_NEGATIVE_VALUE = 0.1;
    private static final double VALUE_ABOVE_ONE = 1.1;

    private static final String UNEXPECTED_WEIGHT = "Expected different weight.";

    private static final double DOUBLE_COMPARISON_DELTA = 0.00001;

    private TestSettings settings;
    private UtilityMessageChooser chooser;
    private DTNHost attachedHost;
    private DTNHost neighbor1;
    private DTNHost neighbor2;

    public UtilityMessageChooserTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    public void setUp() {
        this.settings = new TestSettings();
        DisasterRouterTestUtils.addDisasterRouterSettings(this.settings);

        TestUtils utils = new TestUtils(new ArrayList<>(), new ArrayList<>(), this.settings);
        MessageRouter routerProto = new DisasterRouter(this.settings);
        routerProto.addApplication(new DatabaseApplication(this.settings));
        utils.setMessageRouterProto(routerProto);
        this.attachedHost = utils.createHost();
        this.neighbor1 = utils.createHost();
        this.neighbor2 = utils.createHost();

        this.chooser = new UtilityMessageChooser(this.attachedHost.getRouter());
        this.chooser.setAttachedHost(this.attachedHost);

        this.settings.setNameSpace(UtilityMessageChooser.UTILITY_MESSAGE_CHOOSER_NS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsOnMissingRouter() {
        new UtilityMessageChooser(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsOnNonDisasterRouter() {
        new UtilityMessageChooser(new EpidemicRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsOnNegativeProphetPlusWeight() {
        // Set negative weight.
        this.settings.putSetting(UtilityMessageChooser.PROPHET_PLUS_WEIGHT, Double.toString(NEGATIVE_VALUE));
        // Make sure weights still add up to 1.
        increaseWeight(this.settings, UtilityMessageChooser.REPLICATIONS_DENSITY_WEIGHT, OPPOSITE_OF_NEGATIVE_VALUE);
        // Check SettingsError is thrown.
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsOnNegativeDeliveryPredictabilityWeight() {
        // Set negative weight.
        this.settings.putSetting(
                UtilityMessageChooser.DELIVERY_PREDICTABILITY_WEIGHT, Double.toString(NEGATIVE_VALUE));
        // Make sure weights still add up to 1.
        increaseWeight(this.settings, UtilityMessageChooser.POWER_WEIGHT, OPPOSITE_OF_NEGATIVE_VALUE);
        // Check SettingsError is thrown.
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsOnNegativePowerWeight() {
        // Set negative weight.
        this.settings.putSetting(UtilityMessageChooser.POWER_WEIGHT, Double.toString(NEGATIVE_VALUE));
        // Make sure weights still add up to 1.
        increaseWeight(this.settings, UtilityMessageChooser.DELIVERY_PREDICTABILITY_WEIGHT, OPPOSITE_OF_NEGATIVE_VALUE);
        // Check SettingsError is thrown.
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsOnNegativeReplicationsDensityWeight() {
        // Set negative weight.
        this.settings.putSetting(UtilityMessageChooser.REPLICATIONS_DENSITY_WEIGHT, Double.toString(NEGATIVE_VALUE));
        // Make sure weights still add up to 1.
        increaseWeight(this.settings, UtilityMessageChooser.PROPHET_PLUS_WEIGHT, OPPOSITE_OF_NEGATIVE_VALUE);
        // Check SettingsError is thrown.
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsOnNegativeEncounterValueWeight() {
        // Set negative weight.
        this.settings.putSetting(UtilityMessageChooser.ENCOUNTER_VALUE_WEIGHT, Double.toString(NEGATIVE_VALUE));
        // Make sure weights still add up to 1.
        increaseWeight(this.settings, UtilityMessageChooser.PROPHET_PLUS_WEIGHT, OPPOSITE_OF_NEGATIVE_VALUE);
        // Check SettingsError is thrown.
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForWeightNotAddingTo1() {
        increaseWeight(this.settings, UtilityMessageChooser.POWER_WEIGHT, OPPOSITE_OF_NEGATIVE_VALUE);
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForNegativeThreshold() {
        this.settings.putSetting(UtilityMessageChooser.UTILITY_THRESHOLD, Double.toString(NEGATIVE_VALUE));
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForThresholdAbove1() {
        this.settings.putSetting(UtilityMessageChooser.UTILITY_THRESHOLD, Double.toString(VALUE_ABOVE_ONE));
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test
    public void testGetDeliveryPredictabilityWeight() {
        Assert.assertEquals(UNEXPECTED_WEIGHT,
                DisasterRouterTestUtils.DELIVERY_PREDICTABILITY_WEIGHT * DisasterRouterTestUtils.PROPHET_PLUS_WEIGHT,
                this.chooser.getDeliveryPredictabilityWeight(),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetPowerWeight() {
        Assert.assertEquals(UNEXPECTED_WEIGHT,
                DisasterRouterTestUtils.POWER_WEIGHT * DisasterRouterTestUtils.PROPHET_PLUS_WEIGHT,
                this.chooser.getPowerWeight(),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetReplicationsDensityWeight() {
        Assert.assertEquals(UNEXPECTED_WEIGHT,
                DisasterRouterTestUtils.REPLICATIONS_DENSITY_WEIGHT, this.chooser.getReplicationsDensityWeight(),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetEncounterValueWeight() {
        Assert.assertEquals(UNEXPECTED_WEIGHT,
                DisasterRouterTestUtils.ENCOUNTER_VALUE_WEIGHT, this.chooser.getEncounterValueWeight(),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetUtilityThreshold() {
        Assert.assertEquals("Expected different threshold.",
                DisasterRouterTestUtils.UTILITY_THRESHOLD, this.chooser.getUtilityThreshold(), DOUBLE_COMPARISON_DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplicateThrowsForMissingRouter() {
        this.chooser.replicate(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplicateThrowsForNonDisasterRouter() {
        this.chooser.replicate(new EpidemicRouter(this.settings));
    }

    @Test
    public void testReplicateReturnsUtilityMessageChooser() {
        Assert.assertTrue(
                "Replicate should not change chooser type.",
                this.chooser.replicate(new DisasterRouter(this.settings)) instanceof UtilityMessageChooser);
    }

    @Test
    public void testReplicateCopiesSettings() {
        // Create other router with different settings.
        TestSettings otherRouterSettings = new TestSettings();
        DisasterRouterTestUtils.addDisasterRouterSettings(otherRouterSettings);
        otherRouterSettings.setNameSpace(UtilityMessageChooser.UTILITY_MESSAGE_CHOOSER_NS);
        otherRouterSettings.putSetting(UtilityMessageChooser.UTILITY_THRESHOLD, "0.8");
        increaseWeight(otherRouterSettings, UtilityMessageChooser.DELIVERY_PREDICTABILITY_WEIGHT, NEGATIVE_VALUE);
        increaseWeight(otherRouterSettings, UtilityMessageChooser.POWER_WEIGHT, OPPOSITE_OF_NEGATIVE_VALUE);
        increaseWeight(otherRouterSettings, UtilityMessageChooser.REPLICATIONS_DENSITY_WEIGHT, NEGATIVE_VALUE);
        increaseWeight(otherRouterSettings, UtilityMessageChooser.ENCOUNTER_VALUE_WEIGHT, OPPOSITE_OF_NEGATIVE_VALUE);
        DisasterRouter otherRouter = new DisasterRouter(otherRouterSettings);

        // Replicate chooser.
        UtilityMessageChooser copy = (UtilityMessageChooser)this.chooser.replicate(otherRouter);

        // Check chooser has old settings.
        Assert.assertEquals(
                UNEXPECTED_WEIGHT,
                this.chooser.getReplicationsDensityWeight(), copy.getReplicationsDensityWeight(),
                DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                UNEXPECTED_WEIGHT,
                this.chooser.getDeliveryPredictabilityWeight(), copy.getDeliveryPredictabilityWeight(),
                DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                UNEXPECTED_WEIGHT,
                this.chooser.getEncounterValueWeight(), copy.getEncounterValueWeight(),
                DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                UNEXPECTED_WEIGHT,
                this.chooser.getPowerWeight(), copy.getPowerWeight(),
                DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals("Expected different utility threshold.",
                this.chooser.getUtilityThreshold(), copy.getUtilityThreshold(),
                DOUBLE_COMPARISON_DELTA);
    }

    // check router is set correctly
    // Check setAttachedhost works correclty
    // check router is set correctly (replicate)
    // check messages with low utility are not sent. Influenced by: other's delivery pred, other's power,
    // message's replications density, encountervalue ratio, utility threshold

    /**
     * Checks {@link UtilityMessageChooser#findOtherMessages(Collection, List)} returns data messages for all
     * connections.
     */
    @Test
    public void testFindOtherMessagesReturnsDataMessagesForAllConnections() {
        // Add data item to host.
        DisasterData data = new DisasterData(
                DisasterData.DataType.MARKER, 0, SimClock.getTime(), this.attachedHost.getLocation());
        DatabaseApplication app = DatabaseApplicationUtil.findDatabaseApplication(this.attachedHost.getRouter());
        app.update(this.attachedHost);
        app.disasterDataCreated(this.attachedHost, data);

        // Call findOtherMessages with two connections.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, neighbor1));
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, neighbor2));
        Collection<Tuple<Message, Connection>> messages =
                this.chooser.findOtherMessages(new ArrayList<>(), connections);

        // Check data message has been returned for both neighbors.
        Assert.assertEquals("Expected different number of chosen messages.", 2, messages.size());
        Assert.assertTrue(
                "Data message to first neighbor expected.",
                this.messageToHostsExists(messages, data.toString(), neighbor1));
        Assert.assertTrue(
                "Data message to second neighbor expected.",
                this.messageToHostsExists(messages, data.toString(), neighbor2));
    }

    /**
     * Checks that {@link UtilityMessageChooser#findOtherMessages(Collection, List)} does not return any
     * (message, connection) tuples for which the receiving host would be a final recipient of the message.
     */
    @Test
    public void testFindOtherMessagesDoesNotReturnDirectMessages() {

    }

    /**
     * Checks that {@link UtilityMessageChooser#findOtherMessages(Collection, List)} does not return any
     * (message, connection) tuples for which the receiving host already knows the message.
     */
    @Test
    public void testFindOtherMessagesDoesNotReturnKnownMessages() {

    }

    /**
     * Checks that {@link UtilityMessageChooser#findOtherMessages(Collection, List)} does not return any
     * (message, connection) tuples for which the receiving host is transferring right now.
     */
    @Test
    public void testFindOtherMessagesDoesNotReturnMessagesForTransferringRouter() {

    }

    /**
     * Increases the double setting addressed by the provided key by the provided difference.
     * @param settings The settings to change.
     * @param key The double setting to increase.
     * @param difference The value to increase it by.
     */
    private static void increaseWeight(TestSettings settings, String key, double difference) {
        settings.putSetting(key, Double.toString(settings.getDouble(key) + difference));
    }

    /**
     * Creates a {@link Connection} object.
     * @return The created connection object.
     */
    private static Connection createConnection(DTNHost from, DTNHost to) {
        return new CBRConnection(from, from.getInterfaces().get(0), to, to.getInterfaces().get(0), 1);
    }

    private boolean messageToHostsExists(Collection<Tuple<Message, Connection>> messages, String id, DTNHost host) {
        boolean messageFound = false;
        for (Tuple<Message, Connection> tuple : messages) {
            if (tuple.getKey().getId().equals(id) && tuple.getValue().getOtherNode(this.attachedHost).equals(host)) {
                messageFound = true;
            }
        }
        return messageFound;
    }
}
