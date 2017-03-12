package test;

import core.BroadcastMessage;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimClock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import report.BroadcastDeliveryReport;
import report.Report;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Contains tests for the {@link BroadcastDeliveryReport} class.
 *
 * Created by Britta Heymann on 08.03.2017.
 */
public class BroadcastDeliveryReportTest extends AbstractReportTest {
    private static final int WARM_UP_TIME = 50;
    private static final int AFTER_WARM_UP_TIME = WARM_UP_TIME + 1;

    // Some times in a certain tests. Chosen arbitrarily.
    private static final int CREATION_TIME = 180;
    private static final int FIRST_DELIVERY_TIME = 230;
    private static final int SECOND_DELIVERY_TIME = 245;
    private static final int SIMULATION_TIME = 543;

    private static final String TEST_MESSAGE_ID = "M1";

    private static final String EXPECTED_FIRST_LINE = "Time # Prio";
    private static final String UNEXPECTED_FIRST_LINE = "First line was not as expected.";

    private static final String FORMAT_OF_M1_REPORT_LINE = "%d M1 1";
    private static final String UNEXPECTED_CREATION_LINE = "Line for message creation was not as expected.";
    private static final String UNEXPECTED_FIRST_DELIVERY_LINE = "Line for first delivery should have been different.";

    private static final String FORMAT_OF_SIM_TIME_LINE = "%d";
    private static final String UNEXPECTED_MESSAGE_LINE = "Expected line about total simulation time.";

    private TestUtils utils;
    private BroadcastDeliveryReport report;
    private SimClock clock = SimClock.getInstance();

    public BroadcastDeliveryReportTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    @Override
    public void setUp() throws IOException {
        // Let base do the basic report setup.
        super.setUp();

        // Add warm up time and report interval.
        this.settings.setNameSpace(this.getReportName());
        this.settings.putSetting(Report.WARMUP_S, Integer.toString(WARM_UP_TIME));
        this.settings.restoreNameSpace();

        // Set clock to 0.
        this.clock.setTime(0);

        // Create report.
        this.report = new BroadcastDeliveryReport();

        // Create test utils with the report as a message listener.
        ArrayList<MessageListener> messageListeners = new ArrayList<>();
        messageListeners.add(this.report);
        this.utils = new TestUtils(null, messageListeners, this.settings);

    }

    /**
     * Resets the clock to 0.
     */
    @After
    public void resetClock() {
        this.clock.setTime(0);
    }

    /**
     * Gets the name of the report class to test.
     * @return The name of the report class to test.
     */
    @Override
    protected String getReportName() {
        return "BroadcastDeliveryReport";
    }

