package routing.util;

import core.BroadcastMessage;
import core.Message;
import core.Settings;
import core.SimClock;
import routing.DisasterRouter;
import routing.MessageRouter;

import java.util.Comparator;
import java.util.HashMap;

/**
 * Compares two messages deciding which one should be deleted first, if the need occurs.
 *
 * Created by Britta Heymann on 21.07.2017.
 */
// Suppress warning about unserializable comparator because the rule's assumption that the overhead to make it
// serializable is low does not fit for this one (routers are not serializable!).
@SuppressWarnings({"squid:S2063"})
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
     * Comparator used between messages which have a high rank / should be deleted later than others because they
     * have not traveled far in the network and have been in the host's buffer only for a short time.
     *
     * These messages are first sorted by hop count, then by time spent in the buffer. Messages which have been
     * in the buffer for a longer time and have a higher hop count are deleted faster.
     */
    private Comparator<Message> highRankMessageComparator =
            Comparator.<Message> comparingInt(m -> (-1) * m.getHopCount()).thenComparing(Message::getReceiveTime);

    /**
     * Caches deletion rank values for messages not having a high rank. Very useful because comparators may be called
     * multiple times for each item.
     * Invalidated every timestep to ensure correct value.
     */
    private HashMap<Message, Double> deletionRankCache = new HashMap<>();
    /**
     * The simulation time the current {@link #deletionRankCache} is for.
     */
    private double cacheTime;

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
        // If both messages have a high rank or both don't, use respective sorting mechanisms.
        if (this.hasHighRank(m1) && this.hasHighRank(m2)) {
            return this.highRankMessageComparator.compare(m1, m2);
        }
        if (!this.hasHighRank(m1) && !this.hasHighRank(m2)) {
            return Double.compare(this.computeDeletionRankValue(m1), this.computeDeletionRankValue(m2));
        }
        // Else, return a value depending on which message has a high rank.
        return Boolean.compare(this.hasHighRank(m1), this.hasHighRank(m2));
    }

    /**
     * Returns whether the provided message has a high rank / should be deleted later than others because it
     * has not traveled far in the network and has been in the host's buffer only for a short time.
     * @param m The message to check.
     * @return Whether the provided message has a high rank.
     */
    private boolean hasHighRank(Message m) {
        return m.getHopCount() < this.hopThreshold && (SimClock.getTime() - m.getReceiveTime()) < this.ageThreshold;
    }

    /**
     * Computes a value indicating how useful it is to keep the provided message in the host's buffer. In other words,
     * high values mean that the message should be deleted later.
     * @param m Message to compute the value for.
     * @return Value indicating how useful the message is in the host's buffer.
     */
    private double computeDeletionRankValue(Message m) {
        // Invalidate cache if required.
        this.possiblyInvalidateCache();

        // Then: If we already have the deletion rank cached, don't compute it.
        Double cachedValue = this.deletionRankCache.get(m);
        if (cachedValue != null) {
            return cachedValue;
        }

        // Else: Compute the value...
        Double deletionRank;
        if (m instanceof BroadcastMessage) {
            deletionRank = 1 - this.attachedRouter.getReplicationsDensity(m);
        } else {
            deletionRank = this.attachedRouter.getDeliveryPredictability(m);
        }

        // ...and cache it before returning.
        this.deletionRankCache.put(m, deletionRank);
        return deletionRank;
    }

    /**
     * Invalidates the {@link #deletionRankCache} if it is obsolete.
     */
    private void possiblyInvalidateCache() {
        if (SimClock.getTime() > this.cacheTime) {
            this.deletionRankCache.clear();
            this.cacheTime = SimClock.getTime();
        }
    }

    /**
     * Returns the used hop count threshold.
     * @return The hop count threshold.
     */
    public int getHopThreshold() {
        return hopThreshold;
    }

    /**
     * Returns the used age threshold.
     * @return The age threshold.
     */
    public double getAgeThreshold() {
        return ageThreshold;
    }
}
