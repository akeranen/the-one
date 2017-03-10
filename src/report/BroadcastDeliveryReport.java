package report;

import core.BroadcastMessage;
import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Prints a line for each broadcast delivery.
 *
 * Format is like
 * # Prio Time_Since_Creation
 * M3 2 7
 * M3 2 12
 * M1 3 101
 * ...
 * where the first column is the message ID, the second is the broadcast's priority and the last column shows the time
 * that passed between creation and delivery.
 *
 * Created by Britta Heymann on 08.03.2017.
 */
public final class BroadcastDeliveryReport extends Report implements MessageListener {
    public BroadcastDeliveryReport() {
        super();

        this.write("# Prio Time_Since_Creation");
    }
    /**
     * Method is called when a new message is created
     *
     * @param m Message that was created
     */
    @Override
    public void newMessage(Message m) {
        if (m instanceof BroadcastMessage && isWarmup()) {
            this.addWarmupID(m.getId());
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
        if (!firstDelivery || !(m instanceof BroadcastMessage) || this.isWarmupID(m.getId())) {
            return;
        }

        // TODO: Get correct priority here as soon as message priorities are implemented.
        int priority = 1;
        int timeAfterCreation = (int)this.getSimTime() - (int)m.getCreationTime();
        this.write(String.format("%s %d %d", m.getId(), priority, timeAfterCreation));
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
