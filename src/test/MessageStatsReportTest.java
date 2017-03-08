package test;

import core.BroadcastMessage;
import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SimScenario;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import report.MessageStatsReport;

import test.TestSettings;
import test.TestUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Contains tests for the MessageStatsReportClass.
 *
 * Created by Britta Heymann on 16.02.2017.
 */
public class MessageStatsReportTest {
    private MessageStatsReport report;
    private TestUtils utils;
    private File outFile;

    @Before
    public void init() throws IOException{
        SimScenario.reset();
        Settings.init(null);
        java.util.Locale.setDefault(java.util.Locale.US);

        this.outFile = File.createTempFile("mgtest", ".tmp");
        this.outFile.deleteOnExit();

        TestSettings settings = new TestSettings();
        settings.putSetting("MessageStatsReport.output", outFile.getAbsolutePath());
        this.addSettingsToEnableSimScenario(settings);

        this.report = new MessageStatsReport();
        ArrayList<MessageListener> messageListeners = new ArrayList<>(1);
        messageListeners.add(this.report);

        this.utils = new TestUtils(new ArrayList<ConnectionListener>(), messageListeners, settings);
        this.utils.setGroupId("group");

        this.playScenario();
    }

    private void addSettingsToEnableSimScenario(TestSettings settings) {
        settings.putSetting("Group.groupID", "group");
        settings.putSetting("Group.nrofHosts", "3");
        settings.putSetting("Group.nrofInterfaces", "0");
        settings.putSetting("Group.movementModel", "StationaryMovement");
        settings.putSetting("Group.nodeLocation", "0, 0");
        settings.putSetting("Group.router", "EpidemicRouter");
    }

    @After
    public void cleanUp() {
        // SimScenario was called by MessageStatsReport and therefore initiated with this specific test's settings.
        // Cleanup to make further tests with other settings possible.
        SimScenario.reset();
    }

    private void playScenario() {
        DTNHost a = this.utils.createHost();
        DTNHost b = this.utils.createHost();
        DTNHost c = this.utils.createHost();

        a.createNewMessage(new BroadcastMessage(a, "m1", 50));
        a.sendMessage("m1", b);
        b.messageTransferred("m1", a);
        a.sendMessage("m1", c);
        c.messageAborted("m1", a, 34);
        b.deleteMessage("m1", false);

        b.createNewMessage(new Message(b, c, "m2", 100));
        b.sendMessage("m2", a);
        a.messageTransferred("m2", b);
        a.sendMessage("m2", c);
        c.messageTransferred("m2", a);
        a.deleteMessage("m2", true);
    }

    /**
     * Tests that message events like creation, delivery and dropping are counted correctly.
     * Also tests some, but not all, of the other statistics. // TODO: Test all.
     * @throws IOException If the temporary file cannot be opened, read or closed.
     */
    @Test
    public void testDoneCorrectlyCountsMessageEvents() throws  IOException{

		this.report.done();

		try (
                BufferedReader reader = new BufferedReader(new FileReader(outFile))
                ){

            reader.skip(2); // read comment lines

            assertEquals(
                    "Reported number of created messages should have been different.",
                    "created: 3",
                    reader.readLine());
            assertEquals(
                    "Reported number of started messages should have been different.",
                    "started: 4",
                    reader.readLine());
            assertEquals(
                    "Reported number of relayed messages should have been different.",
                    "relayed: 3",
                    reader.readLine());
            assertEquals(
                    "Reported number of aborted messages should have been different.",
                    "aborted: 1",
                    reader.readLine());
            assertEquals(
                    "Reported number of dropped messages should have been different.",
                    "dropped: 1",
                    reader.readLine());
            assertEquals(
                    "Reported number of removed messages should have been different.",
                    "removed: 1",
                    reader.readLine());
            assertEquals(
                    "Reported number of delivered messages should have been different.",
                    "delivered: 2",
                    reader.readLine());
            assertEquals(
                    "Reported delivery probability should have been different.",
                    "delivery_prob: 0.6667",
                    reader.readLine());
            reader.skip(1); // responses are not tested in this scenario
            assertEquals(
                    "Reported overhead ratio should have been different",
                    "overhead_ratio: 0.5000",
                    reader.readLine());
            reader.skip(2); // latency is not tested in this scenario
            assertEquals(
                    "Reported average hopcount should have been different.",
                    "hopcount_avg: 1.5000",
                    reader.readLine());
            assertEquals(
                    "Reported median hopcount should have been different.",
                    "hopcount_med: 2",
                    reader.readLine());
        }

	}
}
