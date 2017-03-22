package report;

import core.BroadcastMessage;
import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Prints a line for each broadcast delivery or creation.
 *
 * Format is like
 * Time # Prio
 * 7 M3 2
 * 12 M3 2
 * 101 M1 3
 * ...
 * 530
 * where the first column is the simulation time, the second the message ID, and the third one the broadcast's
 * priority. The final line is the simulation time at the end of the report.
 *
 * Created by Britta Heymann on 08.03.2017.
 */
public final class BroadcastDeliveryReport extends Report implements MessageListener {
    public BroadcastDeliveryReport() {
        super();
        this.write("Time # Prio");
    }

    /**
     * Called when the simulation is done, user requested
     * premature termination or intervalled report generating decided
     * that it's time for the next report.
     */
    @Override
    public void done() {
        this.write(Integer.toString((int)this.getSimTime()));
        super.done();
    }

    /**
     * Method is called when a new message is created
     *
     * @param m Message that was created
     */
    @Override
    public void newMessage(Message m) {
        if (!(m instanceof BroadcastMessage)) {
            return;
        }

        if (isWarmup()) {
            this.addWarmupID(m.getId());
            return;
        }

        this.writeMessageLine(m);
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
        if (firstDelivery && (m instanceof BroadcastMessage) && !this.isWarmupID(m.getId())) {
            this.writeMessageLine(m);
        }
    }

    /**
     * Writes a line of the format "simTime messageId priority".
     * @param m Message to write the line about.
     */
    private void writeMessageLine(Message m) {
        // TODO: Get correct priority here as soon as message priorities are implemented.
        int priority = 1;
        this.write(String.format("%d %s %d", (int)this.getSimTime(), m.getId(), priority));
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
