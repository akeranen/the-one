package test;

import applications.DatabaseApplication;
import core.BroadcastMessage;
import core.CBRConnection;
import core.Connection;
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
import routing.EpidemicRouter;
import routing.MessageRouter;
import routing.choosers.UtilityMessageChooser;
import routing.util.DatabaseApplicationUtil;
import util.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
    private TestUtils utils;
    private DTNHost attachedHost;
    private DTNHost neighbor1;
    private DTNHost neighbor2;

    private SimClock clock = SimClock.getInstance();

    public UtilityMessageChooserTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    public void setUp() {
        this.settings = new TestSettings();
        DisasterRouterTestUtils.addDisasterRouterSettings(this.settings);

        this.utils = new TestUtils(new ArrayList<>(), new ArrayList<>(), this.settings);
        MessageRouter routerProto = new DisasterRouter(this.settings);
        routerProto.addApplication(new DatabaseApplication(this.settings));
        this.utils.setMessageRouterProto(routerProto);

        this.attachedHost = this.utils.createHost();
        this.neighbor1 = this.utils.createHost();
        this.neighbor2 = this.utils.createHost();

        this.chooser = new UtilityMessageChooser(this.attachedHost.getRouter());
        this.chooser.setAttachedHost(this.attachedHost);
    }

    @After
    public void cleanUp() {
        SimClock.reset();
        DTNHost.reset();
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
        this.settings.setNameSpace(UtilityMessageChooser.UTILITY_MESSAGE_CHOOSER_NS);
        this.settings.putSetting(UtilityMessageChooser.PROPHET_PLUS_WEIGHT, Double.toString(NEGATIVE_VALUE));
        // Make sure weights still add up to 1.
        increaseWeight(this.settings, UtilityMessageChooser.REPLICATIONS_DENSITY_WEIGHT, OPPOSITE_OF_NEGATIVE_VALUE);
        // Check SettingsError is thrown.
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsOnNegativeDeliveryPredictabilityWeight() {
        // Set negative weight.
        this.settings.setNameSpace(UtilityMessageChooser.UTILITY_MESSAGE_CHOOSER_NS);
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
        this.settings.setNameSpace(UtilityMessageChooser.UTILITY_MESSAGE_CHOOSER_NS);
        this.settings.putSetting(UtilityMessageChooser.POWER_WEIGHT, Double.toString(NEGATIVE_VALUE));
        // Make sure weights still add up to 1.
        increaseWeight(this.settings, UtilityMessageChooser.DELIVERY_PREDICTABILITY_WEIGHT, OPPOSITE_OF_NEGATIVE_VALUE);
        // Check SettingsError is thrown.
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsOnNegativeReplicationsDensityWeight() {
        // Set negative weight.
        this.settings.setNameSpace(UtilityMessageChooser.UTILITY_MESSAGE_CHOOSER_NS);
        this.settings.putSetting(UtilityMessageChooser.REPLICATIONS_DENSITY_WEIGHT, Double.toString(NEGATIVE_VALUE));
        // Make sure weights still add up to 1.
        increaseWeight(this.settings, UtilityMessageChooser.PROPHET_PLUS_WEIGHT, OPPOSITE_OF_NEGATIVE_VALUE);
        // Check SettingsError is thrown.
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsOnNegativeEncounterValueWeight() {
        // Set negative weight.
        this.settings.setNameSpace(UtilityMessageChooser.UTILITY_MESSAGE_CHOOSER_NS);
        this.settings.putSetting(UtilityMessageChooser.ENCOUNTER_VALUE_WEIGHT, Double.toString(NEGATIVE_VALUE));
        // Make sure weights still add up to 1.
        increaseWeight(this.settings, UtilityMessageChooser.PROPHET_PLUS_WEIGHT, OPPOSITE_OF_NEGATIVE_VALUE);
        // Check SettingsError is thrown.
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForWeightNotAddingTo1() {
        this.settings.setNameSpace(UtilityMessageChooser.UTILITY_MESSAGE_CHOOSER_NS);
        increaseWeight(this.settings, UtilityMessageChooser.POWER_WEIGHT, OPPOSITE_OF_NEGATIVE_VALUE);
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForNegativeThreshold() {
        this.settings.setNameSpace(UtilityMessageChooser.UTILITY_MESSAGE_CHOOSER_NS);
        this.settings.putSetting(UtilityMessageChooser.UTILITY_THRESHOLD, Double.toString(NEGATIVE_VALUE));
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForThresholdAbove1() {
        this.settings.setNameSpace(UtilityMessageChooser.UTILITY_MESSAGE_CHOOSER_NS);
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
        // Create message to neighbor 1.
        Message m = new Message(this.attachedHost, neighbor1, "M1", 0);
        this.attachedHost.createNewMessage(m);

        // Call findOtherMessages with two connections, one of them to neighbor 1.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, neighbor1));
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, neighbor2));
        Collection<Tuple<Message, Connection>> messages =
                this.chooser.findOtherMessages(Collections.singletonList(m), connections);

        // Make sure the direct message was not returned.
        Assert.assertEquals("Expected different number of chosen messages.", 1, messages.size());
        Assert.assertFalse(
                "Direct message should not have been returned.",
                this.messageToHostsExists(messages, m.getId(), neighbor1));
        Assert.assertTrue(
                "Message to second neighbor expected.", this.messageToHostsExists(messages, m.getId(), neighbor2));
    }

    /**
     * Checks that {@link UtilityMessageChooser#findOtherMessages(Collection, List)} does not return any
     * (message, connection) tuples for which the receiving host already knows the message.
     */
    @Test
    public void testFindOtherMessagesDoesNotReturnKnownMessages() {
        // Create message which is known by neighbor 1.
        Message m = new Message(this.attachedHost, this.utils.createHost(), "M1", 0);
        this.attachedHost.createNewMessage(m);
        this.neighbor1.createNewMessage(m);

        // Call findOtherMessages with two connections, one of them to neighbor 1.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, neighbor1));
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, neighbor2));
        Collection<Tuple<Message, Connection>> messages =
                this.chooser.findOtherMessages(Collections.singletonList(m), connections);

        // Make sure the known message was not returned.
        Assert.assertEquals("Expected different number of chosen messages.", 1, messages.size());
        Assert.assertFalse(
                "Known message should not have been returned.",
                this.messageToHostsExists(messages, m.getId(), neighbor1));
        Assert.assertTrue(
                "Message to second neighbor expected.", this.messageToHostsExists(messages, m.getId(), neighbor2));
    }

    /**
     * Checks that {@link UtilityMessageChooser#findOtherMessages(Collection, List)} does not return any
     * (message, connection) tuples for which the receiving host is transferring right now.
     */
    @Test
    public void testFindOtherMessagesDoesNotReturnMessagesForTransferringRouter() {
        // Make sure neighbor 1 is transferring to neighbor 2.
        Message directMessage = new BroadcastMessage(this.neighbor1, "M1", 0);
        this.neighbor1.createNewMessage(directMessage);
        this.neighbor1.forceConnection(neighbor2, null, true);
        this.neighbor1.update(true);
        this.neighbor2.update(true);

        // Take a look at a non-transferring host for verification.
        DTNHost otherHost = this.utils.createHost();

        // Give a message to our host.
        Message m = new Message(this.attachedHost, this.utils.createHost(), "M1", 0);
        this.attachedHost.createNewMessage(m);

        // Call findOtherMessages with connections to all other three hosts.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, neighbor1));
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, neighbor2));
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, otherHost));
        Collection<Tuple<Message, Connection>> messages =
                this.chooser.findOtherMessages(Collections.singletonList(m), connections);

        // Make sure only the non-transferring host got the message.
        Assert.assertEquals("Expected different number of chosen messages.", 1, messages.size());
        Assert.assertFalse(
                "Host which initiated a transfer should not get messages.",
                this.messageToHostsExists(messages, m.getId(), neighbor1));
        Assert.assertFalse(
                "Host in a transfer should not get messages.",
                this.messageToHostsExists(messages, m.getId(), neighbor2));
        Assert.assertTrue(
                "Message to other neighbor expected.", this.messageToHostsExists(messages, m.getId(), otherHost));
    }

    @Test
    public void testDeliveryPredictabilityInfluencesMessageChoosing() {
        // Prepare two messages with high replications density (shouldn't be sent on that alone).
        Message lowDeliveryPredMessage = new Message(this.attachedHost, this.utils.createHost(), "M1", 0);
        Message highDeliveryPredMessage = new Message(this.attachedHost, this.neighbor2, "M2", 0);
        this.createMessagesWithHighReplicationsDensity(lowDeliveryPredMessage, highDeliveryPredMessage);

        // Make sure neighbor 1 knows the receiver of one of the messages.
        this.neighbor1.forceConnection(highDeliveryPredMessage.getTo(), null, true);

        // Call findOtherMessages with the messages to neighbor 1.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, this.neighbor1));
        Collection<Tuple<Message, Connection>> messages = this.chooser.findOtherMessages(
                Arrays.asList(lowDeliveryPredMessage, highDeliveryPredMessage), connections);

        // Check only the one whose receiver neighbor 1 knows is returned.
        Assert.assertEquals("Expected different number of chosen messages.", 1, messages.size());
        Assert.assertFalse(
                "Popular message with no chance of delivery should not be returned.",
                this.messageToHostsExists(messages, lowDeliveryPredMessage.getId(), this.neighbor1));
        Assert.assertTrue(
                "Message with high delivery predictability should be returned.",
                this.messageToHostsExists(messages, highDeliveryPredMessage.getId(), this.neighbor1));

    }

    @Test
    public void testRemainingPowerInfluencesMessageChoosing() {

    }

    @Test
    public void testReplicationsDensityInfluencesMessageChoosing() {

    }

    @Test
    public void testEncounterValueRatioInfluencesMessageChoosing() {

    }

    @Test
    public void testUtilityThresholdInfluencesMessageChoosing() {

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

    private void createMessagesWithHighReplicationsDensity(Message... messages) {
        DTNHost hostKnowningMessages = this.utils.createHost();
        for (Message m : messages) {
            this.attachedHost.createNewMessage(m);
            hostKnowningMessages.createNewMessage(m);
        }
        this.attachedHost.forceConnection(hostKnowningMessages, null, true);
        this.clock.advance(DisasterRouterTestUtils.RD_WINDOW_LENGTH);
        this.attachedHost.update(true);
    }
}
