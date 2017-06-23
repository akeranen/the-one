package routing.choosers;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SettingsError;
import routing.DisasterRouter;
import routing.MessageChoosingStrategy;
import routing.MessageRouter;
import util.Tuple;

import java.util.Collection;
import java.util.List;

/**
 * Chooses the messages to send to neighbors dependent on the utility value of (neighbor, message) pairs. The utility
 * value itself is a weighted sum of the neighbor's delivery predictability, power, encounter value, and the message's
 * replications density.
 *
 * Created by Britta Heymann on 23.06.2017.
 */
public class UtilityMessageChooser implements MessageChoosingStrategy {
    /** Namespace for all utility message chooser settings. */
    public static final String UTILITY_MESSAGE_CHOOSER_NS = "UtilityMessageChooser";

    public static final String PROPHET_PLUS_WEIGHT = "prophetPlusWeight";
    public static final String DELIVERY_PREDICTABILITY_WEIGHT = "dpWeight";
    public static final String POWER_WEIGHT = "powerWeight";

    public static final String REPLICATIONS_DENSITY_WEIGHT = "rdWeight";
    public static final String ENCOUNTER_VALUE_WEIGHT = "evWeight";

    public static final String UTILITY_THRESHOLD = "messageUtilityThreshold";

    /* Weights used in utility function. */
    private double deliveryPredictabilityWeight;
    private double powerWeight;
    private double replicationsDensityWeight;
    private double encounterValueWeight;

    /** Utility threshold above which messages are sent. */
    private double utilityThreshold;

    /** Router choosing the messages. */
    private DisasterRouter attachedRouter;
    /** Host choosing the messages. */
    private DTNHost attachedHost;

    /**
     * Initializes a new instance of the {@link UtilityMessageChooser} class.
     * @param attachedRouter Router choosing the messages.
     * @throws SettingsError if weights or thresholds are not valid.
     * @throws IllegalArgumentException if the attached router is {@code null} or not a {@link DisasterRouter}.
     */
    public UtilityMessageChooser(MessageRouter attachedRouter) {
        // Read settings:
        // Create settings object with correct namespace.
        Settings s = new Settings(UTILITY_MESSAGE_CHOOSER_NS);

        // Read weights belonging to prophet plus value.
        double prophetPlusWeight = s.getDouble(PROPHET_PLUS_WEIGHT);
        this.deliveryPredictabilityWeight = prophetPlusWeight * s.getDouble(DELIVERY_PREDICTABILITY_WEIGHT);
        this.powerWeight = prophetPlusWeight * s.getDouble(POWER_WEIGHT);

        // Read other weights.
        this.replicationsDensityWeight = s.getDouble(REPLICATIONS_DENSITY_WEIGHT);
        this.encounterValueWeight = s.getDouble(ENCOUNTER_VALUE_WEIGHT);

        // Read thresholds.
        this.utilityThreshold = s.getDouble(UTILITY_THRESHOLD);
        // TODO: power threshold (protocol v3).

        // Check all values are valid.
        this.validateWeights();
        if (this.utilityThreshold < 0 || this.utilityThreshold > 1) {
            throw new SettingsError("Utility threshold must be in [0, 1], but is " + this.utilityThreshold + "!");
        }

        // Then set attached router.
        UtilityMessageChooser.checkRouterIsDisasterRouter(attachedRouter);
        this.attachedRouter = (DisasterRouter)attachedRouter;
    }

    /**
     * Copy constructor.
     * @param chooser Chooser to copy.
     * @param attachedRouter Router choosing the messages.
     */
    private UtilityMessageChooser(UtilityMessageChooser chooser, MessageRouter attachedRouter) {
        this.deliveryPredictabilityWeight = chooser.deliveryPredictabilityWeight;
        this.powerWeight = chooser.powerWeight;
        this.replicationsDensityWeight = chooser.replicationsDensityWeight;
        this.encounterValueWeight = chooser.encounterValueWeight;
        this.utilityThreshold = chooser.utilityThreshold;
        // TODO: power threshold (protocol v3)

        UtilityMessageChooser.checkRouterIsDisasterRouter(attachedRouter);
        this.attachedRouter = (DisasterRouter)attachedRouter;
    }

