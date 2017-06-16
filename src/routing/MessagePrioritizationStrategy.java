package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import util.Tuple;

import java.util.Collection;
import java.util.List;

/**
 * Strategy to prioritize messages in routing.
 *
 * Created by Britta Heymann on 19.05.2017.
 */
public interface MessagePrioritizationStrategy {
    /**
     * Sorts the provided message - connection tuples according to strategy.
     * @param messages The message - connection tuples to sort.
     * @return The provided messages in sorted order, most important messages first.
     */
    List<Tuple<Message, Connection>> sortMessages(Collection<Tuple<Message, Connection>> messages);

    /**
     * Creates a replicate of this message prioritization strategy. The replicate has the same settings as this message
     * prioritization strategy but is attached to the provided router and has no attached host.
     *
     * @param attachedRouter Router prioritizing the messages.
     * @return The replicate.
     */
    MessagePrioritizationStrategy replicate(MessageRouter attachedRouter);

    /**
     * Sets the attached host.
     * @param host host prioritizing the messages.
     */
    void setAttachedHost(DTNHost host);
}
