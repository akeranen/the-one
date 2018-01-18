package test;

import core.SimError;
import input.ExternalEvent;
import input.VhmEventEndEvent;
import input.VhmEventReader;
import input.VhmEventStartEvent;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the {@link input.VhmEventReader} class.
 *
 * Created by Britta Heymann on 31.03.2017.
 */
public class VhmEventReaderTest {
    /* Different file paths used in test.s */
    private static final String CORRECT_JSON = "ee/test/VHMTestEvents.json";
    private static final String JSON_MISSING_VERSION = "ee/test/VHMTestMissingVersion.json";
    private static final String JSON_WRONG_VERSION = "ee/test/VhmEventReaderTestWrongVersion.json";
    private static final String NON_EXISTENT_PATH = "foo/bar/woo";
    private static final String JSON_SYNTAX_ERROR = "ee/test/VhmEventReaderTestSyntaxError.json";

    /** Number of external events per VhmEvent (VhmEventStartEvent and VhmEventEndEvent). */
    private static final int EXTERNAL_EVENTS_PER_VHM_EVENT = 2;

    /** Number of events in the CORRECT_JSON file. */
    private static final int MAX_EVENT_COUNT = 3;

    /** File at CORRECT_JSON path. */
    private File correctFile;

    /**
     * Load file needed for tests.
     * @throws FileNotFoundException might throw an exception if the file is not found
     */
    public VhmEventReaderTest() throws FileNotFoundException {
        this.correctFile = new File(CORRECT_JSON);
    }

    @Test (expected = SimError.class)
    public void testConstructorThrowsErrorForFileMissingReaderVersion(){
        new VhmEventReader(new File(JSON_MISSING_VERSION));
    }

    @Test(expected = SimError.class)
    public void testConstructorThrowsErrorForPathToNonExistentFile() {
        new VhmEventReader(new File(NON_EXISTENT_PATH));
    }

    @Test(expected = SimError.class)
    public void testConstructorThrowsErrorForFileWithWrongReaderVersion() {
        new VhmEventReader(new File(JSON_WRONG_VERSION));
    }

    @Test(expected = SimError.class)
    public void testConstructorThrowsErrorForFileWithSyntaxError() {
        new VhmEventReader(new File(JSON_SYNTAX_ERROR));
    }

    @Test
    public void testConstructorDoesNotThrowForCorrectJson() {
        VhmEventReader reader = new VhmEventReader(correctFile);
        assertNotNull("Expected VhmEventReader object.", reader);
    }

    @Test
    public void testReadEventsCreatesStartAndEndEvents() {
        List<ExternalEvent> firstEvent = loadVhmEventsFile(correctFile, 1);
        assertEquals(
                "Expected two external events for each original event.",
                EXTERNAL_EVENTS_PER_VHM_EVENT,
                firstEvent.size());
        assertTrue("Expected start event.", firstEvent.get(0) instanceof VhmEventStartEvent);
        assertTrue("Expected end event.", firstEvent.get(1) instanceof VhmEventEndEvent);
    }

    @Test
    public void testReadEventsReadsSpecifiedNumberOfEvents() {
        List<ExternalEvent> events = loadVhmEventsFile(correctFile, MAX_EVENT_COUNT - 1);
        assertEquals(
                "Expected different number of events.",
                (long) EXTERNAL_EVENTS_PER_VHM_EVENT * (MAX_EVENT_COUNT - 1),
                (long) events.size());
    }

    /**
     * Load a specified number of events from a file
     *
     * @param eventFile the used file
     * @param eventCount the maximal number of loaded events
     * @return a list with events
     */
    private static List<ExternalEvent> loadVhmEventsFile(File eventFile, int eventCount){
        try {
            VhmEventReader reader = new VhmEventReader(eventFile);
            return reader.readEvents(eventCount);
        } catch (SimError e) {
            TestCase.fail("Correct VHM events file could not be loaded: " + e);
        }
        return new ArrayList<>();
    }
}
