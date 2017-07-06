package routing.prioritizers;

import core.Connection;
import core.DTNHost;
import core.DataMessage;
import core.Message;
import core.Settings;
import core.SettingsError;
import core.SimClock;
import routing.DisasterRouter;
import routing.MessageRouter;
import routing.util.MessageConnectionTuple;
import util.Tuple;

import java.util.Comparator;
import java.util.HashMap;

/**
 * A message-connection tuple prioritization depending on delivery predictability and replications density. Also uses
 * {@link DataMessage#getUtility()}.
 *
 * Created by Britta Heymann on 24.05.2017.
 */
// Suppress warning about unserializable comparator because the rule's assumption that the overhead to make it
// serializable is low does not fit for this one (DTNHost, routers are not serializable!).
@SuppressWarnings({"squid:S2063"})
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
     * Caches priority function evaluations. Very useful because comparators may be called multiple times for each
     * item.
     * Invalidated every timestep to ensure correct value.
     */
    private HashMap<MessageConnectionTuple, Double> priorityFunctionValueCache = new HashMap<>();
    /**
     * The simulation time the current {@link #priorityFunctionValueCache} is for.
     */
    private double cacheTime;

    /**
     * Initializes a new instance of the {@link DisasterPrioritization} class.
     * @param s Settings to use.
     */
    public DisasterPrioritization(Settings s, DisasterRouter attachedRouter) {
        // Set weights.
        this.deliveryPredictabilityWeight = s.getDouble(DELIVERY_PREDICTABILITY_WEIGHT);
        if (this.deliveryPredictabilityWeight < 0 || this.deliveryPredictabilityWeight > 1) {
            throw new SettingsError("Delivery predictability weight has to be between 0 and 1!");
        }
        this.replicationsDensityWeight = 1 - deliveryPredictabilityWeight;

        // Set attached router.
        DisasterPrioritization.checkRouterIsNotNull(attachedRouter);
        this.attachedRouter = attachedRouter;
    }

    /**
     * Copy constructor.
     * @param prio Original {@link DisasterPrioritization} to copy settings from.
     */
    public DisasterPrioritization(DisasterPrioritization prio, DisasterRouter attachedRouter) {
        this.deliveryPredictabilityWeight = prio.deliveryPredictabilityWeight;
        this.replicationsDensityWeight = prio.replicationsDensityWeight;
        DisasterPrioritization.checkRouterIsNotNull(attachedRouter);
        this.attachedRouter = attachedRouter;
    }

    /**
     * Sets the host prioritizing the messages.
     * @param host Host prioritizing the messages. Should be the one that will get the {@link #attachedRouter} as
     *             router.
     */
    public void setAttachedHost(DTNHost host) {
        if (host == null) {
            throw new IllegalArgumentException("Host is null!");
        }
        this.attachedHost = host;
        // Note: Cannot set router at this place because the host's router is only set afterwards.
    }

    /**
     * Checks the provided router is not null and throws an {@link IllegalArgumentException} otherwise.
     * @param router Router to check.
     */
    private static void checkRouterIsNotNull(DisasterRouter router) {
        if (router == null) {
            throw new IllegalArgumentException("Attached router is null!");
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
        // Invalidate value cache if it is time for it.
        this.possiblyInvalidateCache();

        // If we already have the function value cached, don't compute it.
        Double cachedValue = this.priorityFunctionValueCache.get(new MessageConnectionTuple(t));
        if (cachedValue != null) {
            return cachedValue;
        }

        // Else: Compute.
        DisasterRouter neighborRouter = DisasterPrioritization.getRouter(t.getValue().getOtherNode(this.attachedHost));
        Message m = t.getKey();
        double priorityFunctionValue;
        switch (m.getType()) {
            case ONE_TO_ONE:
            case MULTICAST:
                double deliveryPredictability = neighborRouter.getDeliveryPredictability(m);
                double replicationsDensity = this.attachedRouter.getReplicationsDensity(m);
                priorityFunctionValue = this.deliveryPredictabilityWeight * deliveryPredictability
                        + this.replicationsDensityWeight * (1 - replicationsDensity);
                break;
            case DATA:
                priorityFunctionValue = ((DataMessage)m).getUtility();
                break;
            default:
                throw new IllegalArgumentException(
                        "Priority function only defined for 1-to-1, multicasts and data messages, not for type "
                        + m.getType() + "!");
        }

        // Cache before returning.
        this.priorityFunctionValueCache.put(new MessageConnectionTuple(t), priorityFunctionValue);
        return priorityFunctionValue;
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
     * Invalidates the {@link #priorityFunctionValueCache} if it is obsolete.
     */
    private void possiblyInvalidateCache() {
        if (SimClock.getTime() > this.cacheTime) {
            this.priorityFunctionValueCache.clear();
            this.cacheTime = SimClock.getTime();
        }
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
