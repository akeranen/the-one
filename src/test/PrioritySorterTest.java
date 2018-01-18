package test;

import core.BroadcastMessage;
import core.DTNHost;
import core.Message;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import routing.prioritizers.PrioritySorter;

import java.util.ArrayList;

/**
 * Contains tests for the {@link PrioritySorter} class.
 *
 * Created by Britta Heymann on 21.05.2017.
 */
public class PrioritySorterTest {
    /* Priorities used in tests. */
    private static final int PRIORITY = 6;
    private static final int HIGH_PRIORITY = 7;

    private static final String EXPECTED_DIFFERENT_VALUE = "Expected different comparison value.";

    /* Sender of compared messages. */
    private DTNHost sender;

    private PrioritySorter sorter = new PrioritySorter();

    public PrioritySorterTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    public void setUp() {
        TestUtils testUtils = new TestUtils(new ArrayList<>(), new ArrayList<>(), new TestSettings());
        this.sender = testUtils.createHost();
    }

    @Test
    public void testCompareReturnsZeroForEqualPriorities() {
        Message m1 = this.createMessageWithPriority(PRIORITY);
        Message m2 = this.createMessageWithPriority(PRIORITY);
        Assert.assertEquals(EXPECTED_DIFFERENT_VALUE, 0, this.sorter.compare(m1, m2));
    }

    @Test
    public void testCompareReturnsNegativeValueForHigherPriority() {
        Message importantMessage = this.createMessageWithPriority(HIGH_PRIORITY);
        Message ordinaryMessage = this.createMessageWithPriority(PRIORITY);
        Assert.assertTrue(EXPECTED_DIFFERENT_VALUE, this.sorter.compare(importantMessage, ordinaryMessage) < 0);
    }

    @Test
    public void testCompareReturnsPositiveValueForLowerPriority() {
        Message importantMessage = this.createMessageWithPriority(HIGH_PRIORITY);
        Message ordinaryMessage = this.createMessageWithPriority(PRIORITY);
        Assert.assertTrue(EXPECTED_DIFFERENT_VALUE, this.sorter.compare(ordinaryMessage, importantMessage) > 0);
    }

    /**
     * Creates a message with the provided priority.
     * @param priority Priority to use.
     * @return The created message.
     */
    private Message createMessageWithPriority(int priority) {
        return new BroadcastMessage(this.sender, "M1", 0, priority);
    }
}
