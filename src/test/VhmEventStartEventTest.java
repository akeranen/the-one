package test;

import input.VhmEventNotifier;
import input.VhmEventStartEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Contains tests for the {@link input.VhmEventStartEvent} class.
 *
 * Created by Britta Heymann on 02.04.2017.
 */
public class VhmEventStartEventTest extends AbstractProcessableVhmEventTest {
    @Test
    public void testEventTimeEqualsVhmEventStart() {
        VhmEventStartEvent startEvent = new VhmEventStartEvent(this.event);
        assertEquals(
                "Time should have been equal to the event's start.",
                this.event.getStartTime(),
                startEvent.getTime(),
                DOUBLE_COMPARING_DELTA);
    }

    /**
     * Verifies that processEvent sends an event started notification via the {@link VhmEventNotifier}.
     */
    @Override
    public void testProcessEvent() {
        // Set up a listener.
        RecordingVhmEventListener recorder = new RecordingVhmEventListener();
        VhmEventNotifier.addListener(recorder);

        // Create start event.
        VhmEventStartEvent startEvent = new VhmEventStartEvent(this.event);

        // Process it.
        startEvent.processEvent(this.world);

        // Check the listener.
        assertEquals(
                "Number of recorded vhmEventStarted calls should have been different.",
                1,
                recorder.getNumberOfEventStartedCalls());
        assertEquals("Recorded event is not as expected.", startEvent, recorder.getLastEvent());
    }
}
