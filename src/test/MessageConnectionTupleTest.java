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
 * Created by Britta Heymann on 06.07.2017.
 */
public class MessageConnectionTupleTest {
    private TestUtils testUtils = new TestUtils(new ArrayList<>(), new ArrayList<>(), new TestSettings());

    @Test
    public void testEqualsWorksForSameTuple() {
        Tuple<Message, Connection> t = new Tuple<>(
                new Message(this.testUtils.createHost(), this.testUtils.createHost(), "M1", 0),
                this.createConnection());
        MessageConnectionTuple t1 = new MessageConnectionTuple(t);
        MessageConnectionTuple t2 = new MessageConnectionTuple(t);
        Assert.assertEquals(t1, t2);
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
