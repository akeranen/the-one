package test;

import core.Coord;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.Message;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Contains tests for the {@link DataMessage} class.
 *
 * Created by Britta Heymann on 13.04.2017.
 */
public class DataMessageTest {
    /** Properties of data resp. data message used in tests. */
    private static final int DATA_SIZE = 34;
    private static final int PRIORITY = 2;
    private static final double UTILITY = 0.1;

    /** Data used in test message. */
    private DisasterData data = new DisasterData(DisasterData.DataType.MARKER, DATA_SIZE, 0, new Coord(0, 0));
    /** Test message. */
    private DataMessage message;

    private TestUtils utils;

    public DataMessageTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    public void setUp() {
        this.utils = new TestUtils(new ArrayList<>(), new ArrayList<>(), new TestSettings());
        this.message = new DataMessage(utils.createHost(), utils.createHost(), "D1", this.data, UTILITY, PRIORITY);
    }

    @Test
    public void testGetType() {
        TestCase.assertEquals("Type should have been DATA.", Message.MessageType.DATA, this.message.getType());
    }

    @Test
    public void testGetData() {
        TestCase.assertEquals("Expected different data item.", this.data, this.message.getData());
    }

    @Test
    public void testGetUtility() {
        TestCase.assertEquals("Expected different utility.", UTILITY, this.message.getUtility());
    }

    @Test
    public void testGetPriority() {
        TestCase.assertEquals("Expected different priority.", PRIORITY, this.message.getPriority());
    }

    @Test
    public void testSizeEqualsDataSize() {
        TestCase.assertEquals("Expected size to equal data size.", this.data.getSize(), this.message.getSize());
    }

    @Test
    public void testReplicateDoesNotChangeType() {
        Message copy = this.message.replicate();
        TestCase.assertTrue("Copy should still be a data message.", copy instanceof DataMessage);
    }

    @Test
    public void testReplicateCopiesData() {
        Message copy = this.message.replicate();
        TestCase.assertEquals("Copy should point to same data.", this.data, ((DataMessage)copy).getData());
    }

    @Test
    public void testReplicateCopiesUtility() {
        Message copy = this.message.replicate();
        TestCase.assertEquals("Utility value should not have changed.", UTILITY, ((DataMessage)copy).getUtility());
    }

    @Test
    public void testInstantiateForChangesReceiver() {
        DTNHost newReceiver = this.utils.createHost();
        Message instantiation = this.message.instantiateFor(newReceiver);
        TestCase.assertEquals("New message should go to new receiver.", newReceiver, instantiation.getTo());
    }

    @Test
    public void testInstantiateForCopiesData() {
        DataMessage instantiation = this.message.instantiateFor(this.utils.createHost());
        TestCase.assertEquals("Instantiation should point to same data.", this.data, instantiation.getData());
    }

    @Test
    public void testInstantiateForCopiesUtility() {
        DataMessage instantiation = this.message.instantiateFor(this.utils.createHost());
        TestCase.assertEquals("Utility value should not have changed.", UTILITY, instantiation.getUtility());
    }
}
