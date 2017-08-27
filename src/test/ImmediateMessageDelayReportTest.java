package test;

import core.BroadcastMessage;
import core.Coord;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.Group;
import core.Message;
import core.MessageListener;
import core.MulticastMessage;
import core.SimClock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import report.ImmediateMessageDelayReport;
import report.Report;
import util.Tuple;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Contains tests for the {@link ImmediateMessageDelayReport} class.
 * Created by Britta Heymann on 15.03.2017.
 */
public class ImmediateMessageDelayReportTest extends AbstractReportTest {
    private static final int AFTER_WARM_UP_TIME = WARM_UP_TIME + 1;

    // Some times in a certain tests. Chosen arbitrarily.
    private static final int CREATION_TIME = AFTER_WARM_UP_TIME + 34;
    private static final int DELIVERY_TIME = CREATION_TIME + 56;

    // A priority used in a test.
    private static final int PRIORITY = 7;

    private static final String TEST_MESSAGE_ID = "M1";

    private static final Coord DATA_LOCATION = new Coord(0,0);

    private static final String EXPECTED_FIRST_LINE = "Type Prio Delay";
    private static final String UNEXPECTED_FIRST_LINE = "First line was not as expected.";
    private static final String UNEXPECTED_MESSAGE_TYPE = "Printed message type was not as expected.";
    private static final String UNEXPECTED_LINE = "Expected end of file.";

    private ImmediateMessageDelayReport report;
    private SimClock clock = SimClock.getInstance();

    // Hosts to send and receive messages.
    private DTNHost sender;
    private DTNHost receiver;

    public ImmediateMessageDelayReportTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    @Override
    public void setUp() throws IOException {
        // Let base do the basic report setup.
        super.setUp();

        // Add warm up time.
        this.settings.setNameSpace(this.getReportClass().getSimpleName());
        this.settings.putSetting(Report.WARMUP_S, Integer.toString(WARM_UP_TIME));
        this.settings.restoreNameSpace();

        // Set clock to 0.
        this.clock.setTime(0);

        // Create report.
        this.report = new ImmediateMessageDelayReport();

        // Create test utils with the report as a message listener.
        ArrayList<MessageListener> messageListeners = new ArrayList<>();
        messageListeners.add(this.report);
        TestUtils utils = new TestUtils(null, messageListeners, this.settings);

        // Use them to create hosts to act as sender and receiver.
        this.sender = utils.createHost();
        this.receiver = utils.createHost();
    }

    /**
     * Resets the clock to 0 and clears all groups.
     */
    @After
    public void tearDown() {
        this.clock.setTime(0);
        Group.clearGroups();
    }

    /***
     * Gets the report class to test.
     * @return The report class to test.
     */
    @Override
    protected Class getReportClass() {
        return ImmediateMessageDelayReport.class;
    }

