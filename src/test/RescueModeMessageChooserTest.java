package test;

import applications.DatabaseApplication;
import core.Connection;
import core.Coord;
import core.DTNHost;
import core.DisasterData;
import core.Message;
import core.SettingsError;
import core.SimClock;
import org.junit.Assert;
import org.junit.Test;
import routing.DisasterRouter;
import routing.MessageChoosingStrategy;
import routing.MessageRouter;
import routing.choosers.RescueModeMessageChooser;
import routing.util.DatabaseApplicationUtil;
import routing.util.EnergyModel;
import util.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Contains tests for the {@link RescueModeMessageChooser} class.
 *
 * Created by Britta Heymann on 26.07.2017.
 */
public class RescueModeMessageChooserTest extends AbstractMessageChoosingStrategyTest {
    /* Some values needed in tests. */
    private static final double NEGATIVE_VALUE = -0.1;
    private static final double VALUE_ABOVE_ONE = 1.1;
    private static final Coord FAR_AWAY = new Coord(30_000, 4_000);
    private static final double SHORT_TIMESPAN = 0.1;

    public RescueModeMessageChooserTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    /**
     * Adds all necessary settings to the settings object {@link #settings}.
     */
    @Override
    protected void addNecessarySettings() {
        DisasterRouterTestUtils.addDisasterRouterSettings(this.settings);
    }

    /**
     * Creates the router to use for all hosts.
     *
     * @return A prototype of the router.
     */
    @Override
    protected MessageRouter createMessageRouterPrototype() {
        MessageRouter routerProto = new DisasterRouter(this.settings);
        routerProto.addApplication(new DatabaseApplication(this.settings));
        return routerProto;
    }

