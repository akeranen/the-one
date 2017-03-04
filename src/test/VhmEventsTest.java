package test;

import core.SimError;
import gui.playfield.VhmEventGraphic;
import input.ExternalEvent;
import input.VhmEvent;
import input.VhmEventReader;
import input.VhmEventStartEvent;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class tests classes depending on VhmEvent
 *
 * Created by Marius Meyer on 17.02.17.
 */
public class VhmEventsTest {

    public static final String CORRECT_FILE = "ee/test/VHMTestEvents.json";
    public static final String MISSING_VERSION = "ee/test/VHMTestMissingVersion.json";

    public static final int MAX_EVENT_COUNT = 3;

    File correctFile;
    File incorrectFile;

    /**
     * Load the needed files for the tests
     *
     * @throws FileNotFoundException might throw an exception if the files are not found
     */
    @Before
    public void setUp() throws FileNotFoundException{
        correctFile = new File(CORRECT_FILE);
        incorrectFile = new File(MISSING_VERSION);
    }

    /**
     * Tests the parsing of a correct event file
     */
    @Test
    public void testLoadCorrectFile(){
        List<ExternalEvent> eventList;
        for (int eventCount = 0; eventCount < MAX_EVENT_COUNT; eventCount++) {
            eventList = loadVhmEventsFile(correctFile,eventCount);
            TestCase.assertTrue(eventCount + " events should be in list. Currently: " +
                    eventList.size(), eventList.size() == 2 * eventCount);
        }
    }

    /**
     * Tests the equals method of the VhmEvent
     */
    @Test
    public void testVhmEventEqualsMethod(){
        List<ExternalEvent> eventList = loadVhmEventsFile(correctFile,MAX_EVENT_COUNT);
        testObjectEquality((List<Object>) (List<?>) eventList);
    }

    /**
     * Tests the equals method of the VhmEventGraphics
     */
    @Test
    public void testVhmEventGraphicsEqualsMethod(){
        List<ExternalEvent> eventList = loadVhmEventsFile(correctFile,MAX_EVENT_COUNT);
        List<VhmEventGraphic> graphicsList = new ArrayList<>();
        for (ExternalEvent ev: eventList){
            if (ev instanceof VhmEventStartEvent){
                graphicsList.add(new VhmEventGraphic((VhmEvent) ev));
            }
        }
        testObjectEquality((List<Object>) (List<?>) graphicsList);
    }

    /**
     * Tests loading a invalid event file
     */
    @Test (expected = SimError.class)
    public void testLoadIncorrectFile(){
        VhmEventReader reader = new VhmEventReader(incorrectFile);
        reader.readEvents(MAX_EVENT_COUNT);
    }

    /**
     * Load a specified number of events from a file
     *
     * @param eventFile the used file
     * @param eventCount the maximal number of loaded events
     * @return a list with events
     */
    private List<ExternalEvent> loadVhmEventsFile(File eventFile,int eventCount){
        try {
            VhmEventReader reader = new VhmEventReader(eventFile);
            return reader.readEvents(eventCount);
        } catch (Exception e) {
            TestCase.fail("Correct VHM events file could not be loaded: " + e);
        }
        return new ArrayList<>();
    }

    /**
     * Check for the whole given list, if the equals method is working
     *
     * @param list the list that is checked
     */
    private void testObjectEquality(List<Object> list){
        for (int i = 0; i < list.size(); i++){
            for (int k = 0; k < list.size(); k++){
                if (i == k){
                    TestCase.assertEquals(list.get(i),list.get(k));
                } else {
                    TestCase.assertNotSame(list.get(i),list.get(k));
                }
            }
        }
    }
}
