package routing.util;

import core.DTNHost;
import core.Message;
import core.MulticastMessage;
import core.Settings;
import core.SettingsError;
import core.SimClock;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages a host's delivery predictabilities. Delivery predictability for a host B from the point of view of host A
 * indicates how likely it is that node A will be able to deliver a message to B.
 *
 * The measure is implemented as described in   A. Lindgren, A. Doria and O. Schelén (2003): Probabilistic Routing in
 * Intermittently Connected Networks, SIGMOBILE Mob. Comput. Commun. Rev., vol.3, 19-20.
 *
 * Created by Britta Heymann on 18.05.2017.
 */
public class DeliveryPredictabilityStorage {
    /** Namespace for all delivery predictability storage settings. */
    public static final String DELIVERY_PREDICTABILITY_STORAGE_NS = "DeliveryPredictabilityStorage";

    /**
     * Constant used as a summand in delivery predictability -setting id ({@value}).
     * Constant in [0, 1]. The constant term used in direct updates, also known as DP_init.
     */
    public static final String SUMMAND_S = "dpInit";
    /**
     * Constant used as a decay factor in delivery predictability -setting id ({@value}).
     * Constant in [0, 1] that determines how fast delivery predictabilities decay.
     */
    public static final String GAMMA_S = "dpGamma";
    /**
     * Constant used to control transitivity importance in delivery predictability -setting id ({@value}).
     * Constant in [0, 1] indicating the importance of transitivity updates.
     */
    public static final String BETA_S = "dpBeta";
    /**
     * Constant used to control decay -setting ide ({@value}).
     * Positive constant describing how many seconds are in a time unit (used for decay).
     */
    public static final String TIME_UNIT_S = "dpTimeUnit";

    /** Constant in [0, 1] used in direct updates, also known as DP_init. */
    private double summand;
    /** Constant in [0, 1] that determines how fast delivery predictabilities decay. */
    private double gamma;
    /** Constant in [0, 1] indicating the importance of transitivity updates. */
    private double beta;
    /** Describes how many seconds are in a time unit (used for decay). */
    private double secondsInTimeUnit;

    /** Address of the host attached to this storage. */
    private int ownAddress;

    /**
     * Maps host addresses to delivery predictabilities.
     */
    private Map<Integer, Double> deliveryPredictabilites = new HashMap<>();
    /** The last time delivery predictabilities have been updated. */
    private double lastUpdate;

    /**
     * Initializes a new instance of the {@link DeliveryPredictabilityStorage} class.
     */
    public DeliveryPredictabilityStorage() {
        Settings s = new Settings(DELIVERY_PREDICTABILITY_STORAGE_NS);

        this.summand = s.getDouble(SUMMAND_S);
        assertValueBetweenZeroAndOne(this.summand, SUMMAND_S);
        this.gamma = s.getDouble(GAMMA_S);
        assertValueBetweenZeroAndOne(this.gamma, GAMMA_S);
        this.beta = s.getDouble(BETA_S);
        assertValueBetweenZeroAndOne(this.beta, BETA_S);
        this.secondsInTimeUnit = s.getDouble(TIME_UNIT_S);
        if (this.secondsInTimeUnit <= 0) {
            throw new SettingsError("Setting " + TIME_UNIT_S + " should be positive, but is " + this.secondsInTimeUnit);
        }
    }

    /**
     * Copy constructor.
     */
    public DeliveryPredictabilityStorage(DeliveryPredictabilityStorage storage) {
        this.summand = storage.summand;
        this.gamma = storage.gamma;
        this.beta = storage.beta;
        this.secondsInTimeUnit = storage.secondsInTimeUnit;
    }

    /**
     * Sets the attached host.
     * @param host Host attached to this storage.
     */
    public void setAttachedHost(DTNHost host) {
        if (host == null) {
            throw new IllegalArgumentException("Host should not be null!");
        }
        this.ownAddress = host.getAddress();
    }

    /**
     * Asserts that the given value is between 0 and 1 and throws a {@link SettingsError} if that is not the case.
     * @param value The value to check.
     * @param name The value's name, important for error message.
     */
    private static void assertValueBetweenZeroAndOne(double value, String name) {
        if (value < 0 || value > 1) {
            throw new SettingsError(name + " should be between 0 and 1, but is " + value);
        }
    }

    /**
     * Updates the delivery predictabilities for two hosts meeting each other.
     * Warning: Only call this for one of the hosts per meeting!
     * @param storage1 Delivery predictability storage of the first host.
     * @param storage2 Delivery predictability storage of the second host.
     */
    public static void updatePredictabilitiesForBothHosts(
            DeliveryPredictabilityStorage storage1, DeliveryPredictabilityStorage storage2) {
        // Update internal delivery predictabilites before information exchange.
        storage1.updateOnConnection(storage2.getAttachedHostAddress());
        storage2.updateOnConnection(storage1.getAttachedHostAddress());

        // Exchange information.
        // Make sure to copy the delivery predictabilites which are used to update the second host s.t. it is not
        // dependent the first host's update.
        Map<Integer, Double> currentPredictabilities1 = new HashMap<>(storage1.deliveryPredictabilites);
        Map<Integer, Double> currentPredictabilities2 = storage2.deliveryPredictabilites;

        // Use it to update transitive delivery predictabilities.
        storage1.updateTransitiveDeliveryPredictabilities(storage2.getAttachedHostAddress(), currentPredictabilities2);
        storage2.updateTransitiveDeliveryPredictabilities(storage1.getAttachedHostAddress(), currentPredictabilities1);
    }

