package test;

import core.Coord;
import core.DTNHost;
import core.DisasterData;
import core.UpdateListener;
import core.World;
import input.DisasterDataCreateEvent;
import input.DisasterDataGenerator;
import input.DisasterDataNotifier;
import input.EventQueue;
import input.ExternalEvent;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains tests for the {@link input.DisasterDataGenerator} class.
 *
 * Created by Britta Heymann on 06.04.2017.
 */
public class DisasterDataGeneratorTest {
    /* Properties used in tests. */
    private static final int MIN_SIZE = 3;
    private static final int MAX_SIZE = 10;
    private static final int MIN_OFFSET = 7;
    private static final int MAX_OFFSET = 11;
    private static final double MIN_TIME_DIFFERENCE = 0.5;
    private static final double MAX_TIME_DIFFERENCE = 34;
    private static final int SEED = 42;

    /** Number of tries in tests using (pseudo) randomness. */
    private static final int NUM_TRIES = 100;

    private TestSettings settings;
    private DisasterDataGenerator generator;

    /** Creator location used for tests. */
    private static final Coord CREATOR_LOCATION = new Coord(34, 502);

    /** World used in tests. */
    private World world;
    private static final int WORLD_WIDTH = 100;
    private static final int WORLD_HEIGHT = 50;

    /** Object recording created disaster data. */
    private RecordingDisasterDataCreationListener recorder = new RecordingDisasterDataCreationListener();

    public DisasterDataGeneratorTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    public void setUp() {
        /* Create settings. */
        this.settings = new TestSettings();
        this.settings.putSetting(
                DisasterDataGenerator.DATA_SIZE,
                String.format("%d,%d", MIN_SIZE, MAX_SIZE));
        this.settings.putSetting(
                DisasterDataGenerator.DATA_LOCATION_OFFSET,
                String.format("%d,%d", MIN_OFFSET, MAX_OFFSET));
        this.settings.putSetting(
                DisasterDataGenerator.DATA_TIME_DIFFERENCE,
                String.format("%f,%f", MIN_TIME_DIFFERENCE, MAX_TIME_DIFFERENCE));
        this.settings.putSetting(DisasterDataGenerator.SEED, Integer.toString(SEED));

        /* Use them to create the generator. */
        this.generator = new DisasterDataGenerator(this.settings);

        /* Record created data. */
        DisasterDataNotifier.addListener(this.recorder);

        /* Create a single host to always use as creator. */
        TestUtils utils = new TestUtils(null, null, this.settings);
        List<DTNHost> hosts = new ArrayList<>();
        hosts.add(utils.createHost(CREATOR_LOCATION));

        /* Initialize a world object, needed for processing events to look at them. */
        this.world = new World(
                hosts,
                WORLD_WIDTH,
                WORLD_HEIGHT,
                1,
                new ArrayList<UpdateListener>(),
                false,
                new ArrayList<EventQueue>());
    }

    @Test
    public void testNextEventCreatesDisasterDataCreateEvent() {
        ExternalEvent event = this.generator.nextEvent();
        TestCase.assertTrue("Expected different type of event.", event instanceof DisasterDataCreateEvent);
    }

    @Test
    public void testSizeIsCorrect() {
        int minUsedSize = Integer.MAX_VALUE;
        int maxUsedSize = Integer.MIN_VALUE;
        for (int i = 0; i < NUM_TRIES; i++) {
            int dataSize = this.createNextData().getSize();
            minUsedSize = Integer.min(minUsedSize, dataSize);
            maxUsedSize = Integer.max(maxUsedSize, dataSize);

            TestCase.assertTrue("Data was too small.", dataSize >= MIN_SIZE);
            TestCase.assertTrue("Data was too large.", dataSize <= MAX_SIZE);
        }
        TestCase.assertEquals("Minimum data size was not used.", MIN_SIZE, minUsedSize);
        TestCase.assertEquals("Maximum data size was not used.", MAX_SIZE, maxUsedSize);
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
    public void testSeedGetsUsed() {
        DisasterDataGenerator secondGenerator = new DisasterDataGenerator(this.settings);
        TestCase.assertEquals(
                "Generators with same seed should have the same first event time.",
                this.generator.nextEventsTime(),
                secondGenerator.nextEventsTime());
    }

    private DisasterData createNextData() {
        DisasterDataCreateEvent event = (DisasterDataCreateEvent)this.generator.nextEvent();
        event.processEvent(this.world);
        return this.recorder.getLastData();
    }
}
