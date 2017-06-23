package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import util.Tuple;

import java.util.Collection;
import java.util.List;

/**
 * Strategy to choose messages in routing.
 *
 * Created by Britta Heymann on 19.05.2017.
 */
public interface MessageChoosingStrategy {
    /**
     * Chooses non-direct messages to send.
     * @param messages All messages in buffer.
     * @param connections All connections the host has.
     * @return Which messages should be send to which neighbors.
     */
    Collection<Tuple<Message, Connection>> findOtherMessages(
            Collection<Message> messages, List<Connection> connections);

    /**
     * Creates a replicate of this message choosing strategy. The replicate has the same settings as this message
     * choosing strategy but is attached to the provided router and has no attached host.
     *
     * @param attachedRouter Router choosing the messages.
     * @return The replicate.
     */
    MessageChoosingStrategy replicate(MessageRouter attachedRouter);

    /**
     * Sets the attached host.
     * @param host host choosing the messages.
     */
    void setAttachedHost(DTNHost host);
}
