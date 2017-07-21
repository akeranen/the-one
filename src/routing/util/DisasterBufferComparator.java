package routing.util;

import core.Message;
import core.Settings;
import core.SettingsError;
import core.SimClock;
import routing.DisasterRouter;
import routing.MessageRouter;

import java.util.Comparator;

/**
 * Compares two messages deciding which one should be deleted first, if the need occurs.
 *
 * Created by Britta Heymann on 21.07.2017.
 */
public class DisasterBufferComparator implements Comparator<Message> {
    /** Namespace for all disaster buffer comparator settings. */
    public static final String DISASTER_BUFFER_NS = "DisasterBufferComparator";

    /**
     * Exclusive maximum number of hops for which a message may be assigned a high rank -setting id ({@value}).
     * Messages which meet this threshold and the age threshold {@link #AGE_THRESHOLD_S} are deleted only after the
     * messages not meeting one of these two thresholds.
     */
    public static final String HOP_THRESHOLD_S = "hopThreshold";

    /**
     * Exclusive maximum time span for which a message may be assigned a high rank -setting id ({@value}.
     * Messages which meet this threshold and the hop threshold {@link #HOP_THRESHOLD_S} are deleted only after the
     * messages not meeting one of these two thresholds.
     */
    public static final String AGE_THRESHOLD_S = "ageThreshold";

    /**
     * Exclusive maximum number of hops for which a message may be assigned a high rank.
     * Messages which meet this threshold and the age threshold {@link #ageThreshold} are deleted only after the
     * messages not meeting one of these two thresholds.
     */
    private int hopThreshold;

    /**
     * Exclusive maximum time span for which a message may be assigned a high rank -setting id ({@value}.
     * Messages which meet this threshold and the hop threshold {@link #hopThreshold} are deleted only after the
     * messages not meeting one of these two thresholds.
     */
    private double ageThreshold;

    /**
     * The router handling this buffer.
     */
    private DisasterRouter attachedRouter;

    /**
     * Initializes a new instance of the {@link DisasterBufferComparator} class.
     * @param attachedRouter Router deleting the messages from buffer.
     */
    public DisasterBufferComparator(MessageRouter attachedRouter) {
        Settings s = new Settings(DISASTER_BUFFER_NS);
        this.hopThreshold = s.getInt(HOP_THRESHOLD_S);
        this.ageThreshold = s.getDouble(AGE_THRESHOLD_S);

        DisasterBufferComparator.checkRouterIsDisasterRouter(attachedRouter);
        this.attachedRouter = (DisasterRouter)attachedRouter;
    }

    /**
     * Copy constructor.
     * @param bufferComparator Original {@link DisasterBufferComparator} to copy settings from.
     * @param attachedRouter Router prioritizing the messages.
     */
    public DisasterBufferComparator(DisasterBufferComparator bufferComparator, MessageRouter attachedRouter) {
        this.hopThreshold = bufferComparator.hopThreshold;
        this.ageThreshold = bufferComparator.ageThreshold;

        DisasterBufferComparator.checkRouterIsDisasterRouter(attachedRouter);
        this.attachedRouter = (DisasterRouter)attachedRouter;
    }

    /**
     * Checks if the router is a {@link DisasterRouter} and throws an {@link IllegalArgumentException} if it isn't.
     * @param router Router to check
     */
    private static void checkRouterIsDisasterRouter(MessageRouter router) {
        if (router == null) {
            throw new IllegalArgumentException("Router is null!");
        }
        if (!(router instanceof DisasterRouter)) {
            throw new IllegalArgumentException(
                    "Disaster routing buffer management cannot handle routers of type " + router.getClass() + "!");
        }
    }

    /**
     * Compares two messages.
     * @param m1 the first message to be compared.
     * @param m2 the second message to be compared.
     * @return a negative integer, zero, or a positive integer as the
     * first message should be deleted before, at the same time, or after the second message.
     */
    @Override
    public int compare(Message m1, Message m2) {
        if (this.isHighRank(m1) && this.isHighRank(m2)) {
            return Comparator.comparing(Message::getReceiveTime).thenComparing(Message::getHopCount).compare(m1, m2);
        }
        if (!this.isHighRank(m1) && !this.isHighRank(m2)) {

        }
    }

    private boolean isHighRank(Message m) {
        return m.getHopCount() < this.hopThreshold && (SimClock.getTime() - m.getReceiveTime()) < this.ageThreshold;
    }
}
