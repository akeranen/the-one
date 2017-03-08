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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.Buffer;
import java.util.Vector;

/**
 * Contains tests for the {@link BroadcastSpreadReport} class.
 *
 * Created by Britta Heymann on 08.03.2017.
 */
public class BroadcastSpreadReportTest extends AbstractReportTest {
    private static final int WARM_UP_TIME = 50;
    private static final int REPORT_INTERVAL = 100;
    private static final int SECOND_REPORT_INTERVAL = 2 * REPORT_INTERVAL;

    private TestUtils utils;
    private BroadcastSpreadReport report;
    private SimClock clock;

    @Before
    public void setUp() throws IOException {
        super.setUp();

        this.settings.putSetting(Report.WARMUP_S, Integer.toString(WARM_UP_TIME));
        this.settings.putSetting(
                this.getReportName() + "." + SamplingReport.SAMPLE_INTERVAL_SETTING, Integer.toString(REPORT_INTERVAL));

        this.clock = SimClock.getInstance();
        this.clock.setTime(0);

        Vector<MessageListener> messageListeners = new Vector<>();
        this.report = new BroadcastSpreadReport();
        messageListeners.add(this.report);
        this.utils = new TestUtils(null, messageListeners, this.settings);

        // this.generateMessages();
    }

    @After
    public void resetClock() {
        this.clock.setTime(0);
    }

    /***
     * Gets the name of the report class to test.
     * @return The name of the report class to test.
     */
    @Override
    protected String getReportName() {
        return "BroadcastSpreadReport";
    }

    public void generateMessages() {
        // Create hosts to create and deliver messages.
        DTNHost h1 = utils.createHost();
        DTNHost h2 = utils.createHost();
        DTNHost h3 = utils.createHost();
        DTNHost h4 = utils.createHost();
        DTNHost h5 = utils.createHost();

        // Create first message within warmup period.
        h1.createNewMessage(new BroadcastMessage(h1, "M1", 0));

        // Create four other messages after warmup: two 1-to-1 and two broadcast messages.
        this.clock.setTime(WARM_UP_TIME);
        h2.createNewMessage(new Message(h2, h3, "M2", 0));
        h3.createNewMessage(new Message(h3, h4, "M3", 0));
        h4.createNewMessage(new BroadcastMessage(h4, "M4", 0));
        h5.createNewMessage(new BroadcastMessage(h5, "M5", 0));

        // Add some transfers before the first report point.
        h3.messageTransferred("M2", h2);
        h2.messageTransferred("M3", h3);
        h1.messageTransferred("M4", h4);

        // Set the clock to first report point.
        this.clock.setTime(REPORT_INTERVAL);

        // Add more transfers of broadcast messages.
        h2.messageTransferred("M4", h4);
        h3.messageTransferred("M4", h4);
        h5.messageTransferred("M4", h4);
        h2.messageTransferred("M1", h1);

        // Set the clock to one time step before the second report point.
        this.clock.setTime(SECOND_REPORT_INTERVAL - 1.0);

        // Add another broadcast message transfer.
        h1.messageTransferred("M5", h5);

        // Set the clock to second report point.
        this.clock.setTime(SECOND_REPORT_INTERVAL);
    }

    @Test
    public void reportPrintsAtCorrectTimes() throws IOException {
        this.clock.setTime(REPORT_INTERVAL);
        this.report.updated(null);
        this.clock.setTime(SECOND_REPORT_INTERVAL);
        this.report.updated(null);
        this.report.done();

        try(BufferedReader reader = new BufferedReader(new FileReader(this.outputFile))) {
            Assert.assertEquals(
                    "First sampling time should have been different.","[100]", reader.readLine());
            Assert.assertEquals(
                    "Second sampling time should have been differrent.","[200]", reader.readLine());
        }
    }

    @Test
    public void reportPrintsCorrectPriorities() {

    }

    @Test
    public void reportPrintsNewlyCreatedBroadcasts() {

    }

    @Test
    public void reportPrintsCorrectDeliveryCount() {

    }

    @Test
    public void reportIgnoresOneToOneMessages() {

    }

    @Test
    public void reportIgnoresWarmUp() {

    }
}
