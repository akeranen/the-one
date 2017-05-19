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
     * Sorts the provided connection - message tuples according to strategy.
     * @param messages The connection - message tuples to sort.
     * @return The provided messages in sorted order, most important messages first.
     */
    List<Tuple<Connection, Message>> sortMessages(Collection<Tuple<Connection, Message>> messages);
}
