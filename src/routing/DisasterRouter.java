package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SettingsError;
import core.SimClock;
import routing.choosers.RescueModeMessageChooser;
import routing.choosers.UtilityMessageChooser;
import routing.prioritizers.DisasterPrioritizationStrategy;
import routing.prioritizers.PrioritySorter;
import routing.prioritizers.PriorityTupleSorter;
import routing.util.DatabaseApplicationUtil;
import routing.util.DeliveryPredictabilityStorage;
import routing.util.DisasterBufferComparator;
import routing.util.EncounterValueManager;
import routing.util.ReplicationsDensityManager;
import util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Flexible router that can be used in a disaster scenario.
 *
 * Created by Britta Heymann on 19.05.2017.
 */
public class DisasterRouter extends ActiveRouter {
    /** Namespace for all general disaster router settings. */
    public static final String DISASTER_ROUTER_NS = "DisasterRouter";

    /**
     * If the host's relative power is below this threshold, it will change into a rescue mode. -setting id ({@value}).
     * In resuce mode, the host tries to save all its messages and recent data from deletion by sending them out.
     */
    public static final String POWER_THRESHOLD = "powerThreshold";

    /* Comparators to sort direct messages. */
    private Comparator<Message> directMessageComparator;
    private Comparator<Tuple<Message, Connection>> directMessageTupleComparator;

    /* Strategies for non-direct messages: Which to send in what order? */
    private MessageChoosingStrategy messageChooser;
    private MessagePrioritizationStrategy messagePrioritizer;

    /* Buffer management strategy. */
    private Comparator<Message> rankComparator;

    /* Rating mechanism helpers. */
    private EncounterValueManager encounterValueManager;
    private ReplicationsDensityManager replicationsDensityManager;
    private DeliveryPredictabilityStorage deliveryPredictabilityStorage;

    /**
     * A cache for non-direct messages to all neighbors, sorted in the order in which they should be sent.
     * The cache is recomputed every {@link #messageOrderingInterval} seconds or in the case that a new connection comes
     * up. As soon as a connection breaks, the respective messages are removed from this cache.
     *
     * The introduction of this cache leads to higher memory usage, but more efficiency. It also has the downside that
     * newly created or received messages are not directly sent to other hosts as they are not in the cache yet, while
     * messages deleted from cache might still be sent. We can tolerate this as long as {@link #messageOrderingInterval}
     * is not chosen too high.
     */
    private List<Tuple<Message, Connection>> cachedNonDirectMessages = new ArrayList<>();

    /**
     * If the {@link DTNHost}'s relative power is below this threshold, it will change into a rescue mode in which
     * it tries to save all its messages and recent data from deletion by sending them out.
     */
    private double powerThreshold;
    
    /**
     * Number of tuples of messages/hosts which are remembered, such that they are not sent again
     */
    private static final int MESSAGE_HISTORY_SIZE = 1000;
    
    /**
     * List storing the last x message IDs and host IDs that are not sent again. The size of the list is restricted to {@link #MESSAGE_HISTORY_SIZE}. 
     */
    private List<Tuple<String, Integer>> messageSentToHostHistory = new ArrayList<>();
    
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
        this.rankComparator = new DisasterBufferComparator(this);

        // Read power threshold from settings.
        s.setNameSpace(DISASTER_ROUTER_NS);
        this.powerThreshold = s.getDouble(POWER_THRESHOLD);
        if (this.powerThreshold < 0 || this.powerThreshold > 1) {
            throw new SettingsError("Power threshold should be in [0, 1], but is " + this.powerThreshold + "!");
        }
        s.restoreNameSpace();
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
        this.rankComparator = new DisasterBufferComparator(this);

        // Copy power threshold.
        this.powerThreshold = router.powerThreshold;
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