    @Test
    public void reportPrintsOnCreation() throws IOException {
        // Go to creation time and create broadcast message.
        this.clock.setTime(CREATION_TIME);
        DTNHost sender = utils.createHost();
        sender.createNewMessage(new BroadcastMessage(sender, TEST_MESSAGE_ID, 0));

        this.report.done();
        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    "Report about creation should have been different.",
                    String.format(FORMAT_OF_M1_REPORT_LINE, CREATION_TIME),
                    reader.readLine());
        }
    }

    @Test
    public void reportPrintsOnDelivery() throws IOException {
        // Go to creation time and create broadcast message.
        this.clock.setTime(CREATION_TIME);
        DTNHost sender = utils.createHost();
        sender.createNewMessage(new BroadcastMessage(sender, TEST_MESSAGE_ID, 0));

        // Go to delivery times and transfer the message at them.
        this.clock.setTime(FIRST_DELIVERY_TIME);
        BroadcastDeliveryReportTest.transferMessage(TEST_MESSAGE_ID, sender, utils.createHost());
        this.clock.setTime(SECOND_DELIVERY_TIME);
        BroadcastDeliveryReportTest.transferMessage(TEST_MESSAGE_ID, sender, utils.createHost());

        this.report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(UNEXPECTED_CREATION_LINE,
                    String.format(FORMAT_OF_M1_REPORT_LINE, CREATION_TIME),
                    reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_FIRST_DELIVERY_LINE,
                    String.format(FORMAT_OF_M1_REPORT_LINE, FIRST_DELIVERY_TIME),
                    reader.readLine());
            Assert.assertEquals(
                    "Report about second delivery should have been different.",
                    String.format(FORMAT_OF_M1_REPORT_LINE, SECOND_DELIVERY_TIME),
                    reader.readLine());
        }
    }

    @Test
    public void reportPrintsSimulationTimeWhenDone() throws IOException {
        this.clock.setTime(SIMULATION_TIME);
        this.report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    "Last report line should have been different.",
                    Integer.toString(SIMULATION_TIME),
                    reader.readLine());
        }
    }

    @Test
    public void reportIgnoresOneToOneMessages() throws IOException {
        // Skip warm up time.
        this.clock.setTime(AFTER_WARM_UP_TIME);

        // Create 1-to-1 message.
        DTNHost h1 = utils.createHost();
        h1.createNewMessage(new Message(h1, h1, TEST_MESSAGE_ID, 0));

        this.report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_MESSAGE_LINE,
                    String.format(FORMAT_OF_SIM_TIME_LINE, AFTER_WARM_UP_TIME),
                    reader.readLine());
        }
    }

    /**
     * Checks that broadcasts created in warm up interval won't go into the statistic, even if they are delivered at
     * later times.
     * @throws IOException
     */
    @Test
    public void reportIgnoresWarmUp() throws IOException {
        // Create broadcast at time before warm up has finished.
        this.clock.setTime(0);
        DTNHost h1 = utils.createHost();
        h1.createNewMessage(new BroadcastMessage(h1, TEST_MESSAGE_ID, 0));

        // Leave warm up time and transfer the message.
        this.clock.setTime(AFTER_WARM_UP_TIME);
        BroadcastDeliveryReportTest.transferMessage(TEST_MESSAGE_ID, h1, utils.createHost());

        this.report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_MESSAGE_LINE,
                    String.format(FORMAT_OF_SIM_TIME_LINE, AFTER_WARM_UP_TIME),
                    reader.readLine());
        }
    }

    /**
     * If a broadcast gets delivered twice to the same host, it should only be reported the first time.
     * @throws IOException
     */
    @Test
    public void reportIgnoresSecondDeliveryToSameHost() throws IOException {
        // Go to creation time and create broadcast message.
        this.clock.setTime(CREATION_TIME);
        DTNHost sender = utils.createHost();
        sender.createNewMessage(new BroadcastMessage(sender, TEST_MESSAGE_ID, 0));

        // Go to delivery times and transfer the message at them.
        DTNHost recipient = utils.createHost();
        this.clock.setTime(FIRST_DELIVERY_TIME);
        BroadcastDeliveryReportTest.transferMessage(TEST_MESSAGE_ID, sender, recipient);
        this.clock.setTime(SECOND_DELIVERY_TIME);
        BroadcastDeliveryReportTest.transferMessage(TEST_MESSAGE_ID, sender, recipient);

        this.report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_CREATION_LINE,
                    String.format(FORMAT_OF_M1_REPORT_LINE, CREATION_TIME),
                    reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_FIRST_DELIVERY_LINE,
                    String.format(FORMAT_OF_M1_REPORT_LINE, FIRST_DELIVERY_TIME),
                    reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_MESSAGE_LINE,
                    String.format(FORMAT_OF_SIM_TIME_LINE, SECOND_DELIVERY_TIME),
                    reader.readLine());
        }
    }

    /**
     * Transfers the specified message between the specified hosts.
     */
    private static void transferMessage(String messageId, DTNHost from, DTNHost to) {
        from.sendMessage(messageId, to);
        to.messageTransferred(messageId, from);
    }

    /**
     * Create a buffered reader that assumes the output file was written using UTF8 encoding.
     * @return The buffered reader.
     * @throws IOException
     */
    private BufferedReader createBufferedReader() throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(this.outputFile), StandardCharsets.UTF_8));
    }
}
