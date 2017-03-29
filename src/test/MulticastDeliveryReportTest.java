package test;

import core.BroadcastMessage;
import core.DTNHost;
import core.Group;
import core.Message;
import core.MessageListener;
import core.MulticastMessage;
import core.SimClock;
import org.junit.After;
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
public class MulticastDeliveryReportTest extends AbstractReportTest {

    private static final int AFTER_WARM_UP_TIME = AbstractReportTest.WARM_UP_TIME + 1;

    // Some times in a certain tests. Chosen arbitrarily.
    private static final int CREATION_TIME = 180;
    private static final int FIRST_DELIVERY_TIME = 230;
    private static final int SECOND_DELIVERY_TIME = 245;
    private static final int SIMULATION_TIME = 543;

    private static final double HALF_GROUP_RATIO = 0.5;
    private static final double THIRDS_GROUP_RATIO = 1.0 / 3.0;
    private static final double TWO_THIRDS_GROUP_RATIO = 2.0 / 3.0;
    private static final double ALL_GROUP_RATIO = 1.0;

    private static final String TEST_MESSAGE_ID = "M1";

    private static final String EXPECTED_FIRST_LINE = "#message, sent, received, ratio";
    private static final String UNEXPECTED_FIRST_LINE = "First line was not as expected.";

    private static final String FORMAT_OF_M1_REPORT_LINE = "M1 %d %d %s";
    private static final String UNEXPECTED_CREATION_LINE = "Line for message creation was not as expected.";
    private static final String UNEXPECTED_FIRST_DELIVERY_LINE = "Line for first delivery should have been different.";

    private static final String FORMAT_OF_SIM_TIME_LINE = "%d";
    private static final String UNEXPECTED_MESSAGE_LINE = "Expected line about total simulation time.";

    private TestUtils utils;
    private MulticastMessageDeliveryReport report;
    private SimClock clock = SimClock.getInstance();

    public MulticastDeliveryReportTest(){
        //setUp is called by junit before every test
    }

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

    @After
    public void resetClock(){
        clock.setTime(0);
    }

