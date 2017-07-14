package test;

import core.CBRConnection;
import core.Connection;
import core.DTNHost;
import core.Message;
import org.junit.Assert;
import org.junit.Test;
import routing.util.MessageConnectionTuple;
import util.Tuple;

import java.util.ArrayList;

/**
 * Contains tests for the {@link MessageConnectionTuple} class.
 *
 * Created by Britta Heymann on 06.07.2017.
 */
public class MessageConnectionTupleTest {
    private TestUtils testUtils = new TestUtils(new ArrayList<>(), new ArrayList<>(), new TestSettings());
    private Message m1 = new Message(this.testUtils.createHost(), this.testUtils.createHost(), "M1", 0);

    @Test
    public void testEqualsReturnsTrueForSameTuple() {
        Tuple<Message, Connection> t = new Tuple<>(this.m1, this.createConnection());
        Assert.assertEquals("Wrappers around same tuple should be equal.",
                new MessageConnectionTuple(t), new MessageConnectionTuple(t));
    }

    @Test
    public void testEqualsReturnsFalseForNull() {
        Tuple<Message, Connection> t = new Tuple<>(this.m1, this.createConnection());
        Assert.assertNotEquals("Tuple should never be equal to null.", new MessageConnectionTuple(t), null);
    }

    @Test
    public void testEqualsReturnsFalseForWrongType() {
        Tuple<Message, Connection> t = new Tuple<>(this.m1, this.createConnection());
        Assert.assertNotEquals("Tuple should not be equal to object of other type.", new MessageConnectionTuple(t), t);
    }

    @Test
    public void testEqualsReturnsFalseForOtherMessage() {
        Connection c = this.createConnection();
        MessageConnectionTuple t1 = new MessageConnectionTuple(new Tuple<>(this.m1, c));
        MessageConnectionTuple t2 = new MessageConnectionTuple(new Tuple<>(this.m1.replicate(), c));
        Assert.assertNotEquals("Tuple should not be equal if message is different.", t1, t2);
    }

    @Test
    public void testEqualsReturnsFalseForOtherConnection() {
        MessageConnectionTuple t1 = new MessageConnectionTuple(new Tuple<>(this.m1, this.createConnection()));
        MessageConnectionTuple t2 = new MessageConnectionTuple(new Tuple<>(this.m1, this.createConnection()));
        Assert.assertNotEquals("Tuple should not be equal if connection is different.", t1, t2);
    }

    @Test
    public void testHashCodeIsConsistent() {
        MessageConnectionTuple t1 = new MessageConnectionTuple(new Tuple<>(this.m1, this.createConnection()));
        Assert.assertEquals("All invocations of hashCode should return the same value.", t1.hashCode(), t1.hashCode());
    }

    @Test
    public void testHashCodeIsEqualForEqualObjects() {
        Tuple<Message, Connection> t = new Tuple<>(this.m1, this.createConnection());
        MessageConnectionTuple t1 = new MessageConnectionTuple(t);
        MessageConnectionTuple t2 = new MessageConnectionTuple(t);
        Assert.assertEquals("Wrappers around same tuple should be equal.", t1, t2);
        Assert.assertEquals("Hash codes should be equal.", t1.hashCode(), t2.hashCode());
    }

    @Test
    public void testHashCodeIsDifferentForDifferentMessage() {
        Connection c = this.createConnection();
        MessageConnectionTuple t1 = new MessageConnectionTuple(new Tuple<>(this.m1, c));
        MessageConnectionTuple t2 = new MessageConnectionTuple(new Tuple<>(this.m1.replicate(), c));
        Assert.assertNotEquals("Expected different hash codes.", t1.hashCode(), t2.hashCode());
    }

    @Test
    public void testHashCodeIsDifferentForDifferentConnection() {
        MessageConnectionTuple t1 = new MessageConnectionTuple(new Tuple<>(this.m1, this.createConnection()));
        MessageConnectionTuple t2 = new MessageConnectionTuple(new Tuple<>(this.m1, this.createConnection()));
        Assert.assertNotEquals("Expected different hash codes.", t1.hashCode(), t2.hashCode());
    }

    /**
     * Creates a {@link Connection} object.
     * @return The created connection object.
     */
    private Connection createConnection() {
        DTNHost from = this.testUtils.createHost();
        DTNHost to = this.testUtils.createHost();
        return new CBRConnection(from, from.getInterfaces().get(0), to, to.getInterfaces().get(0), 1);
    }
}
