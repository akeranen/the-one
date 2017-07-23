package test;

import applications.DatabaseApplication;
import core.BroadcastMessage;
import core.Connection;
import core.Coord;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.Message;
import input.DisasterDataNotifier;
import junit.framework.TestCase;
import org.junit.Test;
import routing.EpidemicRouter;
import routing.MessageRouter;
import routing.util.DatabaseApplicationUtil;
import util.Tuple;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains tests for the {@link routing.util.DatabaseApplicationUtil} class.
 *
 * Created by Britta Heymann on 21.04.2017.
 */
public class DatabaseApplicationUtilTest {
    /* Some DisasterData used in tests. */
    private static final DisasterData MARKER_DATA =
            new DisasterData(DisasterData.DataType.MARKER, 0, 0.0, new Coord(0,0));
    private static final DisasterData RESOURCE_DATA =
            new DisasterData(DisasterData.DataType.RESOURCE, 0, 0.0, new Coord(0,0));

    /** Expected number of {@link core.DataMessage}s in some tests. */
    private static final int TWO_MESSAGES = 2;

    /** The maximum number of database items per message. */
    private static final int ITEMS_PER_MESSAGE = 2;

    private TestSettings testSettings = new TestSettings();
    private TestUtils utils = new TestUtils(new ArrayList<>(), new ArrayList<>(), this.testSettings);

    @Test
    public void testHasNoMessagesToSendReturnsFalseIfDatabaseApplicationExists() {
        this.addDatabaseApplication();
        DTNHost host = this.utils.createHost();
        TestCase.assertFalse(
                "Database exists, but checking for messages to send was aborted.",
                DatabaseApplicationUtil.hasNoMessagesToSend(host.getRouter()));
    }

    @Test
    public void testHasNoMessagesToSendReturnsFalseIfMessageCollectionHasMessages() {
        DTNHost host = this.utils.createHost();
        host.createNewMessage(new BroadcastMessage(host, "M1", 0));
        TestCase.assertFalse(
                "Messages exist, but checking for messages to send was aborted.",
                DatabaseApplicationUtil.hasNoMessagesToSend(host.getRouter()));
    }

    @Test
    public void testHasNoMessagesToSendReturnsTrueForEmptyMessageCollectionAndNoDatabaseApplication() {
        DTNHost host = this.utils.createHost();
        TestCase.assertTrue(
                "Neither database nor messages exist, but continued checking for messages to send anyway.",
                DatabaseApplicationUtil.hasNoMessagesToSend(host.getRouter()));
    }

    @Test
    public void testWrapUsefulDataIntoMessagesReturnsEmptyListForNoDatabaseApplication() {
        DTNHost host = this.utils.createHost();
        DisasterDataNotifier.dataCreated(host, MARKER_DATA);

        DTNHost connected = this.utils.createHost();
        host.connect(connected);

        List<Tuple<Message, Connection>> dataMessages =
                DatabaseApplicationUtil.wrapUsefulDataIntoMessages(host.getRouter(), host, host.getConnections());
        TestCase.assertEquals("No data messages should have been returned.", 0, dataMessages.size());
    }

    @Test
    public void testWrapUsefulDataIntoMessagesReturnsEmptyListIfNoConnectionsAvailable() {
        this.addDatabaseApplication();
        DTNHost host = this.utils.createHost();
        DisasterDataNotifier.dataCreated(host, MARKER_DATA);

        List<Tuple<Message, Connection>> dataMessages =
                DatabaseApplicationUtil.wrapUsefulDataIntoMessages(host.getRouter(), host, host.getConnections());
        TestCase.assertEquals("No data messages should have been returned.", 0, dataMessages.size());
    }

    @Test
    public void testWrapUsefulDataIntoMessagesReturnsMessagesWithCorrectAppId() {
        this.addDatabaseApplication();
        DTNHost host = this.utils.createHost();
        DisasterDataNotifier.dataCreated(host, MARKER_DATA);

        DTNHost connected = this.utils.createHost();
        host.connect(connected);

        List<Tuple<Message, Connection>> dataMessages =
                DatabaseApplicationUtil.wrapUsefulDataIntoMessages(host.getRouter(), host, host.getConnections());
        TestCase.assertEquals(
                "App ID not set correctly.", DatabaseApplication.APP_ID, dataMessages.get(0).getKey().getAppID());

    }

