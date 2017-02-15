package core;

/**
 * Message which should be delivered to every node.
 *
 * Created by Britta on 10.02.2017.
 */
public class BroadcastMessage extends Message {
    /**
     * Creates a new BroadcastMessage.
     *
     * @param from Who the message is (originally) from
     * @param id   Message identifier (must be unique for message but
     *             will be the same for all replicates of the message)
     * @param size Size of the message (in bytes)
     */
    public BroadcastMessage(DTNHost from, String id, int size) {
        super(from, null, id, size);
    }

    /**
     * Returns the node this message is originally to
     * @return the node this message is originally to
     */
    @Override
    public DTNHost getTo() {
        throw new UnsupportedOperationException(
                "Cannot call getTo on BroadcastMessage because it has no single recipient");
    }

    /**
     * Determines whether the provided node is a final recipient of the message.
     * @param host Node to check.
     * @return Whether the node is a final recipient of the message.
     */
    @Override
    public boolean isFinalRecipient(DTNHost host) {
        return true;
    }

    /**
     * Checks whether a successful sending to the provided DTNHost would mean a completed message delivery.
     * @param receiver The node that the message is sent to.
     * @return Whether or not delivery will be completed by a successful send operation.
     */
    @Override
    public boolean completesDelivery(DTNHost receiver) {
        return false;
    }

    /**
     * Returns a string representation of the message's recipient(s).
     * @return a string representation of the message's recipient(s).
     */
    @Override
    public String recipientsToString() {
        return "everyone";
    }

    /**
     * Returns a replicate of this message (identical except for the unique id)
     * @return A replicate of the message
     */
    @Override
    public Message replicate() {
        Message m = new BroadcastMessage(from, id, size);
        m.copyFrom(this);
        return m;
    }

    /**
     * Gets the message type.
     * @return The message type.
     */
    @Override
    public MessageType getType() {
        return MessageType.BROADCAST;
    }
}
