package routing.choosers;

import core.Connection;
import core.DTNHost;
import core.Message;
import routing.MessageChoosingStrategy;
import routing.util.DatabaseApplicationUtil;
import util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@link MessageChoosingStrategy} trying to send all messages to all connections.
 *
 * Created by Britta Heymann on 21.05.2017.
 */
public class EpidemicMessageChooser implements MessageChoosingStrategy {
    private DTNHost attachedHost;

    public EpidemicMessageChooser(DTNHost attachedHost) {
        this.attachedHost = attachedHost;
    }

    /**
     * Chooses non-direct messages to send.
     *
     * @param messages    All messages in buffer.
     * @param connections All connections the host has.
     * @return Which messages should be send to which neighbors.
     */
    @Override
    public Collection<Tuple<Message, Connection>> findOtherMessages(
            Collection<Message> messages, List<Connection> connections) {
        Collection<Tuple<Message, Connection>> chosenMessages = new ArrayList<>();

        // Add ordinary messages.
        for (Connection con : connections) {
            DTNHost neighbor = con.getOtherNode(this.attachedHost);
            for (Message m : messages) {
                // Only choose non-direct messages.
                if (m.isFinalRecipient(neighbor)) {
                    continue;
                }
                chosenMessages.add(new Tuple<>(m, con));
            }
        }

        // Add data messages.
        chosenMessages.addAll(DatabaseApplicationUtil.createDataMessages(
                this.attachedHost.getRouter(), this.attachedHost, connections));

        return chosenMessages;
    }
}