    /**
     * Creates the message chooser to test.
     *
     * @return The chooser to test.
     */
    @Override
    protected MessageChoosingStrategy createMessageChooser() {
        return new RescueModeMessageChooser();
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForNegativePowerThreshold() {
        this.settings.setNameSpace(RescueModeMessageChooser.RESCUE_MODE_MESSAGE_CHOOSER_NS);
        this.settings.putSetting(RescueModeMessageChooser.POWER_THRESHOLD, Double.toString(NEGATIVE_VALUE));
        this.settings.restoreNameSpace();

        new RescueModeMessageChooser();
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForPowerThresholdAbove1() {
        this.settings.setNameSpace(RescueModeMessageChooser.RESCUE_MODE_MESSAGE_CHOOSER_NS);
        this.settings.putSetting(RescueModeMessageChooser.POWER_THRESHOLD, Double.toString(VALUE_ABOVE_ONE));
        this.settings.restoreNameSpace();

        new RescueModeMessageChooser();
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForNegativeShortTimeSpan() {
        this.settings.setNameSpace(RescueModeMessageChooser.RESCUE_MODE_MESSAGE_CHOOSER_NS);
        this.settings.putSetting(RescueModeMessageChooser.SHORT_TIMESPAN_THRESHOLD, Double.toString(NEGATIVE_VALUE));
        this.settings.restoreNameSpace();

        new RescueModeMessageChooser();
    }

    @Test
    public void testGetPowerThreshold() {
        Assert.assertEquals("Expected different power threshold.",
                DisasterRouterTestUtils.POWER_THRESHOLD,
                this.getChooser().getPowerThreshold(), DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetShortTimespanThreshold() {
        Assert.assertEquals("Expected different short timespan threshold.",
                DisasterRouterTestUtils.SHORT_TIMESPAN_THRESHOLD, this.getChooser().getShortTimespanThreshold(),
                DOUBLE_COMPARISON_DELTA);
    }


    /**
     * Checks that {@link MessageChoosingStrategy#replicate(MessageRouter)} returns a message choosing strategy of the
     * correct type.
     */
    @Override
    public void testReplicateReturnsCorrectType() {
        MessageChoosingStrategy copy = this.chooser.replicate(this.attachedHost.getRouter());
        Assert.assertTrue("Copy is of wrong class.", copy instanceof RescueModeMessageChooser);
    }

    /**
     * Checks that {@link MessageChoosingStrategy#replicate(MessageRouter)} copies all settings.
     */
    @Override
    public void testReplicateCopiesSettings() {
        MessageChoosingStrategy copy = this.chooser.replicate(this.attachedHost.getRouter());
        Assert.assertEquals("Expected different power threshold.",
                this.getChooser().getPowerThreshold(), ((RescueModeMessageChooser)copy).getPowerThreshold(),
                DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals("Expected different short timespan threshold.",
                this.getChooser().getShortTimespanThreshold(),
                ((RescueModeMessageChooser)copy).getShortTimespanThreshold(),
                DOUBLE_COMPARISON_DELTA);
    }

    /***
     * Checks that {@link RescueModeMessageChooser#chooseNonDirectMessages(Collection, List)} does not return any
     * (message, connection) tuples for which the receiving host does not have sufficient power right now.
     */
    @Test
    public void testChooseNonDirectMessagesDoesNotReturnMessagesForLowPowerRouter() {
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

    /**
     * Checks that each ordinary message is sent to every neighbor who is neither busy (transferring) nor has low
     * energy.
     */
    @Test
    public void testChooseNonDirectMessagesReturnsAllMessagesForAllAcceptingConnections() {
        // Create a message...
        Message m = new Message(this.attachedHost, this.utils.createHost(), "M1", 0);
        this.attachedHost.createNewMessage(m);

        // ...with high replications density (--> not that important to send).
        DTNHost hostWithMessage = this.utils.createHost();
        hostWithMessage.createNewMessage(m);
        this.attachedHost.forceConnection(hostWithMessage, null, true);
        this.attachedHost.forceConnection(hostWithMessage, null, false);
        this.clock.advance(DisasterRouterTestUtils.RD_WINDOW_LENGTH);
        this.attachedHost.update(true);

        // Call chooseNonDirectMessages with connections to two hosts.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, this.neighbor1));
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, this.neighbor2));
        Collection<Tuple<Message, Connection>> messages =
                this.chooser.chooseNonDirectMessages(Collections.singletonList(m), connections);

        // Check non-direct messages are returned for each connection.
        Assert.assertTrue("Expected the one-to-one message to be sent to all neighbors.",
                this.messageToHostsExists(messages, m.getId(), neighbor1));
        Assert.assertTrue("Expected the one-to-one message to be sent to all neighbors.",
                this.messageToHostsExists(messages, m.getId(), neighbor2));
    }

    /**
     * Checks that {@link RescueModeMessageChooser#chooseNonDirectMessages(Collection, List)} only chooses useful data.
     */
    @Test
    public void testChooseNonDirectMessagesOnlyChoosesUsefulData() {
        // Create two data items, one of them useful.
        this.attachedHost.setLocation(FAR_AWAY);
        DisasterData usefulData = new DisasterData(
                DisasterData.DataType.MARKER, 0, SimClock.getTime(), this.attachedHost.getLocation());
        DisasterData uselessData = new DisasterData(
                DisasterData.DataType.MARKER, 0, SimClock.getTime(), new Coord(0, 0));
        DatabaseApplication app = DatabaseApplicationUtil.findDatabaseApplication(this.attachedHost.getRouter());
        app.update(this.attachedHost);
        app.disasterDataCreated(this.attachedHost, usefulData);
        app.disasterDataCreated(this.attachedHost, uselessData);

        // Call chooseNonDirectMessages with connection to one host.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, this.neighbor1));
        Collection<Tuple<Message, Connection>> messages =
                this.chooser.chooseNonDirectMessages(new ArrayList<>(), connections);

        // Check only useful data item is returned.
        String idForData = "D" + Arrays.asList(usefulData).hashCode();
        Assert.assertTrue("Expected the correct data to be returned.",
                this.messageToHostsExists(messages, idForData, neighbor1));
    }

    /**
     * Checks that {@link RescueModeMessageChooser#chooseNonDirectMessages(Collection, List)} only chooses data which
     * has been modified recently.
     */
    @Test
    public void testChooseNonDirectMessagesOnlyChoosesRecentData() {
        // Create two data items, one of them modified recently.
        this.clock.setTime(DisasterRouterTestUtils.SHORT_TIMESPAN_THRESHOLD + SHORT_TIMESPAN);
        DisasterData recentData = new DisasterData(
                DisasterData.DataType.MARKER, 0, SimClock.getTime(), this.attachedHost.getLocation());
        DisasterData oldData = new DisasterData(
                DisasterData.DataType.MARKER, 0, 0, this.attachedHost.getLocation());
        DatabaseApplication app = DatabaseApplicationUtil.findDatabaseApplication(this.attachedHost.getRouter());
        app.update(this.attachedHost);
        app.disasterDataCreated(this.attachedHost, recentData);
        app.disasterDataCreated(this.attachedHost, oldData);

        // Call chooseNonDirectMessages with connection to one host.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, this.neighbor1));
        Collection<Tuple<Message, Connection>> messages =
                this.chooser.chooseNonDirectMessages(new ArrayList<>(), connections);

        // Check only recent data item is returned.
        String idForData = "D" + Arrays.asList(recentData).hashCode();
        Assert.assertTrue("Expected the correct data to be returned.",
                this.messageToHostsExists(messages, idForData, neighbor1));
    }

    /**
     * Checks that each data message is sent to every neighbor (who is neither busy (transferring) nor has low energy).
     */
    @Test
    public void testChooseNonDirectMessagesReturnsDataMessagesForAllAcceptingConnections() {
        // Create a useful data item.
        DisasterData usefulData = new DisasterData(
                DisasterData.DataType.MARKER, 0, SimClock.getTime(), this.attachedHost.getLocation());
        DatabaseApplication app = DatabaseApplicationUtil.findDatabaseApplication(this.attachedHost.getRouter());
        app.update(this.attachedHost);
        app.disasterDataCreated(this.attachedHost, usefulData);

        // Create two connections.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, this.neighbor1));
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, this.neighbor2));
        Collection<Tuple<Message, Connection>> messages =
                this.chooser.chooseNonDirectMessages(new ArrayList<>(), connections);

        // Check data item is sent to both hosts.
        String idForData = "D" + Arrays.asList(usefulData).hashCode();
        Assert.assertTrue("Expected the data to be returned for every neighbor.",
                this.messageToHostsExists(messages, idForData, neighbor1));
        Assert.assertTrue("Expected the data to be returned for every neighbor.",
                this.messageToHostsExists(messages, idForData, neighbor2));
    }

    /**
     * Returns {@link #chooser} as a {@link RescueModeMessageChooser}.
     * @return The chooser we are testing.
     */
    private RescueModeMessageChooser getChooser() {
        return (RescueModeMessageChooser)this.chooser;
    }
}
