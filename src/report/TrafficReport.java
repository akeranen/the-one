package report;

import core.Connection;
import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reports the amount of traffic per message type. Format is like
 *
 * Traffic by message type:
 * ONE_TO_ONE:  10.00% (8000 Bytes)
 * BROADCASTS:  25.00% (20000 Bytes)
 * MULTICASTS:  50.00% (40000 Bytes)
 * DATABASE_SYNC:  10.00% (8000 Bytes)
 * QUERIES:   5.00% (4000 Bytes)
 *
 * All messages created after the warm up period will be counted. Both aborted and successful relays go into the
 * statistic.
 *
 * Created by Britta Heymann on 17.03.2017.
 */
public class TrafficReport extends Report implements MessageListener, ConnectionListener{
    /**
     * Scaling factor to translate a percentage given as double between 0 and 1 to a double between 0 and 100.
     */
    private static final int PERCENTAGE_SCALING_FACTOR = 100;

    /**
     * Traffic observed so far, grouped by message type.
     */
    private ConcurrentMap<Message.MessageType, AtomicLong> trafficByMessageType = new ConcurrentHashMap<>();

    public TrafficReport() {
        super();
        for (Message.MessageType type : Message.MessageType.values()) {
            trafficByMessageType.putIfAbsent(type, new AtomicLong(0));
        }
    }

    /**
     * Called when the simulation is done, user requested
     * premature termination or intervalled report generating decided
     * that it's time for the next report.
     */
    @Override
    public void done() {
        this.write("Traffic by message type:");

        // Find total traffic to compute averages.
        long totalBytes = this.trafficByMessageType.values().stream().mapToLong(AtomicLong::get).sum();

        // If there were no messages, set totalBytes to 1 to get 0% everywhere.
        boolean noMessages = totalBytes == 0;
        if (noMessages) {
            totalBytes = 1;
        }

        // Sort map entries, then fetch and print the statistics.
        Map<Message.MessageType, AtomicLong> sortedMap = new TreeMap<>(this.trafficByMessageType);
        for (Map.Entry<Message.MessageType, AtomicLong> traffic : sortedMap.entrySet()) {
            // Fetch statistics.
            long bytes = traffic.getValue().get();
            double percentage = ((double)bytes / totalBytes) * PERCENTAGE_SCALING_FACTOR;

            // Print them.
            this.write(String.format("%s: %5.2f%% (%d Bytes)", traffic.getKey(), percentage, bytes));
        }

        super.done();
    }

    /**
     * Method is called when a new message is created
     *
     * @param m Message that was created
     */
    @Override
    public void newMessage(Message m) {
        if (this.isWarmup()) {
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
        if (!this.isWarmupID(m.getId())) {
            AtomicLong currentByteCount = this.trafficByMessageType.get(m.getType());
            currentByteCount.updateAndGet(x -> x + m.getSize());
        }
    }

    /**
     * Method is called when connection between hosts is disconnected.
     *
     * @param host1 Host that initiated the disconnection
     * @param host2 Host at the other end of the connection
     */
    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {
        Connection breakingConnection = findConnection(host1, host2);
        Message currentMessage = breakingConnection.getMessage();
        if (currentMessage != null && !this.isWarmupID(currentMessage.getId())) {
            int transferredBytesUpToNow = currentMessage.getSize() - breakingConnection.getRemainingByteCount();
            this.trafficByMessageType
                    .get(currentMessage.getType())
                    .updateAndGet(x -> x + transferredBytesUpToNow);
        }
    }

    private static Connection findConnection(DTNHost host1, DTNHost host2) {
        for (Connection con : host1.getConnections()) {
            if (con.getOtherNode(host1).equals(host2)) {
                return con;
            }
        }
        throw new IllegalStateException(
                "Connection between hosts " + host1 + " and " + host2 + " is closing, but could not be found.");
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

    /**
     * Method is called when two hosts are connected.
     *
     * @param host1 Host that initiated the connection
     * @param host2 Host that was connected to
     */
    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        // Nothing to do here.
    }
}
