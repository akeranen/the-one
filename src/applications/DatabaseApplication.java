package applications;

import core.Application;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.LocalDatabase;
import core.Message;
import core.Settings;
import core.SettingsError;
import core.SimClock;
import input.DisasterDataCreationListener;
import input.DisasterDataNotifier;
import routing.MessageRouter;

import routing.util.EnergyModel;
import util.Tuple;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * An application handling the {@link LocalDatabase} for the {@link DTNHost} the application instance is attached to.
 *
 * Created by Britta Heymann on 12.04.2017.
 */
public class DatabaseApplication extends Application implements DisasterDataCreationListener {
    /**
     * Utility threshold -setting id ({@value}).
     * Value between 0 and 1.
     * The minimum utility a {@link DisasterData} item needs to have to be exchanged between nodes.
     */
    public static final String UTILITY_THRESHOLD = "utility_threshold";
    /**
     * Local database size range -setting id ({@value}).
     * Both bounds are inclusive.
     * The uniformly distributed values the byte size of a host's local database may take.
     */
    public static final String DATABASE_SIZE_RANGE = "db_size";
    /**
     * Database size randomizer seed -setting id ({@value}).
     * Part of the seed used for the pseudo-random number generator that sets local database sizes.
     * The other part of the seed is the {@link DTNHost}'s address.
     */
    public static final String SIZE_RANDOMIZER_SEED = "seed";
    /**
     * Minimum time between map sending in seconds -setting id ({@value}).
     * If {@link routing.util.DatabaseApplicationUtil#wrapUsefulDataIntoMessages(MessageRouter, DTNHost, List)} is called and
     * no map has been sent in the specified interval, a randomly chosen map data item is sent to all neighbors.
     */
    public static final String MIN_INTERVAL_MAP_SENDING = "mapSendingInterval";
    /**
     * The number of database items to sent per database message -setting id ({@value}).
     * Messages with less database items may be created if the total number to sent is not a multiple of this value.
     */
    public static final String ITEMS_PER_MESSAGE = "itemsPerMessage";

    /** Application ID */
    public static final String APP_ID = "AdHocNetworksInDisasterScenarios";

    /**
     * Factor used for seeding the pseudo-random number generator that decides the database size.
     * Without this factor, hosts with similar addresses would get similar database sizes.
     */
    private static final int SEED_VARIETY_FACTOR = 7079;

    /**
     * DataMessages, like 1-to-1s, always have lowest priority (0).
     */
    private static final int DATA_MESSAGE_PRIORITY = 0;

    /* Some properties that are the same across all application instances. */
    private double utilityThreshold;
    private long[] databaseSizeRange;
    private int seed;
    private double mapSendingInterval;
    private int itemsPerMessage;

    /** The host this application instance is attached to. */
    private DTNHost host;

    /** The host's database. */
    private LocalDatabase database;

    /** Stash for created data if attached host is not known yet. */
    private List<Tuple<DTNHost, DisasterData>> stashedCreationData = new ArrayList<>();

    /** The last time when a map data item was sent to neighbors. */
    private double lastMapSendingTime;

    /** Pseudo-random number generator for this application. */
    private Random pseudoRandom;

    /**
     * Creates a new instance of the {@link DatabaseApplication} class.
     * @param s A settings object to read properties from.
     */
    public DatabaseApplication(Settings s) {
        super();

        /* Read properties from settings */
        this.utilityThreshold = s.getDouble(UTILITY_THRESHOLD);
        this.databaseSizeRange = s.getCsvLongs(DATABASE_SIZE_RANGE, Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE);
        this.seed = s.getInt(SIZE_RANDOMIZER_SEED);
        this.mapSendingInterval = s.getDouble(MIN_INTERVAL_MAP_SENDING);
        this.itemsPerMessage = s.getInt(ITEMS_PER_MESSAGE);

        /* Check they are valid. */
        DatabaseApplication.checkUtilityThreshold(this.utilityThreshold);
        DatabaseApplication.checkDatabaseSizeRange(this.databaseSizeRange);
        DatabaseApplication.checkItemsPerMessage(this.itemsPerMessage);

        /* Set last map sending time. */
        this.lastMapSendingTime = 0.0;

        /* Set App ID. */
        super.setAppID(APP_ID);
    }

