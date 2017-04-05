package test;

import java.io.File;
import input.VhmEvent;
import input.VhmEventNotifier;
import input.VhmEventReader;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Contains tests for the {@link input.VhmEventNotifier} class
 *
 * Created by Britta Heymann on 01.04.2017.
 */
public class VhmEventNotifierTest {
    private RecordingVhmEventListener recorder;
    private VhmEvent event = VhmEventNotifierTest.loadSingleVhmEventFromFile();

    public VhmEventNotifierTest() {
        // Initialization of private variables happens in @Before method.
    }

    @Before
    public void addNewRecorderAsListener() {
        this.recorder = new RecordingVhmEventListener();
        VhmEventNotifier.addListener(recorder);
    }

    /**
     * Checks that added listeners are notified when eventEnded gets called.
     */
    @Test
    public void testEventEnded() {
        // Call eventEnded.
        VhmEventNotifier.eventEnded(event);

        // Assert it was forwarded to the listener.
        assertEquals(
                "Number of recorded vhmEventEnded calls should have been different.",
                1,
                recorder.getNumberOfEventEndedCalls());
        assertEquals("Recorded event is not as expected.", event, recorder.getLastEvent());
    }

    /**
     * Checks that added listeners are notified when eventStarted gets called.
     */
    @Test
    public void testEventStarted() {
        // Call eventStarted.
        VhmEventNotifier.eventStarted(event);

        // Assert it was forwarded to the listener.
        assertEquals(
                "Number of recorded vhmEventStarted calls should have been different.",
                1,
                recorder.getNumberOfEventStartedCalls());
        assertEquals("Recorded event is not as expected.", event, recorder.getLastEvent());
    }

    /**
     * Loads a single VHM event from a file containing test VHM events.
     * @return The loaded VHM event.
     */
    private static VhmEvent loadSingleVhmEventFromFile() {
        VhmEventReader reader = new VhmEventReader(new File("ee/test/VHMTestEvents.json"));
        return (VhmEvent)reader.readEvents(1).get(0);
    }
}
