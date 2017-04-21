package input;

import core.Settings;

/**
 * Broadcast creation external events generator. Creates uniformly distributed
 * broadcast creation patterns whose message size and inter-message intervals can
 * be configured.
 *
 * Created by Britta Heymann on 22.02.2017.
 */
public class BroadcastEventGenerator extends AbstractMessageEventGenerator {
    public BroadcastEventGenerator(Settings s) {
        super(s, true);
    }

    /**
     * Returns the next broadcast creation event.
     * @see EventQueue#nextEvent()
     */
    @Override
    public ExternalEvent nextEvent() {
        /* Message is a one way message */
        int responseSize = 0;

        /* Draw additional message properties and create message. */
        double interval = this.drawNextEventTimeDiff();
        int sender = this.drawHostAddress(this.hostRange);
        int priority = this.drawPriority();
        ExternalEvent messageCreateEvent = new BroadcastCreateEvent(
                sender,
                this.getID(),
                this.drawMessageSize(),
                responseSize,
                this.nextEventsTime,
                priority);

        /* Update next event time before returning. */
        this.advanceToNextEvent(interval);
        return messageCreateEvent;
    }
}
