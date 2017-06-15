package core;

/**
 * A message wrapping a data item.
 *
 * Created by Britta Heymann on 12.04.2017.
 */
public class DataMessage extends Message {
    private DisasterData data;
    private double utility;

    /**
     * Initializes a new instance of the {@link DataMessage} class.
     * @param from The message's sender.
     * @param to The message receiver.
     * @param id The message's ID.
     * @param data The {@link DisasterData} this message is wrapping.
     * @param utility Utility of the {@link DisasterData} when this message was created.
     * @param priority Priority of the message.
     */
    public DataMessage(DTNHost from, DTNHost to, String id, DisasterData data, double utility, int priority) {
        super(from, to, id, data.getSize(), priority);
        this.utility = utility;
        this.data = data;
    }

    /**
     * Instantiates a copy of the message with the provided receiver.
     *
     * @param receiver Receiver of the message.
     * @return The new message.
     */
    public DataMessage instantiateFor(DTNHost receiver) {
        DataMessage m = new DataMessage(this.from, receiver, this.id, this.data, this.utility, this.getPriority());
        m.copyFrom(this);
        return m;
    }

    /**
     * Returns a replicate of this message (identical except for the unique id)
     *
     * @return A replicate of the message
     */
    @Override
    public Message replicate() {
        return this.instantiateFor(this.getTo());
    }

    /**
     * Gets the data this message is wrapping.
     * @return The wrapped {@link DisasterData}.
     */
    public DisasterData getData() {
        return data;
    }

    /**
     * Gets the utility that has been computed when this message was created.
     * @return The original utility.
     */
    public double getUtility() {
        return this.utility;
    }

    /**
     * Gets the message type.
     *
     * @return The message type.
     */
    @Override
    public MessageType getType() {
        return MessageType.DATA;
    }
}
