package applications;

import core.Application;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.LocalDatabase;
import core.Message;
import core.Settings;
import input.DisasterDataCreationListener;
import input.DisasterDataNotifier;
import routing.MessageRouter;
import util.Tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    /** Application ID */
    public static final String APP_ID = "AdHocNetworksInDisasterScenarios";

    /**
     * Factor used for seeding the pseudo-random number generator that decides the database size.
     * Without this factor, hosts with similar addresses would get similar database sizes.
     */
    private static final int SEED_VARIETY_FACTOR = 7079;

    /* Some properties that are the same across all application instances. */
    private double utilityThreshold;
    private long[] databaseSizeRange;
    private int seed;

    /** The host this application instance is attached to. */
    private DTNHost host;

    /** The host's database. */
    private LocalDatabase database;

    /** Stash for created data if attached host is not known yet. */
    private List<Tuple<DTNHost, DisasterData>> stashedCreationData = Collections.synchronizedList(new ArrayList<>());

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

        /* Check they are valid. */
        DatabaseApplication.checkUtilityThreshold(this.utilityThreshold);
        DatabaseApplication.checkDatabaseSizeRange(this.databaseSizeRange);

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

        DisasterDataNotifier.addListener(this);
    }

    /**
     * Returns the {@link DatabaseApplication} instance of the provided {@link MessageRouter}, if it has one.
     *
     * @param router The router to check for a {@link DatabaseApplication}.
     * @return The found {@link DatabaseApplication} or null if there is none.
     */
    public static DatabaseApplication findDatabaseApplication(MessageRouter router) {
        for (Application application : router.getApplications(APP_ID)) {
            if (application instanceof DatabaseApplication) {
                return (DatabaseApplication)application;
            }
        }
        return null;
    }

    /**
     * Checks that the utility threshold is between 0 and 1.
     *
     * @param threshold Threshold to check.
     * @throws IllegalArgumentException for wrong thresholds.
     */
    private static void checkUtilityThreshold(double threshold) {
        if (threshold < 0 || threshold > 1) {
            throw new IllegalArgumentException("Utility threshold must be between 0 and 1.");
        }
    }

    /**
     * Checks that the sizes form a non-empty range and are all non-negative.
     *
     * @param databaseSizeRange Database size range to check.
     * @throws IllegalArgumentException for a wrong range.
     */
    private static void checkDatabaseSizeRange(long[] databaseSizeRange) {
        if (databaseSizeRange[1] < databaseSizeRange[0]) {
            throw new IllegalArgumentException("Database size range must be non-empty!");
        }
        if (databaseSizeRange[0] < 0) {
            throw new IllegalArgumentException("Database size may not be negative!");
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
            this.database.add(((DataMessage) msg).getData());
            return null;
        } else {
            // Else act as you would usually do.
            return msg;
        }
    }

    /**
     * Creates database synchronization messages that should be sent to the provided receiver.
     *
     * @param databaseOwner The DTNHost this instance of the application is attached to.
     * @param receiver DTNHost to find data messages for.
     * @return The created messages.
     */
    public List<DataMessage> createDataMessages(DTNHost databaseOwner, DTNHost receiver) {
        // If we don't know who the application is attached to yet, use the new knowledge for initialization.
        if (!this.isInitialized()) {
            this.initialize(databaseOwner);
        }

        List<DisasterData> interestingData = this.database.getAllDataWithMinimumUtility(this.utilityThreshold);
        List<DataMessage> messages = new ArrayList<>(interestingData.size());
        for (DisasterData data : interestingData) {
            // Create message out of the data.
            // DataMessages, like 1-to-1s, always have lowest priority (0).
            DataMessage message = new DataMessage(this.host, receiver, data.toString(), data, 0);
            message.setAppID(APP_ID);
            messages.add(message);
        }
        return messages;
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

        // Otherwise, check if the creator is the attached host and add the data to database if that's the case.
        if (creator.equals(this.host)) {
            this.database.add(data);
        }
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
        // Random generators need to be different for each host to have different database sizes.
        // Create one.
        Random pseudoRandom = new Random(this.seed ^ (SEED_VARIETY_FACTOR * this.host.getAddress()));
        // Then, randomly choose a database size.
        return Math.round(this.databaseSizeRange[0]
                + pseudoRandom.nextDouble() * (this.databaseSizeRange[1] - this.databaseSizeRange[0]));
    }

    @Override
    public Application replicate() {
        return new DatabaseApplication(this);
    }

}
