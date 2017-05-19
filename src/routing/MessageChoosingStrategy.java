package routing;

import core.Connection;
import core.Message;
import util.Tuple;

import java.util.Collection;
import java.util.List;

/**
 * Strategy to choose messages in routing.
 *
 * Created by Britta Heymann on 19.05.2017.
 */
@FunctionalInterface
public interface MessageChoosingStrategy {
    /**
     * Chooses non-direct messages to send.
     * @param messages All messages in buffer.
     * @param connections All connections the host has.
     * @return Which messages should be send to which neighbors.
     */
    Collection<Tuple<Connection, Message>> findOtherMessages(
            Collection<Message> messages, List<Connection> connections);
}
