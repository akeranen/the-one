package test;

import applications.DatabaseApplication;
import core.Application;
import core.BroadcastMessage;
import core.Coord;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.Group;
import core.Message;
import core.MulticastMessage;
import core.Settings;
import core.SettingsError;
import core.SimClock;
import input.DisasterDataNotifier;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import routing.util.EnergyModel;
import util.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.fail;

/**
 * Contains tests for the {@link applications.DatabaseApplication} class.
 *
 * Created by Britta Heymann on 21.04.2017.
 */
public class DatabaseApplicationTest {
    private static final int NUM_TESTS = 100;

    /* Properties of the application. */
    private static final long BIGGEST_DB_SIZE = 3_000_000_000L;
    private static final long SMALLEST_DB_SIZE = 2_000_000_000L;
    private static final double MIN_UTILITY = 0.5;
    private static final double MAP_SENDING_INTERVAL = 43.2;
    private static final int SEED = 0;
    private static final int ITEMS_PER_MESSAGE = 2;

    /** Small time difference used for tests about map sending. */
    private static final double SMALL_TIME_DIFF = 0.1;
    private static final int TIME_IN_DISTANT_FUTURE = 600_000;

    /** Used to check that some database sizes are completely in the interval, not on the border. */
    private static final int DISTANCE_FROM_BORDER = 10;

    /* Number of data items used in several tests. */
    private static final int TWO_DATA_ITEMS = 2;
    private static final int THREE_DATA_ITEMS = 3;

    /* Number of messages expected in some tests. */
    private static final int TWO_DATA_MESSAGES = 2;

    /* Error messages */
    private static final String UNEXPECTED_NUMBER_DATA_MESSAGES = "Expected different number of data messages.";
    private static final String EXPECTED_INITIALIZED_APPLICATION = "Application should be set up now.";
    private static final String UNEXPECTED_DATA_MESSAGE = "Did not expect any data messages.";

    private TestSettings settings = new TestSettings();
    private TestUtils utils;
    private DatabaseApplication app;
    private DTNHost hostAttachedToApp;

    private SimClock clock = SimClock.getInstance();

    public DatabaseApplicationTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    public void setUp() {
        /* Add settings for database application */
        this.settings.putSetting(DatabaseApplication.UTILITY_THRESHOLD, Double.toString(MIN_UTILITY));
        this.settings.putSetting(DatabaseApplication.SIZE_RANDOMIZER_SEED, Integer.toString(SEED));
        this.settings.putSetting(
                DatabaseApplication.DATABASE_SIZE_RANGE, String.format("%d,%d", SMALLEST_DB_SIZE, BIGGEST_DB_SIZE));
        this.settings.putSetting(DatabaseApplication.MIN_INTERVAL_MAP_SENDING, Double.toString(MAP_SENDING_INTERVAL));
        this.settings.putSetting(DatabaseApplication.ITEMS_PER_MESSAGE, Integer.toString(ITEMS_PER_MESSAGE));

        /* Create test utils. */
        this.utils = new TestUtils(new ArrayList<>(), new ArrayList<>(), this.settings);

        /* Create and initialize database application. */
        DatabaseApplication prototype = new DatabaseApplication(this.settings);
        this.app = new DatabaseApplication(prototype);
        this.hostAttachedToApp = this.utils.createHost();
        this.app.update(this.hostAttachedToApp);
    }

