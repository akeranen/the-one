package test;

import core.BroadcastMessage;
import core.ConnectionListener;
import core.Coord;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.Group;
import core.Message;
import core.MessageListener;
import core.MulticastMessage;
import core.Settings;
import core.SimScenario;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import report.MessageStatsReport;
import util.Tuple;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

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
        DTNHost.reset();
        Settings.init(null);
        java.util.Locale.setDefault(java.util.Locale.US);

        this.outFile = File.createTempFile("mgtest", ".tmp");
        this.outFile.deleteOnExit();

        TestSettings settings = new TestSettings();
        settings.putSetting("MessageStatsReport.output", outFile.getAbsolutePath());
        TestSettings.addSettingsToEnableSimScenario(settings);

        this.report = new MessageStatsReport();
        ArrayList<MessageListener> messageListeners = new ArrayList<>(1);
        messageListeners.add(this.report);

        this.utils = new TestUtils(new ArrayList<ConnectionListener>(), messageListeners, settings);
        this.utils.setGroupId("group");

        this.playScenario();
    }

    @After
    public void cleanUp() {
        // SimScenario was called by MessageStatsReport and therefore initiated with this specific test's settings.
        // Cleanup to make further tests with other settings possible.
        SimScenario.reset();
        DTNHost.reset();
        Group.clearGroups();
    }

    private void playScenario() {
        DTNHost a = this.utils.createHost("a");
        DTNHost b = this.utils.createHost("b");
        DTNHost c = this.utils.createHost("c");
        DTNHost d = this.utils.createHost("d");

        //BroadcastMessage
        a.createNewMessage(new BroadcastMessage(a, "m1", 50));
        a.sendMessage("m1", b);
        b.messageTransferred("m1", a);
        a.sendMessage("m1", c);
        c.messageAborted("m1", a, 34);
        b.deleteMessage("m1", false);

        //OneToOne Messsage
        b.createNewMessage(new Message(b, c, "m2", 100));
        b.sendMessage("m2", a);
        a.messageTransferred("m2", b);
        a.sendMessage("m2", c);
        c.messageTransferred("m2", a);
        a.deleteMessage("m2", true);

        //Multicast Message
        Group g = Group.createGroup(0);
        g.addHost(c);
        g.addHost(b);
        g.addHost(d);
        c.createNewMessage(new MulticastMessage(c,g,"m3",150));
        c.sendMessage("m3",a);
        a.messageTransferred("m3",c);
        a.sendMessage("m3",b);
        b.messageTransferred("m3",a);
        b.sendMessage("m3",d);
        d.messageTransferred("m3",b);
        d.deleteMessage("m3",true);

        //Data Message
        DisasterData data = new DisasterData(DisasterData.DataType.MAP, 0, 0, new Coord(0,0));
        d.createNewMessage(new DataMessage(d, a, "m4", Collections.singleton(new Tuple<>(data, 1D)), 1));
        d.sendMessage("m4", a);
        a.messageTransferred("m4", d);
        d.createNewMessage(new DataMessage(d, b, "m5", Collections.singleton(new Tuple<>(data, 0.5)), 0));
        d.deleteMessage("m5", true);

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

            reader.readLine(); // read comment lines
            reader.readLine(); // read comment lines
            assertEquals(
                    "Reported number of created messages should have been different.",
                    "created: 7",
                    reader.readLine());
            assertEquals(
                    "Reported number of started messages should have been different.",
                    "started: 8",
                    reader.readLine());
            assertEquals(
                    "Reported number of relayed messages should have been different.",
                    "relayed: 7",
                    reader.readLine());
            assertEquals(
                    "Reported number of aborted messages should have been different.",
                    "aborted: 1",
                    reader.readLine());
            assertEquals(
                    "Reported number of dropped messages should have been different.",
                    "dropped: 3",
                    reader.readLine());
            assertEquals(
                    "Reported number of removed messages should have been different.",
                    "removed: 1",
                    reader.readLine());
            assertEquals(
                    "Reported number of delivered messages should have been different.",
                    "delivered: 5",
                    reader.readLine());
            assertEquals(
                    "Reported delivery probability should have been different.",
                    "delivery_prob: 0.7143",
                    reader.readLine());
            reader.readLine(); // responses are not tested in this scenario
            assertEquals(
                    "Reported overhead ratio should have been different",
                    "overhead_ratio: 0.4000",
                    reader.readLine());
            reader.readLine(); // latency is not tested in this scenario
            reader.readLine(); // latency is not tested in this scenario
            assertEquals(
                    "Reported average hopcount should have been different.",
                    "hopcount_avg: 1.8000",
                    reader.readLine());
            assertEquals(
                    "Reported median hopcount should have been different.",
                    "hopcount_med: 2",
                    reader.readLine());
        }

	}
}
