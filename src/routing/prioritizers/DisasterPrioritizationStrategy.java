package routing.prioritizers;

import core.Connection;
import core.DTNHost;
import core.DataMessage;
import core.Message;
import core.Settings;
import core.SettingsError;
import core.SimClock;
import routing.DisasterRouter;
import routing.MessagePrioritizationStrategy;
import routing.MessageRouter;
import util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * A prioritization strategy that enables new messages to get a head start. In addition, the message prioritization
 * differentiates between data messages and ordinary messages.
 *
 * Created by Britta Heymann on 25.05.2017.
 */
public class DisasterPrioritizationStrategy implements MessagePrioritizationStrategy {
    /**
     * Maximum time since creation for which a message is a allowed a head start -setting id ({@value}).
     * Value in seconds. Messages which meet this threshold are sorted before all messages which don't.
     */
    public static final String HEAD_START_THRESHOLD_S = "headStartThreshold";

    /**
     * Value to return if the first index storing an item with a certain property was searched in a list, but no such
     * item was found.
     */
    private static final int INDEX_NOT_FOUND = -1;

    /**
     * Maximum time since creation for which a message is a allowed a head start, i.e. is sorted before all messages not
     * meeting the threshold.
     */
    private double headStartThreshold;

    /**
     * Prioritization used for all messages not getting a head start.
     */
    private DisasterPrioritization nonHeadStartPrioritization;

    /**
     * Initializes a new instance of the {@link DisasterPrioritizationStrategy} class.
     * @param s Settings to use.
     * @param attachedRouter Router prioritizing the messages.
     */
    public DisasterPrioritizationStrategy(Settings s, MessageRouter attachedRouter) {
        this.headStartThreshold = s.getDouble(HEAD_START_THRESHOLD_S);
        if (this.headStartThreshold < 0) {
            throw new SettingsError(
                    "Head start threshold must be non-negative, but is " + this.headStartThreshold + "!");
        }

        DisasterPrioritizationStrategy.checkRouterIsDisasterRouter(attachedRouter);
        this.nonHeadStartPrioritization = new DisasterPrioritization(s, (DisasterRouter)attachedRouter);
    }

    /**
     * Copy constructor.
     * @param strategy Original {@link DisasterPrioritizationStrategy} to copy settings from.
     * @param attachedRouter Router prioritizing the messages.
     */
    public DisasterPrioritizationStrategy(DisasterPrioritizationStrategy strategy, MessageRouter attachedRouter) {
        this.headStartThreshold = strategy.headStartThreshold;

        DisasterPrioritizationStrategy.checkRouterIsDisasterRouter(attachedRouter);
        this.nonHeadStartPrioritization =
                new DisasterPrioritization(strategy.nonHeadStartPrioritization, (DisasterRouter)attachedRouter);
    }

    /**
     * Checks if the router is a {@link DisasterRouter} and throws an {@link IllegalArgumentException} if it isn't.
     * @param router Router to check
     */
    private static void checkRouterIsDisasterRouter(MessageRouter router) {
        if (!(router instanceof DisasterRouter)) {
            throw new IllegalArgumentException(
                    "Disaster prioritization strategy cannot handle routers of type " + router.getClass() + "!");
        }
    }

    /**
     * Sets the attached host.
     *
     * @param host host prioritizing the messages.
     */
    @Override
    public void setAttachedHost(DTNHost host) {
        this.nonHeadStartPrioritization.setAttachedHost(host);
    }

    /**
     * Sorts the provided message - connection tuples according to strategy.
     *
     * @param messages The message - connection tuples to sort.
     * @return The provided messages in sorted order, most important messages first.
     */
    @Override
    public List<Tuple<Message, Connection>> sortMessages(Collection<Tuple<Message, Connection>> messages) {
        // Differentiate between head start and non head start messages.
        List<Tuple<Message, Connection>> headStartMessages = new ArrayList<>();
        List<Tuple<Message, Connection>> nonHeadStartMessages = new ArrayList<>();
        for (Tuple<Message, Connection> m : messages) {
            if (this.isHeadStartMessage(m.getKey())) {
                headStartMessages.add(m);
            } else {
                nonHeadStartMessages.add(m);
            }
        }

        // Sort non head start messages.
        nonHeadStartMessages.sort(this.nonHeadStartPrioritization);
        // Sort head start messages by creation time, newer messages first.
        headStartMessages.sort(Comparator.comparingDouble(t -> (-1) * t.getKey().getCreationTime()));

        // Insert head start messages before all other ordinary messages.
        int headStartMessagesIndex = DisasterPrioritizationStrategy.findFirstNonDataMessageIndex(nonHeadStartMessages);
        if (headStartMessagesIndex == INDEX_NOT_FOUND) {
            headStartMessagesIndex = nonHeadStartMessages.size();
        }
        nonHeadStartMessages.addAll(headStartMessagesIndex, headStartMessages);

        // Return the sorted messages.
        return nonHeadStartMessages;
    }

    /**
     * Finds the first index at which an item is stored that does not wrap a {@link DataMessage}.
     * @param messages Message list to search.
     * @return The found index, or {@link #INDEX_NOT_FOUND} if there was none.
     */
    private static int findFirstNonDataMessageIndex(List<Tuple<Message, Connection>> messages) {
        int firstOrdinaryMessageIndex = INDEX_NOT_FOUND;
        for (int i = 0; i < messages.size(); i++) {
            if (!(messages.get(i).getKey() instanceof DataMessage)) {
                firstOrdinaryMessageIndex = i;
                break;
            }
        }
        return firstOrdinaryMessageIndex;
    }

    /**
     * Determines whether the message should get a head start.
     * @param m The message to check.
     * @return True if the message is new and not a data message.
     */
    private boolean isHeadStartMessage(Message m) {
        return !(m instanceof DataMessage) && (SimClock.getTime() - m.getCreationTime()) <= this.headStartThreshold;
    }

    /**
     * Creates a replicate of this message prioritization strategy. The replicate has the same settings as this message
     * prioritization strategy but is attached to the provided router and has no attached host.
     *
     * @param attachedRouter Router prioritizing the messages.
     * @return The replicate.
     */
    @Override
    public MessagePrioritizationStrategy replicate(MessageRouter attachedRouter) {
        return new DisasterPrioritizationStrategy(this, attachedRouter);
    }
}
