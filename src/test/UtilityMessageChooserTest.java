package test;

import applications.DatabaseApplication;
import core.Connection;
import core.DTNHost;
import core.DisasterData;
import core.Message;
import core.SettingsError;
import core.SimClock;
import org.junit.Assert;
import org.junit.Test;
import routing.DisasterRouter;
import routing.EpidemicRouter;
import routing.MessageChoosingStrategy;
import routing.MessageRouter;
import routing.choosers.UtilityMessageChooser;
import routing.util.DatabaseApplicationUtil;
import routing.util.EnergyModel;
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
public class UtilityMessageChooserTest extends AbstractMessageChoosingStrategyTest {
    /* Some values needed in tests. */
    private static final double NEGATIVE_VALUE = -0.1;
    private static final double OPPOSITE_OF_NEGATIVE_VALUE = 0.1;
    private static final double VALUE_ABOVE_ONE = 1.1;
    private static final double QUITE_LOW_REPLICATIONS_DENSITY = 0.4;
    private static final double MEDIUM_REPLICATIONS_DENSITY = 0.5;
    private static final double MEDIUM_ENERGY_VALUE = 0.5;
    private static final int ONE_HUNDRED_HOSTS = 100;

    /* Error messages. */
    private static final String UNEXPECTED_WEIGHT = "Expected different weight.";

    public UtilityMessageChooserTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Override
    protected void addNecessarySettings() {
        DisasterRouterTestUtils.addDisasterRouterSettings(this.settings);
    }

    @Override
    protected MessageRouter createMessageRouterPrototype() {
        MessageRouter routerProto = new DisasterRouter(this.settings);
        routerProto.addApplication(new DatabaseApplication(this.settings));
        return routerProto;
    }

