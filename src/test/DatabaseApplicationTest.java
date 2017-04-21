package test;

import org.junit.Test;

/**
 * Contains tests for the {@link applications.DatabaseApplication} class.
 *
 * Created by Britta Heymann on 21.04.2017.
 */
public class DatabaseApplicationTest {
    public void testConstructorThrowsForNegativeUtilityThreshold() {

    }

    public void testConstructorThrowsForUtilityThresholdAbove1() {

    }

    public void testConstructorThrowsForMissingRangeBorder() {

    }

    public void testConstructorThrowsForMaxDatabaseSizeGreaterMin() {

    }

    public void testDatabaseSize() {

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
