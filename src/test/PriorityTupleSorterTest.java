package test;

import core.BroadcastMessage;
import core.CBRConnection;
import core.Connection;
import core.DTNHost;
import core.Message;
import org.junit.Assert;
import org.junit.Test;
import routing.prioritizers.PrioritySorter;
import routing.prioritizers.PriorityTupleSorter;
import util.Tuple;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains tests for the {@link PriorityTupleSorter} class.
 *
 * Created by Britta Heymann on 21.05.2017.
 */
public class PriorityTupleSorterTest {
    /* Priorities used in tests. */
    private static final int PRIORITY = 6;
    private static final int HIGH_PRIORITY = 7;

    private static final String VALUE_DIFFERS_FROM_MESSAGE_COMPARATOR =
            "Comparison value of tuple comparator differs from that of the message comparator..";

    private TestUtils testUtils = new TestUtils(new ArrayList<>(), new ArrayList<>(), new TestSettings());

    private PrioritySorter underlyingSorter = new PrioritySorter();
    private PriorityTupleSorter sorter = new PriorityTupleSorter();

    /**
     * Checks that {@link PriorityTupleSorter#compare(Tuple, Tuple)} returns the same values as
     * {@link PrioritySorter#compare(Message, Message)} does on the tuples' messages.
     */
    @Test
    public void testActsLikePrioritySorter() {
        // Create messages with different priorities...
        Message ordinaryMessages = this.createMessageWithPriority(PRIORITY);
        Message importantMessage = this.createMessageWithPriority(HIGH_PRIORITY);
        // ...and two different connections.
        Connection c1 = this.createConnection();
        Connection c2 = this.createConnection();

        // Take a look at all possible combinations.
        List<Tuple<Message, Connection>> allTuples = new ArrayList<>();
        allTuples.add(new Tuple<>(ordinaryMessages, c1));
        allTuples.add(new Tuple<>(ordinaryMessages, c2));
        allTuples.add(new Tuple<>(importantMessage, c1));
        allTuples.add(new Tuple<>(importantMessage, c2));

        // The compare result should always match the one from the message comparator.
        for (Tuple<Message, Connection> t1 : allTuples) {
            for (Tuple<Message, Connection> t2 : allTuples) {
                Assert.assertEquals(
                        VALUE_DIFFERS_FROM_MESSAGE_COMPARATOR,
                        this.underlyingSorter.compare(t1.getKey(), t2.getKey()), this.sorter.compare(t1, t2));
            }
        }
    }

    /**
     * Creates a message with the provided priority.
     * @param priority Priority to use.
     * @return The created message.
     */
    private Message createMessageWithPriority(int priority) {
        return new BroadcastMessage(this.testUtils.createHost(), "M1", 0, priority);
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