    /**
     * Copy constructor for the {@link DatabaseApplication} class.
     *
     * @param application The {@link DatabaseApplication} instance to copy.
     */
    public DatabaseApplication(DatabaseApplication application) {
        super(application);
        this.utilityThreshold = application.utilityThreshold;
        this.databaseSizeRange = application.databaseSizeRange;
        this.seed = application.seed;
        this.mapSendingInterval = application.mapSendingInterval;
        this.itemsPerMessage = application.itemsPerMessage;

        DisasterDataNotifier.addListener(this);
    }

    /**
     * Checks that the utility threshold is between 0 and 1.
     *
     * @param threshold Threshold to check.
     * @throws SettingsError for wrong thresholds.
     */
    private static void checkUtilityThreshold(double threshold) {
        if (threshold < 0 || threshold > 1) {
            throw new SettingsError("Utility threshold must be between 0 and 1.");
        }
    }

    /**
     * Checks that the sizes form a non-empty range and are all non-negative.
     *
     * @param databaseSizeRange Database size range to check.
     * @throws SettingsError for a wrong range.
     */
    private static void checkDatabaseSizeRange(long[] databaseSizeRange) {
        if (databaseSizeRange[1] < databaseSizeRange[0]) {
            throw new SettingsError("Database size range must be non-empty!");
        }
        if (databaseSizeRange[0] < 0) {
            throw new SettingsError("Database size may not be negative!");
        }
    }

    /**
     * Checks that we have at least one item per message.
     *
     * @param itemsPerMessage Setting to check.
     * @throws SettingsError if less than one item should be sent per message.
     */
    private static void checkItemsPerMessage(double itemsPerMessage) {
        if (itemsPerMessage <= 0) {
            throw new SettingsError("Items per message must be at least 1!");
        }
    }

    /**
     * Called every simulation cycle.
     *
     * If the application instance is not aware of the host it is attached to at this point, it will use that new
     * knowledge.
     *
     * @param host The host this application instance is attached to.
     */
    @Override
    public void update(DTNHost host) {
        if (!this.isInitialized()) {
            this.initialize(host);
        }
    }

    /**
     * This method handles application functionality related to
     * processing of the bundle. Application handles a messages,
     * which arrives to the node hosting this application.
     *
     * In this case, all data messages to the host this application is attached to are unwrapped, the data is stored,
     * and those messages are not used any further.
     *
     * @param msg  The incoming message.
     * @param host The host this application instance is attached to.
     * @return the (possibly modified) message to forward or <code>null</code>
     * if the application wants the router to stop forwarding the
     * message.
     */
    @Override
    public Message handle(Message msg, DTNHost host) {
        // If we don't know who the application is attached to yet, use the new knowledge for initialization.
        if (!this.isInitialized()) {
            this.initialize(host);
        }

        // If the message is a data message sent to the host the application instance is attached to, unwrap and store
        // the data and don't forward the message any further.
        if (msg instanceof DataMessage && msg.getTo().equals(this.host)) {
            for (DisasterData dataItem : ((DataMessage)msg).getData()) {
                this.database.add(dataItem);
            }
            return null;
        } else {
            // Else act as you would usually do.
            return msg;
        }
    }

