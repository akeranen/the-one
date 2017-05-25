package routing.prioritizers;

import core.Connection;
import core.DTNHost;
import core.DataMessage;
import core.Message;
import core.Settings;
import core.SettingsError;
import routing.DisasterRouter;
import routing.MessageRouter;
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

    private DTNHost attachedHost;
    private DisasterRouter attachedRouter;

    /**
     * Initializes a new instance of the {@link DisasterPrioritization} class.
     * @param s Settings to use.
     * @param attachedHost Host prioritizing the messages.
     */
    public DisasterPrioritization(Settings s, DTNHost attachedHost) {
        // Set attached host.
        this.attachedHost = attachedHost;
        this.checkAttachedHostNotNull();
        this.attachedRouter = DisasterPrioritization.getRouter(this.attachedHost);

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
     * @param attachedHost Host prioritizing the messages.
     */
    public DisasterPrioritization(DisasterPrioritization prio, DTNHost attachedHost) {
        this.attachedHost = attachedHost;
        this.checkAttachedHostNotNull();
        this.attachedRouter = DisasterPrioritization.getRouter(attachedHost);

        this.deliveryPredictabilityWeight = prio.deliveryPredictabilityWeight;
        this.replicationsDensityWeight = prio.replicationsDensityWeight;
    }

    /**
     * Checks that {@link #attachedHost} is not {@code null} and throws an {@link IllegalArgumentException} if
     * it is the case.
     */
    private void checkAttachedHostNotNull() {
        if (this.attachedHost == null) {
            throw new IllegalArgumentException("Attached host is null!");
        }
    }

    /**
     * Compares two message-connection tuples using {@link #computePriorityFunction(Tuple)}.
     * @param t1 First tuple to compare.
     * @param t2 Second tuple to compare.
     * @return the value {@code 0} if {@code computePriorityFunction(t1) == computePriorityFunction(t2)};
     *         a value less than {@code 0} if {@code computePriorityFunction(t1) > computePriorityFunction(t2)};
     *         and a value greater than {@code 0} if {@code computePriorityFunction(t1) < computePriorityFunction(t2)}.
     */
    @Override
    public int compare(Tuple<Message, Connection> t1, Tuple<Message, Connection> t2) {
        return (-1) * Double.compare(this.computePriorityFunction(t1), this.computePriorityFunction(t2));
    }

    /**
     * Computes the value used for messages-connection prioritization.
     * @param t The message-connection tuple to compute the value for.
     * @return The computed value.
     */
    private double computePriorityFunction(Tuple<Message, Connection> t) {
        DisasterRouter neighborRouter = DisasterPrioritization.getRouter(t.getValue().getOtherNode(this.attachedHost));
        Message m = t.getKey();

        switch (m.getType()) {
            case ONE_TO_ONE:
            case MULTICAST:
                double deliveryPredictability = neighborRouter.getDeliveryPredictability(m);
                double replicationsDensity = this.attachedRouter.getReplicationsDensity(m);
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

    private static DisasterRouter getRouter(DTNHost host) {
        MessageRouter neighborRouter = host.getRouter();
        if (!(neighborRouter instanceof DisasterRouter)) {
            throw new IllegalArgumentException(
                    "Disaster prioritization cannot handle routers of type " + neighborRouter.getClass() + "!");
        }
        return (DisasterRouter)neighborRouter;
    }

    /**
     * Gets the delivery predictability weight.
     * @return The delivery predictability weight.
     */
    public double getDeliveryPredictabilityWeight() {
        return deliveryPredictabilityWeight;
    }

    /**
     * Gets the replications density weight.
     * @return The replications density weight.
     */
    public double getReplicationsDensityWeight() {
        return replicationsDensityWeight;
    }
}
