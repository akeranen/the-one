package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Report that immediately prints a line about delay at each unique delivery.
 * Ignores the messages that were created during the warm up period.
 *
 * Format is like
 * Type Prio Delay
 * Broadcast 2 20
 * OneToOne 10 2
 * OneToOne 10 40
 * Multicast 10 4
 * Data 11 1
 * ...
 *
 * If you are fine with a less immediate and more storage intensive approach, and don't care about message types or
 * priorities, check out {@link MessageDelayReport}
 * Created by Britta Heymann on 15.03.2017.
 */
public final class ImmediateMessageDelayReport extends Report implements MessageListener {
    public ImmediateMessageDelayReport() {
        super();
        this.write("Type Prio Delay");
    }

    /**
     * Method is called when a new message is created
     *
     * @param m Message that was created
     */
    @Override
    public void newMessage(Message m) {
        if (isWarmup()) {
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
        if (firstDelivery && !this.isWarmupID(m.getId())) {
            this.write(String.format(
                    "%s %d %d", m.getType(), m.getPriority(), (int)(this.getSimTime() - m.getCreationTime())));
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
