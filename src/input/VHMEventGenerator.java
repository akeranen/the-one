package input;

import java.util.*;

/**
 * Created by Marius Meyer on 15.02.17.
 */
public class VHMEventGenerator implements EventQueue {

    private Queue<VHMEvent> eventStartQueue;
    private Queue<VHMEvent> eventEndQueue;
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
