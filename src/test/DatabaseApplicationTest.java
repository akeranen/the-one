package test;

import applications.DatabaseApplication;
import core.SettingsError;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Contains tests for the {@link applications.DatabaseApplication} class.
 *
 * Created by Britta Heymann on 21.04.2017.
 */
public class DatabaseApplicationTest {
    private static final int NUM_TESTS = 100;
    private static final long BIGGEST_DB_SIZE = 3_000_000_000L;
    private static final long SMALLEST_DB_SIZE = 2_000_000_000L;
    /** Used to check that some database sizes are completely in the interval, not on the border. */
    private static final int DISTANCE_FROM_BORDER = 10;

    private TestSettings settings = new TestSettings();
    private TestUtils utils;

    @Before
    public void setUp() {
        this.settings.putSetting(DatabaseApplication.UTILITY_THRESHOLD, "0.0");
        this.settings.putSetting(DatabaseApplication.SIZE_RANDOMIZER_SEED, "0");
        this.settings.putSetting(
                DatabaseApplication.DATABASE_SIZE_RANGE, String.format("%d,%d", SMALLEST_DB_SIZE, BIGGEST_DB_SIZE));
        this.settings.putSetting(DatabaseApplication.MIN_INTERVAL_MAP_SENDING, "0");

        this.utils = new TestUtils(new ArrayList<>(), new ArrayList<>(), this.settings);
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
            DatabaseApplication app = new DatabaseApplication(this.settings);
            app.update(this.utils.createHost());
            TestCase.assertTrue("Database is too large.", app.getDatabaseSize() <= BIGGEST_DB_SIZE);
            TestCase.assertTrue("Database is too small.", app.getDatabaseSize() >= SMALLEST_DB_SIZE);

            if (app.getDatabaseSize() > SMALLEST_DB_SIZE + DISTANCE_FROM_BORDER
                    && app.getDatabaseSize() < BIGGEST_DB_SIZE - DISTANCE_FROM_BORDER) {
                foundNonExtremeDatabase = true;
            }
        }

        TestCase.assertTrue("No database had a size between range borders.", foundNonExtremeDatabase);
    }

    public void testGetAppId() {

    }

    public void testCopyConstructorRegistersToDisasterDataNotifier() {

    }

    public void testCopyConstructorCopiesAllProperties() {

    }

    public void testReplicateCopiesAllProperties() {

    }

    public void testHostIsInitializedAfterFirstUpdate() {

    }

    public void testHandleMessageStoresNewDisasterData() {

    }

    public void testHandleMessageIgnoresDataMessagesToOtherRecipients() {

    }

    public void testHandleMessageDropsDataMessage() {

    }

    public void testHandleMessageForwardsOtherMessageTypes() {

    }

    public void testCreateDataMessagesCreatesCorrectMessageForEachInterestingDataItem() {

    }

    public void testCreateDataMessagesOnlySendsOutInterestingData() {

    }

    public void testCreateDataMessagesSendsMapOutAfterMinInterval() {

    }

    public void testCreateDataMessagesDoesNotSendMapShortlyBeforeIntervalEnd() {

    }

    public void testCreateDataMessagesMaySendOutEachMap() {

    }

    public void testSameSeedAndSameHostLeadsToSameDatabaseSize() {

    }

    public void testDisasterDataCreatedAddsOwnDataToDatabase() {

    }

    public void testDisasterDataCreatedDoesNotAddForeignDataToDatabase() {

    }

    public void testDataCreatedBeforeInitializationIsAddedToDatabaseOnInitialization() {

    }
}
