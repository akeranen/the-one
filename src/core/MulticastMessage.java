package core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Message which should be delivered to a certain group of nodes
 *
 * Created by Marius Meyer on 08.03.17.
 */
public class MulticastMessage extends Message {

    /**
     * the group this message is dedicated to
     */
    private Group group;

    /**
     * Recipients that have been reached on this message copy's path.
     */
    private HashSet<Integer> reachedRecipients = new HashSet<>();

    /**
     * Creates a new Message.
     *
     * @param from Who the message is (originally) from
     * @param to   Who the message is (originally) to
     * @param id   Message identifier (must be unique for message but
     *             will be the same for all replicates of the message)
     * @param size Size of the message (in bytes)
     */
    public MulticastMessage(DTNHost from, Group to, String id, int size) {
        this(from, to, id, size, INVALID_PRIORITY);
    }
    
    /**
     * Creates a new Message that also has a priority.
     *
     * @param from Who the message is (originally) from
     * @param to   Who the message is (originally) to
     * @param id   Message identifier (must be unique for message but
     *             will be the same for all replicates of the message)
     * @param size Size of the message (in bytes)
     * @param prio Priority of the message
     */
    public MulticastMessage(DTNHost from, Group to, String id, int size, int prio) {
        super(from, null, id, size, prio);
        if  (!to.contains(from.getAddress())){
            throw new SimError("Sender must be in same group as the destination group," +
                    " but host "+ from + " is not " + to);
        }
        this.group = to;
        this.reachedRecipients.add(from.getAddress());
    }

    /**
     * Returns the node this message is originally to
     * @return the node this message is originally to
     */
    @Override
    public DTNHost getTo() {
        throw new UnsupportedOperationException(
                "Cannot call getTo on MulticastMessage because it has no single recipient");
    }

    /**
     * Returns the group this message is addressed to
     *
     * @return the group this message is addressed to
     */
    public Group getGroup(){
        return group;
    }

    /**
     * Gets the addresses of all group members that neither have been passed on this message copy's path before nor
     * have received a direct copy of this message copy.
     *
     * @return Addresses of all group members that haven't been reached so far.
     */
    public Collection<Integer> getRemainingRecipients() {
        List<Integer> remainingRecipients = new ArrayList<>();
        for (Integer address : this.group.getMembers()) {
            if (!this.reachedRecipients.contains(address)) {
                remainingRecipients.add(address);
            }
        }
        return remainingRecipients;
    }

    /**
     * Adds a {@link DTNHost} to reached recipients if it is a final recipient of the message.
     * @param host {@link DTNHost} to add.
     */
    public void addReachedHost(DTNHost host) {
        if (this.isFinalRecipient(host)) {
            this.reachedRecipients.add(host.getAddress());
        }
    }

    /**
     * Determines whether the provided node is a final recipient of the message.
     * @param host Node to check.
     * @return Whether the node is a final recipient of the message.
     */
    @Override
    public boolean isFinalRecipient(DTNHost host) {
        return group.contains(host.getAddress());
    }

    /**
     * Checks whether a successful sending to the provided DTNHost would mean a completed message delivery.
     * @param receiver The node that the message is sent to.
     * @return Whether or not delivery will be completed by a successful send operation.
     */
    @Override
    public boolean completesDelivery(DTNHost receiver) {
        // Check whether all hosts have already been reached.
        int groupSize = this.group.getMembers().length;
        if (this.reachedRecipients.size() == groupSize) {
            return true;
        }

        // Then check if only the current receiver is missing.
        return this.reachedRecipients.size() == groupSize - 1
                && this.isFinalRecipient(receiver)
                && !this.reachedRecipients.contains(receiver.getAddress());
    }

    /**
     * Adds a new node on the list of nodes this message has passed
     *
     * @param node The node to add
     */
    @Override
    public void addNodeOnPath(DTNHost node) {
        super.addNodeOnPath(node);

        // Only add a reached recipient if we are not in initialization.
        // We cannot add reached recipients in initialization because the necessary fields are not yet set. However,
        // adding the single host this message was created by is handled in constructor.
        if (this.reachedRecipients != null && this.isFinalRecipient(node)) {
            this.reachedRecipients.add(node.getAddress());
        }
    }

    /**
     * Returns a string representation of the message's recipient(s).
     * @return a string representation of the message's recipient(s).
     */
    @Override
    public String recipientsToString() {
        return group.toString();
    }

    /**
     * Returns a replicate of this message (identical except for the unique id)
     * @return A replicate of the message
     */
    @Override
    public Message replicate() {
        Message m = new MulticastMessage(from, group, id, size);
        m.copyFrom(this);
        return m;
    }


    /**
     * Copies the parameters from a different MulticastMessage instance
     * @param m the other MulticastMessage instance
     */
    public void copyFrom(MulticastMessage m){
        super.copyFrom(m);
        this.group = m.group;
        this.reachedRecipients = new HashSet<>(m.reachedRecipients);
    }

    /**
     * Gets the message type.
     * @return The message type.
     */
    @Override
    public MessageType getType() {
        return MessageType.MULTICAST;
    }
}