    @Test
    public void testReportPrintsLegendAsFirstLine() throws IOException {
        // Complete report.
        this.report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
        }
    }

    @Test
    public void testReportPrintsMessageTypes() throws IOException {
        // Skip warm up time.
        this.clock.setTime(AFTER_WARM_UP_TIME);

        //Create group for multicast message
        Group group = Group.createGroup(1);
        group.addHost(this.sender);
        group.addHost(this.receiver);
        //Create data for data message
        DisasterData data = new DisasterData(DisasterData.DataType.SKILL, 0, AFTER_WARM_UP_TIME, DATA_LOCATION);
        // Create and deliver messages of all types.
        this.sender.createNewMessage(new Message(this.sender, this.receiver, "M1", 0));
        this.sender.createNewMessage(new BroadcastMessage(this.sender, "M2", 0));
        this.sender.createNewMessage(new MulticastMessage(this.sender, group, "M3", 0));
        this.sender.createNewMessage(new DataMessage(
                this.sender, this.receiver, "M4", Collections.singleton(new Tuple<>(data, 1D)), 1));
        ImmediateMessageDelayReportTest.transferMessage("M1", this.sender, this.receiver);
        ImmediateMessageDelayReportTest.transferMessage("M2", this.sender, this.receiver);
        ImmediateMessageDelayReportTest.transferMessage("M3", this.sender, this.receiver);
        ImmediateMessageDelayReportTest.transferMessage("M4", this.sender, this.receiver);

        // Complete report.
        this.report.done();

        // Check output for message types.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_MESSAGE_TYPE,
                    Message.MessageType.ONE_TO_ONE.toString(),
                    ImmediateMessageDelayReportTest.getTypeFromLine(reader.readLine()));
            Assert.assertEquals(
                    UNEXPECTED_MESSAGE_TYPE,
                    Message.MessageType.BROADCAST.toString(),
                    ImmediateMessageDelayReportTest.getTypeFromLine(reader.readLine()));
            Assert.assertEquals(
                    UNEXPECTED_MESSAGE_TYPE,
                    Message.MessageType.MULTICAST.toString(),
                    ImmediateMessageDelayReportTest.getTypeFromLine(reader.readLine()));
            Assert.assertEquals(
                    UNEXPECTED_MESSAGE_TYPE,
                    Message.MessageType.DATA.toString(),
                    ImmediateMessageDelayReportTest.getTypeFromLine(reader.readLine()));
        }
    }

    @Test
    public void testReportPrintsCorrectDelay() throws IOException {
        // Create message at a certain time point.
        this.clock.setTime(CREATION_TIME);
        this.sender.createNewMessage(new Message(this.sender, this.receiver, TEST_MESSAGE_ID, 0));

        // Deliver it at another.
        this.clock.setTime(DELIVERY_TIME);
        ImmediateMessageDelayReportTest.transferMessage(TEST_MESSAGE_ID, this.sender, this.receiver);

        // Complete report.
        this.report.done();

        // Check output for delay.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    "Unexpected message delay.",
                    Integer.toString(DELIVERY_TIME - CREATION_TIME),
                    ImmediateMessageDelayReportTest.getDelayFromLine(reader.readLine()));
        }
    }

    @Test
    public void testReportPrintsCorrectPriority() throws IOException {
        // Skip warm up time.
        this.clock.setTime(AFTER_WARM_UP_TIME);

        // Create and deliver a message of a certain priority.
        this.sender.createNewMessage(new Message(this.sender, this.receiver, "M1", 0, PRIORITY));
        ImmediateMessageDelayReportTest.transferMessage("M1", this.sender, this.receiver);

        // Complete report.
        this.report.done();

        // Check output for priority.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    "Unexpected message priority.",
                    Integer.toString(PRIORITY),
                    ImmediateMessageDelayReportTest.getPriorityFromLine(reader.readLine()));
        }
    }

    /**
     * Checks that messages created in warm up interval won't go into the statistic, even if they are delivered at
     * later times.
     * @throws IOException
     */
    @Test
    @Override
    public void testReportCorrectlyHandlesWarmUpTime() throws IOException {
        // Create message at time before warm up has finished.
        this.clock.setTime(0);
        this.sender.createNewMessage(new BroadcastMessage(this.sender, TEST_MESSAGE_ID, 0));

        // Leave warm up time and transfer the message.
        this.clock.setTime(AFTER_WARM_UP_TIME);
        ImmediateMessageDelayReportTest.transferMessage(TEST_MESSAGE_ID, this.sender, this.receiver);

        // Complete report.
        this.report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(UNEXPECTED_LINE, null, reader.readLine());
        }
    }

    /**
     * If a message gets delivered twice to the same host, it should only be reported the first time.
     * @throws IOException
     */
    @Test
    public void testReportIgnoresSecondDeliveryToSameHost() throws IOException {
        // Go to creation time and create message.
        this.clock.setTime(CREATION_TIME);
        this.sender.createNewMessage(new BroadcastMessage(this.sender, TEST_MESSAGE_ID, 0));

        // Go to delivery time and transfer the message twice.
        this.clock.setTime(DELIVERY_TIME);
        ImmediateMessageDelayReportTest.transferMessage(TEST_MESSAGE_ID, this.sender, this.receiver);
        ImmediateMessageDelayReportTest.transferMessage(TEST_MESSAGE_ID, this.sender, this.receiver);

        // Complete report.
        this.report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals("Unexpected message line.",
                    String.format("%s %d %d",
                            Message.MessageType.BROADCAST, Message.INVALID_PRIORITY, DELIVERY_TIME - CREATION_TIME),
                    reader.readLine());
            Assert.assertEquals(UNEXPECTED_LINE, null, reader.readLine());
        }
    }

    /**
     * Extracts the message type from a line in "type priority delay" format.
     */
    private static String getTypeFromLine(String line) {
        return line.split(" ")[0];
    }

    /**
     * Extracts the message prioirty from a line in "type priority delay" format.
     */
    private static String getPriorityFromLine(String line) {
        return line.split(" ")[1];
    }

    /**
     * Extracts the delay from a line in "type priority delay" format.
     */
    private static String getDelayFromLine(String line) {
        String[] words = line.split(" ");
        return words[words.length - 1];
    }

    /**
     * Transfers the specified message between the specified hosts.
     */
    private static void transferMessage(String messageId, DTNHost from, DTNHost to) {
        from.sendMessage(messageId, to);
        to.messageTransferred(messageId, from);
    }
}
