package routing.util;

import core.Message;
import core.Settings;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * Replications densities mapped to message IDs.
     */
    private Map<String, Double> replicationsDensities = new HashMap<>();

    private Map<String, Integer> encounteredMessagesInTimeWindow = new HashMap<>();
    private int encountersInTimeWindow;

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
     * @param knownMessages The messages known to the encountered node.
     */
    public void addEncounter(Collection<Message> knownMessages) {
        // Update total encounter number.
        this.encountersInTimeWindow++;

        // For each known message ID we care about, update the message encounter number.
        List<String> knownMessageIds =
                knownMessages.stream().map(Message::getId).distinct().collect(Collectors.toList());
        for (String msgId : knownMessageIds) {
            // Don't add values for messages the host will never request the replications density for because it does
            // not have it in buffer.
            if (this.replicationsDensities.containsKey(msgId)) {
                int currEncounterNumber = this.encounteredMessagesInTimeWindow.getOrDefault(msgId, 0);
                this.encounteredMessagesInTimeWindow.put(msgId, currEncounterNumber + 1);
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
        // Update all replications densities:
        for (String msgId : this.replicationsDensities.keySet()) {
            double newReplicationsDensity = 0;
            // If some hosts were met:
            if (this.encountersInTimeWindow != 0) {
                // Set replications density for a message to the rate of hosts met with that message.
                newReplicationsDensity =
                        (double) this.encounteredMessagesInTimeWindow.get(msgId) / this.encountersInTimeWindow;
            }
            this.replicationsDensities.put(msgId, newReplicationsDensity);
        }

        // Clear time window variables.
        this.encountersInTimeWindow = 0;
        this.encounteredMessagesInTimeWindow.clear();
    }

    /**
     * Adds the provided message ID to the query-able replications densities.
     * Call this if a new message is stored in the host's buffer.
     *
     * @param messageId Message ID to add.
     */
    public void addMessage(String messageId) {
        this.replicationsDensities.putIfAbsent(messageId, 0D);
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
