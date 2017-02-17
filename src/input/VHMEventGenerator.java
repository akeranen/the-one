package input;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Generator for VHMEvents. This class does not strictly generate events,
 * but reads defined events from the settings file and provides an event queue
 * for the use with the ONE
 *
 * Created by Marius Meyer on 15.02.17.
 */
public class VHMEventGenerator implements EventQueue {

    /**
     * Queue that includes all not yet started events ordered by their start time.
     */
    private Queue<VHMEvent> eventStartQueue;

    /**
     * Queue that includes all not yet ended events ordered by their end time.
     */
    private Queue<VHMEvent> eventEndQueue;

    /**
     * Time point of the next event
     */
    private double nextEventTime;


    public VHMEventGenerator(){
        this.eventStartQueue = new ArrayDeque<>();
        this.eventEndQueue = new ArrayDeque<>();
        this.nextEventTime = Double.MAX_VALUE;

        //TODO: Create events and insert them into queues
    }

    @Override
    public ExternalEvent nextEvent() {
        VHMEvent nextEvent = null;
        if (!eventStartQueue.isEmpty()) {
            nextEventTime = eventStartQueue.peek().getStartTime();
            nextEvent = new VHMEventStartEvent(eventStartQueue.poll());
        }
        if (!eventEndQueue.isEmpty() && nextEventTime > eventStartQueue.peek().getStartTime()) {
            nextEventTime = eventStartQueue.peek().getStartTime();
            nextEvent = new VHMEventEndEvent(eventEndQueue.poll());
        }
        return nextEvent;
    }

    @Override
    public double nextEventsTime() {
        return nextEventTime;
    }
}
