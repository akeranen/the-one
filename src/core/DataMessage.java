package core;

import util.Tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A message wrapping multiple data items.
 *
 * Created by Britta Heymann on 12.04.2017.
 */
public class DataMessage extends Message {
    private List<DisasterData> data;
    private double utility;

    /**
     * Initializes a new instance of the {@link DataMessage} class.
     * @param from The message's sender.
     * @param to The message receiver.
     * @param id The message's ID.
     * @param dataWithUtility The {@link DisasterData} items this message is wrapping and the utility of each
     * {@link DisasterData} at the time this message was created.
     * @param priority Priority of the message.
     */
    public DataMessage(
            DTNHost from, DTNHost to, String id, Iterable<Tuple<DisasterData, Double>> dataWithUtility, int priority) {
        super(from, to, id, computeTotalDataSize(dataWithUtility), priority);
        this.utility = computeUtility(dataWithUtility);

        List<DisasterData> dataList = new ArrayList<>();
        for (Tuple<DisasterData, Double> dataItem : dataWithUtility) {
            dataList.add(dataItem.getKey());
        }
        this.data = Collections.unmodifiableList(dataList);
    }

    /**
     * Copy constructor of {@link DataMessage} which changes its receiver.
     * @param message Message to copy.
     * @param receiver The new receiver.
     */
    private DataMessage(DataMessage message, DTNHost receiver) {
        super(message.from, receiver, message.getId(), message.size, message.getPriority());
        this.utility = message.utility;
        this.data = message.data;
    }

    /**
     * Computes the total size of the given data.
     * @param dataWithUtility Data to compute total size for.
     * @return The sum of all item sizes.
     */
    private static int computeTotalDataSize(Iterable<Tuple<DisasterData, Double>> dataWithUtility) {
        int size = 0;
        for (Tuple<DisasterData, Double> item : dataWithUtility) {
            size += item.getKey().getSize();
        }
        return size;
    }

    /**
     * Computes overall utility for a message wrapping all provided data items by taking the items' average.
     * @param dataWithUtility The data items to wrap and their utilities.
     * @return A combined utility for the whole message.
     */
    private static double computeUtility(Iterable<Tuple<DisasterData, Double>> dataWithUtility) {
        double utilitySum = 0;
        int numberItems = 0;
        for (Tuple<DisasterData, Double> item : dataWithUtility) {
            utilitySum += item.getValue();
            numberItems++;
        }

        if (numberItems == 0) {
            throw new IllegalArgumentException("There must be at least one data item in a data message!");
        }
        return utilitySum / numberItems;
    }

    /**
     * Instantiates a copy of the message with the provided receiver.
     *
     * @param receiver Receiver of the message.
     * @return The new message.
     */
    public DataMessage instantiateFor(DTNHost receiver) {
        DataMessage m = new DataMessage(this, receiver);
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
    public List<DisasterData> getData() {
        return Collections.unmodifiableList(data);
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
