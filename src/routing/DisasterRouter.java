package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import routing.choosers.UtilityMessageChooser;
import routing.prioritizers.DisasterPrioritizationStrategy;
import routing.prioritizers.PrioritySorter;
import routing.prioritizers.PriorityTupleSorter;
import routing.util.DatabaseApplicationUtil;
import routing.util.DeliveryPredictabilityStorage;
import routing.util.EncounterValueManager;
import routing.util.ReplicationsDensityManager;
import util.Tuple;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Flexible router that can be used in a disaster scenario.
 *
 * Created by Britta Heymann on 19.05.2017.
 */
public class DisasterRouter extends ActiveRouter {
    /* Comparators to sort direct messages. */
    private Comparator<Message> directMessageComparator;
    private Comparator<Tuple<Message, Connection>> directMessageTupleComparator;

    /* Strategies for non-direct messages: Which to send in what order? */
    private MessageChoosingStrategy messageChooser;
    private MessagePrioritizationStrategy messagePrioritizer;

    /* Rating mechanism helpers. */
    private EncounterValueManager encounterValueManager;
    private ReplicationsDensityManager replicationsDensityManager;
    private DeliveryPredictabilityStorage deliveryPredictabilityStorage;

    /**
     * Initializes a new instance of the {@link DisasterRouter} class.
     * @param s Settings to use.
     */
    public DisasterRouter(Settings s) {
        super(s);

        // Initialize rating mechanism managers.
        this.encounterValueManager = new EncounterValueManager();
        this.replicationsDensityManager = new ReplicationsDensityManager();
        this.deliveryPredictabilityStorage = new DeliveryPredictabilityStorage();

        // Initialize message chooser.
        this.messageChooser = new UtilityMessageChooser(this);

        // Initialize message orderers.
        this.messagePrioritizer = new DisasterPrioritizationStrategy(this);
        this.directMessageComparator = new PrioritySorter();
        this.directMessageTupleComparator = new PriorityTupleSorter();
    }

    /**
     * Copy constructor.
     */
    private DisasterRouter(DisasterRouter router) {
        super(router);

        // Copy rating mechanism managers.
        this.encounterValueManager = new EncounterValueManager(router.encounterValueManager);
        this.replicationsDensityManager = new ReplicationsDensityManager(router.replicationsDensityManager);
        this.deliveryPredictabilityStorage = new DeliveryPredictabilityStorage(router.deliveryPredictabilityStorage);

        // Copy message chooser.
        this.messageChooser = router.messageChooser.replicate(this);

        // Copy message orderers.
        this.messagePrioritizer = router.messagePrioritizer.replicate(this);
        this.directMessageComparator = router.directMessageComparator;
        this.directMessageTupleComparator = router.directMessageTupleComparator;
    }

    /**
     * Checks if the router is a {@link DisasterRouter} and throws an {@link IllegalArgumentException} if it isn't.
     * @param router Router to check
     */
    public static void checkRouterIsDisasterRouter(MessageRouter router) {
        if (router == null) {
            throw new IllegalArgumentException("Router is null!");
        }
        if (!(router instanceof DisasterRouter)) {
            throw new IllegalArgumentException("Cannot handle routers of type " + router.getClass() + "!");
        }
    }

    /**
     * Initializes the router; i.e. sets the host this router is in and
     * message listeners that need to be informed about message related
     * events etc.
     * @param host The host this router is in
     * @param mListeners The message listeners
     */
    @Override
    public void init(DTNHost host, List<MessageListener> mListeners) {
        super.init(host, mListeners);
        this.deliveryPredictabilityStorage.setAttachedHost(host);
        this.messagePrioritizer.setAttachedHost(host);
        this.messageChooser.setAttachedHost(host);
    }

    /**
     * Creates a replicate of this router. The replicate has the same
     * settings as this router but empty buffers and routing tables.
     *
     * @return The replicate
     */
    @Override
    public MessageRouter replicate() {
        return new DisasterRouter(this);
    }

