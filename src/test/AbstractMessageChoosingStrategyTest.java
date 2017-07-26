package test;

import applications.DatabaseApplication;
import core.BroadcastMessage;
import core.CBRConnection;
import core.Connection;
import core.DTNHost;
import core.DisasterData;
import core.Message;
import core.SimClock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import routing.MessageChoosingStrategy;
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
 * Base class for all test classes testing classes implementing {@link routing.MessageChoosingStrategy}.
 *
 * Created by Britta Heymann on 26.07.2017.
 */
public abstract class AbstractMessageChoosingStrategyTest {
    /** Some values needed in tests. */
    protected static final int TWO_MESSAGES = 2;

    /** Error messages. */
    protected static final String UNEXPECTED_NUMBER_OF_CHOSEN_MESSAGES =
            "Expected different number of chosen messages.";

    /** The maximum delta when comparing for double equality. */
    protected static final double DOUBLE_COMPARISON_DELTA = 0.00001;

    protected TestUtils utils;
    protected SimClock clock = SimClock.getInstance();

    protected TestSettings settings;
    protected MessageChoosingStrategy chooser;
    protected DTNHost attachedHost;

    protected DTNHost neighbor1;
    protected DTNHost neighbor2;

    @Before
    public void setUp() {
        this.settings = new TestSettings();
        this.addNecessarySettings();

        this.utils = new TestUtils(new ArrayList<>(), new ArrayList<>(), this.settings);
        this.utils.setMessageRouterProto(this.createMessageRouterPrototype());

        this.attachedHost = this.utils.createHost();
        this.neighbor1 = this.utils.createHost();
        this.neighbor2 = this.utils.createHost();
        this.attachedHost.update(true);
        this.neighbor1.update(true);
        this.neighbor2.update(true);

        this.chooser = this.createMessageChooser();
        this.chooser.setAttachedHost(this.attachedHost);
    }

    @After
    public void cleanUp() {
        SimClock.reset();
        DTNHost.reset();
    }

    /**
     * Adds all necessary settings to the settings object {@link #settings}.
     */
    protected abstract void addNecessarySettings();

    /**
     * Creates the router to use for all hosts.
     * @return A prototype of the router.
     */
    protected abstract MessageRouter createMessageRouterPrototype();

    /**
     * Creates the message chooser to test.
     * @return The chooser to test.
     */
    protected abstract MessageChoosingStrategy createMessageChooser();

    /**
     * Checks that {@link MessageChoosingStrategy#replicate(MessageRouter)} returns a message choosing strategy of the
     * correct type.
     */
    @Test
    public abstract void testReplicateReturnsCorrectType();

    /**
     * Checks that {@link MessageChoosingStrategy#replicate(MessageRouter)} copies all settings.
     */
    @Test
    public abstract void testReplicateCopiesSettings();

    /**
     * Checks that {@link MessageChoosingStrategy#chooseNonDirectMessages(Collection, List)} does not return any
     * (message, connection) tuples for which the receiving host would be a final recipient of the message.
     */
    @Test
    public void testChooseNonDirectMessagesDoesNotReturnDirectMessages() {
        // Create message to neighbor 1.
        Message m = new Message(this.attachedHost, neighbor1, "M1", 0);
        this.attachedHost.createNewMessage(m);

        // Call chooseNonDirectMessages with two connections, one of them to neighbor 1.
        List<Connection> connections = new ArrayList<>();
        connections.add(AbstractMessageChoosingStrategyTest.createConnection(this.attachedHost, neighbor1));
        connections.add(AbstractMessageChoosingStrategyTest.createConnection(this.attachedHost, neighbor2));
        Collection<Tuple<Message, Connection>> messages =
                this.chooser.chooseNonDirectMessages(Collections.singletonList(m), connections);

        // Make sure the direct message was not returned.
        Assert.assertEquals(UNEXPECTED_NUMBER_OF_CHOSEN_MESSAGES, 1, messages.size());
        Assert.assertFalse(
                "Direct message should not have been returned.",
                this.messageToHostsExists(messages, m.getId(), neighbor1));
        Assert.assertTrue(
                "Message to second neighbor expected.", this.messageToHostsExists(messages, m.getId(), neighbor2));
    }

