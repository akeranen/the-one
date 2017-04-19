package core;

/**
 * A message wrapping a data item.
 *
 * Created by Britta Heymann on 12.04.2017.
 */
public class DataMessage extends Message {
    private DisasterData data;
    private double utility;

    public DataMessage(DTNHost from, DTNHost to, String id, DisasterData data, double utility, int priority) {
        super(from, to, id, data.getSize(), priority);
        this.utility = utility;
        this.data = data;
    }
    /**
     * Returns a replicate of this message (identical except for the unique id)
     *
     * @return A replicate of the message
     */
    @Override
    public Message replicate() {
        Message m = new DataMessage(this.from, this.getTo(), this.id, this.data, this.utility, this.getPriority());
        m.copyFrom(this);
        return m;
    }

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
