package report;

import core.BroadcastMessage;
import core.DTNHost;
import core.Message;
import core.MessageListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reports about the number of unique deliveries for each broadcast message at each sampling time.
 *
 * Format is like
 * [10]
 * M3 2 17
 * ...
 * [15]
 * ...
 * where 3 is the message ID, 2 is the broadcast's priority and 17 is the number of hosts the message has been delivered
 * to until time 10.
 *
 * Created by Britta Heymann on 08.03.2017.
 */
public class BroadcastSpreadReport extends SamplingReport implements MessageListener {
    private Map<String, Integer> messageToDeliveries = new HashMap<>();

    @Override
    protected void sample(List<DTNHost> hosts) {
        write(String.format("[%d]", (int)getSimTime()));
        for(Map.Entry<String, Integer> messageInfo : this.messageToDeliveries.entrySet()) {
            // TODO: Get correct priority here as soon as message priorities are implemented.
            int priority = 1;
            this.write(String.format("%s %d %d", messageInfo.getKey(), priority, messageInfo.getValue()));
        }
    }

    /**
     * Method is called when a new message is created
     *
     * @param m Message that was created
     */
    @Override
    public void newMessage(Message m) {
        if (!isWarmup() && m instanceof BroadcastMessage) {
            this.messageToDeliveries.put(m.getId(), 0);
        }
    }

    /**
     * Method is called when a message is successfully transferred from
     * a node to another.
     *
     * @param m             The message that was transferred
     * @param from          Node where the message was transferred from
     * @param to            Node where the message was transferred to
     * @param firstDelivery Was the target node final destination of the message
     */
    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        if (!firstDelivery || !(m instanceof BroadcastMessage)) {
            return;
        }

        Integer currNumberOfDeliveries = this.messageToDeliveries.get(m.getId());
        boolean isWarmUpMessage = currNumberOfDeliveries == null;
        if(!isWarmUpMessage) {
            this.messageToDeliveries.put(m.getId(), currNumberOfDeliveries + 1);
        }
    }

    /**
     * Method is called when a message's transfer is started
     *
     * @param m    The message that is going to be transferred
     * @param from Node where the message is transferred from
     * @param to   Node where the message is transferred to
     */
    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        // Nothing to do here.
    }

    /**
     * Method is called when a message is deleted
     *
     * @param m       The message that was deleted
     * @param where   The host where the message was deleted
     * @param dropped True if the message was dropped, false if removed
     */
    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        // Nothing to do here.
    }

    /**
     * Method is called when a message's transfer was aborted before
     * it finished
     *
     * @param m    The message that was being transferred
     * @param from Node where the message was being transferred from
     * @param to   Node where the message was being transferred to
     */
    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        // Nothing to do here.
    }
}