    @Test
    public void testReportReactsOnCreateMessage() throws IOException{
        clock.setTime(AFTER_WARM_UP_TIME);
        DTNHost h1 = utils.createHost();
        Group g = Group.createGroup(0);
        g.addHost(h1);
        h1.createNewMessage(new MulticastMessage(h1, g, TEST_MESSAGE_ID, 0));

        this.report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_CREATION_LINE,
                    String.format(FORMAT_OF_M1_REPORT_LINE, AFTER_WARM_UP_TIME,  AFTER_WARM_UP_TIME,0.0),
                    reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_MESSAGE_LINE,
                    String.format(FORMAT_OF_SIM_TIME_LINE, AFTER_WARM_UP_TIME),
                    reader.readLine());
        }
    }

    @Test
    public void testReportReactsOnTransferMessage() throws IOException{
        clock.setTime(AFTER_WARM_UP_TIME);
        DTNHost h1 = utils.createHost();
        DTNHost h2 = utils.createHost();
        Group g = Group.createGroup(0);
        g.addHost(h1);
        g.addHost(h2);
        h1.createNewMessage(new MulticastMessage(h1, g, TEST_MESSAGE_ID, 0));

        transferMessage(TEST_MESSAGE_ID, h1, h2);

        this.report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_CREATION_LINE,
                    String.format(FORMAT_OF_M1_REPORT_LINE, AFTER_WARM_UP_TIME,  AFTER_WARM_UP_TIME,0.0),
                    reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_FIRST_DELIVERY_LINE,
                    String.format(FORMAT_OF_M1_REPORT_LINE, AFTER_WARM_UP_TIME,  AFTER_WARM_UP_TIME,
                            HALF_GROUP_RATIO), reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_MESSAGE_LINE,
                    String.format(FORMAT_OF_SIM_TIME_LINE, AFTER_WARM_UP_TIME),
                    reader.readLine());
        }
    }

    @Test
    public void testReportSetsCorrectMsgCreationTime() throws IOException{
        clock.setTime(CREATION_TIME);
        DTNHost h1 = utils.createHost();
        Group g = Group.createGroup(0);
        g.addHost(h1);
        h1.createNewMessage(new MulticastMessage(h1, g, TEST_MESSAGE_ID, 0));

        this.report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_CREATION_LINE,
                    String.format(FORMAT_OF_M1_REPORT_LINE, CREATION_TIME, CREATION_TIME,0.0),
                    reader.readLine());
        }
    }

    @Test
    public void testReportSetsCorrectRecvTime() throws IOException{
        clock.setTime(CREATION_TIME);
        DTNHost h1 = utils.createHost();
        DTNHost h2 = utils.createHost();
        Group g = Group.createGroup(0);
        g.addHost(h1);
        g.addHost(h2);
        h1.createNewMessage(new MulticastMessage(h1, g, TEST_MESSAGE_ID, 0));

        clock.setTime(FIRST_DELIVERY_TIME);
        transferMessage(TEST_MESSAGE_ID, h1, h2);

        this.report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_CREATION_LINE,
                    String.format(FORMAT_OF_M1_REPORT_LINE, CREATION_TIME, CREATION_TIME,0.0),
                    reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_FIRST_DELIVERY_LINE,
                    String.format(FORMAT_OF_M1_REPORT_LINE, CREATION_TIME, FIRST_DELIVERY_TIME, HALF_GROUP_RATIO),
                    reader.readLine());
        }
    }

    @Test
    public void testReportSetsCorrectRatio() throws IOException{
        clock.setTime(CREATION_TIME);
        DTNHost h1 = utils.createHost();
        DTNHost h2 = utils.createHost();
        DTNHost h3 = utils.createHost();
        Group g = Group.createGroup(0);
        g.addHost(h1);
        g.addHost(h2);
        g.addHost(h3);
        h1.createNewMessage(new MulticastMessage(h1, g, TEST_MESSAGE_ID, 0));

        clock.setTime(FIRST_DELIVERY_TIME);
        transferMessage(TEST_MESSAGE_ID, h1, h2);

        clock.setTime(SECOND_DELIVERY_TIME);
        transferMessage(TEST_MESSAGE_ID, h2, h3);

        this.report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_CREATION_LINE,
                    String.format(FORMAT_OF_M1_REPORT_LINE, CREATION_TIME, CREATION_TIME,0.0),
                    reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_FIRST_DELIVERY_LINE,
                    String.format(FORMAT_OF_M1_REPORT_LINE, CREATION_TIME, FIRST_DELIVERY_TIME, THIRDS_GROUP_RATIO),
                    reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_FIRST_DELIVERY_LINE,
                    String.format(FORMAT_OF_M1_REPORT_LINE, CREATION_TIME, SECOND_DELIVERY_TIME,
                            TWO_THIRDS_GROUP_RATIO), reader.readLine());
        }
    }

    @Test
    public void testReportIgnoresSecondDelivery() throws IOException{
        clock.setTime(CREATION_TIME);
        DTNHost h1 = utils.createHost();
        DTNHost h2 = utils.createHost();
        Group g = Group.createGroup(0);
        g.addHost(h1);
        g.addHost(h2);
        h1.createNewMessage(new MulticastMessage(h1, g, TEST_MESSAGE_ID, 0));

        clock.setTime(FIRST_DELIVERY_TIME);
        transferMessage(TEST_MESSAGE_ID, h1, h2);

        clock.setTime(SECOND_DELIVERY_TIME);
        transferMessage(TEST_MESSAGE_ID, h1, h2);

        report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_CREATION_LINE,
                    String.format(FORMAT_OF_M1_REPORT_LINE, CREATION_TIME,  CREATION_TIME,0.0),
                    reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_FIRST_DELIVERY_LINE,
                    String.format(FORMAT_OF_M1_REPORT_LINE, CREATION_TIME,  FIRST_DELIVERY_TIME,
                            HALF_GROUP_RATIO), reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_MESSAGE_LINE,
                    String.format(FORMAT_OF_SIM_TIME_LINE, SECOND_DELIVERY_TIME),
                    reader.readLine());
        }
    }

    @Test
    public void testReportSetsCorrectSimTimeAfterDone() throws IOException{
        clock.setTime(SIMULATION_TIME);
        report.done();

        // Check output.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(UNEXPECTED_FIRST_LINE, EXPECTED_FIRST_LINE, reader.readLine());
            Assert.assertEquals(
                    UNEXPECTED_MESSAGE_LINE,
                    String.format(FORMAT_OF_SIM_TIME_LINE, SIMULATION_TIME),
                    reader.readLine());
        }
    }

    @Test
    public void testReportIgnoresUnicastMessages() throws IOException{
        // Skip warm up time.
        this.clock.setTime(AFTER_WARM_UP_TIME);
        // Create 1-to-1 message.
        DTNHost h1 = utils.createHost();
        h1.createNewMessage(new Message(h1,h1,TEST_MESSAGE_ID,0));

        transferMessage(TEST_MESSAGE_ID,h1,h1);
        testReportIgnoresMessage();
    }

    @Test
    public void testReportIgnoresBroadcastMessages() throws IOException{
        // Skip warm up time.
        this.clock.setTime(AFTER_WARM_UP_TIME);
        // Create 1-to-1 message.
        DTNHost h1 = utils.createHost();
        h1.createNewMessage(new BroadcastMessage(h1,TEST_MESSAGE_ID,0));

        transferMessage(TEST_MESSAGE_ID,h1,h1);
        testReportIgnoresMessage();
    }

    @Override
    @Test
    public void testReportCorrectlyHandlesWarmUpTime() throws IOException {
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

    private void testReportIgnoresMessage() throws IOException{
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

    /**
     * Transfers the specified message between the specified hosts.
     */
    private static void transferMessage(String messageId, DTNHost from, DTNHost to) {
        from.sendMessage(messageId, to);
        to.messageTransferred(messageId, from);
    }
}
