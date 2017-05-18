package test;

import core.SettingsError;
import core.SimClock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import routing.util.EncounterValueManager;

/**
 * Contains tests for the {@link routing.util.EncounterValueManager} class.
 *
 * Created by Britta Heymann on 18.05.2017.
 */
public class EncounterValueManagerTest {
    /** The window length used for the tested {@link EncounterValueManager}. */
    private static final double WINDOW_LENGTH = 21.3;
    /** The weight the current window counter gets when updating the encounter value. */
    private static final double NEW_DATA_WEIGHT = 0.3;

    /* Some values needed for tests. */
    private static final double WEIGHT_GREATER_ONE = 1.1;
    private static final double SHORT_TIME_SPAN = 0.1;
    private static final int TWO_ENCOUNTERS = 2;
    private static final double SOME_ENCOUNTER_VALUE = 0.2;
    private static final double EQUAL_SOCIAL_LEVEL = 0.5;

    /** Acceptable delta on assertEquals comparisons. */
    private static final double DOUBLE_COMPARISON_DELTA = 0.00001;

    private static final String EXPECTED_DIFFERENT_VALUE = "Unexpected encounter value.";
    private static final String EXPECTED_DIFFERENT_RATIO = "Unexpected encounter value ratio.";

    /** The {@link EncounterValueManager} used in tests. */
    private EncounterValueManager evManager =
            EncounterValueManagerTest.createEncounterValueManager(NEW_DATA_WEIGHT, WINDOW_LENGTH);

    private SimClock clock = SimClock.getInstance();

    @After
    public void reset() {
        this.clock.setTime(0);
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForWindowLengthZero() {
        EncounterValueManagerTest.createEncounterValueManager(NEW_DATA_WEIGHT, 0);
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForNegativeWeight() {
        EncounterValueManagerTest.createEncounterValueManager(-1, WINDOW_LENGTH);
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForWeightGreaterOne() {
        EncounterValueManagerTest.createEncounterValueManager(WEIGHT_GREATER_ONE, WINDOW_LENGTH);
    }

    @Test
    public void testNeverUpdatedEncounterValueIsZero() {
        Assert.assertEquals(EXPECTED_DIFFERENT_VALUE, 0, this.evManager.getEncounterValue(), DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testUpdateHappensAfterTimeWindowCompletes() {
        // Remember original encounter value.
        double originalEncounterValue = this.evManager.getEncounterValue();

        // Make sure it gets updated in the future.
        this.evManager.addEncounter();

        // Update shortly before time window ends.
        this.clock.setTime(WINDOW_LENGTH - SHORT_TIME_SPAN);
        this.evManager.update();
        // The update shouldn't have done anything.
        double encounterValueShortlyBeforeTimeWindow = this.evManager.getEncounterValue();
        Assert.assertEquals(
                "Expected encounter value to not have been updated.",
                originalEncounterValue, encounterValueShortlyBeforeTimeWindow, DOUBLE_COMPARISON_DELTA);

        // Update again at end of time window.
        this.clock.setTime(WINDOW_LENGTH);
        this.evManager.update();
        // Now, the encounter value should have been changed.
        double encounterValueAtTimeWindowEnd = this.evManager.getEncounterValue();
        Assert.assertNotEquals(
                "Encounter value should have been updated.",
                originalEncounterValue, encounterValueAtTimeWindowEnd, DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testEncounterValueIsUpdatedCorrectly() {
        // Add encounter.
        this.evManager.addEncounter();

        // Update encounter value.
        this.clock.setTime(WINDOW_LENGTH);
        this.evManager.update();

        // Check the update executed correctly.
        double expectedEncounterValue = NEW_DATA_WEIGHT * 1 + (1 - NEW_DATA_WEIGHT) * 0;
        Assert.assertEquals(
                EXPECTED_DIFFERENT_VALUE,
                expectedEncounterValue, this.evManager.getEncounterValue(), DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testEncounterValueRatioIsCorrectForBothEncounterValuesZero() {
        Assert.assertEquals(
                EXPECTED_DIFFERENT_RATIO,
                EQUAL_SOCIAL_LEVEL,
                this.evManager.computeEncounterValueRatio(0),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testEncounterValueRatioIsCorrectForOneEncounterValueZero() {
        // Test encounter value ratio if neighbor has encounter value not equal to zero.
        Assert.assertEquals(
                EXPECTED_DIFFERENT_RATIO,
                1, this.evManager.computeEncounterValueRatio(SOME_ENCOUNTER_VALUE), DOUBLE_COMPARISON_DELTA);

        // Change own encounter value.
        this.evManager.addEncounter();
        this.clock.setTime(WINDOW_LENGTH);
        this.evManager.update();

        // Test encounter value ratio if neighbor has encounter value of 0.
        Assert.assertEquals(
                EXPECTED_DIFFERENT_RATIO,
                0, this.evManager.computeEncounterValueRatio(0), DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testEncounterValueRatioIsCorrectForNoEncounterValueZero() {
        // Change own encounter value.
        this.evManager.addEncounter();
        this.clock.setTime(WINDOW_LENGTH);
        this.evManager.update();
        double ownEncounterValue = this.evManager.getEncounterValue();

        // Check ratio is computed correctly.
        double expectedEncounterValueRatio = SOME_ENCOUNTER_VALUE / (ownEncounterValue + SOME_ENCOUNTER_VALUE);
        Assert.assertEquals(
                EXPECTED_DIFFERENT_RATIO,
                expectedEncounterValueRatio,
                this.evManager.computeEncounterValueRatio(SOME_ENCOUNTER_VALUE),
                DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Makes sure not only the first encounter value update after the first time window works, but also later ones
     * (i. e. current window counter is set back).
     */
    @Test
    public void testConsecutiveEncounterValueUpdatesWork() {
        this.evManager.addEncounter();
        this.clock.setTime(WINDOW_LENGTH);
        this.evManager.update();
        double encounterValueAfterFirstUpdate = this.evManager.getEncounterValue();

        this.evManager.addEncounter();
        this.evManager.addEncounter();
        this.clock.advance(WINDOW_LENGTH);
        this.evManager.update();

        double expectedValue =
                NEW_DATA_WEIGHT * TWO_ENCOUNTERS + (1 - NEW_DATA_WEIGHT) * encounterValueAfterFirstUpdate;
        Assert.assertEquals(
                EXPECTED_DIFFERENT_VALUE, expectedValue, this.evManager.getEncounterValue(), DOUBLE_COMPARISON_DELTA);
    }

    private static EncounterValueManager createEncounterValueManager(double newDataWeight, double windowLength) {
        TestSettings settings = new TestSettings();
        settings.putSetting(EncounterValueManager.AGING_FACTOR, Double.toString(newDataWeight));
        settings.putSetting(EncounterValueManager.WINDOW_LENGTH_S, Double.toString(windowLength));
        return new EncounterValueManager(settings);
    }
}
