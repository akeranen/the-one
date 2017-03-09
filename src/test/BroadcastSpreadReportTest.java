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
import report.BroadcastSpreadReport;
import report.Report;
import report.SamplingReport;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Contains tests for the {@link BroadcastSpreadReport} class.
 *
 * Created by Britta Heymann on 08.03.2017.
 */
public class BroadcastSpreadReportTest extends AbstractReportTest {
    private static final int WARM_UP_TIME = 50;
    private static final int REPORT_INTERVAL = 100;
    private static final int SECOND_REPORT_TIME = 2 * REPORT_INTERVAL;

    private static final String EXPECTED_FIRST_TIME_STAMP = String.format("[%s]", REPORT_INTERVAL);
    private static final String EXPECTED_SECOND_TIME_STAMP = String.format("[%s]", SECOND_REPORT_TIME);
    private static final String UNEXPECTED_TIME_STAMP = "Time stamp was not as expected.";

    private TestUtils utils;
    private BroadcastSpreadReport report;
    private SimClock clock = SimClock.getInstance();

    @Before
    @Override
    public void setUp() throws IOException {
        // Let base do the basic report setup.
        super.setUp();

        // Add warm up time and report interval.
        this.settings.setNameSpace(this.getReportName());
        this.settings.putSetting(Report.WARMUP_S, Integer.toString(WARM_UP_TIME));
        this.settings.putSetting(SamplingReport.SAMPLE_INTERVAL_SETTING, Integer.toString(REPORT_INTERVAL));
        this.settings.restoreNameSpace();

        // Set clock to 0.
        this.clock.setTime(0);

        // Create report.
        this.report = new BroadcastSpreadReport();

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
        return "BroadcastSpreadReport";
    }

    /**
     * Checks that the report is using the specified report interval.
     * @throws IOException
     */
    @Test
    public void reportPrintsAtCorrectTimes() throws IOException {
        this.clock.setTime(REPORT_INTERVAL);
        this.report.updated(null);
        this.clock.setTime(SECOND_REPORT_TIME);
        this.report.updated(null);
        this.report.done();

        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_TIME_STAMP, EXPECTED_FIRST_TIME_STAMP, reader.readLine());
            Assert.assertEquals(UNEXPECTED_TIME_STAMP, EXPECTED_SECOND_TIME_STAMP, reader.readLine());
        }
    }

    /**
     * Checks that the report also prints broadcasts that have been created, but not deliverd to anyone yet.
     * @throws IOException
     */
    @Test
    public void reportPrintsNewlyCreatedBroadcasts() throws IOException {
        // Go to first report time.
        this.clock.setTime(REPORT_INTERVAL);

        // Create broadcast message which does not get transferred.
        DTNHost h1 = utils.createHost();
        h1.createNewMessage(new BroadcastMessage(h1, "M1", 0));

        this.updateAndFinishReport();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_TIME_STAMP, EXPECTED_FIRST_TIME_STAMP, reader.readLine());
            Assert.assertEquals(
                    "Report about message should have been different.","M1 1 0", reader.readLine());
        }
    }

    @Test
    public void reportPrintsCorrectDeliveryCount() throws IOException {
        // Create hosts to create and deliver messages.
        DTNHost h1 = utils.createHost();
        DTNHost h2 = utils.createHost();
        DTNHost h3 = utils.createHost();
        DTNHost h4 = utils.createHost();

        // Go to first report time.
        this.clock.setTime(REPORT_INTERVAL);

        // Create broadcast message which gets transferred once before updating report.
        h1.createNewMessage(new BroadcastMessage(h1, "M1", 0));
        BroadcastSpreadReportTest.transferMessage("M1", h1, h2);
        this.report.updated(null);

        // Set the clock to second report point.
        this.clock.setTime(SECOND_REPORT_TIME);

        // Transfer the message two more times.
        BroadcastSpreadReportTest.transferMessage("M1", h1, h3);
        BroadcastSpreadReportTest.transferMessage("M1", h1, h4);

        this.updateAndFinishReport();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_TIME_STAMP, EXPECTED_FIRST_TIME_STAMP, reader.readLine());
            Assert.assertEquals(
                    "Report about message should have been different after first time.",
                    "M1 1 1",
                    reader.readLine());
            Assert.assertEquals("Timestamp was not as expected.", EXPECTED_SECOND_TIME_STAMP, reader.readLine());
            Assert.assertEquals(
                    "Report about message should have been different after second time.",
                    "M1 1 3",
                    reader.readLine());
        }
    }

    @Test
    public void reportIgnoresOneToOneMessages() throws IOException {
        // Go to first report time.
        this.clock.setTime(REPORT_INTERVAL);

        // Create 1-to-1 message.
        DTNHost h1 = utils.createHost();
        h1.createNewMessage(new Message(h1, h1, "M1", 0));

        this.updateAndFinishReport();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_TIME_STAMP, EXPECTED_FIRST_TIME_STAMP, reader.readLine());
            Assert.assertNull("No second line expected.", reader.readLine());
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
        this.clock.setTime(WARM_UP_TIME - 1.0);
        DTNHost h1 = utils.createHost();
        h1.createNewMessage(new BroadcastMessage(h1, "M1", 0));

        // Go to first report time and transfer the message.
        this.clock.setTime(REPORT_INTERVAL);
        BroadcastSpreadReportTest.transferMessage("M1", h1, utils.createHost());

        this.updateAndFinishReport();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_TIME_STAMP, EXPECTED_FIRST_TIME_STAMP, reader.readLine());
            Assert.assertNull("No second line expected.", reader.readLine());
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
     * First calls the updated(null), then the done() method of {@link Report}.
     */
    private void updateAndFinishReport() {
        this.report.updated(null);
        this.report.done();
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