    /**
     * Creates database synchronization messages from existing useful data.
     *
     * @param databaseOwner The DTNHost this instance of the application is attached to.
     * @return The created messages. They don't have a receiver yet, so {@link Message#getTo()} will return null.
     */
    public List<DataMessage> wrapUsefulDataIntoMessages(DTNHost databaseOwner) {
        // If we don't know who the application is attached to yet, use the new knowledge for initialization.
        if (!this.isInitialized()) {
            this.initialize(databaseOwner);
        }

        // Find all data which could be interesting for neighbors.
        List<Tuple<DisasterData, Double>> interestingData =
                this.database.getAllNonMapDataWithMinimumUtility(this.utilityThreshold);

        // If it's been a while, add a map item to that data.
        // TODO: Once we use maps again, this should be a single message.
        if (SimClock.getTime() - this.lastMapSendingTime >= this.mapSendingInterval) {
            List<DisasterData> allMaps = this.database.getMapData();
            if (!allMaps.isEmpty()) {
                DisasterData map = allMaps.get(this.pseudoRandom.nextInt(allMaps.size()));
                // Set utility to the maximum one as we just want to generate realistic traffic.
                interestingData.add(new Tuple<>(map, 1.0));
                this.lastMapSendingTime = SimClock.getTime();
            }
        }

        // Then create data messages out of the data items.
        return this.createDataMessagePrototypes(interestingData);
    }

    /**
     * Creates database synchronization messages from existing useful data that has been modified recently.
     *
     * @param databaseOwner The DTNHost this instance of the application is attached to.
     * @param maximumNumberSecondsSinceModification The maximum number of seconds since last modification.
     * @return The created messages. They don't have a receiver yet, so {@link Message#getTo()} will return null.
     */
    public List<DataMessage> wrapRecentUsefulDataIntoMessages(
            DTNHost databaseOwner, int maximumNumberSecondsSinceModification) {
        // If we don't know who the application is attached to yet, use the new knowledge for initialization.
        if (!this.isInitialized()) {
            this.initialize(databaseOwner);
        }

        // Find all interesting data which has been modified recently.
        List<Tuple<DisasterData, Double>> recentData = new ArrayList<>();
        for (Tuple<DisasterData, Double> dataWithUtility :
                this.database.getAllNonMapDataWithMinimumUtility(this.utilityThreshold)) {
            if (SimClock.getTime() - dataWithUtility.getKey().getCreation() <= maximumNumberSecondsSinceModification) {
                recentData.add(dataWithUtility);
            }
        }

        // Then create data messages out of the data items.
        return this.createDataMessagePrototypes(recentData);
    }

    /**
     * Creates a {@link DataMessage} prototype out of every {@link #itemsPerMessage} data items, i.e. a data message
     * without a receiver. The data items are sorted by utility to group them into messages.
     *
     * @param data The data to wrap.
     * @return The created messages.
     */
    private List<DataMessage> createDataMessagePrototypes(List<Tuple<DisasterData, Double>> data) {
        // Sort data items by utility.
        data.sort(Comparator.comparingDouble(t -> (-1) * t.getValue()));

        // Then create a message out of every x data items.
        List<DataMessage> messages = new ArrayList<>(data.size());
        DTNHost unknownReceiver = null;
        for (int i = 0; i < data.size(); i += this.itemsPerMessage) {
            int firstIndexNotToSent = Math.min(i + this.itemsPerMessage, data.size());
            List<Tuple<DisasterData, Double>> subsetToSent = data.subList(i, firstIndexNotToSent);
            DataMessage message = new DataMessage(
                    this.host, unknownReceiver,
                    DatabaseApplication.createMessageId(subsetToSent), subsetToSent,
                    DATA_MESSAGE_PRIORITY);
            message.setAppID(APP_ID);
            messages.add(message);
        }
        return messages;
    }

    private static String createMessageId(List<Tuple<DisasterData, Double>> subsetToSent) {
        List<DisasterData> dataList = new ArrayList<>(subsetToSent.size());
        for (Tuple<DisasterData, Double> item : subsetToSent) {
            dataList.add(item.getKey());
        }
        return "D" + dataList.hashCode();
    }

