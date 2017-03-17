package test;

import core.BroadcastMessage;
import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.NetworkInterface;
import core.SimClock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import report.Report;
import report.TrafficReport;
import routing.EpidemicRouter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Contains tests for the {@link report.TrafficReport} class.
 *
 * Created by Britta Heymann on 17.03.2017.
 */
public class TrafficReportTest extends AbstractReportTest {
    private static final int WARM_UP_TIME = 50;
    private static final int AFTER_WARM_UP_TIME = WARM_UP_TIME + 1;
    private static final int TRANSMIT_SPEED = 2;

    // Time before connection downing in several tests.
    private static final int TIME_BEFORE_ABORTION = 30;

    // Message sizes used in tests.
    private static final int BROADCAST_SIZE = 200;
    private static final int ONE_TO_ONE_SIZE = 100;

    private static final String EXPECTED_FIRST_LINE = "Traffic by message type:";
    private static final String UNEXPECTED_FIRST_LINE = "First line was not as expected.";
    private static final String UNEXPECTED_STATISTIC_FOR_ONE_TO_ONE = "Unexpected values for 1-to-1 messages.";
    private static final String UNEXPECTED_TRAFFIC = "No traffic should have been logged.";

    private static final String TRAFFIC_LINE_FORMAT = "%s: %5.2f%% (%d Bytes)";

    // Scaling factor to translate a percentage given as double between 0 and 1 to a double between 0 and 100.
    private static final int PERCENTAGE_SCALING_FACTOR = 100;

    private static final double FULL_PERCENTAGE = 100.0;

    private TrafficReport report;
    private SimClock clock = SimClock.getInstance();

    // Hosts to send and receive messages.
    private DTNHost sender;
    private DTNHost receiver;

    public TrafficReportTest() {
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

        // Add non trivial transmit speed.
        this.settings.setNameSpace("interface");
        this.settings.putSetting(NetworkInterface.TRANSMIT_RANGE_S, "1.0");
        this.settings.putSetting(NetworkInterface.TRANSMIT_SPEED_S, Integer.toString(TRANSMIT_SPEED));
        this.settings.restoreNameSpace();

        // Set clock to 0.
        this.clock.setTime(0);

        // Create report.
        this.report = new TrafficReport();

        // Create test utils with the report as a message and connection listener.
        ArrayList<MessageListener> messageListeners = new ArrayList<>();
        ArrayList<ConnectionListener> connectionListeners = new ArrayList<>();
        messageListeners.add(this.report);
        connectionListeners.add(this.report);
        TestUtils utils = new TestUtils(connectionListeners, messageListeners, this.settings);

        // Use epidemic router to force the relay of all messages.
        utils.setMessageRouterProto(new EpidemicRouter(this.settings));

        // Use test utils to create hosts to act as sender and receiver.
        this.sender = utils.createHost();
        this.receiver = utils.createHost();
    }

    /**
     * Resets the clock to 0.
     */
    @After
    public void resetClock() {
        this.clock.setTime(0);
    }

    /***
     * Gets the report class to test.
     * @return The the report class to test.
     */
    @Override
    protected Class getReportClass() {
        return TrafficReport.class;
    }

