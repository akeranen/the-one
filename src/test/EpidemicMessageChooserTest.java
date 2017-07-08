package test;

import applications.DatabaseApplication;
import core.Application;
import core.BroadcastMessage;
import core.Connection;
import core.Coord;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.Group;
import core.Message;
import core.MulticastMessage;
import core.SimClock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import routing.choosers.EpidemicMessageChooser;
import util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Contains tests for the {@link EpidemicMessageChooser} class.
 *
 * Created by Britta Heymann on 21.05.2017.
 */
public class EpidemicMessageChooserTest {
    /* Constants needed to initialize DB application instances. */
    private static final int DB_SIZE = 200;
    private static final double MAP_SENDING_INTERVAL = 20;
    private static final int ITEMS_PER_MESSAGE = 2;

    private TestUtils testUtils;
    private SimClock clock = SimClock.getInstance();
    private EpidemicMessageChooser messageChooser;
    private DTNHost attachedHost;

    public EpidemicMessageChooserTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    public void setUp() {
        // Enable database application with super-high utility threshold.
        TestSettings settings = new TestSettings();
        settings.putSetting(DatabaseApplication.UTILITY_THRESHOLD, Double.toString(1D));
        settings.putSetting(DatabaseApplication.SIZE_RANDOMIZER_SEED, Integer.toString(0));
        settings.putSetting(
                DatabaseApplication.DATABASE_SIZE_RANGE, String.format("%d,%d", DB_SIZE, DB_SIZE));
        settings.putSetting(DatabaseApplication.MIN_INTERVAL_MAP_SENDING, Double.toString(MAP_SENDING_INTERVAL));
        settings.putSetting(DatabaseApplication.ITEMS_PER_MESSAGE, Integer.toString(ITEMS_PER_MESSAGE));

        // Create host with such an application.
        this.testUtils = new TestUtils(new ArrayList<>(), new ArrayList<>(), settings);
        this.attachedHost = this.testUtils.createHost();
        this.attachedHost.getRouter().addApplication(new DatabaseApplication(settings));
        this.attachedHost.update(true);

        // Create message chooser.
        this.messageChooser = new EpidemicMessageChooser(this.attachedHost);
    }

    @After
    public void cleanUp() {
        DTNHost.reset();
        Group.clearGroups();
        SimClock.reset();
    }

    @Test
    public void testDirectMessagesAreNotChosen() {
        // Create neighbor in same group as host.
        DTNHost neighbor = this.testUtils.createHost();
        Group group = Group.createGroup(0);
        group.addHost(this.attachedHost);
        group.addHost(neighbor);

        // Connect the two hosts.
        this.attachedHost.forceConnection(neighbor, null, true);

        // Create direct messages of all kinds.
        this.attachedHost.createNewMessage(new Message(this.attachedHost, neighbor, "M1", 0));
        this.attachedHost.createNewMessage(new BroadcastMessage(this.attachedHost, "B1", 0));
        this.attachedHost.createNewMessage(new MulticastMessage(this.attachedHost, group, "G1", 0));

        // Make sure they are not chosen.
        Assert.assertTrue("Direct messages should not be chosen.", this.chooseMessages().isEmpty());
    }

    @Test
    public void testChoosesAllNonDirectMessagesForEachConnections() {
        // Create two neighbors.
        DTNHost neighbor1 = this.testUtils.createHost();
        DTNHost neighbor2 = this.testUtils.createHost();

        // Add connections to the host.
        this.attachedHost.forceConnection(neighbor1, null, true);
        this.attachedHost.forceConnection(neighbor2, null, true);

        // Create a group missing one of the neighbors.
        Group group = Group.createGroup(0);
        group.addHost(this.attachedHost);
        group.addHost(neighbor2);

        // Create some messages.
        Message broadcast = new BroadcastMessage(this.attachedHost, "B1", 0);
        Message multicast = new MulticastMessage(this.attachedHost, group, "G1", 0);
        Message oneToOne = new Message(this.attachedHost, neighbor1, "M1", 0);
        this.attachedHost.createNewMessage(broadcast);
        this.attachedHost.createNewMessage(multicast);
        this.attachedHost.createNewMessage(oneToOne);

        // Check non-direct messages were returned for each connection.
        List<Message> messagesToFirstNeighbor = this.filterMessagesForNeighbor(this.chooseMessages(), neighbor1);
        List<Message> messagesToSecondNeighbor = this.filterMessagesForNeighbor(this.chooseMessages(), neighbor2);
        Assert.assertEquals("Expected one message to each neighbor.", 1, messagesToFirstNeighbor.size());
        Assert.assertEquals("Expected one message to each neighbor.", 1, messagesToSecondNeighbor.size());
        Assert.assertEquals("Expected the multicast to be sent to H1.", multicast, messagesToFirstNeighbor.get(0));
        Assert.assertEquals(
                "Expected the one-to-one message to be sent to H2.", oneToOne, messagesToSecondNeighbor.get(0));
    }

    @Test
    public void testChoosesDataMessagesForSufficientUtility() {
        // Create two data items, only one with perfect utility of 1.
        this.clock.advance(1);
        DisasterData perfectUtilityData = new DisasterData(
                DisasterData.DataType.MARKER, 0, SimClock.getTime(), this.attachedHost.getLocation());
        DisasterData lowUtilityData = new DisasterData(DisasterData.DataType.MARKER, 0, 0, new Coord(0, 0));

        // Let both be handled by our host.
        DTNHost dataCreator = this.testUtils.createHost();
        Application app = this.attachedHost.getRouter().getApplications(DatabaseApplication.APP_ID).iterator().next();
        app.handle(new DataMessage(
                dataCreator, this.attachedHost, "D1", Collections.singleton(new Tuple<>(perfectUtilityData, 1D)), 0),
                this.attachedHost);
        app.handle(new DataMessage(
                dataCreator, this.attachedHost, "D2", Collections.singleton(new Tuple<>(lowUtilityData, 0D)), 0),
                this.attachedHost);

        // Check which ones are returned for neighbors.
        DTNHost neighbor1 = this.testUtils.createHost();
        this.attachedHost.forceConnection(neighbor1, null, true);
        Collection<Tuple<Message, Connection>> selected = this.chooseMessages();
        Assert.assertEquals("Only one message should have been returned.", 1, selected.size());
        DataMessage selectedMessage = (DataMessage)selected.iterator().next().getKey();
        Assert.assertEquals("Correct message has been returned.", perfectUtilityData, selectedMessage.getData().get(0));
    }

    /**
     * Calls {@link routing.MessageChoosingStrategy#findOtherMessages(Collection, List)} on the
     * {@link EpidemicMessageChooser}.
     *
     * @return The returned message-connection tuples.
     */
    private Collection<Tuple<Message, Connection>> chooseMessages() {
        return this.messageChooser.findOtherMessages(
                this.attachedHost.getMessageCollection(), this.attachedHost.getConnections());
    }

    /**
     * Filters the provided messages for messages that will be sent to the provided {@link DTNHost}.
     * @param messages Messages to filter.
     * @param neighbor The {@link DTNHost} to filter for.
     * @return The filtered set of messages.
     */
    private List<Message> filterMessagesForNeighbor(Collection<Tuple<Message, Connection>> messages, DTNHost neighbor) {
        List<Message> messagesToNeighbor = new ArrayList<>();
        for (Tuple<Message, Connection> t : messages) {
            DTNHost receiver = t.getValue().getOtherNode(this.attachedHost);
            if (receiver.equals(neighbor)) {
                messagesToNeighbor.add(t.getKey());
            }
        }

        return messagesToNeighbor;
    }
}
