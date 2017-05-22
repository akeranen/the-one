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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    /** Small time difference used for tests about map sending. */
    private static final double SMALL_TIME_DIFF = 0.1;
    private static final int TIME_IN_DISTANT_FUTURE = 10_000;

    /* The current time and times relevant to it*/
    private static final double CURR_TIME = 1800;
    private static final double FIVE_MINS_AGO=1500;
    private static final double HALF_AN_HOUR_LATER =3600;

    /* Used locations for all DB operations. */
    private static final Coord CURR_LOCATION = new Coord(300, 400);
    private static final Coord CLOSE_TO_CURR_LOCATION = new Coord(400, 400);
    private static final Coord ORIGIN = new Coord(0,0);

    /* Sizes for data items */
    private static final int SMALL_ITEM_SIZE=20;
    private static final int BIG_ITEM_SIZE=55;

    /* Data utility value used in tests */
    private static final double HIGH_UTILITY=0.8;

    /* Number of data items used in several tests. */
    private static final int TWO_DATA_ITEMS = 2;
    private static final int THREE_DATA_ITEMS = 3;

    /* Factors if something is computed for a fraction of the data items */
    private static final double HALF_THE_DATA=0.5;
    private static final double A_THIRD_OF_DATA=0.333;
    private static final double A_FOURTH_OF_DATA = 0.25;

    /** Used to check that some database sizes are completely in the interval, not on the border. */
    private static final int DISTANCE_FROM_BORDER = 10;

    /* Margin of error used for floating point comparisons */
    private static final double DOUBLE_COMPARISON_EXACTNESS = 0.01;

    /* Error messages */
    private static final String UNEXPECTED_NUMBER_DATA_MESSAGES = "Expected different number of data messages.";
    private static final String EXPECTED_INITIALIZED_APPLICATION = "Application should be set up now.";
    private static final String WRONG_USED_MEM_PERCENTAGE="The percentage of used memory was not computed correctly.";
    private static final String WRONG_AVG_DISTANCE="The average data distance was not computed correctly.";
    private static final String WRONG_MAX_DISTANCE ="The maximum data distance was not computed correctly." ;
    private static final String WRONG_AVG_AGE ="The average data age was not computed correctly." ;
    private static final String WRONG_MAX_AGE ="The maximum data age was not computed correctly." ;
    private static final String STATISTICS_SHOULD_NOT_BE_NULL = "The statistics should be empty, but not null.";
    private static final String STATISTICS_SHOULD_BE_EMPTY = "There should be no values in the statistics.";

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
        List<DataMessage> interestingData = copy.createDataMessages(host);
        TestCase.assertEquals("Expected one data item.", 1, interestingData.size());
        TestCase.assertEquals("Expected different data.", usefulData, interestingData.get(0).getData());
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
    public void testApplicationIsInitializedAfterFirstCreateDataMessages() {
        DatabaseApplication uninitializedApp = new DatabaseApplication(this.settings);
        DatabaseApplicationTest.checkAppIsNotInitialized(uninitializedApp);
        uninitializedApp.createDataMessages(this.utils.createHost());
        TestCase.assertNotNull(EXPECTED_INITIALIZED_APPLICATION, uninitializedApp.getDatabaseSize());
    }

    @Test
    public void testHandleMessageStoresNewDisasterData() {
        /* Send data message to app. */
        DisasterData usefulData = DatabaseApplicationTest.createUsefulData(
                DisasterData.DataType.SKILL, this.hostAttachedToApp);
        DataMessage dataMessage = new DataMessage(
                this.utils.createHost(), this.hostAttachedToApp, "data", usefulData, 0, 0);
        this.app.handle(dataMessage, this.hostAttachedToApp);

        /* Check data was added. */
        List<DataMessage> interestingData = this.app.createDataMessages(this.hostAttachedToApp);
        TestCase.assertEquals("Expected one data item.", 1, interestingData.size());
        TestCase.assertEquals("Expected different data.", usefulData, interestingData.get(0).getData());

    }

    @Test
    public void testHandleMessageIgnoresDataMessagesToOtherRecipients() {
        /* Send data message through app. */
        DisasterData usefulData =
                DatabaseApplicationTest.createUsefulData(DisasterData.DataType.SKILL, this.hostAttachedToApp);
        DataMessage dataMessage =
                new DataMessage(this.utils.createHost(), this.utils.createHost(), "data", usefulData, 0, 0);
        this.app.handle(dataMessage, this.hostAttachedToApp);

        /* Check no data was added. */
        List<DataMessage> interestingData = this.app.createDataMessages(this.hostAttachedToApp);
        TestCase.assertEquals("Expected no data item.", 0, interestingData.size());
    }

    @Test
    public void testHandleMessageDropsDataMessage() {
        /* Send data message to app. */
        DisasterData usefulData = DatabaseApplicationTest.createUsefulData(
                DisasterData.DataType.SKILL, this.hostAttachedToApp);
        DataMessage dataMessage =
                new DataMessage(this.utils.createHost(), this.hostAttachedToApp, "data", usefulData, 0, 0);
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
    public void testCreateDataMessagesCreatesCorrectMessageForEachInterestingDataItem() {
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
        List<DataMessage> messages = this.app.createDataMessages(this.hostAttachedToApp);
        TestCase.assertEquals(UNEXPECTED_NUMBER_DATA_MESSAGES, THREE_DATA_ITEMS, messages.size());
        TestCase.assertTrue(
                "Expected marker to be in a message.",
                messages.stream().anyMatch(msg -> msg.getData().equals(marker)));
        TestCase.assertTrue(
                "Expected resource to be in a message.",
                messages.stream().anyMatch(msg -> msg.getData().equals(resource)));
        TestCase.assertTrue(
                "Expected skill to be in a message.",
                messages.stream().anyMatch(msg -> msg.getData().equals(skill)));
    }

    @Test
    public void testCreateDataMessagesOnlySendsOutInterestingData() {
        this.clock.setTime(TIME_IN_DISTANT_FUTURE);
        DisasterData usefulData =
                DatabaseApplicationTest.createUsefulData(DisasterData.DataType.SKILL, this.hostAttachedToApp);
        DisasterData uselessData = new DisasterData(DisasterData.DataType.RESOURCE, 0, 0, new Coord(0, 0));

        DisasterDataNotifier.dataCreated(this.hostAttachedToApp, usefulData);
        DisasterDataNotifier.dataCreated(this.hostAttachedToApp, uselessData);

        List<DataMessage> messages = this.app.createDataMessages(this.hostAttachedToApp);
        TestCase.assertEquals(UNEXPECTED_NUMBER_DATA_MESSAGES, 1, messages.size());
        TestCase.assertTrue(
                "Expected useful data to be in a message.",
                messages.stream().anyMatch(msg -> msg.getData().equals(usefulData)));
        TestCase.assertFalse(
                "Did not expect useless data to be in a message..",
                messages.stream().anyMatch(msg -> msg.getData().equals(uselessData)));
    }

    @Test
    public void testCreateDataMessagesSendsMapOutAfterMinInterval() {
        this.clock.setTime(MAP_SENDING_INTERVAL - SMALL_TIME_DIFF);

        /* Insert map data into database. */
        DisasterData mapData =
                DatabaseApplicationTest.createUsefulData(DisasterData.DataType.MAP, this.hostAttachedToApp);
        DisasterDataNotifier.dataCreated(this.hostAttachedToApp, mapData);

        /* Test map data is not returned shortly before interval... */
        List<DataMessage> messages = this.app.createDataMessages(this.hostAttachedToApp);
        TestCase.assertTrue("Did not expect any data messages.", messages.isEmpty());

        /* ... but is returned after it completed. */
        this.clock.setTime(MAP_SENDING_INTERVAL);
        messages = this.app.createDataMessages(this.hostAttachedToApp);
        TestCase.assertEquals(UNEXPECTED_NUMBER_DATA_MESSAGES, 1, messages.size());
        TestCase.assertTrue(
                "Expected map to be in a message.",
                messages.stream().anyMatch(msg -> msg.getData().equals(mapData)));
    }

    @Test
    public void testCreateDataMessagesMaySendOutEachMap() {
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
            List<DataMessage> messages = this.app.createDataMessages(this.hostAttachedToApp);
            TestCase.assertEquals("Only one map should have been sent out.", 1, messages.size());
            mapsInMessages.add(messages.get(0).getData());
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
        List<DataMessage> messages = this.app.createDataMessages(this.hostAttachedToApp);
        TestCase.assertEquals("Data was not added to database.", 1, messages.size());
        TestCase.assertTrue(
                "Expected created data to be in database.",
                messages.stream().anyMatch(msg -> msg.getData().equals(data)));
    }

    @Test
    public void testDisasterDataCreatedDoesNotAddForeignDataToDatabase() {
        DisasterData data =
                DatabaseApplicationTest.createUsefulData(DisasterData.DataType.SKILL, this.hostAttachedToApp);
        DisasterDataNotifier.dataCreated(this.utils.createHost(), data);
        List<DataMessage> messages = this.app.createDataMessages(this.hostAttachedToApp);
        TestCase.assertTrue("Did not expect any data messages.", messages.isEmpty());
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
        List<DataMessage> messages = uninitializedApp.createDataMessages(host);

        /* Check data was added. */
        TestCase.assertEquals(UNEXPECTED_NUMBER_DATA_MESSAGES, TWO_DATA_ITEMS, messages.size());
        TestCase.assertTrue(
                "Expected data to be in a message.",
                messages.stream().anyMatch(msg -> msg.getData().equals(data1)));
        TestCase.assertTrue(
                "Expected data to be in a message.",
                messages.stream().anyMatch(msg -> msg.getData().equals(data2)));
    }

    /**
     * Tests whether the statistics about {@link DisasterData} are computed correctly.
     * The statistics are about the distance of the data item's location to the host location.
     */
    @Test
    public void testGetDataDistanceStatistics(){
        //Test statistics for empty database
        DoubleSummaryStatistics statistics = app.getDataDistanceStatistics();
        TestCase.assertTrue(STATISTICS_SHOULD_NOT_BE_NULL, statistics != null);
        TestCase.assertTrue(STATISTICS_SHOULD_BE_EMPTY, statistics.getCount()==0);
        //Add a single item that was created at the current time and location
        //It should be very useful. All statistics should just be about this item.
        giveDataToHost(DisasterData.DataType.SKILL, SMALL_ITEM_SIZE, CURR_TIME, CURR_LOCATION);
        TestCase.assertEquals(WRONG_AVG_DISTANCE,
                0, app.getDataDistanceStatistics().getAverage(), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals(WRONG_MAX_DISTANCE,
                0, app.getDataDistanceStatistics().getMax(), DOUBLE_COMPARISON_EXACTNESS);
        //Add a second item that has positive age and distance. All statistics should be about the existing two items
        giveDataToHost(DisasterData.DataType.MAP, SMALL_ITEM_SIZE, 0, ORIGIN);
        TestCase.assertEquals(WRONG_AVG_DISTANCE,
                CURR_LOCATION.distance(ORIGIN)*HALF_THE_DATA,
                app.getDataDistanceStatistics().getAverage(), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals(WRONG_MAX_DISTANCE,
                CURR_LOCATION.distance(ORIGIN),
                app.getDataDistanceStatistics().getMax(), DOUBLE_COMPARISON_EXACTNESS);
        //Add a third data item. As it is big and very useful, it will lead to the removal of the other map data
        //So all statistics refer to bigMapDataItem and skillItem.
        giveDataToHost(DisasterData.DataType.MAP, BIG_ITEM_SIZE, FIVE_MINS_AGO, CLOSE_TO_CURR_LOCATION);
        TestCase.assertEquals(WRONG_AVG_DISTANCE,
                CLOSE_TO_CURR_LOCATION.distance(CURR_LOCATION)*HALF_THE_DATA,
                app.getDataDistanceStatistics().getAverage(), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals(WRONG_MAX_DISTANCE,
                CLOSE_TO_CURR_LOCATION.distance(CURR_LOCATION),
                app.getDataDistanceStatistics().getMax(), DOUBLE_COMPARISON_EXACTNESS);
    }

    /**
     * Tests whether the statistics about {@link DisasterData} are computed correctly.
     * The statistics are about the age of the data items, i.e., the current time minus their creation time.
     */
    @Test
    public void testGetDataAgeStatistics(){
        //Test statistics for empty database
        DoubleSummaryStatistics statistics = app.getDataAgeStatistics();
        TestCase.assertTrue(STATISTICS_SHOULD_NOT_BE_NULL, statistics != null);
        TestCase.assertTrue(STATISTICS_SHOULD_BE_EMPTY, statistics.getCount()==0);
        //Add a single item that was created at the current time and location
        //It should be very useful. All statistics should just be about this item.
        giveDataToHost(DisasterData.DataType.SKILL, SMALL_ITEM_SIZE, CURR_TIME, CURR_LOCATION);
        TestCase.assertEquals(WRONG_AVG_AGE,
                0, app.getDataAgeStatistics().getAverage(), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals(WRONG_MAX_AGE,
                0, app.getDataAgeStatistics().getMax(), DOUBLE_COMPARISON_EXACTNESS);
        //Add a second item that has positive age and distance. All statistics should be about the existing two items
        giveDataToHost(DisasterData.DataType.RESOURCE, SMALL_ITEM_SIZE, 0, ORIGIN);
        TestCase.assertEquals(WRONG_AVG_AGE, CURR_TIME * HALF_THE_DATA,
                app.getDataAgeStatistics().getAverage(), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals(WRONG_MAX_AGE, CURR_TIME,
                app.getDataAgeStatistics().getMax(), DOUBLE_COMPARISON_EXACTNESS);
        //Add a third data item with moderate age
        giveDataToHost(DisasterData.DataType.MARKER, SMALL_ITEM_SIZE,
                FIVE_MINS_AGO, CLOSE_TO_CURR_LOCATION);
        TestCase.assertEquals(WRONG_AVG_AGE, A_THIRD_OF_DATA*CURR_TIME+A_THIRD_OF_DATA*(CURR_TIME-FIVE_MINS_AGO),
                app.getDataAgeStatistics().getAverage(), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals(WRONG_MAX_AGE,
                CURR_TIME, app.getDataAgeStatistics().getMax(), DOUBLE_COMPARISON_EXACTNESS);
    }

    /**
     * Tests whether age statistics do not take map data items into account.
     * As the utility function for map data does not include age, we exclude map data from
     * age statistics. Map data would not be deleted when it gets old, hence it will likely
     * get older and older during the simulation and would throw off our results if we included it.
     */
    @Test
    public void testGetDataAgeStatisticsIgnoresMapData(){
        giveDataToHost(DisasterData.DataType.MAP, SMALL_ITEM_SIZE, FIVE_MINS_AGO, CLOSE_TO_CURR_LOCATION);
        DoubleSummaryStatistics statistics = app.getDataAgeStatistics();
        TestCase.assertTrue("Map data should be ignored by age statistics.", statistics.getCount()==0);
    }

    /**
     * Tests whether the utility statistics about {@link DisasterData} are computed correctly.
     * The statistics are about the utility of the data items, which is computed from the
     * distance of the data items to the host location and the age of the data items.
     */
    @Test
    public void testGetDataUtilityStatistics(){
        //Test statistics for empty database
        DoubleSummaryStatistics statistics = app.getDataUtilityStatistics();
        TestCase.assertTrue(STATISTICS_SHOULD_NOT_BE_NULL, statistics != null);
        TestCase.assertTrue(STATISTICS_SHOULD_BE_EMPTY, statistics.getCount()==0);
        //Add a single item that was created at the current time and location
        //It should be very useful. All statistics should just be about this item.
        giveDataToHost(DisasterData.DataType.SKILL, SMALL_ITEM_SIZE, CURR_TIME, CURR_LOCATION);
        TestCase.assertEquals("The average and max utility should be equal for a single data item",
                app.getDataUtilityStatistics().getAverage(), app.getDataUtilityStatistics().getMax(),
                DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertTrue("Utility for close, recent data items should be high.",
                app.getDataUtilityStatistics().getAverage() > HIGH_UTILITY);
        //Add a second item that has positive age and distance. All statistics should be about the existing two items
        giveDataToHost(DisasterData.DataType.MAP, SMALL_ITEM_SIZE, 0, ORIGIN);
        TestCase.assertTrue("Maximal utility should be high.",
                app.getDataUtilityStatistics().getMax() > HIGH_UTILITY);
        TestCase.assertTrue("The average and max utility should not be equal for two differently useful data items.",
                app.getDataUtilityStatistics().getAverage() < app.getDataUtilityStatistics().getMax());
        //Add a third data item. As it is big and very useful, it will lead to the removal of the other map data
        //So all statistics refer to bigMapDataItem and skillItem.
        giveDataToHost(DisasterData.DataType.MARKER, BIG_ITEM_SIZE, FIVE_MINS_AGO, CLOSE_TO_CURR_LOCATION);
        double maxUtility =app.getDataUtilityStatistics().getMax();
        TestCase.assertTrue("Maximal utility should be high.", maxUtility > HIGH_UTILITY);
        double avgUtility = app.getDataUtilityStatistics().getAverage();
        TestCase.assertTrue("Average utility should be high.", avgUtility > HIGH_UTILITY);
        TestCase.assertTrue("The average and max utility should not be equal for two differently useful data items.",
                avgUtility < maxUtility);
        //If time passes the utilities should be recomputed when accessing the statistics, even if nothing was added
        //to the database.
        SimClock.getInstance().setTime(HALF_AN_HOUR_LATER);
        double maxUtilityLater = app.getDataUtilityStatistics().getMax();
        double avgUtilityLater = app.getDataUtilityStatistics().getAverage();
        TestCase.assertTrue("Average utility should have decreased.", avgUtilityLater < avgUtility);
        TestCase.assertTrue("Maximum utility should have decreased.", maxUtilityLater < maxUtility);
        TestCase.assertTrue("Average and max utility should still be different.", avgUtilityLater < maxUtilityLater);
    }

    /**
     * Tests whether the used memory percentage is computed correctly. The used memory
     * percentage is needed for deletion from the database and for reports.
     */
    @Test
    public void testGetUsedMemoryPercentage(){
        giveDataToHost(DisasterData.DataType.MAP, SMALL_ITEM_SIZE, CURR_TIME, CURR_LOCATION);
        TestCase.assertEquals(WRONG_USED_MEM_PERCENTAGE,
                SMALL_ITEM_SIZE/(double)app.getDatabaseSize(),
                app.getUsedMemoryPercentage(),
                DOUBLE_COMPARISON_EXACTNESS);
        giveDataToHost(DisasterData.DataType.MARKER, SMALL_ITEM_SIZE, 0, ORIGIN);
        TestCase.assertEquals(WRONG_USED_MEM_PERCENTAGE,
                SMALL_ITEM_SIZE+SMALL_ITEM_SIZE/(double)app.getDatabaseSize(),
                app.getUsedMemoryPercentage(),
                DOUBLE_COMPARISON_EXACTNESS);
        giveDataToHost(DisasterData.DataType.MARKER, BIG_ITEM_SIZE, CURR_TIME, CURR_LOCATION);
        //After adding a big useful item, the item with the lower utility should be deleted
        TestCase.assertEquals(WRONG_USED_MEM_PERCENTAGE,
                SMALL_ITEM_SIZE+BIG_ITEM_SIZE/(double)app.getDatabaseSize(),
                app.getUsedMemoryPercentage(),
                DOUBLE_COMPARISON_EXACTNESS);
    }

    /**
     * Tests whether the ratio of items of a certain {@link DisasterData.DataType} type is
     * computed correctly.
     */
    @Test
    public void testGetRatioOfDataItemsPerType(){
        //Add a single item, so we just have skill data
        giveDataToHost(DisasterData.DataType.SKILL, SMALL_ITEM_SIZE, CURR_TIME, CURR_LOCATION);
        Map<DisasterData.DataType, Double> ratioPerType = app.getRatioOfItemsPerDataType();
        TestCase.assertEquals("All data should be skill data.",
                1, ratioPerType.get(DisasterData.DataType.SKILL), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("No markers should be in the database.",
                0, ratioPerType.get(DisasterData.DataType.MARKER), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("No maps should be in the database.",
                0, ratioPerType.get(DisasterData.DataType.MAP), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("No resources should be in the database.",
                0, ratioPerType.get(DisasterData.DataType.RESOURCE), DOUBLE_COMPARISON_EXACTNESS);
        //Add a second data item, which is map data. It is not large enough for anything to
        //get deleted. This item is not that useful
        giveDataToHost(DisasterData.DataType.MAP, SMALL_ITEM_SIZE, 0, ORIGIN);
        ratioPerType = app.getRatioOfItemsPerDataType();
        TestCase.assertEquals("Half the data should be map data.",
                HALF_THE_DATA, ratioPerType.get(DisasterData.DataType.MAP), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("No markers should be in the database",
                0, ratioPerType.get(DisasterData.DataType.MARKER), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("Half the data should be skill data.",
                HALF_THE_DATA, ratioPerType.get(DisasterData.DataType.SKILL), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("No resources should be in the database",
                0, ratioPerType.get(DisasterData.DataType.RESOURCE), DOUBLE_COMPARISON_EXACTNESS);
        //Add a third item, which is also Map data and very useful. The smaller, less useful
        //map data item will be deleted, so we are again left with one skill data and one map data item
        giveDataToHost(DisasterData.DataType.MAP, BIG_ITEM_SIZE, CURR_TIME, CURR_LOCATION);
        ratioPerType = app.getRatioOfItemsPerDataType();
        TestCase.assertEquals("Half the data should be map data.",
                HALF_THE_DATA, ratioPerType.get(DisasterData.DataType.MAP), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("No markers should be in the database.",
                0, ratioPerType.get(DisasterData.DataType.MARKER), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("Half the data should be skill data.",
                HALF_THE_DATA, ratioPerType.get(DisasterData.DataType.SKILL), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("No resources should be in the database.",
                0, ratioPerType.get(DisasterData.DataType.RESOURCE), DOUBLE_COMPARISON_EXACTNESS);
    }

    /**
     * Tests whether statistics about what fraction of all {@link DisasterData} items in a host's database
     * is of a certain {@link DisasterData.DataType} are computed correctly.
     */
    @Test
    public void testRatioOfDataItemsCanIncludeAllDataTypes(){
        for (DisasterData.DataType type : DisasterData.DataType.values()){
            giveDataToHost(type, SMALL_ITEM_SIZE, CURR_TIME, CURR_LOCATION);
        }
        Map<DisasterData.DataType, Double> ratioPerType = app.getRatioOfItemsPerDataType();

        for (DisasterData.DataType type : DisasterData.DataType.values()){
            TestCase.assertEquals("There should be exactly one item of each DataType.", A_FOURTH_OF_DATA,
                    ratioPerType.get(type));
        }

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

    private void giveDataToHost(DisasterData.DataType type, int size, double creation, Coord location){
        DisasterData data = new DisasterData(type, size, creation, location);
        DataMessage msg1 = new DataMessage(null, hostAttachedToApp, "d1", data, 1, 1);
        app.handle(msg1, hostAttachedToApp);
    }
}
