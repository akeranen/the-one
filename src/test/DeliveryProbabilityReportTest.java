package test;

import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SimScenario;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import report.DeliveryProbabilityReport;

import test.TestSettings;
import test.TestUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Contains Unit tests for the DeliveryProbabilityReport Class.
 *
 * @author Nils Weidmann
 */
public class DeliveryProbabilityReportTest {
    private DeliveryProbabilityReport report;
    private TestUtils utils;
    private File outFile;

    public void init() throws IOException{

        SimScenario.reset();
        Settings.init(null);
        java.util.Locale.setDefault(java.util.Locale.US);

        this.outFile = File.createTempFile("mgtest", ".tmp");
        this.outFile.deleteOnExit();

        TestSettings settings = new TestSettings();
        settings.putSetting("DeliveryProbabilityReport.output", outFile.getAbsolutePath());
        this.addSettingsToEnableSimScenario(settings);

        this.report = new DeliveryProbabilityReport();
        ArrayList<MessageListener> messageListeners = new ArrayList<>(1);
        messageListeners.add(this.report);

        this.utils = new TestUtils(new ArrayList<ConnectionListener>(), messageListeners, settings);
        this.utils.setGroupId("group");

        this.playScenario();
    }
    
    /**
     * Adds settings for the scenario  
     * @param settings
     */
    private void addSettingsToEnableSimScenario(TestSettings settings) {
        settings.putSetting("Group.groupID", "group");
        settings.putSetting("Group.nrofHosts", "3");
        settings.putSetting("Group.nrofInterfaces", "0");
        settings.putSetting("Group.movementModel", "StationaryMovement");
        settings.putSetting("Group.nodeLocation", "0, 0");
        settings.putSetting("Group.router", "EpidemicRouter");
    }

    @After
    /**
     * SimScenario was called by DeliveryProbabilityReport and therefore initiated with this specific test's settings.
       Cleanup to make further tests with other settings possible.
     */
    public void cleanUp() {

        SimScenario.reset();
    }
    
    /**
     * Exchange messages between nodes a,b and c. The messages from a to c is aborted, while the others are delivered
     * successfully.
     */
    private void playScenario() {
        DTNHost a = this.utils.createHost();
        DTNHost b = this.utils.createHost();
        DTNHost c = this.utils.createHost();

        a.createNewMessage(new Message(a, c, "m1", 50));
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
        
        c.createNewMessage(new Message(c, a, "m3", 150));
        c.sendMessage("m3", b);
        b.messageTransferred("m3", c);
        b.sendMessage("m3", a);
        a.messageTransferred("m3", b);
        b.deleteMessage("m3", true);
    }

    /**
     * Tests that message events creating and delivery are counted correctly.
     * TODO: Add this test to Test all.
     * @throws IOException If the temporary file cannot be opened, read or closed.
     */
    @Test
    public void testDoneCorrectlyCountsMessageEvents() throws  IOException{

    	init();
		this.report.done();

		try (
                BufferedReader reader = new BufferedReader(new FileReader(outFile))
                ){

            reader.readLine(); // read comment lines
            reader.readLine(); // read comment lines
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
}