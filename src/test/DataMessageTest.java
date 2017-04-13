package test;

import core.Coord;
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

    /** Data used in test message. */
    private DisasterData data = new DisasterData(DisasterData.DataType.MARKER, DATA_SIZE, 0, new Coord(0, 0));
    /** Test message. */
    private DataMessage message;

    public DataMessageTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    public void setUp() {
        TestUtils utils = new TestUtils(new ArrayList<>(), new ArrayList<>(), new TestSettings());
        this.message = new DataMessage(utils.createHost(), utils.createHost(), "D1", this.data, PRIORITY);
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
}
