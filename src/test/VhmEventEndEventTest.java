package test;

import input.VhmEventEndEvent;
import input.VhmEventNotifier;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Contains tests for the {@link input.VhmEventEndEvent} class.
 *
 * Created by Britta Heymann on 02.04.2017.
 */
public class VhmEventEndEventTest extends ProcessableVhmEventTest {
    @Test
    public void testEventTimeEqualsVhmEventEnd() {
        VhmEventEndEvent endEvent = new VhmEventEndEvent(this.event);
        assertEquals(
                "Time should have been equal to the event's end.",
                this.event.getEndTime(),
                endEvent.getTime(),
                DOUBLE_COMPARING_DELTA);
    }

    @Test
    public void testProcessEventSendsEventEndedViaVhmEventNotifier() {
        // Set up a listener.
        RecordingVhmEventListener recorder = new RecordingVhmEventListener();
        VhmEventNotifier.addListener(recorder);

        // Create end event.
        VhmEventEndEvent endEvent = new VhmEventEndEvent(this.event);

        // Process it.
        endEvent.processEvent(this.world);

        // Check the listener.
        assertEquals(
                "Number of recorded vhmEventEnded calls should have been different.",
                1,
                recorder.getNumberOfEventEndedCalls());
        assertEquals("Recorded event is not as expected.", endEvent, recorder.getLastEvent());
    }
}
