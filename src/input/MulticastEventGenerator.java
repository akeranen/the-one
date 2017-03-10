package input;


import core.Settings;

/**
 * Multicast creation external events generator. Creates uniformly distributed
 * multicast creation patterns whose message size and inter-message intervals can
 * be configured.
 *
 * Created by Marius Meyer on 08.03.17.
 */
public class MulticastEventGenerator extends AbstractMessageEventGenerator {

    /** groups address range -setting id ({@value}).
     * The lower bound is inclusive and upper bound exclusive. */
    public static final String GROUP_RANGE_S = "groups";

    /**
     * range group addresses are chosen from
     */
    private int[] groupRange = {0,0};

    /**
     * Constructor, initializes the interval between events,
     * and the size of messages generated, as well as number
     * of hosts in the network.
     *
     * @param s                           Settings for this generator.
     */
    public MulticastEventGenerator(Settings s) {
        super(s, true);
        this.groupRange = s.getCsvInts(GROUP_RANGE_S, Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE);
        s.assertValidRange(this.groupRange, GROUP_RANGE_S);
    }

    /**
     * Returns the next multicast creation event.
     * @see EventQueue#nextEvent()
     */
    @Override
    public ExternalEvent nextEvent() {
        /* Message is a one way message */
        int responseSize = 0;

        /* Draw additional message properties and create message. */
        int interval = this.drawNextEventTimeDiff();
        int sender = this.drawHostAddress(this.hostRange);
        int group = this.drawHostAddress(this.groupRange);
        ExternalEvent messageCreateEvent = new MulticastCreateEvent(
                sender,
                group,
                this.getID(),
                this.drawMessageSize(),
                responseSize,
                this.nextEventsTime);

        /* Update next event time before returning. */
        this.advanceToNextEvent(interval);
        return messageCreateEvent;
    }
}
