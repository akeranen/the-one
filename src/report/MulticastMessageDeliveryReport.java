package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.MulticastMessage;
import core.SimClock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 *
 * Created by Marius Meyer on 10.03.17.
 */
public class MulticastMessageDeliveryReport extends Report implements MessageListener {

    /**
     * header of the resulting output file
     */
    private static final String HEADER =
            "# message, group, delay, delivery ratio";

    /**
     * map storing the number nodes that have already received a certain group message
     */
    private Map<Integer,Integer> receivedNodes = new ConcurrentHashMap<>();

    /**
     * Constructor for the report
     */
    public MulticastMessageDeliveryReport(){
        init();
    }

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
        if (m instanceof MulticastMessage){
            MulticastMessage multicast = (MulticastMessage) m;
            int groupAddress = multicast.getGroup().getAddress();
            receivedNodes.put(groupAddress,1);
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
        if (m instanceof MulticastMessage){
            MulticastMessage multicast = (MulticastMessage) m;
            int groupAddress = multicast.getGroup().getAddress();
            receivedNodes.put(groupAddress,receivedNodes.get(groupAddress) + 1);
            write(multicast.getId() + " "
                    + groupAddress + " "
                    + (SimClock.getTime() -  m.getCreationTime()) + " "
                    + ( receivedNodes.get(groupAddress) / (double) multicast.getGroup().getMemberCount()));
        }
    }
}
