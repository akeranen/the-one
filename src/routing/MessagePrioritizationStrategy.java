package routing;

import core.Connection;
import core.Message;
import util.Tuple;

import java.util.Collection;
import java.util.List;

/**
 * Strategy to prioritize messages in routing.
 *
 * Created by Britta Heymann on 19.05.2017.
 */
@FunctionalInterface
public interface MessagePrioritizationStrategy {
    /**
     * Sorts the provided message - connection tuples according to strategy.
     * @param messages The message - connection tuples to sort.
     * @return The provided messages in sorted order, most important messages first.
     */
    List<Tuple<Message, Connection>> sortMessages(Collection<Tuple<Message, Connection>> messages);
}