    @After
    public void cleanUp() {
        SimClock.reset();
        Group.clearGroups();
        DTNHost.reset();
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForNegativeUtilityThreshold() {
        this.settings.putSetting(DatabaseApplication.UTILITY_THRESHOLD, "-1");
        new DatabaseApplication(this.settings);
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForUtilityThresholdAbove1() {
        this.settings.putSetting(DatabaseApplication.UTILITY_THRESHOLD, "1.1");
        new DatabaseApplication(this.settings);
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForMissingRangeBorder() {
        this.settings.putSetting(DatabaseApplication.DATABASE_SIZE_RANGE, "2");
        new DatabaseApplication(this.settings);
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForMaxDatabaseSizeGreaterMin() {
        this.settings.putSetting(DatabaseApplication.DATABASE_SIZE_RANGE, "2, 1");
        new DatabaseApplication(this.settings);
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForNonPositiveItemsPerMessage() {
        this.settings.putSetting(DatabaseApplication.ITEMS_PER_MESSAGE, "0");
        new DatabaseApplication(this.settings);
    }

    @Test
    public void testDatabaseSize() {
        // Try to find a database that is truly between the extreme values...
        boolean foundNonExtremeDatabase = false;

        // ...and also check for all databases whether they lie in the range.
        for (int i = 0; i < NUM_TESTS; i++) {
            DatabaseApplication dbApp = new DatabaseApplication(this.settings);
            dbApp.update(this.utils.createHost());
            TestCase.assertTrue("Database is too large.", dbApp.getDatabaseSize() <= BIGGEST_DB_SIZE);
            TestCase.assertTrue("Database is too small.", dbApp.getDatabaseSize() >= SMALLEST_DB_SIZE);

            if (dbApp.getDatabaseSize() > SMALLEST_DB_SIZE + DISTANCE_FROM_BORDER
                    && dbApp.getDatabaseSize() < BIGGEST_DB_SIZE - DISTANCE_FROM_BORDER) {
                foundNonExtremeDatabase = true;
            }
        }

        TestCase.assertTrue("No database had a size between range borders.", foundNonExtremeDatabase);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetDatabaseSizeThrowsIfNotInitialized() {
        DatabaseApplication uninitializedApp = new DatabaseApplication(this.settings);
        uninitializedApp.getDatabaseSize();
    }

    @Test
    public void testGetAppId() {
        TestCase.assertEquals("Unexpected App ID.", DatabaseApplication.APP_ID, this.app.getAppID());
    }

    @Test
    public void testGetUtilityThreshold() {
        TestCase.assertEquals("Expected different utility threshold.", MIN_UTILITY, this.app.getUtilityThreshold());
    }

    @Test
    public void testGetSeed() {
        TestCase.assertEquals("Expected different seed.", SEED, this.app.getSeed());
    }

    @Test
    public void testGetMapSendingInterval() {
        TestCase.assertEquals(
                "Expected different map sending interval.", MAP_SENDING_INTERVAL, this.app.getMapSendingInterval());
    }

    @Test
    public void testGetDatabaseSizeRange() {
        long[] dbSizeRange = this.app.getDatabaseSizeRange();
        TestCase.assertEquals(
                "Expected different number of values for a range.",
                Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE,
                dbSizeRange.length);
        TestCase.assertEquals("Expected different minimum database size.", SMALLEST_DB_SIZE, dbSizeRange[0]);
        TestCase.assertEquals("Expected different maximum database size.", BIGGEST_DB_SIZE, dbSizeRange[1]);
    }

    @Test
    public void testGetItemsPerMessage() {
        TestCase.assertEquals("Expected different number of database items per message.",
                ITEMS_PER_MESSAGE, this.app.getItemsPerMessage());
    }

    @Test
    public void testCopyConstructorRegistersToDisasterDataNotifier() {
        /* Use copy constructor. */
        DatabaseApplication copy = new DatabaseApplication(this.app);

        /* Initialize the copied application. */
        DTNHost host = this.utils.createHost();
        copy.update(host);

        /* Use notifier. */
        DisasterData usefulData =
                new DisasterData(DisasterData.DataType.MARKER, 0, SimClock.getTime(), host.getLocation());
        DisasterDataNotifier.dataCreated(host, usefulData);

        /* Check data was added. */
        List<DataMessage> interestingData = copy.wrapUsefulDataIntoMessages(host);
        TestCase.assertEquals("Expected one data item.", 1, interestingData.size());
        TestCase.assertEquals("Expected one data item.", 1, interestingData.get(0).getData().size());
        TestCase.assertEquals("Expected different data.", usefulData, interestingData.get(0).getData().get(0));
    }

    @Test
    public void testCopyConstructorCopiesAllProperties() {
        DatabaseApplication copy = new DatabaseApplication(this.app);
        DatabaseApplicationTest.checkPropertiesAreEqual(this.app, copy);
    }

    @Test
    public void testReplicate() {
        Application replication = this.app.replicate();
        TestCase.assertTrue("Wrong application type.", replication instanceof DatabaseApplication);
        DatabaseApplicationTest.checkPropertiesAreEqual(this.app, (DatabaseApplication)replication);
    }

    @Test
    public void testApplicationIsInitializedAfterFirstUpdate() {
        DatabaseApplication uninitializedApp = new DatabaseApplication(this.settings);
        DatabaseApplicationTest.checkAppIsNotInitialized(uninitializedApp);
        uninitializedApp.update(this.utils.createHost());
        TestCase.assertNotNull(EXPECTED_INITIALIZED_APPLICATION, uninitializedApp.getDatabaseSize());
    }

    @Test
    public void testApplicationIsInitializedAfterFirstHandleMessage() {
        DatabaseApplication uninitializedApp = new DatabaseApplication(this.settings);
        DatabaseApplicationTest.checkAppIsNotInitialized(uninitializedApp);
        uninitializedApp.handle(new BroadcastMessage(this.utils.createHost(), "M", 0), this.utils.createHost());
        TestCase.assertNotNull(EXPECTED_INITIALIZED_APPLICATION, uninitializedApp.getDatabaseSize());
    }

    @Test
    public void testApplicationIsInitializedAfterFirstWrapUsefulDataIntoMessages() {
        DatabaseApplication uninitializedApp = new DatabaseApplication(this.settings);
        DatabaseApplicationTest.checkAppIsNotInitialized(uninitializedApp);
        uninitializedApp.wrapUsefulDataIntoMessages(this.utils.createHost());
        TestCase.assertNotNull(EXPECTED_INITIALIZED_APPLICATION, uninitializedApp.getDatabaseSize());
    }

    @Test
    public void testHandleMessageStoresNewDisasterData() {
        /* Send data message to app. */
        DisasterData usefulData = DatabaseApplicationTest.createUsefulData(
                DisasterData.DataType.SKILL, this.hostAttachedToApp);
        DisasterData secondUsefulData = DatabaseApplicationTest.createUsefulData(
                DisasterData.DataType.MARKER, this.hostAttachedToApp);
        DataMessage dataMessage = new DataMessage(
                this.utils.createHost(), this.hostAttachedToApp, "data",
                Arrays.asList(new Tuple<>(usefulData, 1D), new Tuple<>(secondUsefulData, 1D)),
                0);
        this.app.handle(dataMessage, this.hostAttachedToApp);

        /* Check data was added. */
        List<DataMessage> interestingData = this.app.wrapUsefulDataIntoMessages(this.hostAttachedToApp);
        TestCase.assertEquals("Expected two data items in one message.", 1, interestingData.size());
        TestCase.assertEquals("Expected two data items.", TWO_DATA_ITEMS, interestingData.get(0).getData().size());
        TestCase.assertTrue("Expected data to include both handled data items.",
                interestingData.get(0).getData().contains(usefulData));
        TestCase.assertTrue("Expected data to include both handled data items.",
                interestingData.get(0).getData().contains(secondUsefulData));
    }

    @Test
    public void testHandleMessageIgnoresDataMessagesToOtherRecipients() {
        /* Send data message through app. */
        DisasterData usefulData =
                DatabaseApplicationTest.createUsefulData(DisasterData.DataType.SKILL, this.hostAttachedToApp);
        DataMessage dataMessage = new DataMessage(
                this.utils.createHost(), this.utils.createHost(),
                "data", Collections.singleton(new Tuple<>(usefulData, 1D)), 0);
        this.app.handle(dataMessage, this.hostAttachedToApp);

        /* Check no data was added. */
        List<DataMessage> interestingData = this.app.wrapUsefulDataIntoMessages(this.hostAttachedToApp);
        TestCase.assertEquals("Expected no data item.", 0, interestingData.size());
    }

    @Test
    public void testHandleMessageDropsDataMessage() {
        /* Send data message to app. */
        DisasterData usefulData = DatabaseApplicationTest.createUsefulData(
                DisasterData.DataType.SKILL, this.hostAttachedToApp);
        DataMessage dataMessage = new DataMessage(
                this.utils.createHost(), this.hostAttachedToApp,
                "data", Collections.singleton(new Tuple<>(usefulData, 1D)), 0);
        TestCase.assertNull(
                "Data message should have been dropped.", this.app.handle(dataMessage, this.hostAttachedToApp));
    }

    @Test
    public void testHandleMessageForwardsOtherMessageTypes() {
        /* Create sender. */
        DTNHost other = this.utils.createHost();

        /* Create non-data messages to host. */
        Message oneToOne = new Message(other, this.hostAttachedToApp, "M", 0);
        Message broadcast = new BroadcastMessage(this.hostAttachedToApp, "B", 0);
        Group group = Group.createGroup(0);
        group.addHost(other);
        group.addHost(this.hostAttachedToApp);
        Message multicast = new MulticastMessage(other, group, "C", 0);

        /* Make sure they are forwarded. */
        TestCase.assertEquals(
                "1-to-1 message should have been forwarded.",
                oneToOne,
                this.app.handle(oneToOne, this.hostAttachedToApp));
        TestCase.assertEquals(
                "Broadcast message should have been forwarded.",
                broadcast,
                this.app.handle(broadcast, this.hostAttachedToApp));
        TestCase.assertEquals(
                "Multicast message should have been forwarded.",
                multicast,
                this.app.handle(multicast, this.hostAttachedToApp));
    }

    @Test
    public void testWrapUsefulDataIntoMessagesCreatesCorrectMessageForInterestingDataItems() {
        /* Create data. */
        Coord currLocation = this.hostAttachedToApp.getLocation();
        double currTime = SimClock.getTime();
        DisasterData marker = new DisasterData(DisasterData.DataType.MARKER, 0, currTime, currLocation);
        DisasterData resource = new DisasterData(DisasterData.DataType.RESOURCE, 0, currTime, currLocation);
        DisasterData skill = new DisasterData(DisasterData.DataType.SKILL, 0, currTime, currLocation);

        /* Add to database. */
        DisasterDataNotifier.dataCreated(this.hostAttachedToApp, marker);
        DisasterDataNotifier.dataCreated(this.hostAttachedToApp, resource);
        DisasterDataNotifier.dataCreated(this.hostAttachedToApp, skill);

        /* Check all data items are returned as messages. */
        List<DataMessage> messages = this.app.wrapUsefulDataIntoMessages(this.hostAttachedToApp);
        TestCase.assertEquals(UNEXPECTED_NUMBER_DATA_MESSAGES,
                (int)Math.ceil((double)THREE_DATA_ITEMS / ITEMS_PER_MESSAGE), messages.size());
        TestCase.assertTrue(
                "Expected marker to be in a message.",
                messages.stream().anyMatch(msg -> msg.getData().contains(marker)));
        TestCase.assertTrue(
                "Expected resource to be in a message.",
                messages.stream().anyMatch(msg -> msg.getData().contains(resource)));
        TestCase.assertTrue(
                "Expected skill to be in a message.",
                messages.stream().anyMatch(msg -> msg.getData().contains(skill)));
    }

    @Test
    public void testWrapUsefulDataIntoMessagesOnlySendsOutInterestingData() {
        this.clock.setTime(TIME_IN_DISTANT_FUTURE);
        DisasterData usefulData =
                DatabaseApplicationTest.createUsefulData(DisasterData.DataType.SKILL, this.hostAttachedToApp);
        DisasterData uselessData = new DisasterData(DisasterData.DataType.RESOURCE, 0, 0, new Coord(0, 0));

        DisasterDataNotifier.dataCreated(this.hostAttachedToApp, usefulData);
        DisasterDataNotifier.dataCreated(this.hostAttachedToApp, uselessData);

        List<DataMessage> messages = this.app.wrapUsefulDataIntoMessages(this.hostAttachedToApp);
        TestCase.assertEquals(UNEXPECTED_NUMBER_DATA_MESSAGES, 1, messages.size());
        TestCase.assertTrue(
                "Expected useful data to be in a message.",
                messages.stream().anyMatch(msg -> msg.getData().contains(usefulData)));
        TestCase.assertFalse(
                "Did not expect useless data to be in a message..",
                messages.stream().anyMatch(msg -> msg.getData().contains(uselessData)));
    }

    @Test
    public void testWrapUsefulDataIntoMessagesGroupsDataByUtility() {
        // Create app sending out everything.
        this.settings.putSetting(DatabaseApplication.UTILITY_THRESHOLD, "0");
        // Use copy constructor to subscribe as data listener.
        DatabaseApplication floodingApp = new DatabaseApplication(new DatabaseApplication(this.settings));
        floodingApp.update(this.hostAttachedToApp);

        // Add useful and not that useful data items.
        this.clock.setTime(TIME_IN_DISTANT_FUTURE);
        DisasterData[] usefulData = new DisasterData[ITEMS_PER_MESSAGE];
        DisasterData[] uselessData = new DisasterData[ITEMS_PER_MESSAGE];
        for (int i = 0; i < ITEMS_PER_MESSAGE; i++) {
            usefulData[i] =
                    DatabaseApplicationTest.createUsefulData(DisasterData.DataType.SKILL, this.hostAttachedToApp);
            uselessData[i] = new DisasterData(DisasterData.DataType.RESOURCE, 0, 0, new Coord(0, 0));
            DisasterDataNotifier.dataCreated(this.hostAttachedToApp, usefulData[i]);
            DisasterDataNotifier.dataCreated(this.hostAttachedToApp, uselessData[i]);
        }

        // Create messages.
        List<DataMessage> messages = floodingApp.wrapUsefulDataIntoMessages(this.hostAttachedToApp);
        TestCase.assertEquals(UNEXPECTED_NUMBER_DATA_MESSAGES, TWO_DATA_MESSAGES, messages.size());
        TestCase.assertTrue("Expected all useful data in one message.",
                messages.get(0).getData().containsAll(Arrays.asList(usefulData))
                        && messages.get(0).getData().size() == ITEMS_PER_MESSAGE);
        TestCase.assertTrue("Expected all useless data in one message.",
                messages.get(1).getData().containsAll(Arrays.asList(uselessData))
                        && messages.get(1).getData().size() == ITEMS_PER_MESSAGE);
    }

    @Test
    public void testWrapUsefulDataIntoMessagesSendsMapOutAfterMinInterval() {
        this.clock.setTime(MAP_SENDING_INTERVAL - SMALL_TIME_DIFF);

        /* Insert map data into database. */
        DisasterData mapData =
                DatabaseApplicationTest.createUsefulData(DisasterData.DataType.MAP, this.hostAttachedToApp);
        DisasterDataNotifier.dataCreated(this.hostAttachedToApp, mapData);

        /* Test map data is not returned shortly before interval... */
        List<DataMessage> messages = this.app.wrapUsefulDataIntoMessages(this.hostAttachedToApp);
        TestCase.assertTrue(UNEXPECTED_DATA_MESSAGE, messages.isEmpty());

        /* ... but is returned after it completed. */
        this.clock.setTime(MAP_SENDING_INTERVAL);
        messages = this.app.wrapUsefulDataIntoMessages(this.hostAttachedToApp);
        TestCase.assertEquals(UNEXPECTED_NUMBER_DATA_MESSAGES, 1, messages.size());
        TestCase.assertTrue(
                "Expected map to be in a message.",
                messages.stream().anyMatch(msg -> msg.getData().contains(mapData)));
    }

    @Test
    public void testWrapUsefulDataIntoMessagesMaySendOutEachMap() {
        List<DisasterData> mapData = new ArrayList<>();
        for (int i = 0; i < THREE_DATA_ITEMS; i++) {
            DisasterData usefulMap =
                    DatabaseApplicationTest.createUsefulData(DisasterData.DataType.MAP, this.hostAttachedToApp);
            mapData.add(usefulMap);
            DisasterDataNotifier.dataCreated(this.hostAttachedToApp, usefulMap);
        }

        Set<DisasterData> mapsInMessages = new HashSet<>();
        for (int i = 0; i < NUM_TESTS; i++) {
            this.clock.advance(MAP_SENDING_INTERVAL + SMALL_TIME_DIFF);
            List<DataMessage> messages = this.app.wrapUsefulDataIntoMessages(this.hostAttachedToApp);
            TestCase.assertEquals("Only one map should have been sent out.", 1, messages.size());
            TestCase.assertEquals("Only one map should have been sent out.", 1, messages.get(0).getData().size());
            mapsInMessages.add(messages.get(0).getData().get(0));
        }

        TestCase.assertEquals("Not all maps have been returned.", mapData.size(), mapsInMessages.size());
    }

    @Test
    public void testSameSeedAndSameHostLeadsToSameDatabaseSize() {
        DatabaseApplication app1 = new DatabaseApplication(this.settings);
        DatabaseApplication app2 = new DatabaseApplication(this.settings);
        app1.update(this.hostAttachedToApp);
        app2.update(this.hostAttachedToApp);
        TestCase.assertEquals(
                "Expected same database size for same random seeds.",
                app1.getDatabaseSize(),
                app2.getDatabaseSize());
    }

    @Test
    public void testDisasterDataCreatedAddsOwnDataToDatabase() {
        DisasterData data =
                DatabaseApplicationTest.createUsefulData(DisasterData.DataType.SKILL, this.hostAttachedToApp);
        DisasterDataNotifier.dataCreated(this.hostAttachedToApp, data);
        List<DataMessage> messages = this.app.wrapUsefulDataIntoMessages(this.hostAttachedToApp);
        TestCase.assertEquals("Data was not added to database.", 1, messages.size());
        TestCase.assertTrue(
                "Expected created data to be in database.",
                messages.stream().anyMatch(msg -> msg.getData().get(0).equals(data)));
    }

    @Test
    public void testDisasterDataCreatedDoesNotAddForeignDataToDatabase() {
        DisasterData data =
                DatabaseApplicationTest.createUsefulData(DisasterData.DataType.SKILL, this.hostAttachedToApp);
        DisasterDataNotifier.dataCreated(this.utils.createHost(), data);
        List<DataMessage> messages = this.app.wrapUsefulDataIntoMessages(this.hostAttachedToApp);
        TestCase.assertTrue(UNEXPECTED_DATA_MESSAGE, messages.isEmpty());
    }

    @Test
    public void testDiasterDataCreatedDoesNotAddOwnDataToDatabaseOnEmptyBattery() {
        this.hostAttachedToApp.getComBus().updateProperty(EnergyModel.ENERGY_VALUE_ID, 0D);
        DisasterData data =
                DatabaseApplicationTest.createUsefulData(DisasterData.DataType.SKILL, this.hostAttachedToApp);
        DisasterDataNotifier.dataCreated(this.utils.createHost(), data);
        List<DataMessage> messages = this.app.wrapUsefulDataIntoMessages(this.hostAttachedToApp);
        TestCase.assertTrue(UNEXPECTED_DATA_MESSAGE, messages.isEmpty());
    }

    @Test
    public void testDataCreatedBeforeInitializationIsAddedToDatabaseOnInitialization() {
        /* Create uninitialized app. Use copy constructor to register as listener. */
        DatabaseApplication uninitializedApp = new DatabaseApplication(new DatabaseApplication(this.settings));
        DatabaseApplicationTest.checkAppIsNotInitialized(uninitializedApp);
        DTNHost host = this.utils.createHost();

        /* Notify about created data. */
        DisasterData data1 = DatabaseApplicationTest.createUsefulData(DisasterData.DataType.SKILL, host);
        DisasterData data2 = DatabaseApplicationTest.createUsefulData(DisasterData.DataType.SKILL, host);
        DisasterDataNotifier.dataCreated(host, data1);
        DisasterDataNotifier.dataCreated(host, data2);

        /* Call create database messages and thereby initialize the application. */
        List<DataMessage> messages = uninitializedApp.wrapUsefulDataIntoMessages(host);

        /* Check data was added. */
        TestCase.assertEquals(UNEXPECTED_NUMBER_DATA_MESSAGES, 1, messages.size());
        TestCase.assertTrue(
                "Expected data to be in a message.",
                messages.get(0).getData().contains(data1));
        TestCase.assertTrue(
                "Expected data to be in a message.",
                messages.get(0).getData().contains(data2));
    }

    /**
     * Checks that the given app has not been initialized, i.e. is not connected to a {@link core.LocalDatabase} yet.
     *
     * @param uninitializedApp The app to check.
     */
    private static void checkAppIsNotInitialized(DatabaseApplication uninitializedApp) {
        try {
            uninitializedApp.getDatabaseSize();
            fail();
        } catch (IllegalStateException e) {
            TestCase.assertEquals(
                    "Expected different exception.",
                    "Cannot get database size before application was initialized!",
                    e.getMessage());
        }
    }

    /**
     * Checks that all properties that have originally be read from settings are equal for the given
     * {@link DatabaseApplication}s.
     *
     * @param original The original application (expected values).
     * @param copy The copy (checked values).
     */
    private static void checkPropertiesAreEqual(DatabaseApplication original, DatabaseApplication copy) {
        TestCase.assertEquals(
                "Expected different utility threshold.", original.getUtilityThreshold(), copy.getUtilityThreshold());
        TestCase.assertEquals(
                "Expected different map sending interval.",
                original.getMapSendingInterval(),
                copy.getMapSendingInterval());
        TestCase.assertEquals("Expected different seed.", original.getSeed(), copy.getSeed());
        TestCase.assertTrue(
                "Expected different database size range.",
                Arrays.equals(original.getDatabaseSizeRange(), copy.getDatabaseSizeRange()));
        TestCase.assertEquals("Expected different number of database items per message.",
                original.getItemsPerMessage(), copy.getItemsPerMessage());
    }

    /**
     * Creates a {@link DisasterData} object which is very useful wrt to current time and host position.
     *
     * @param type The type of {@link DisasterData} to create.
     * @param host The host which decides the data's position.
     * @return The created data.
     */
    private static DisasterData createUsefulData(DisasterData.DataType type, DTNHost host) {
        return new DisasterData(type, 0, SimClock.getTime(), host.getLocation());
    }
}
