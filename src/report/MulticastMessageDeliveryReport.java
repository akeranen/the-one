package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.MulticastMessage;

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
            "#message, group, sent, received, ratio";

    /**
     * map storing the number nodes that have already received a certain group message
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
            int groupAddress = multicast.getGroup().getAddress();
            receivedNodes.put(m.getId(),0);
            write(multicast.getId() + " "
                    + groupAddress + " "
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
            int groupAddress = multicast.getGroup().getAddress();
            receivedNodes.put(m.getId(),receivedNodes.get(m.getId()) + 1);
            write(multicast.getId() + " "
                    + groupAddress + " "
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