    /**
     * Checks that {@link MessageChoosingStrategy#chooseNonDirectMessages(Collection, List)} does not return any
     * (message, connection) tuples for which the receiving host already knows the message.
     */
    @Test
    public void testChooseNonDirectMessagesDoesNotReturnKnownMessages() {
        // Create message which is known by neighbor 1.
        Message m = new Message(this.attachedHost, this.utils.createHost(), "M1", 0);
        this.attachedHost.createNewMessage(m);
        this.neighbor1.createNewMessage(m);

        // Call chooseNonDirectMessages with two connections, one of them to neighbor 1.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, neighbor1));
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, neighbor2));
        Collection<Tuple<Message, Connection>> messages =
                this.chooser.chooseNonDirectMessages(Collections.singletonList(m), connections);

        // Make sure the known message was not returned.
        Assert.assertEquals(UNEXPECTED_NUMBER_OF_CHOSEN_MESSAGES, 1, messages.size());
        Assert.assertFalse(
                "Known message should not have been returned.",
                this.messageToHostsExists(messages, m.getId(), neighbor1));
        Assert.assertTrue(
                "Message to second neighbor expected.", this.messageToHostsExists(messages, m.getId(), neighbor2));
    }

    /**
     * Checks that {@link MessageChoosingStrategy#chooseNonDirectMessages(Collection, List)} does not return any
     * (message, connection) tuples for which the receiving host is transferring right now.
     */
    @Test
    public void testChooseNonDirectMessagesDoesNotReturnMessagesForTransferringRouter() {
        // Make sure neighbor 1 is transferring to neighbor 2.
        Message directMessage = new BroadcastMessage(this.neighbor1, "M1", 0);
        this.neighbor1.createNewMessage(directMessage);
        this.neighbor1.forceConnection(neighbor2, null, true);
        this.neighbor1.update(true);
        this.neighbor2.update(true);

        // Take a look at a non-transferring host for verification.
        DTNHost otherHost = this.utils.createHost();

        // Give a data item and a message to our host.
        DisasterData data = new DisasterData(
                DisasterData.DataType.MARKER, 0, SimClock.getTime(), this.attachedHost.getLocation());
        DatabaseApplication app = DatabaseApplicationUtil.findDatabaseApplication(this.attachedHost.getRouter());
        app.update(this.attachedHost);
        app.disasterDataCreated(this.attachedHost, data);
        Message m = new Message(this.attachedHost, this.utils.createHost(), "M1", 0);
        this.attachedHost.createNewMessage(m);

        // Call chooseNonDirectMessages with connections to all other three hosts.
        List<Connection> connections = new ArrayList<>();
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, neighbor1));
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, neighbor2));
        connections.add(UtilityMessageChooserTest.createConnection(this.attachedHost, otherHost));
        Collection<Tuple<Message, Connection>> messages =
                this.chooser.chooseNonDirectMessages(Collections.singletonList(m), connections);

        // Make sure only the non-transferring host got the messages.
        String idForDataMessage = "D" + Arrays.asList(data).hashCode();
        Assert.assertEquals(UNEXPECTED_NUMBER_OF_CHOSEN_MESSAGES, TWO_MESSAGES, messages.size());
        Assert.assertFalse("Host which initiated a transfer should not get messages.",
                this.messageToHostsExists(messages, m.getId(), neighbor1));
        Assert.assertFalse("Host which initiated a transfer should not get data.",
                this.messageToHostsExists(messages, idForDataMessage, neighbor1));
        Assert.assertFalse("Host in a transfer should not get messages.",
                this.messageToHostsExists(messages, m.getId(), neighbor2));
        Assert.assertFalse("Host in a transfer should not get data.",
                this.messageToHostsExists(messages, idForDataMessage, neighbor2));
        Assert.assertTrue("Message to other neighbor expected.",
                this.messageToHostsExists(messages, m.getId(), otherHost));
        Assert.assertTrue("Data message to other neighbor expected.",
                this.messageToHostsExists(messages, idForDataMessage, otherHost));
    }

    /**
     * Creates a {@link Connection} object.
     * @return The created connection object.
     */
    protected static Connection createConnection(DTNHost from, DTNHost to) {
        return new CBRConnection(from, from.getInterfaces().get(0), to, to.getInterfaces().get(0), 1);
    }

    /**
     * Checks the provided message-connection tuple list for the existence of a tuple mapping a message with the
     * provided ID to a connection where the host which is not {@link #attachedHost} is the provided host.
     *
     * @param messages List to check.
     * @param id Message ID to look for.
     * @param host Host to look for.
     * @return True if such a message can be found.
     */
    protected boolean messageToHostsExists(Collection<Tuple<Message, Connection>> messages, String id, DTNHost host) {
        for (Tuple<Message, Connection> tuple : messages) {
            if (tuple.getKey().getId().equals(id) && tuple.getValue().getOtherNode(this.attachedHost).equals(host)) {
                return true;
            }
        }
        return false;
    }
}