    @Override
    protected MessageChoosingStrategy createMessageChooser() {
        return new UtilityMessageChooser(this.attachedHost.getRouter());
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
    public void testConstructorThrowsForNegativeUtilityThreshold() {
        this.settings.setNameSpace(UtilityMessageChooser.UTILITY_MESSAGE_CHOOSER_NS);
        this.settings.putSetting(UtilityMessageChooser.UTILITY_THRESHOLD, Double.toString(NEGATIVE_VALUE));
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForUtilityThresholdAbove1() {
        this.settings.setNameSpace(UtilityMessageChooser.UTILITY_MESSAGE_CHOOSER_NS);
        this.settings.putSetting(UtilityMessageChooser.UTILITY_THRESHOLD, Double.toString(VALUE_ABOVE_ONE));
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForNegativePowerThreshold() {
        this.settings.setNameSpace(UtilityMessageChooser.UTILITY_MESSAGE_CHOOSER_NS);
        this.settings.putSetting(UtilityMessageChooser.POWER_THRESHOLD, Double.toString(NEGATIVE_VALUE));
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForPowerThresholdAbove1() {
        this.settings.setNameSpace(UtilityMessageChooser.UTILITY_MESSAGE_CHOOSER_NS);
        this.settings.putSetting(UtilityMessageChooser.POWER_THRESHOLD, Double.toString(VALUE_ABOVE_ONE));
        new UtilityMessageChooser(new DisasterRouter(this.settings));
    }

    @Test
    public void testGetDeliveryPredictabilityWeight() {
        Assert.assertEquals(UNEXPECTED_WEIGHT,
                DisasterRouterTestUtils.DELIVERY_PREDICTABILITY_WEIGHT * DisasterRouterTestUtils.PROPHET_PLUS_WEIGHT,
                this.getChooser().getDeliveryPredictabilityWeight(),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetPowerWeight() {
        Assert.assertEquals(UNEXPECTED_WEIGHT,
                DisasterRouterTestUtils.POWER_WEIGHT * DisasterRouterTestUtils.PROPHET_PLUS_WEIGHT,
                this.getChooser().getPowerWeight(),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetReplicationsDensityWeight() {
        Assert.assertEquals(UNEXPECTED_WEIGHT,
                DisasterRouterTestUtils.REPLICATIONS_DENSITY_WEIGHT, this.getChooser().getReplicationsDensityWeight(),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetEncounterValueWeight() {
        Assert.assertEquals(UNEXPECTED_WEIGHT,
                DisasterRouterTestUtils.ENCOUNTER_VALUE_WEIGHT, this.getChooser().getEncounterValueWeight(),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetUtilityThreshold() {
        Assert.assertEquals("Expected different threshold.",
                DisasterRouterTestUtils.UTILITY_THRESHOLD, this.getChooser().getUtilityThreshold(),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetPowerThreshold() {
        Assert.assertEquals("Expected different threshold.",
                DisasterRouterTestUtils.POWER_THRESHOLD, this.getChooser().getPowerThreshold(),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplicateThrowsForMissingRouter() {
        this.chooser.replicate(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplicateThrowsForNonDisasterRouter() {
        this.chooser.replicate(new EpidemicRouter(this.settings));
    }

    @Override
    @Test
    public void testReplicateReturnsCorrectType() {
        Assert.assertTrue(
                "Replicate should not change chooser type.",
                this.chooser.replicate(new DisasterRouter(this.settings)) instanceof UtilityMessageChooser);
    }

    @Override
    @Test
    public void testReplicateCopiesSettings() {
        // Create other router with different settings.
        TestSettings otherRouterSettings = new TestSettings();
        DisasterRouterTestUtils.addDisasterRouterSettings(otherRouterSettings);
        otherRouterSettings.setNameSpace(UtilityMessageChooser.UTILITY_MESSAGE_CHOOSER_NS);
        otherRouterSettings.putSetting(UtilityMessageChooser.UTILITY_THRESHOLD, "0.8");
        otherRouterSettings.putSetting(UtilityMessageChooser.POWER_THRESHOLD, "0.6");
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
                this.getChooser().getReplicationsDensityWeight(), copy.getReplicationsDensityWeight(),
                DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                UNEXPECTED_WEIGHT,
                this.getChooser().getDeliveryPredictabilityWeight(), copy.getDeliveryPredictabilityWeight(),
                DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                UNEXPECTED_WEIGHT,
                this.getChooser().getEncounterValueWeight(), copy.getEncounterValueWeight(),
                DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                UNEXPECTED_WEIGHT,
                this.getChooser().getPowerWeight(), copy.getPowerWeight(),
                DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals("Expected different utility threshold.",
                this.getChooser().getUtilityThreshold(), copy.getUtilityThreshold(),
                DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals("Expected different power threshold.",
                this.getChooser().getPowerThreshold(), copy.getPowerThreshold(),
                DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Checks {@link UtilityMessageChooser#chooseNonDirectMessages(Collection, List)} returns data messages for all
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

        // Call chooseNonDirectMessages with two connections.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, neighbor1));
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, neighbor2));
        Collection<Tuple<Message, Connection>> messages =
                this.chooser.chooseNonDirectMessages(new ArrayList<>(), connections);

        // Check data message has been returned for both neighbors.
        String idForDataMessage = "D" + Arrays.asList(data).hashCode();
        Assert.assertEquals(UNEXPECTED_NUMBER_OF_CHOSEN_MESSAGES, TWO_MESSAGES, messages.size());
        Assert.assertTrue(
                "Data message to first neighbor expected.",
                this.messageToHostsExists(messages, idForDataMessage, neighbor1));
        Assert.assertTrue(
                "Data message to second neighbor expected.",
                this.messageToHostsExists(messages, idForDataMessage, neighbor2));
    }

    /**
     * Checks that {@link UtilityMessageChooser#chooseNonDirectMessages(Collection, List)} does not return any
     * (message, connection) tuples for which the receiving host does not have sufficient power right now.
     */
    @Test
    public void testFindOtherMessagesDoesNotReturnMessagesForLowPowerRouter() {
        // Make sure neighbor 1 has low power.
        this.neighbor1.getComBus().updateProperty(
                EnergyModel.ENERGY_VALUE_ID, DisasterRouterTestUtils.POWER_THRESHOLD - SMALL_POWER_DIFFERENCE);

        // Give a data item and a message to our host.
        DisasterData data = new DisasterData(
                DisasterData.DataType.MARKER, 0, SimClock.getTime(), this.attachedHost.getLocation());
        DatabaseApplication app = DatabaseApplicationUtil.findDatabaseApplication(this.attachedHost.getRouter());
        app.update(this.attachedHost);
        app.disasterDataCreated(this.attachedHost, data);
        Message m = new Message(this.attachedHost, this.utils.createHost(), "M1", 0);
        this.attachedHost.createNewMessage(m);

        // Call chooseNonDirectMessages with connections to two hosts, one of them with low power.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, this.neighbor1));
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, this.neighbor2));
        Collection<Tuple<Message, Connection>> messages =
                this.chooser.chooseNonDirectMessages(Collections.singletonList(m), connections);

        // Make sure only the host with high power got the messages.
        String idForDataMessage = "D" + Arrays.asList(data).hashCode();
        Assert.assertEquals(UNEXPECTED_NUMBER_OF_CHOSEN_MESSAGES, TWO_MESSAGES, messages.size());
        Assert.assertFalse("Host with low power should not get messages.",
                this.messageToHostsExists(messages, m.getId(), neighbor1));
        Assert.assertFalse("Host with low power should not get messages.",
                this.messageToHostsExists(messages, idForDataMessage, neighbor1));
        Assert.assertTrue("Message to other neighbor expected.",
                this.messageToHostsExists(messages, m.getId(), this.neighbor2));
        Assert.assertTrue("Data message to other neighbor expected.",
                this.messageToHostsExists(messages, idForDataMessage, this.neighbor2));
    }

    @Test
    public void testDeliveryPredictabilityInfluencesMessageChoosing() {
        // Prepare two messages with high replications density (shouldn't be sent on that alone).
        Message lowDeliveryPredMessage = new Message(this.attachedHost, this.utils.createHost(), "M1", 0);
        Message highDeliveryPredMessage = new Message(this.attachedHost, this.neighbor2, "M2", 0);
        this.createMessagesWithReplicationsDensityOf(1, lowDeliveryPredMessage, highDeliveryPredMessage);

        // Make sure neighbor 1 knows the receiver of one of the messages.
        this.neighbor1.forceConnection(highDeliveryPredMessage.getTo(), null, true);

        // Call chooseNonDirectMessages with the messages to neighbor 1.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, this.neighbor1));
        Collection<Tuple<Message, Connection>> messages = this.chooser.chooseNonDirectMessages(
                Arrays.asList(lowDeliveryPredMessage, highDeliveryPredMessage), connections);

        // Check only the one whose receiver neighbor 1 knows is returned.
        Assert.assertEquals(UNEXPECTED_NUMBER_OF_CHOSEN_MESSAGES, 1, messages.size());
        Assert.assertFalse(
                "Popular message with no chance of delivery should not be returned.",
                this.messageToHostsExists(messages, lowDeliveryPredMessage.getId(), this.neighbor1));
        Assert.assertTrue(
                "Message with high delivery predictability should be returned.",
                this.messageToHostsExists(messages, highDeliveryPredMessage.getId(), this.neighbor1));
    }

    @Test
    public void testRemainingPowerInfluencesMessageChoosing() {
        // Prepare a message with replications density above 0 (shouldn't be sent on that alone).
        Message m = new Message(this.attachedHost, this.utils.createHost(), "M1", 0);
        this.createMessagesWithReplicationsDensityOf(MEDIUM_REPLICATIONS_DENSITY, m);

        // Make sure neighbor 1 has low power.
        this.neighbor1.getComBus().updateProperty(EnergyModel.ENERGY_VALUE_ID, MEDIUM_ENERGY_VALUE);

        // Call chooseNonDirectMessages with two connections, one of them to neighbor 1.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, this.neighbor1));
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, this.neighbor2));
        Collection<Tuple<Message, Connection>> messages =
                this.chooser.chooseNonDirectMessages(Collections.singletonList(m), connections);

        // Check the message is only send to the neighbor with full power.
        Assert.assertEquals(UNEXPECTED_NUMBER_OF_CHOSEN_MESSAGES, 1, messages.size());
        Assert.assertFalse(
                "Message should not be returned for host with lower battery.",
                this.messageToHostsExists(messages, m.getId(), this.neighbor1));
        Assert.assertTrue(
                "Message should be returned for host with full battery.",
                this.messageToHostsExists(messages, m.getId(), this.neighbor2));
    }

    @Test
    public void testReplicationsDensityInfluencesMessageChoosing() {
        // Prepare one message with low and one message with high replications density.
        Message lowReplicationsDensityMessage = new Message(this.attachedHost, this.utils.createHost(), "M2", 0);
        Message highReplicationsDensityMessage = new Message(this.attachedHost, this.utils.createHost(), "M1", 0);
        this.createMessagesWithReplicationsDensityOf(0, lowReplicationsDensityMessage);
        this.createMessagesWithReplicationsDensityOf(1, highReplicationsDensityMessage);

        // Call chooseNonDirectMessages with the messages to neighbor 1.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, this.neighbor1));
        Collection<Tuple<Message, Connection>> messages = this.chooser.chooseNonDirectMessages(
                Arrays.asList(highReplicationsDensityMessage, lowReplicationsDensityMessage), connections);

        // Check only the one with low replications density is returned.
        Assert.assertEquals(UNEXPECTED_NUMBER_OF_CHOSEN_MESSAGES, 1, messages.size());
        Assert.assertFalse(
                "Message with high replications density should not be returned.",
                this.messageToHostsExists(messages, highReplicationsDensityMessage.getId(), this.neighbor1));
        Assert.assertTrue(
                "Message with low replications density should be returned.",
                this.messageToHostsExists(messages, lowReplicationsDensityMessage.getId(), this.neighbor1));
    }

    @Test
    public void testEncounterValueRatioInfluencesMessageChoosing() {
        // Make sure neighbor 2 and the attached host have a positive encounter value.
        this.neighbor2.forceConnection(this.attachedHost, null, true);
        this.neighbor2.forceConnection(this.attachedHost, null, false);
        this.clock.advance(DisasterRouterTestUtils.EV_WINDOW_LENGTH);
        this.neighbor2.update(true);
        this.attachedHost.update(true);

        // Prepare a message with replications density above 0 (shouldn't be sent on that alone).
        Message m = new Message(this.attachedHost, this.utils.createHost(), "M1", 0);
        this.createMessagesWithReplicationsDensityOf(QUITE_LOW_REPLICATIONS_DENSITY, m);

        // Call chooseNonDirectMessages with two connections, one of them to neighbor 2.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, this.neighbor1));
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, this.neighbor2));
        Collection<Tuple<Message, Connection>> messages =
                this.chooser.chooseNonDirectMessages(Collections.singletonList(m), connections);

        // Check the message is only send to the equally social neighbor (2).
        Assert.assertEquals(UNEXPECTED_NUMBER_OF_CHOSEN_MESSAGES, 1, messages.size());
        Assert.assertFalse(
                "Message should not be returned for less social host.",
                this.messageToHostsExists(messages, m.getId(), this.neighbor1));
        Assert.assertTrue(
                "Message should be returned for equally social host.",
                this.messageToHostsExists(messages, m.getId(), this.neighbor2));
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
     * Adds the provided messages to {@link #attachedHost}'s buffer and makes sure that at the current time, they all
     * have a replications density that is less than 0.01 different from the provided density.
     *
     * @param replicationsDensity Density to set.
     * @param messages Messages to add to buffer.
     */
    private void createMessagesWithReplicationsDensityOf(double replicationsDensity, Message... messages) {
        // Create 100 neighbors to meet.
        DTNHost[] hostsToMeet = new DTNHost[ONE_HUNDRED_HOSTS];
        for (int i = 0; i < hostsToMeet.length; i++) {
            hostsToMeet[i] = this.utils.createHost();
        }

        // Make sure the correct percentage knows the message.
        int numHostsKnowingMessage = (int)(replicationsDensity * ONE_HUNDRED_HOSTS);
        for (Message m : messages) {
            this.attachedHost.createNewMessage(m);
            for (int i = 0; i < numHostsKnowingMessage; i++) {
                hostsToMeet[i].createNewMessage(m);
            }
        }

        // Meet all the hosts.
        for (DTNHost hostToMeet : hostsToMeet) {
            this.attachedHost.forceConnection(hostToMeet, null, true);
            this.attachedHost.forceConnection(hostToMeet, null, false);
        }

        // Update replications density.
        this.clock.advance(DisasterRouterTestUtils.RD_WINDOW_LENGTH);
        this.attachedHost.update(true);
    }

    private UtilityMessageChooser getChooser() {
        return (UtilityMessageChooser)this.chooser;
    }
}