    /**
     * Called when a connection's state changes. If energy modeling is enabled,
     * and a new connection is created to this node, reduces the energy for the
     * device discovery (scan response) amount
     *
     * @param con The connection whose state changed
     */
    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con);

        // For new connections:
        if (con.isUp()) {
            DTNHost encounteredHost = con.getOtherNode(this.getHost());

            // Update rating mechanisms.
            this.encounterValueManager.addEncounter(encounteredHost);
            this.replicationsDensityManager.addEncounter(encounteredHost);
            // Initiator updates the delivery predictabilities for both hosts so that the values
            // for both are updated before exchanging them and doing transitive updates.
            if (con.isInitiator(this.getHost())) {
                DisasterRouter encounteredRouter = (DisasterRouter)encounteredHost.getRouter();
                DeliveryPredictabilityStorage.updatePredictabilitiesForBothHosts(
                        this.deliveryPredictabilityStorage, encounteredRouter.deliveryPredictabilityStorage);
            }
        }
    }

    /**
     * Checks out all sending connections to finalize the ready ones
     * and abort those whose connection went down. Also drops messages
     * whose TTL <= 0 (checking every one simulated minute).
     *
     * @see #addToSendingConnections(Connection)
     */
    @Override
    public void update() {
        super.update();

        // Update rating mechanisms.
        this.encounterValueManager.update();
        this.replicationsDensityManager.update();

        // Don't continue computing if there is no chance any message will be sent.
        if (this.isTransferring() || !this.canStartTransfer()) {
            return;
        }

        // First try to send or receive direct messages.
        if (this.exchangeDeliverableMessages() != null) {
            return;
        }

        // If non are available, try to send other messages.
        this.tryOtherMessages();
    }

    /**
     * Checks whether this router has anything to send out.
     *
     * @return Whether or not the router has anything to send out.
     */
    @Override
    protected boolean hasNothingToSend() {
        return DatabaseApplicationUtil.hasNoMessagesToSend(this);
    }

    /**
     * Tries to send non-direct messages to connected hosts.
     */
    private void tryOtherMessages() {
        Collection<Tuple<Message, Connection>> messages =
                this.messageChooser.chooseNonDirectMessages(this.getMessageCollection(), this.getConnections());
        List<Tuple<Message, Connection>> prioritizedMessages = this.messagePrioritizer.sortMessages(messages);
        this.tryMessagesForConnected(prioritizedMessages);
    }

    /**
     * Adds a message to the message buffer and informs message listeners
     * about new message (if requested).
     *
     * @param m          The message to add
     * @param newMessage If true, message listeners are informed about a new
     */
    @Override
    protected void addToMessages(Message m, boolean newMessage) {
        super.addToMessages(m, newMessage);
        this.replicationsDensityManager.addMessage(m.getId());
    }

    /**
     * Removes and returns a message from the message buffer.
     *
     * @param id Identifier of the message to remove
     * @return The removed message or null if message for the ID wasn't found
     */
    @Override
    protected Message removeFromMessages(String id) {
        this.replicationsDensityManager.removeMessage(id);
        return super.removeFromMessages(id);
    }

    /**
     * Computes a ratio between the encounter value of this router and the one of the provided router.
     * A ratio less than 0.5 signifies that the other host is less social than this one, a
     * ratio higher than 0.5 signifies the opposite.
     *
     * @param otherRouter The router to compare this router to.
     * @return A ratio between 0 and 1.
     */
    public double computeEncounterValueRatio(DisasterRouter otherRouter) {
        return this.encounterValueManager.computeEncounterValueRatio(otherRouter.getEncounterValue());
    }

    /**
     * Returns the host's encounter value.
     * @return The host's encounter value.
     */
    public double getEncounterValue() {
        return this.encounterValueManager.getEncounterValue();
    }

    /**
     * Returns the message's replications density according to this host's point of view.
     * @param m The message to find the replications density for.
     * @return The replications density.
     */
    public double getReplicationsDensity(Message m) {
        return this.replicationsDensityManager.getReplicationsDensity(m.getId());
    }

    /**
     * Returns the delivery predictability this host has for the provided message.
     * @param m Message to find delivery predictability for.
     * @return The delivery predictability.
     */
    public double getDeliveryPredictability(Message m) {
        return this.deliveryPredictabilityStorage.getDeliveryPredictability(m);
    }

    /**
     * Gets prioritized messages for connected hosts.
     * @return The ordered messages, most important messages first.
     */
    @Override
    protected List<Tuple<Message, Connection>> getSortedMessagesForConnected() {
        List<Tuple<Message, Connection>> messages = this.getMessagesForConnected();
        messages.sort(this.directMessageTupleComparator);
        return messages;
    }

    /**
     * Gets prioritized messages for the provided host.
     * @param connected A connected host.
     * @return The ordered messages, most important messages first.
     */
    @Override
    protected List<Message> getSortedMessagesForConnected(DTNHost connected) {
        List<Message> messages = super.getSortedMessagesForConnected(connected);
        messages.sort(this.directMessageComparator);
        return messages;
    }
}