    /**
     * Checks that the read weights sum up to 1 and are all in [0, 1].
     * @throws SettingsError if that is not the case.
     */
    private void validateWeights() {
        // Check their sum is 1.
        double sum = this.deliveryPredictabilityWeight + this.powerWeight
                + this.replicationsDensityWeight + this.encounterValueWeight;
        if (Math.abs(1 - sum) >= 0.0001) {
            throw new SettingsError("Weights must sum up to 1, but sum up to " + sum + " instead!");
        }

        // Check all are between 0 and 1.
        if (this.deliveryPredictabilityWeight < 0 || this.deliveryPredictabilityWeight > 1) {
            throw new SettingsError("Delivery predictability weight must be in [0, 1], but is "
                    + this.deliveryPredictabilityWeight + "!");
        }
        if (this.powerWeight < 0 || this.powerWeight > 1) {
            throw new SettingsError("Power weight must be in [0, 1], but is " + this.powerWeight + "!");
        }
        if (this.replicationsDensityWeight < 0 || this.replicationsDensityWeight > 1) {
            throw new SettingsError("Replications density weight must be in [0, 1], but is "
                    + this.replicationsDensityWeight + "!");
        }
        if (this.encounterValueWeight < 0 || this.encounterValueWeight > 1) {
            throw new SettingsError("Encounter value weight must be in [0, 1], but is "
                    + this.encounterValueWeight + "!");
        }
    }

    /**
     * Checks if the router is a {@link DisasterRouter} and throws an {@link IllegalArgumentException} if it isn't.
     * @param router Router to check
     */
    private static void checkRouterIsDisasterRouter(MessageRouter router) {
        if (router == null) {
            throw new IllegalArgumentException("Router is null!");
        }
        if (!(router instanceof DisasterRouter)) {
            throw new IllegalArgumentException(
                    "Utility message chooser cannot handle routers of type " + router.getClass() + "!");
        }
    }

    /**
     * Sets the attached host.
     *
     * @param host host prioritizing the messages.
     */
    public void setAttachedHost(DTNHost host) {
        this.attachedHost = host;
    }

    /**
     * Chooses non-direct messages to send.
     *
     * @param messages    All messages in buffer.
     * @param connections All connections the host has.
     * @return Which messages should be sent to which neighbors.
     */
    @Override
    public Collection<Tuple<Message, Connection>> findOtherMessages(Collection<Message> messages, List<Connection> connections) {
        return null;
    }

    /**
     * Computes the utility of a message - neighbor pair.
     * @param m The message.
     * @param otherRouter The neighbor's router object.
     * @return The computed utility.
     */
    private double computeUtility(Message m, DisasterRouter otherRouter) {
        return this.deliveryPredictabilityWeight * otherRouter.getDeliveryPredictability(m)
                + this.powerWeight * otherRouter.remainingEnergyRatio()
                + this.replicationsDensityWeight * (1 - this.attachedRouter.getReplicationsDensity(m))
                + this.encounterValueWeight * otherRouter.getEncounterValue();
    }

    /**
     * Determines whether the provided message should be sent to the provided host right now.
     * This is only the case if the host is not transferring, does not know the message yet, and the message - host
     * pair's utility is sufficiently high.
     * @param m Message to check.
     * @param otherHost Host to check.
     * @return True iff the message should be sent.
     */
    private boolean shouldBeSent(Message m, DTNHost otherHost) {
        UtilityMessageChooser.checkRouterIsDisasterRouter(otherHost.getRouter());
        DisasterRouter otherRouter = (DisasterRouter)otherHost.getRouter();
        return !otherRouter.isTransferring()
                && !otherRouter.hasMessage(m.getId())
                && this.computeUtility(m, otherRouter) > this.utilityThreshold;
        // TODO: also check other's energy (routing protocol v3)
    }
}