    /**
     * Executes the updates that have to be done by a host directly on connection to another host.
     * @param othersAddress Address of the host we connected to.
     */
    private void updateOnConnection(int othersAddress) {
        this.decayDeliveryPredictabilities();
        this.updateDirectDeliveryPredictabilityTo(othersAddress);
        this.lastUpdate = SimClock.getTime();
    }

    /**
     * Decays all entries in the delivery predictabilities.
     */
    private void decayDeliveryPredictabilities() {
        double timeDiff = (SimClock.getTime() - this.lastUpdate) / this.secondsInTimeUnit;
        double decay = Math.pow(this.gamma, timeDiff);
        for (Map.Entry<Integer, Double> entry : this.deliveryPredictabilites.entrySet()) {
            entry.setValue(entry.getValue() * decay);
        }
    }

    /**
     * Updates delivery predictability for a host.
     * @param hostAddress Address of the host we just met.
     */
    private void updateDirectDeliveryPredictabilityTo(int hostAddress) {
        double oldValue = this.getDeliveryPredictability(hostAddress);
        this.deliveryPredictabilites.put(hostAddress, oldValue + (1 - oldValue) * this.summand);
    }

    /**
     * Updates transitive (A->B->C) delivery predictabilities.
     * @param othersAddress Address of the host B we just met.
     * @param otherDeliveryPredictabilities The delivery predictability storage of the B host who we just met.
     */
    private void updateTransitiveDeliveryPredictabilities(
            int othersAddress, Map<Integer, Double> otherDeliveryPredictabilities) {
        // Change probabilities for all host the other knows...
        for (Map.Entry<Integer, Double> addressToDeliveryPredictability : otherDeliveryPredictabilities.entrySet()) {
            int knownAddress = addressToDeliveryPredictability.getKey();
            if (knownAddress == this.ownAddress) {
                // ...safe for yourself.
                continue;
            }

            // Change them using the transitive delivery predictability equation:
            double oldValue = this.getDeliveryPredictability(knownAddress);
            double predictabilityToNeighbor = this.getDeliveryPredictability(othersAddress);
            double neighborsValue = addressToDeliveryPredictability.getValue();
            this.deliveryPredictabilites.put(
                    knownAddress, oldValue + (1 - oldValue) * predictabilityToNeighbor * neighborsValue * this.beta);
        }
    }

    /**
     * Returns all known host addresses.
     * @return All known host addresses.
     */
    public Collection<Integer> getKnownAddresses() {
        return this.deliveryPredictabilites.keySet();
    }

    /**
     * Returns the delivery predictability to the provided host.
     * @param to Host to return the delivery predictability to.
     * @return The stored delivery predictability or 0 if the host is unknown.
     */
    public double getDeliveryPredictability(DTNHost to) {
        return this.getDeliveryPredictability(to.getAddress());
    }

    /**
     * Returns the delivery predictability for the provided message.
     * @param message The message to return the delivery predictability for.
     * @return For one-to-one messages, the predictability to the recipient is returned, for multicast, the maximum
     * predictability to any of the recipients.
     * @throws IllegalArgumentException if a message not of type one-to-one or multicast is provided.
     */
    public double getDeliveryPredictability(Message message) {
        switch (message.getType()) {
            case ONE_TO_ONE:
                return this.getDeliveryPredictability(message.getTo());
            case MULTICAST:
                MulticastMessage multicast = (MulticastMessage)message;
                return this.getMaximumDeliveryPredictability(multicast.getGroup().getMembers());
            default:
                throw new IllegalArgumentException(
                        "No delivery predictability for messages of type " + message.getType() + " defined!");
        }
    }

    /**
     * Returns the maximum delivery predictability to any of the provided addresses.
     * @param addresses The addresses to check the delivery predictability for.
     * @return The maximum delivery predictability.
     */
    private double getMaximumDeliveryPredictability(Integer[] addresses) {
        double maxDeliveryPred = 0;
        for (int address : addresses) {
            maxDeliveryPred = Math.max(maxDeliveryPred, this.getDeliveryPredictability(address));
        }
        return maxDeliveryPred;
    }

    /**
     * Returns the delivery predictability for the provided address.
     * @param address The address to return the delivery predictability for.
     * @return The delivery predictability.
     */
    private double getDeliveryPredictability(int address) {
        return this.deliveryPredictabilites.getOrDefault(address, 0D);
    }

    /**
     * Returns the constant used in direct updates, also known as DP_init.
     * @return The direct update constant.
     */
    public double getSummand() {
        return summand;
    }

    /**
     * Returns the constant that determines how fast delivery predictabilities decay.
     * @return The decay constant.
     */
    public double getGamma() {
        return gamma;
    }

    /**
     * Returns the constant indicating the importance of transitivity updates.
     * @return The transitivity constant.
     */
    public double getBeta() {
        return beta;
    }

    /**
     * Returns the constant describing how many seconds are in a time unit.
     * @return Number of seconds in a time unit.
     */
    public double getSecondsInTimeUnit() {
        return this.secondsInTimeUnit;
    }

    /**
     * Returns the address of the attached host.
     * @return Address of the host attached to this storage.
     */
    public int getAttachedHostAddress() {
        return this.ownAddress;
    }
}
