package routing.util;

import core.DTNHost;
import core.Message;
import core.Settings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages a host's replications densities. Replications density is a rating mechanism to measure the rate of hosts
 * possessing a certain message from a certain host's point of view.
 *
 * The measure is implemented as described in  X. Wang, Y. Shu, Z. Jin, Q. Pan and B. S. Lee (2009): Adaptive Randomized
 * Epidemic Routing for Disruption Tolerant Networks, Fifth International Conference on Mobile Ad-hoc and Sensor
 * Networks, 424-429.
 *
 * Created by Britta Heymann on 18.05.2017.
 */
public class ReplicationsDensityManager extends AbstractIntervalRatingMechanism {
    /**
     * The default replications density value if nothing is known about a message because we haven't observed it
     * long enough.
     */
    private static final double UNKNOWN_REPLICATIONS_DENSITY = 0.5;

    /**
     * Replications densities mapped to message IDs.
     */
    private Map<String, Double> replicationsDensities = new HashMap<>();

    /**
     * Remembers which message IDs have been stored by which hosts we encountered in the time window.
     *
     * It is not sufficient to just count the number of times we have seen the message here, because we might meet some
     * hosts multiple times and don't want to count the messages they carry more than once. We are also not able to just
     * look at messages of hosts we haven't met before in the time window, because a host's messages may change between
     * meetings and we might therefore miss messages if we do so.
     */
    private Map<String, Set<DTNHost>> encounteredMessagesInTimeWindow = new HashMap<>();
    /**
     * Remembers all hosts we have encountered in the time window.
     */
    private Set<DTNHost> uniqueEncountersInTimeWindow = new HashSet<>();

    /**
     * Initializes a new instance of the {@link ReplicationsDensityManager} class.
     * @param s Settings to use.
     */
    public ReplicationsDensityManager(Settings s) {
        super(s);
    }

    /**
     * Copy constructor. All constants are copied over, but the replications densities are not.
     * */
    public ReplicationsDensityManager(ReplicationsDensityManager manager) {
        super(manager);
    }

    /**
     * Adds a new encounter that will be considered when computing the replications densities.
     * @param host The encountered host.
     */
    public void addEncounter(DTNHost host) {
        // Update unique encounters.
        this.uniqueEncountersInTimeWindow.add(host);

        // For each known message ID we care about, update the encountered messages.
        for (Message msg : host.getMessageCollection()) {
            // Don't add messages for message IDs the host will never request the replications density for because it
            // does not have it in buffer.
            if (this.replicationsDensities.containsKey(msg.getId())) {
                this.encounteredMessagesInTimeWindow.putIfAbsent(msg.getId(), new HashSet<>());
                Set<DTNHost> hostsWithMessages = this.encounteredMessagesInTimeWindow.get(msg.getId());
                hostsWithMessages.add(host);
            }
        }
    }

    /**
     * Returns the replications density of a message.
     * @param messageId An ID of a message the host knows about.
     * @return The replications density.
     * @throws IllegalArgumentException if the host doesn't know the message ID after all.
     */
    public double getReplicationsDensity(String messageId) {
        Double replicationsDensity = this.replicationsDensities.get(messageId);
        if (replicationsDensity == null) {
            throw new IllegalArgumentException("Asked for a non-stored message!");
        }
        return replicationsDensity;
    }

    /**
     * Updates the rating mechanism after a time window has ended.
     */
    @Override
    protected void updateRatingMechanism() {
        // Keep old values if node was isolated.
        if (uniqueEncountersInTimeWindow.isEmpty()) {
            return;
        }

        // Else, update all replications densities:
        int numberUniqueEncounters = this.uniqueEncountersInTimeWindow.size();
        for (String msgId : this.replicationsDensities.keySet()) {
            if (!this.encounteredMessagesInTimeWindow.containsKey(msgId)) {
                // If no hosts were met, keep old density.
                continue;
            }
            // Else, set replications density for a message to the rate of hosts met with that message.
            double newReplicationsDensity =
                (double) this.encounteredMessagesInTimeWindow.get(msgId).size() / numberUniqueEncounters;
            this.replicationsDensities.put(msgId, newReplicationsDensity);
        }

        // Clear time window variables.
        this.uniqueEncountersInTimeWindow.clear();
        for (Map.Entry<String, Set<DTNHost>> entry : this.encounteredMessagesInTimeWindow.entrySet()) {
            entry.getValue().clear();
        }
    }

    /**
     * Adds the provided message ID to the query-able replications densities.
     * Call this if a new message is stored in the host's buffer.
     *
     * @param messageId Message ID to add.
     */
    public void addMessage(String messageId) {
        this.replicationsDensities.putIfAbsent(messageId, UNKNOWN_REPLICATIONS_DENSITY);
    }

    /**
     * Removes the provided message ID from the query-able replications densities.
     * Call this if a message is deleted from the host's buffer.
     *
     * @param messageId Message ID to remove.
     */
    public void removeMessage(String messageId) {
        this.replicationsDensities.remove(messageId);
    }
}
