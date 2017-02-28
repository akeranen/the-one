package test;

import core.ConnectionListener;
import core.MessageListener;
import core.SimError;
import input.ExternalEvent;
import input.VhmEvent;
import input.VHMEventReader;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class tests the parsing of VHMEvents from json files
 *
 * Created by mellich on 17.02.17.
 */
public class VhmEventsTest {

    public static final String CORRECT_FILE = "ee/test/VHMTestEvents.json";
    public static final String MISSING_VERSION = "ee/test/VHMTestMissingVersion.json";

    public static final int MAX_EVENT_COUNT = 3;

    TestUtils utils;

    File correctFile;
    File incorrectFile;

    @Before
    public void setUp() throws Exception{
        correctFile = new File(CORRECT_FILE);
        incorrectFile = new File(MISSING_VERSION);
        TestSettings settings = new TestSettings();
        this.addSettingsToEnableSimScenario(settings);
        utils = new TestUtils(new ArrayList<ConnectionListener>(), new ArrayList<MessageListener>(), settings);
    }

    @Test
    public void testLoadCorrectFile(){
        List<ExternalEvent> eventList = new ArrayList<>();
        for (int eventCount = 0; eventCount < MAX_EVENT_COUNT; eventCount++) {
            try {
                VHMEventReader reader = new VHMEventReader(correctFile);
                eventList = reader.readEvents(eventCount);
                TestCase.assertTrue(eventCount + " events should be in list. Currently: " +
                        eventList.size(), eventList.size() == 2 * eventCount);
            } catch (Exception e) {
                TestCase.fail("Correct VHM events file could not be loaded!");
            }
            for (ExternalEvent e : eventList){
                VhmEvent ev = (VhmEvent) e;
                TestCase.assertTrue("Start time is before end time!",ev.getStartTime() <= ev.getEndTime());
            }
        }


    }

    private void addSettingsToEnableSimScenario(TestSettings settings) {
        settings.putSetting("Group.groupID", "group");
        settings.putSetting("Group.nrofHosts", "3");
        settings.putSetting("Group.nrofInterfaces", "0");
        settings.putSetting("Group.movementModel", "StationaryMovement");
        settings.putSetting("Group.nodeLocation", "0, 0");
        settings.putSetting("Group.router", "EpidemicRouter");
    }

    @Test (expected = SimError.class)
    public void testLoadIncorrectFile(){
        VHMEventReader reader = new VHMEventReader(incorrectFile);
    }
}