    /**
     * Called when a {@link DisasterData} got created by a {@link input.DisasterDataCreateEvent}.
     *
     * Adds the newly created data to local database if the creator is the host this application instance is attached
     * to.
     *
     * @param creator   {@link DTNHost} that created the data.
     * @param data The created {@link DisasterData}.
     */
    @Override
    public void disasterDataCreated(DTNHost creator, DisasterData data) {
        // If the host is not known yet, stash the information until it is known.
        if (!this.isInitialized()) {
            this.stashedCreationData.add(new Tuple<>(creator, data));
            return;
        }

        // If battery is empty, no data will be created.
        if (this.host.getComBus().getDouble(EnergyModel.ENERGY_VALUE_ID, 1) <= 0) {
            return;
        }

        // Otherwise, check if the creator is the attached host and add the data to database if that's the case.
        if (creator.equals(this.host)) {
            this.database.add(data);
        }
    }

    /**
     * Gets the total size of the database this application manages.
     *
     * @return The application's database's total size.
     */
    public long getDatabaseSize() {
        if (!this.isInitialized()) {
            throw new IllegalStateException("Cannot get database size before application was initialized!");
        }

        return this.database.getTotalSize();
    }

    /**
     * Checks whether we already know the host this application instance is attached to.
     *
     * @return A value indicating whether we know the host this application instance is attached to.
     */
    private boolean isInitialized() {
        return this.host != null;
    }

    /**
     * Initializes instance fields that are dependent on the host the application instance is attached to.
     *
     * @param host The host the application instance is attached to.
     */
    private void initialize(DTNHost host) {
        this.host = host;
        this.pseudoRandom = new Random(this.seed ^ (SEED_VARIETY_FACTOR * this.host.getAddress()));
        this.database = new LocalDatabase(this.host, selectRandomDatabaseSize());

        // We can now handle our stash.
        for (Tuple<DTNHost, DisasterData> creatorDataTuple : this.stashedCreationData) {
            if (creatorDataTuple.getKey().equals(this.host)) {
                this.database.add(creatorDataTuple.getValue());
            }
        }
        this.stashedCreationData.clear();
    }

    /**
     * Selects a valid random database size.
     * @return The selected size.
     */
    private long selectRandomDatabaseSize() {
        return Math.round(this.databaseSizeRange[0]
                + this.pseudoRandom.nextDouble() * (this.databaseSizeRange[1] - this.databaseSizeRange[0]));
    }

    @Override
    public Application replicate() {
        return new DatabaseApplication(this);
    }

    public double getUtilityThreshold() {
        return utilityThreshold;
    }

    public long[] getDatabaseSizeRange() {
        return databaseSizeRange.clone();
    }

    public int getSeed() {
        return seed;
    }

    public double getMapSendingInterval() {
        return mapSendingInterval;
    }

    public int getItemsPerMessage() {
        return this.itemsPerMessage;
    }

    /**
     * Computes the statistics about the data utility across all {@link DisasterData} items
     * @return statistics about the data utility across all {@link DisasterData} items
     */
    public DoubleSummaryStatistics getDataUtilityStatistics(){
        return database.getDataUtilityStatistics();
    }

    /**
     * Computes the statistics about the data age across all {@link DisasterData} items,
     * which are not of type map
     * @return statistics about the data age across all non-map {@link DisasterData} items
     */
    public DoubleSummaryStatistics getDataAgeStatistics(){
        return database.getDataAgeStatistics();
    }

    /**
     * Computes the statistics about the distance of the data to the host which owns the
     * the {@link DatabaseApplication} across all {@link DisasterData} items
     * @return statistics about the data distance across all {@link DisasterData} items
     */
    public DoubleSummaryStatistics getDataDistanceStatistics(){
        return database.getDataDistanceStatistics();
    }

    /**
     * Percentage of memory available for {@link DisasterData} which is used as a value between 0 and 1
     * @return percentage of available memory for {@link DisasterData} which is used as a value between 0 and 1
     */
    public double getUsedMemoryPercentage(){
        return database.getUsedMemoryPercentage();
    }

    /**
     * Returns the ratio of {@link DisasterData} items in the database per
     * {@link DisasterData.DataType} to the total number of items.
     * @return A hashmap containing a ratio between 0 and 1 for each {@link DisasterData.DataType}
     */
    public Map<DisasterData.DataType, Double> getRatioOfItemsPerDataType(){
        return database.getRatioOfItemsPerDataType();
    }
}
