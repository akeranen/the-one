package input;

import core.Settings;
import core.SettingsError;

/**
 * Message creation -external events generator. Creates uniformly distributed
 * message creation patterns to single hosts whose message size and inter-message intervals can
 * be configured.
 *
 * Created by Britta Heymann on 22.02.2017.
 */
public class MessageEventGenerator extends AbstractMessageEventGenerator {
    /** (Optional) receiver address range -setting id ({@value}).
     * If a value for this setting is defined, the destination hosts are
     * selected from this range and the source hosts from the
     * {@link #HOST_RANGE_S} setting's range.
     * The lower bound is inclusive and upper bound exclusive. */
    public static final String TO_HOST_RANGE_S = "tohosts";

    /** Range of host addresses that can be receivers */
    protected int[] toHostRange = null;
    /**
     * Constructor, initializes the interval between events,
     * and the size of messages generated, as well as number
     * of hosts in the network.
     *
     * @param s Settings for this generator.
     */
    public MessageEventGenerator(Settings s) {
        super(s, false);

        if (s.contains(TO_HOST_RANGE_S)) {
            this.toHostRange = s.getCsvInts(TO_HOST_RANGE_S, 2);
        }
        else {
            this.toHostRange = null;
        }

        this.checkHostRanges();
    }

    /**
     * Checks host ranges' validity and throws errors on invalid configurations.
     */
    private void checkHostRanges() {
        if (this.hostRange[1] - this.hostRange[0] < NUMBER_HOSTS_NEEDED_FOR_COMMUNICATION) {
            if (this.toHostRange == null) {
                throw new SettingsError("Host range must contain at least two "
                        + "nodes unless toHostRange is defined");
            } else if (toHostRange[0] == this.hostRange[0] &&
                    toHostRange[1] == this.hostRange[1]) {
                // XXX: teemuk: Since (X,X) == (X,X+1) in drawHostAddress()
                // there's still a boundary condition that can cause an
                // infinite loop.
                throw new SettingsError("If to and from host ranges contain" +
                        " only one host, they can't be the equal");
            }
        }
    }

    /**
     * Draws a destination host address that is different from the "from"
     * address
     * @param hostRange The range of hosts
     * @param from the "from" address
     * @return a destination address from the range, but different from "from"
     */
    protected int drawToAddress(int hostRange[], int from) {
        int to;
        do {
            to = this.toHostRange != null ? drawHostAddress(this.toHostRange):
                    drawHostAddress(this.hostRange);
        } while (from==to);

        return to;
    }

    /**
     * Returns the next message creation event
     *
     * @see EventQueue#nextEvent()
     */
    @Override
    public ExternalEvent nextEvent() {
        int responseSize = 0; /* zero stands for one way messages */
        int msgSize;
        int interval;
        int from;
        int to;

		/* Get two *different* nodes randomly from the host ranges */
        from = drawHostAddress(this.hostRange);
        to = drawToAddress(hostRange, from);

        msgSize = drawMessageSize();
        interval = drawNextEventTimeDiff();
        int priority = drawPriority();

		/* Create event and advance to next event */
        ExternalEvent messageCreateEvent = new MessageCreateEvent(
                from, to, this.getID(), msgSize, responseSize, this.nextEventsTime, priority);

        this.advanceToNextEvent(interval);

        return messageCreateEvent;
    }
}
