package test;

import core.BroadcastMessage;
import core.DTNHost;
import core.Group;
import core.Message;
import core.MessageListener;
import core.MulticastMessage;
import core.SimClock;
import core.SimScenario;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import report.DeliveryProbabilityReport;

import test.TestSettings;
import test.TestUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Contains Unit tests for the DeliveryProbabilityReport Class.
 *
 * @author Nils Weidmann
 */
public class DeliveryProbabilityReportTest extends AbstractReportTest {
    private DeliveryProbabilityReport report;
    private TestUtils utils;

    public DeliveryProbabilityReportTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    /**
     * Gets the report class to test.
     *
     * @return The report class to test.
     */
    @Override
    protected Class getReportClass() {
        return DeliveryProbabilityReport.class;
    }

    /**
     * Creates the report writing to a temporary file and adds it as message listener.
     */
    @Before
    @Override
    public void setUp() throws IOException {
        // Call generic report set up adding important settings and the temporary file.
        super.setUp();

        // Make SimScenario usage possible.
        SimScenario.reset();
        TestSettings.addSettingsToEnableSimScenario(settings);

        // Set locale for periods instead of commas in doubles.
        java.util.Locale.setDefault(java.util.Locale.US);

        // Create report and set it as message listener.
        this.report = new DeliveryProbabilityReport();
        ArrayList<MessageListener> messageListeners = new ArrayList<>(1);
        messageListeners.add(this.report);
        this.utils = new TestUtils(new ArrayList<>(), messageListeners, settings);
    }

    @After
    /**
     * SimScenario was called by DeliveryProbabilityReport and therefore initiated with this specific test's settings.
     Cleanup to make further tests with other settings possible.
     Similarly, groups were used.
     */
    public void cleanUp() {
        SimScenario.reset();
        Group.clearGroups();
        SimClock.reset();
    }

    /**
     * Exchange messages between nodes a,b and c. The messages from a to c is aborted, while the others are delivered
     * successfully.
     */
    private void playScenario() {
        DTNHost a = this.utils.createHost();
        DTNHost b = this.utils.createHost();
        DTNHost c = this.utils.createHost();

        a.createNewMessage(new Message(a, c, "m1", 0));
        a.sendMessage("m1", b);
        b.messageTransferred("m1", a);
        a.sendMessage("m1", c);
        c.messageAborted("m1", a, 0);
        b.deleteMessage("m1", false);

        b.createNewMessage(new Message(b, c, "m2", 0));
        b.sendMessage("m2", a);
        a.messageTransferred("m2", b);
        a.sendMessage("m2", c);
        c.messageTransferred("m2", a);
        a.deleteMessage("m2", true);

        c.createNewMessage(new Message(c, a, "m3", 0));
        c.sendMessage("m3", b);
        b.messageTransferred("m3", c);
        b.sendMessage("m3", a);
        a.messageTransferred("m3", b);
        b.deleteMessage("m3", true);
    }

    /**
     * Tests that message events creating and delivery are counted correctly.
     *
     * @throws IOException If the temporary file cannot be opened, read or closed.
     */
    @Test
    public void testDoneCorrectlyCountsMessageEvents() throws IOException {
        this.skipWarmUpTime();

        this.playScenario();
        this.report.done();

        try (BufferedReader reader = this.createBufferedReader()) {

            assertEquals("First command line is not as expected.",
                    "Message stats for scenario TEST-Scenario",
                    reader.readLine());
            assertEquals("Second command line is not as expected.",
                    String.format("sim_time: %.4f", SimClock.getTime()),
                    reader.readLine());
            assertEquals(
                    "Reported number of created messages should have been different.",
                    "created: 3",
                    reader.readLine());
            assertEquals(
                    "Reported number of delivered messages should have been different.",
                    "delivered: 2",
                    reader.readLine());
            assertEquals(
                    "Reported delivery probability should have been different.",
                    "delivery_prob: 0.6667",
                    reader.readLine());
        }
    }

    @Test
    /**
     * Tests if the warm-up time is considered in the report
     * @throws IOException rethrows the IOException of the init method
     */
    @Override
    public void testReportCorrectlyHandlesWarmUpTime() throws IOException {
        DTNHost a = this.utils.createHost();
        DTNHost b = this.utils.createHost();

        a.createNewMessage(new Message(a, b, "m1", 0));
        a.sendMessage("m1", b);
        b.messageTransferred("m1", a);
        assertEquals(report.getNrofDelivered(), 0);
    }

    @Test
    /**
     * Tests if broadcast messages are ignored
     * @throws IOException IOException rethrows the IOException of the init method
     */
    public void testIgnoreOtherMessageTypes() throws IOException {
        this.skipWarmUpTime();

        DTNHost a = this.utils.createHost();
        DTNHost b = this.utils.createHost();
        Group g = Group.createGroup(0);
        g.addHost(a);
        g.addHost(b);

        a.createNewMessage(new BroadcastMessage(a, "m1", 0));
        a.sendMessage("m1", b);
        b.messageTransferred("m1", a);
        assertEquals("Broadcast messages should be ignored.", report.getNrofDelivered(), 0);

        a.createNewMessage(new MulticastMessage(a, g, "m1", 0));
        a.sendMessage("m1", b);
        b.messageTransferred("m1", a);
        assertEquals("Multicast messages should be ignored.", report.getNrofDelivered(), 0);

    }

    @Test
    /**
     * Tests if the second time, a node receives a message, it is ignored
     * @throws IOException IOException rethrows the IOException of the init method
     */
    public void testIgnoreSecondDelivery() throws IOException {
        this.skipWarmUpTime();

        DTNHost a = this.utils.createHost();
        DTNHost b = this.utils.createHost();
        DTNHost c = this.utils.createHost();

        a.createNewMessage(new Message(a, b, "m1", 0));

        a.sendMessage("m1", b);
        b.messageTransferred("m1", a);
        assertEquals(report.getNrofDelivered(), 1);

        a.sendMessage("m1", c);
        c.messageTransferred("m1", a);
        assertEquals(report.getNrofDelivered(), 1);
    }
}