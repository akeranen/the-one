package routing.prioritizers;

import core.Connection;
import core.DataMessage;
import core.Message;
import core.Settings;
import core.SettingsError;
import routing.util.DeliveryPredictabilityStorage;
import routing.util.ReplicationsDensityManager;
import util.Tuple;

import java.util.Comparator;

/**
 * A message-connection tuple prioritization depending on delivery predictability and replications density. Also uses
 * {@link DataMessage#getUtility()}.
 *
 * Created by Britta Heymann on 24.05.2017.
 */
public class DisasterPrioritization implements Comparator<Tuple<Message, Connection>> {
    /**
     * Weight of delivery predictability in message prioritization function -setting id ({@value}).
     * A value between 0 and 1.
     * Weight of replications density will be automatically chosen s.t. they add up to 1.
     */
    public static final String DELIVERY_PREDICTABILITY_WEIGHT = "dpWeight";

    private double deliveryPredictabilityWeight;
    private double replicationsDensityWeight;

    private DeliveryPredictabilityStorage deliveryPredictabilities;
    private ReplicationsDensityManager replicationsDensities;

    /**
     * Initializes a new instance of the {@link DisasterPrioritization} class.
     * @param s Settings to use.
     * @param deliveryPredictabilities Storage of delivery predictabilities.
     * @param replicationsDensities Storage of replications densities.
     */
    public DisasterPrioritization(
            Settings s,
            DeliveryPredictabilityStorage deliveryPredictabilities,
            ReplicationsDensityManager replicationsDensities) {
        // Set rating mechanism managers.
        this.deliveryPredictabilities = deliveryPredictabilities;
        this.replicationsDensities = replicationsDensities;

        // Set weights.
        this.deliveryPredictabilityWeight = s.getDouble(DELIVERY_PREDICTABILITY_WEIGHT);
        if (this.deliveryPredictabilityWeight < 0 || this.deliveryPredictabilityWeight > 1) {
            throw new SettingsError("Delivery predictability weight has to be between 0 and 1!");
        }
        this.replicationsDensityWeight = 1 - deliveryPredictabilityWeight;
    }

    /**
     * Copy constructor.
     * @param prio Original {@link DisasterPrioritization} to copy settings from.
     * @param deliveryPredictabilities Storage of delivery predictabilities.
     * @param replicationsDensities Storage of replications densities.
     */
    public DisasterPrioritization(
            DisasterPrioritization prio,
            DeliveryPredictabilityStorage deliveryPredictabilities,
            ReplicationsDensityManager replicationsDensities) {
        this.deliveryPredictabilities = deliveryPredictabilities;
        this.replicationsDensities = replicationsDensities;
        this.deliveryPredictabilityWeight = prio.deliveryPredictabilityWeight;
        this.replicationsDensityWeight = prio.replicationsDensityWeight;
    }

    /**
     * Compares two message-connection tuples using {@link #computePriorityFunction(Message)}.
     * @param t1 First tuple to compare.
     * @param t2 Second tuple to compare.
     * @return the value {@code 0} if
     *         {@code computePriorityFunction(t1.getKey()) == computePriorityFunction(t2.getKey())};
     *         a value less than {@code 0} if
     *         {@code computePriorityFunction(t1.getKey()) > computePriorityFunction(t2.getKey())};
     *         and a value greater than {@code 0} if
     *         {@code computePriorityFunction(t1.getKey()) > computePriorityFunction(t2.getKey())}.
     */
    @Override
    public int compare(Tuple<Message, Connection> t1, Tuple<Message, Connection> t2) {
        return (-1) *
                Double.compare(this.computePriorityFunction(t1.getKey()), this.computePriorityFunction(t2.getKey()));
    }

    /**
     * Computes the value depending on which messages are prioritized.
     * @param m The message to compute the value for.
     * @return The computed value.
     */
    private double computePriorityFunction(Message m) {
        switch (m.getType()) {
            case ONE_TO_ONE:
            case MULTICAST:
                double deliveryPredictability = this.deliveryPredictabilities.getDeliveryPredictability(m);
                double replicationsDensity = this.replicationsDensities.getReplicationsDensity(m.getId());
                return this.deliveryPredictabilityWeight * deliveryPredictability
                        + this.replicationsDensityWeight * (1 - replicationsDensity);
            case DATA:
                return ((DataMessage)m).getUtility();
            default:
                throw new IllegalArgumentException(
                        "Priority function only defined for 1-to-1, multicasts and data messages, not for type "
                        + m.getType() + "!");
        }
    }
}
