package test;

import core.SimClock;
import org.junit.After;

/**
 * Base test class of tests testing classes extending {@link routing.util.AbstractIntervalRatingMechanism}.
 *
 * Created by Britta Heymann on 18.05.2017.
 */
public abstract class AbstractIntervalRatingMechanismTest {
    /** The window length used for the tested {@link routing.util.AbstractIntervalRatingMechanism}. */
    protected static final double WINDOW_LENGTH = 21.3;

    protected static final double SHORT_TIME_SPAN = 0.1;

    /** Acceptable delta on assertEquals comparisons. */
    protected static final double DOUBLE_COMPARISON_DELTA = 0.00001;

    protected SimClock clock = SimClock.getInstance();

    @After
    public void reset() {
        this.clock.setTime(0);
    }

    /**
     * Tests that the rating mechanism's constructor throws a {@link core.SettingsError} if a window length of 0 is
     * provided.
     */
    public abstract void testConstructorThrowsForWindowLengthZero();

    /**
     * Tests that the value returned if the rating mechanism has never been updated is correct.
     */
    public abstract void testNeverUpdatedRatingMechanismIsCorrect();

    /**
     * Checks that the rating mechanism is only changed on update after a time window is completed.
     */
    public abstract void testUpdateHappensAfterTimeWindowCompletes();

    public abstract void testRatingMechanismIsUpdatedCorrectly();

    /**
     * Makes sure not only the update after the first time window works, but also later ones.
     */
    public abstract void testConsecutiveUpdatesWork();
}