            // Add messages to this new neighbor to message cache.
            this.recomputeMessageCache();
        } else {
            // For broken connections, clean up message cache.
            this.removeConnectionFromMessageCache(con);
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

        // Change strategies depending on remaining energy.
        this.switchBetweenStrategiesDependingOnEnergy();

        // Update rating mechanisms.
        this.encounterValueManager.update();
        this.replicationsDensityManager.update();
        this.deliveryPredictabilityStorage.update();

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
     * Checks whether the current {@link #messageChooser} uses the correct strategy with reference to the
     * {@link DTNHost}'s remaining power.
     */
    private void switchBetweenStrategiesDependingOnEnergy() {
        boolean rescueModeRequired = this.remainingEnergyRatio() < this.powerThreshold;
        boolean inRescueMode = this.messageChooser instanceof RescueModeMessageChooser;
        if (rescueModeRequired && !inRescueMode) {
            this.messageChooser = new RescueModeMessageChooser();
            this.messageChooser.setAttachedHost(this.getHost());
        }
        if (!rescueModeRequired && inRescueMode) {
            this.messageChooser = new UtilityMessageChooser(this);
            this.messageChooser.setAttachedHost(this.getHost());
        }
    }
    
    /**
     * Method is called just before a transfer is finalized
     * at {@link #update()}.
     * Subclasses that are interested of the event may want to override this.
     * @param con The connection whose transfer was finalized
     */
    protected void transferDone(Connection con) {
        addMessageAndHostToHistory(con.getMessage(), con.getOtherNode(getHost()));
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
        if(SimClock.getTime() - this.lastMessageOrdering >= this.messageOrderingInterval){
            this.recomputeMessageCache();
        }
        this.tryMessagesForConnected(this.cachedNonDirectMessages);
    }

    /**
     * Recomputes messages that should be sent and the order in which they should be sent.
     */
    private void recomputeMessageCache() {
        Collection<Tuple<Message, Connection>> messages =
                this.messageChooser.chooseNonDirectMessages(this.getMessageCollection(), this.getConnections());
        this.cachedNonDirectMessages = this.messagePrioritizer.sortMessages(messages);

        removeMessagesContainedInHistory(cachedNonDirectMessages);
        
        this.lastMessageOrdering = SimClock.getTime();
    }

    /**
     * Removes all message-connection pairs with the provided connection from {@link #cachedNonDirectMessages}.
     * @param con Connection which should not get any messages anymore.
     */
    private void removeConnectionFromMessageCache(Connection con) {
        Iterator<Tuple<Message,Connection>> it = this.cachedNonDirectMessages.listIterator();
        while (it.hasNext()){
            if (it.next().getValue().equals(con)){
                it.remove();
            }
        }
    }

    /**
     * Returns the lowest ranked message in the message buffer
     * (that is not being sent if excludeMsgBeingSent is true).
     *
     * @param excludeMsgBeingSent If true, excludes message(s) that are
     *                            being sent from the check (i.e. if lowest rank message is
     *                            being sent, the second lowest rank message is returned)
     * @return The lowest rank message or null if no message could be returned
     * (no messages in buffer or all messages in buffer are being sent and
     * exludeMsgBeingSent is true)
     */
    @Override
    protected Message getNextMessageToRemove(boolean excludeMsgBeingSent) {
        Message lowestRankMessage = null;
        for (Message m : this.getMessageCollection()) {
            if (excludeMsgBeingSent && isSending(m.getId())) {
                continue;
            }

            if (lowestRankMessage == null || this.rankComparator.compare(m, lowestRankMessage) < 0) {
                lowestRankMessage = m;
            }
        }

        return lowestRankMessage;
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
     * Adds a message / host pair to the message history. Also deletes one if the list has reached its maximum size
     * @param m Message to be added
     * @param h Host to be added
     */
    private void addMessageAndHostToHistory(Message message, DTNHost host) {
        Tuple<String, Integer> historyItem = new Tuple<>(message.getId(), host.getAddress());
        
        if (this.messageSentToHostHistory.size() < MESSAGE_HISTORY_SIZE) {
            this.messageSentToHostHistory.add(0, historyItem);
        } else {
            while (this.messageSentToHostHistory.size() >= MESSAGE_HISTORY_SIZE) {
              this.messageSentToHostHistory.remove(this.messageSentToHostHistory.size() - 1);
            }
            
            this.messageSentToHostHistory.add(0, historyItem);
        }
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
        removeMessagesContainedInHistory(messages);
        
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
        
        messages = removeMessagesContainedInHistory(messages, connected);
        
        messages.sort(this.directMessageComparator);
        return messages;
    }
    
    /**
     * Removes messages that already occur in the message history
     * @param messages: 
     * @return
     */
    private void removeMessagesContainedInHistory(List<Tuple<Message, Connection>> messages) {
        
        Iterator<Tuple<Message, Connection>> iter = messages.iterator();

        while (iter.hasNext()) {
            Tuple<Message, Connection> t = iter.next();
            Tuple<String, Integer> historyEntry = new Tuple<>(t.getKey().getId(), 
                    t.getValue().getOtherNode(getHost()).getAddress());
            
            if (messageSentToHostHistory.contains(historyEntry)) {
                iter.remove();
            }
        }
    }
    
    /**
     * Removes messages that already occur in the message history
     * @param messages: 
     * @return
     */
    private List<Message> removeMessagesContainedInHistory(List<Message> messages, DTNHost host) {
        
        Iterator<Message> iter = messages.iterator();
        
        while (iter.hasNext()) {
            Message message = iter.next();
            Tuple<String, Integer> historyEntry = new Tuple<>(message.getId(), host.getAddress());
            
            if (messageSentToHostHistory.contains(historyEntry)) {
                iter.remove();
            }
        }
        
        return messages;
    }
    
    /**
     * Returns the power threshold.
     * @return The power threshold.
     */
    public double getPowerThreshold() {
        return this.powerThreshold;
    }
    
    public List<Tuple<String, Integer>> getMessageSentToHostHistory() {
        return messageSentToHostHistory;
    }
    
    public static int getMessageHistorySize() {
        return MESSAGE_HISTORY_SIZE;
    }
}
