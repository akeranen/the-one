package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.MulticastMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prints a line for each multicast delivery or creation.
 *
 * Format is like
 * #message, sent, received, ratio
 * M3 10 10 0.0
 * M3 10 15 0.2
 * M1 20 20 0.0
 * ...
 * 530
 * where the first column is the message ID, the second the creation time of the message, and the third one the
 * time, a multicast message was received by a node of the destination group.
 * The last column represents the delivery ratio of the message calculated with #nodes_received_msg / #nodes_in_group
 * The final line is the simulation time at the end of the report.
 *
 * Created by Marius Meyer on 10.03.17.
 */
public class MulticastMessageDeliveryReport extends Report implements MessageListener {

    /**
     * header of the resulting output file
     */
    private static final String HEADER =
            "#message, sent, received, ratio";

    /**
     * A map storing GroupMessageID -> number of recipients that have received that message
     */
    private Map<String,Integer> receivedNodes = new ConcurrentHashMap<>();

    /**
     * Initializes the report and writes the header to the file
     */
    @Override
    protected void init(){
        super.init();
        write(HEADER);
    }

    /**
     * Handle a new created message by putting it to the map, if it is a multicast message
     * @param m Message that was created
     */
    @Override
    public void newMessage(Message m) {
        if (isWarmup()){
            addWarmupID(m.getId());
        } else if (m instanceof MulticastMessage){
            MulticastMessage multicast = (MulticastMessage) m;
            receivedNodes.put(m.getId(),0);
            write(multicast.getId() + " "
                    + (int) m.getCreationTime() + " "
                    + (int) getSimTime() + " "
                    + 0.0);
        }
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        //Not relevant in this context
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        //Not relevant in this context
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        //Not relevant in this context
    }

    /**
     * Handle a transferred message by calculating the delivery ratio of it and
     * write it in the report
     *
     * @param m The message that was transferred
     * @param from Node where the message was transferred from
     * @param to Node where the message was transferred to
     * @param firstDelivery Was the target node final destination of the message
     */
    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        if (m instanceof MulticastMessage && firstDelivery && !isWarmupID(m.getId())){
            MulticastMessage multicast = (MulticastMessage) m;
            receivedNodes.put(m.getId(),receivedNodes.get(m.getId()) + 1);
            write(multicast.getId() + " "
                    + (int) m.getCreationTime() + " "
                    + (int) getSimTime() + " "
                    + ( receivedNodes.get(m.getId()) / ((double) multicast.getGroup().getMembers().length - 1)));
        }
    }

    @Override
    public void done(){
        write(Integer.toString((int) getSimTime()));
        super.done();

    }
}
