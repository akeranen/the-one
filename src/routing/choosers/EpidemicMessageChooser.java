package routing.choosers;

import core.Connection;
import core.DTNHost;
import core.Message;
import routing.MessageChoosingStrategy;
import routing.MessageRouter;
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
    /**
     * The {@link DTNHost} attached to this chooser, i.e. the host sending the messages.
     */
    private DTNHost attachedHost = null;

    /**
     * Sets the attached host.
     *
     * @param host host choosing the messages.
     */
    @Override
    public void setAttachedHost(DTNHost host) {
        this.attachedHost = host;
    }
    /**
     * Chooses non-direct messages to send.
     *
     * @param messages    All messages in buffer.
     * @param connections All connections the host has.
     * @return Which messages should be send to which neighbors.
     */
    @Override
    public Collection<Tuple<Message, Connection>> chooseNonDirectMessages(
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

        // Wrap useful data stored at host in data messages to neighbors and add them to the messages to sent.
        chosenMessages.addAll(DatabaseApplicationUtil.wrapUsefulDataIntoMessages(
                this.attachedHost.getRouter(), this.attachedHost, connections));

        return chosenMessages;
    }

    /**
     * Creates a replicate of this message choosing strategy. The replicate has the same settings as this message
     * choosing strategy but is attached to the provided router and has no attached host.
     *
     * @param attachedRouter Router choosing the messages.
     * @return The replicate.
     */
    @Override
    public MessageChoosingStrategy replicate(MessageRouter attachedRouter) {
        return new EpidemicMessageChooser();
    }
}
