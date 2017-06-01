package routing.prioritizers;

import core.Message;

import java.util.Comparator;

/**
 * A message comparator based on priority.
 *
 * Created by Britta Heymann on 21.05.2017.
 */
public class PrioritySorter implements Comparator<Message> {
    /**
     * Compares two {@link Message} objects by their priorities.
     *
     * @param  m1 the first {@link Message} to compare
     * @param  m2 the second {@link Message} to compare
     * @return the value {@code 0} if {@code m1.getPriority() == m2.getPriority()};
     *         a value less than {@code 0} if {@code m1.getPriority() > m2.getPriority()}; and
     *         a value greater than {@code 0} if {@code m1.getPriority() < m2.getPriority()}
     */
    @Override
    public int compare(Message m1, Message m2) {
        return (-1) * Integer.compare(m1.getPriority(), m2.getPriority());
    }
}