    @Test
    public void testWrapUsefulDataIntoMessagesReturnsEachMessageForEachNeighbor() {
        this.addDatabaseApplication();
        DTNHost host = this.utils.createHost();
        DisasterDataNotifier.dataCreated(host, MARKER_DATA);

        DTNHost neighbor1 = this.utils.createHost();
        DTNHost neighbor2 = this.utils.createHost();
        host.connect(neighbor1);
        host.connect(neighbor2);

        List<Tuple<Message, Connection>> dataMessages =
                DatabaseApplicationUtil.wrapUsefulDataIntoMessages(host.getRouter(), host, host.getConnections());
        TestCase.assertEquals("Expected different number of data messages.", TWO_MESSAGES, dataMessages.size());

        DataMessage message1 = (DataMessage)dataMessages.get(0).getKey();
        DataMessage message2 = (DataMessage)dataMessages.get(1).getKey();
        TestCase.assertEquals("Expected different receiver.", message1.getTo(), neighbor1);
        TestCase.assertEquals("Expected different receiver.", message2.getTo(), neighbor2);
        TestCase.assertEquals("Data should be the same for both messages.", message1.getData(), message2.getData());
        TestCase.assertEquals(
                "Utility should be the same for both messages.", message1.getUtility(), message2.getUtility());
        TestCase.assertEquals(
                "Creation should be the same for both messages.",
                message1.getCreationTime(),
                message2.getCreationTime());
        TestCase.assertEquals("Size should be the same for both messages", message1.getSize(), message2.getSize());
        TestCase.assertEquals("Sender should be the same for both messages", message1.getFrom(), message2.getFrom());
    }

    @Test
    public void testWrapUsefulDataIntoMessagesCreatesMessagesForAllInterestingDataItems() {
        this.addDatabaseApplication();
        DTNHost host = this.utils.createHost();
        DisasterDataNotifier.dataCreated(host, MARKER_DATA);
        DisasterDataNotifier.dataCreated(host, RESOURCE_DATA);

        DTNHost connected = this.utils.createHost();
        host.connect(connected);

        List<Tuple<Message, Connection>> dataMessages =
                DatabaseApplicationUtil.wrapUsefulDataIntoMessages(host.getRouter(), host, host.getConnections());
        TestCase.assertEquals("Expected different number of data messages.", 1, dataMessages.size());
        DataMessage message = (DataMessage)dataMessages.get(0).getKey();
        TestCase.assertEquals("Expected different number of data items.", ITEMS_PER_MESSAGE, message.getData().size());
        TestCase.assertTrue("Marker should have been returned.", message.getData().contains(MARKER_DATA));
        TestCase.assertTrue("Resource should have been returned.", message.getData().contains(RESOURCE_DATA));
    }

    private void addDatabaseApplication() {
        DatabaseApplicationUtilTest.addDummyValuesForDatabaseApplication(this.testSettings);
        this.testSettings.putSetting(DatabaseApplication.ITEMS_PER_MESSAGE, Integer.toString(ITEMS_PER_MESSAGE));

        MessageRouter router = new EpidemicRouter(this.testSettings);
        router.addApplication(new DatabaseApplication(this.testSettings));
        this.utils.setMessageRouterProto(router);
    }

    private static void addDummyValuesForDatabaseApplication(TestSettings s) {
        s.putSetting(DatabaseApplication.UTILITY_THRESHOLD, "0.0");
        s.putSetting(DatabaseApplication.SIZE_RANDOMIZER_SEED, "0");
        s.putSetting(DatabaseApplication.DATABASE_SIZE_RANGE, "0,0");
        s.putSetting(DatabaseApplication.MIN_INTERVAL_MAP_SENDING, "0");
    }
}
