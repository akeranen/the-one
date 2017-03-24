package test;

import core.DTNHost;
import core.Group;
import core.MessageListener;
import core.MulticastMessage;
import core.SimClock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import report.MulticastMessageDeliveryReport;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Contains tests for the {@link MulticastMessageDeliveryReport}
 *
 * Created by Marius Meyer on 22.03.17.
 */
public class MulticastDeliveryReportTest extends AbstractMessageReportTest {

    private static final int AFTER_WARM_UP_TIME = AbstractReportTest.WARM_UP_TIME + 1;

    // Some times in a certain tests. Chosen arbitrarily.
    private static final int CREATION_TIME = 180;
    private static final int FIRST_DELIVERY_TIME = 230;
    private static final int SECOND_DELIVERY_TIME = 245;
    private static final int SIMULATION_TIME = 543;

    private static final String TEST_MESSAGE_ID = "M1";

    private static final String EXPECTED_FIRST_LINE = "#message, group, sent, received, ratio";
    private static final String UNEXPECTED_FIRST_LINE = "First line was not as expected.";

    private static final String FORMAT_OF_M1_REPORT_LINE = "M1 %d %d %d %d";
    private static final String UNEXPECTED_CREATION_LINE = "Line for message creation was not as expected.";
    private static final String UNEXPECTED_FIRST_DELIVERY_LINE = "Line for first delivery should have been different.";

    private static final String FORMAT_OF_SIM_TIME_LINE = "%d";
    private static final String UNEXPECTED_MESSAGE_LINE = "Expected line about total simulation time.";

    private TestUtils utils;
    private MulticastMessageDeliveryReport report;
    private SimClock clock = SimClock.getInstance();

    @Before
    @Override
    public void setUp() throws IOException {
        // Let base do the basic report setup.
        super.setUp();

        Group.clearGroups();

        // Set clock to 0.
        this.clock.setTime(0);

        // Create report.
        this.report = new MulticastMessageDeliveryReport();

        // Create test utils with the report as a message listener.
        ArrayList<MessageListener> messageListeners = new ArrayList<>();
        messageListeners.add(this.report);
        this.utils = new TestUtils(null, messageListeners, this.settings);

    }

    @Override
    @Test
    public void reportCorrectlyHandlesWarmUpTime() throws IOException {
        // Create broadcast at time before warm up has finished.
        this.clock.setTime(0);
        DTNHost h1 = utils.createHost();
        DTNHost h2 = utils.createHost();
        Group g = Group.createGroup(0);
        g.addHost(h1);
        g.addHost(h2);
        h1.createNewMessage(new MulticastMessage(h1, g, TEST_MESSAGE_ID, 0));

        // Leave warm up time and transfer the message.
        this.clock.setTime(AFTER_WARM_UP_TIME);
        transferMessage(TEST_MESSAGE_ID, h1, h2);

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

    @Override
    protected Class getReportClass() {
        return MulticastMessageDeliveryReport.class;
    }
}
