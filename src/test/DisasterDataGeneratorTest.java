package test;

import core.Coord;
import core.DTNHost;
import core.DisasterData;
import input.AbstractDisasterDataGenerator;
import input.DisasterDataGenerator;
import junit.framework.TestCase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains tests for the {@link input.DisasterDataGenerator} class.
 *
 * Created by Britta Heymann on 06.04.2017.
 */
public class DisasterDataGeneratorTest extends AbstractDisasterDataGeneratorTest {
    /* Properties for the generator used in tests. */
    private static final int MIN_OFFSET = 7;
    private static final int MAX_OFFSET = 11;
    private static final double MIN_TIME_DIFFERENCE = 0.5;
    private static final double MAX_TIME_DIFFERENCE = 34;

    /** Number of hosts in the world. */
    private static final int NUMBER_HOSTS = 3;

    /** Creator location used for tests. */
    private static final Coord CREATOR_LOCATION = new Coord(34, 502);

    /** Number of tries in tests using (pseudo) randomness. */
    private static final int NUM_TRIES = 100;

    @Override
    protected AbstractDisasterDataGenerator createGenerator() {
        /* Adds DisasterDataGenerator specific properties to settings object:
        location offset range and time difference range.*/
        this.settings.putSetting(
                DisasterDataGenerator.DATA_LOCATION_OFFSET,
                String.format("%d,%d", MIN_OFFSET, MAX_OFFSET));
        this.settings.putSetting(
                DisasterDataGenerator.DATA_TIME_DIFFERENCE,
                String.format("%f,%f", MIN_TIME_DIFFERENCE, MAX_TIME_DIFFERENCE));

        /* Create generator with those settings. */
        return new DisasterDataGenerator(this.settings);
    }

    @Override
    protected List<DTNHost> createHosts() {
        TestUtils utils = new TestUtils(null, null, this.settings);
        List<DTNHost> hosts = new ArrayList<>();
        for (int i = 0; i < NUMBER_HOSTS; i++) {
            hosts.add(utils.createHost(CREATOR_LOCATION));
        }

        return hosts;
    }

    @Test
    public void testAllHostsMayBeChosen() {
        Set<DTNHost> selectedHosts = new HashSet<>();

        for (int i = 0; i < NUM_TRIES; i++) {
            this.createNextData();
            selectedHosts.add(this.recorder.getLastCreator());
        }

        TestCase.assertEquals("Not all hosts have been selected.", NUMBER_HOSTS, selectedHosts.size());
    }

    @Test
    public void testLocationOffsetIsCorrectForXCoordinate() {
        for (int i = 0; i < NUM_TRIES; i++) {
            double xOffset = this.createNextData().getLocation().getX() - CREATOR_LOCATION.getX();

            TestCase.assertTrue("Data created too close to creator.", xOffset >= MIN_OFFSET);
            TestCase.assertTrue("Data created too far from creator.", xOffset <= MAX_OFFSET);
        }
    }

    @Test
    public void testLocationOffsetIsCorrectForYCoordinate() {
        for (int i = 0; i < NUM_TRIES; i++) {
            double yOffset = this.createNextData().getLocation().getY() - CREATOR_LOCATION.getY();

            TestCase.assertTrue("Data created too close to creator.", yOffset >= MIN_OFFSET);
            TestCase.assertTrue("Data created too far from creator.", yOffset <= MAX_OFFSET);
        }
    }

    @Test
    public void testTimeDifferenceIsCorrect() {
        double lastTime = 0;
        for (int i = 0; i < NUM_TRIES; i++) {
            double eventTime = this.generator.nextEventsTime();

            TestCase.assertTrue("Data created too early.", eventTime >= lastTime + MIN_TIME_DIFFERENCE);
            TestCase.assertTrue("Data created too late.", eventTime < lastTime + MAX_TIME_DIFFERENCE);

            lastTime = eventTime;
            this.generator.nextEvent();
        }
    }

    @Test
    @Override
    public void testSeedGetsUsed() {
        DisasterDataGenerator secondGenerator = new DisasterDataGenerator(this.settings);
        TestCase.assertEquals(
                "Generators with same seed should have the same first event time.",
                this.generator.nextEventsTime(),
                secondGenerator.nextEventsTime());
    }

    /**
     * Returns the number of tries in tests using (pseudo) randomness.
     */
    @Override
    protected int getNumberTries() {
        return NUM_TRIES;
    }
}
