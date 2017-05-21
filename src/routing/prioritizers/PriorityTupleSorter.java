package routing.prioritizers;

import core.Connection;
import core.Message;
import util.Tuple;

import java.util.Comparator;

/**
 * Sorts message-connection tuples by message priority.
 *
 * Created by Britta Heymann on 21.05.2017.
 */
public class PriorityTupleSorter implements Comparator<Tuple<Message, Connection>> {
    private static PrioritySorter messagePrioritySorter = new PrioritySorter();

    /**
     * Compares two message-connection tuples using {@link PrioritySorter} and comparing the messages only.
     *
     * @param  t1 the first tuple to compare
     * @param  t2 the second tuple to compare
     * @return the value returned by {@link PrioritySorter#compare(Message, Message)} executed on the tuples' messages.
     */
    @Override
    public int compare(Tuple<Message, Connection> t1, Tuple<Message, Connection> t2) {
        return messagePrioritySorter.compare(t1.getKey(), t2.getKey());
    }
}
