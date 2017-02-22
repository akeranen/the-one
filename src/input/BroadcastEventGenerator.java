package input;

import core.Settings;
import core.SettingsError;

/**
 * Broadcast creation external events generator. Creates uniformly distributed
 * broadcast creation patterns whose message size and inter-message intervals can
 * be configured.
 *
 * Created by Britta Heymann on 22.02.2017.
 */
public class BroadcastEventGenerator extends MessageEventGenerator {
    public BroadcastEventGenerator(Settings s) {
        super(s);

        if (this.toHostRange != null) {
            throw new SettingsError("Cannot handle receiver host range for broadcasts.");
        }
    }

    /**
     * Returns the next broadcast creation event.
     * @see EventQueue#nextEvent()
     */
    @Override
    public ExternalEvent nextEvent() {
        /* Message is a one way messages */
        int responseSize = 0;

        /* Draw additional message properties and create message. */
        int interval = this.drawNextEventTimeDiff();
        int sender = this.drawHostAddress(this.hostRange);
        ExternalEvent messageCreateEvent = new BroadcastCreateEvent(
                sender,
                this.getID(),
                this.drawMessageSize(),
                responseSize,
                this.nextEventsTime);

        /* Update next event time before returning. */
        this.advanceToNextEvent(interval);
        return messageCreateEvent;
    }
}
