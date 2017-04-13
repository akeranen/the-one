package applications;

import core.Application;
import core.Connection;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.LocalDatabase;
import core.Message;
import core.Settings;
import core.SimScenario;
import core.World;
import input.DisasterDataCreationListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * An application handling one local database per host.
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
     * The seed used for the pseudo-random number generator that sets local database sizes.
     */
    public static final String SIZE_RANDOMIZER_SEED = "seed";

    // TODO: Should all these be static? Does every host have its own application?
    private double utilityThreshold;
    private Map<DTNHost, LocalDatabase> hostsToDatabases;
    private Random random;

    public DatabaseApplication() {
        super();
        this.hostsToDatabases = Collections.synchronizedMap(new HashMap<>());
    }

    private DatabaseApplication(DatabaseApplication application) {
        super(application);
        this.hostsToDatabases = Collections.synchronizedMap(application.hostsToDatabases);
        this.utilityThreshold = application.utilityThreshold;
    }

    private void initialize() {
        Settings s = new Settings(this.getClass().getSimpleName());

        /* Read properties from settings */
        this.utilityThreshold = s.getDouble(UTILITY_THRESHOLD);
        int[] databaseSizeRange = s.getCsvInts(DATABASE_SIZE_RANGE, Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE);
        int seed = s.getInt(SIZE_RANDOMIZER_SEED);

        /* Check they are valid. */
        DatabaseApplication.checkUtilityThreshold(this.utilityThreshold);
        DatabaseApplication.checkDatabaseSizeRange(databaseSizeRange);

        /* Create randomizer. */
        this.random = new Random(seed);

        /* Initialize host -> database map. */
        World world = SimScenario.getInstance().getWorld();
        for (DTNHost host : world.getHosts()) {
            hostsToDatabases.put(host, new LocalDatabase(host, selectRandomInteger(databaseSizeRange)));
        }
    }

    private static void checkUtilityThreshold(double threshold) {
        if (threshold < 0 || threshold > 1) {
            throw new IllegalArgumentException("Utility threshold must be between 0 and 1.");
        }
    }

    private static void checkDatabaseSizeRange(int[] databaseSizeRange) {
        if (databaseSizeRange[1] < databaseSizeRange[0]) {
            throw new IllegalArgumentException("Database size range must be non-empty!");
        }
        if (databaseSizeRange[0] < 0) {
            throw new IllegalArgumentException("Database size may not be negative!");
        }
    }

    private int selectRandomInteger(int[] range) {
        return range[0] + this.random.nextInt(range[1] + 1);
    }

    /**
     * Called when a {@link DisasterData} got created by a {@link input.DisasterDataCreateEvent}.
     * Adds the newly created data to the host's local database.
     *
     * @param creator   {@link DTNHost} that created the data.
     * @param data The created {@link DisasterData}.
     */
    @Override
    public void disasterDataCreated(DTNHost creator, DisasterData data) {
        LocalDatabase creatorDatabase = hostsToDatabases.get(creator);
        creatorDatabase.add(data);
    }

    /**
     * This method handles application functionality related to
     * processing of the bundle. Application handles a messages,
     * which arrives to the node hosting this application. After
     * performing application specific handling, this method returns
     * a list of messages. If node wishes to continue forwarding the
     * incoming
     *
     * @param msg  The incoming message.
     * @param host The host this application instance is attached to.
     * @return the (possibly modified) message to forward or <code>null</code>
     * if the application wants the router to stop forwarding the
     * message.
     */
    @Override
    public Message handle(Message msg, DTNHost host) {
        if (msg instanceof DataMessage) {
            LocalDatabase database = hostsToDatabases.get(host);
            database.add(((DataMessage) msg).getData());

            // Never put data message into buffer.
            return null;
        } else {
            return msg;
        }
    }

    /**
     * Creates database synchronization messages that should be sent for the provided {@link Connection}.
     *
     * @param connection Connection to find data messages for.
     * @return The created messages.
     */
    public List<DataMessage> createDataMessagesFor(Connection connection) {
        DTNHost sender = connection.getInitiator();
        DTNHost receiver = connection.getOtherNode(sender);
        LocalDatabase senderDatabase = this.hostsToDatabases.get(sender);

        List<DisasterData> interestingData = senderDatabase.getAllDataWithMinimumUtility(this.utilityThreshold);
        List<DataMessage> messages = new ArrayList<>(interestingData.size());
        for (DisasterData data : interestingData) {
            // Create message out of the data.
            // DataMessages, like 1-to-1s, always have lowest priority (0).
            messages.add(new DataMessage(sender, receiver, "DbSync", data, 0));
        }
        return messages;
    }

    /**
     * Called every simulation cycle.
     *
     * @param host The host this application instance is attached to.
     */
    @Override
    public void update(DTNHost host) {
        if (this.isInitialized()) {
            this.initialize();
        }
    }

    @Override
    public Application replicate() {
        // TODO: When is this used? Is the following the correct solution for it?
        return new DatabaseApplication(this);
    }

    private boolean isInitialized() {
        return this.hostsToDatabases == null;
    }
}