    @Test
    public void reportPrintsExplanationAsFirstLine() throws IOException {
        // Complete report.
        this.report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
        }
    }

    @Test
    public void reportAlwaysPrintsAllMessageTypes() throws IOException {
        // Complete report.
        this.report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    "1-to-1 message traffic should have been printed.",
                    String.format(TRAFFIC_LINE_FORMAT, Message.MessageType.ONE_TO_ONE, 0.0, 0),
                    reader.readLine());
            Assert.assertEquals(
                    "Broadcast traffic should have been printed.",
                    String.format(TRAFFIC_LINE_FORMAT, Message.MessageType.BROADCAST, 0.0, 0),
                    reader.readLine());
            // TODO: Add further types once we have them
        }
    }

    @Test
    public void reportPrintsCorrectValuesForTransferredMessages() throws IOException {
        this.clock.setTime(AFTER_WARM_UP_TIME);

        // Create broadcasts and 1-to-1 message.
        this.sender.createNewMessage(new BroadcastMessage(this.sender, "M1", BROADCAST_SIZE));
        this.sender.createNewMessage(new BroadcastMessage(this.sender, "M2", BROADCAST_SIZE));
        this.sender.createNewMessage(new Message(this.sender, this.receiver, "M3", ONE_TO_ONE_SIZE));

        // Transfer messages.
        this.sender.sendMessage("M1", this.receiver);
        this.sender.sendMessage("M2", this.receiver);
        this.sender.sendMessage("M3", this.receiver);
        this.receiver.messageTransferred("M1", this.sender);
        this.receiver.messageTransferred("M2", this.sender);
        this.receiver.messageTransferred("M3", this.sender);

        // Compute expected percentages and totals.
        int expectedBytesBroadcast = BROADCAST_SIZE + BROADCAST_SIZE;
        int expectedBytesOneToOne = ONE_TO_ONE_SIZE;
        int total = expectedBytesBroadcast + expectedBytesOneToOne;
        double expectedPercentageBroadcast = (double)expectedBytesBroadcast / total * PERCENTAGE_SCALING_FACTOR;
        double expectedPercentageOneToOne = (double)expectedBytesOneToOne / total * PERCENTAGE_SCALING_FACTOR;

        // Complete report.
        this.report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_STATISTIC_FOR_ONE_TO_ONE,
                    String.format(
                            TRAFFIC_LINE_FORMAT,
                            Message.MessageType.ONE_TO_ONE,
                            expectedPercentageOneToOne,
                            expectedBytesOneToOne),
                    reader.readLine());
            Assert.assertEquals(
                    "Unexpected values for broadcasts.",
                    String.format(
                            TRAFFIC_LINE_FORMAT,
                            Message.MessageType.BROADCAST,
                            expectedPercentageBroadcast,
                            expectedBytesBroadcast),
                    reader.readLine());
        }
    }

    @Test
    public void reportIncludesRelayedPartOfAbortedMessageTransfers() throws IOException {
        this.clock.setTime(AFTER_WARM_UP_TIME);

        // Create message.
        this.sender.createNewMessage(new Message(this.sender, this.receiver, "M1", ONE_TO_ONE_SIZE));

        // Set up connection and let the router begin transferring messages.
        this.sender.forceConnection(this.receiver, null, true);
        this.sender.getRouter().update();

        // Update connection after a while.
        this.clock.advance(TIME_BEFORE_ABORTION);
        this.sender.getConnections().get(0).update();

        // Then destroy it.
        this.sender.forceConnection(this.receiver, null, false);

        // Complete report.
        this.report.done();

        // Check output contains the partly transferred message.
        int expectedTransferredBytes = TIME_BEFORE_ABORTION * TRANSMIT_SPEED;
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_STATISTIC_FOR_ONE_TO_ONE,
                    String.format(
                            TRAFFIC_LINE_FORMAT,
                            Message.MessageType.ONE_TO_ONE,
                            FULL_PERCENTAGE,
                            expectedTransferredBytes),
                    reader.readLine());
        }
    }

    @Test
    public void reportIgnoresWarmUpMessageEvenIfTransferredLater() throws IOException {
        // Create message in warm up time.
        this.sender.createNewMessage(new Message(this.sender, this.receiver, "M1", ONE_TO_ONE_SIZE));

        // Advance to after warm up.
        this.clock.setTime(AFTER_WARM_UP_TIME);

        // Transfer message.
        this.sender.sendMessage("M1", this.receiver);
        this.receiver.messageTransferred("M1", this.sender);

        // Complete report.
        this.report.done();

        // Check no message was logged.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_TRAFFIC,
                    String.format(TRAFFIC_LINE_FORMAT, Message.MessageType.ONE_TO_ONE, 0.0, 0),
                    reader.readLine());
        }
    }

    @Test
    public void reportIgnoresWarmUpMessageEvenIfInAbortedTransferLater() throws IOException {
        // Create message in warm up time.
        this.sender.createNewMessage(new Message(this.sender, this.receiver, "M1", ONE_TO_ONE_SIZE));

        // Advance to after warm up.
        this.clock.setTime(AFTER_WARM_UP_TIME);

        // Set up connection and let the router begin transferring messages.
        this.sender.forceConnection(this.receiver, null, true);
        this.sender.getRouter().update();

        // Update connection after a while.
        this.clock.advance(TIME_BEFORE_ABORTION);
        this.sender.getConnections().get(0).update();

        // Then destroy it.
        this.sender.forceConnection(this.receiver, null, false);

        // Complete report.
        this.report.done();

        // Check no message was logged.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_TRAFFIC,
                    String.format(TRAFFIC_LINE_FORMAT, Message.MessageType.ONE_TO_ONE, 0.0, 0),
                    reader.readLine());
        }
    }
}
