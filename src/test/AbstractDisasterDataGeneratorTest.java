package test;

import core.DTNHost;
import core.DisasterData;
import core.World;
import input.AbstractDisasterDataGenerator;
import input.DisasterDataCreateEvent;
import input.DisasterDataGenerator;
import input.DisasterDataNotifier;
import input.ExternalEvent;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * A common test class for all classes implementing {@link AbstractDisasterDataGenerator}.
 *
 * Created by Britta Heymann on 07.04.2017.
 */
public abstract class AbstractDisasterDataGeneratorTest {
    /* Properties for the generator used in tests. */
    private static final int MIN_SIZE = 3;
    private static final int MAX_SIZE = 10;
    private static final int SEED = 42;

    /** Number of tries in tests using (pseudo) randomness. */
    protected static final int NUM_TRIES = 100;

    /** World used in tests. */
    protected World world;
    private static final int WORLD_WIDTH = 100;
    private static final int WORLD_HEIGHT = 50;

    /** Settings used to initialize the generator. */
    protected TestSettings settings = new TestSettings();

    /** The generator used in tests. */
    protected AbstractDisasterDataGenerator generator;

    /** Object recording created disaster data. */
    protected RecordingDisasterDataCreationListener recorder = new RecordingDisasterDataCreationListener();

    @Before
    public void setUp() {
        java.util.Locale.setDefault(java.util.Locale.US);

        /* Record created data. */
        DisasterDataNotifier.addListener(this.recorder);

        /* Initialize a world object, needed for processing events to look at them. */
        DTNHost.reset();
        this.world = new World(
                this.createHosts(),
                WORLD_WIDTH,
                WORLD_HEIGHT,
                1,
                new ArrayList<>(),
                false,
                new ArrayList<>());

        /* Write generator properties into settings. */
        this.settings.putSetting(
                AbstractDisasterDataGenerator.HOST_RANGE,
                String.format("%d,%d", 0, this.world.getHosts().size() - 1));
        this.settings.putSetting(
                AbstractDisasterDataGenerator.DATA_SIZE,
                String.format("%d,%d", MIN_SIZE, MAX_SIZE));
        this.settings.putSetting(AbstractDisasterDataGenerator.SEED, Integer.toString(SEED));

        /* Create a generator. */
        this.generator = this.createGenerator();
    }


    /**
     * Creates the {@link AbstractDisasterDataGenerator} to test.
     *
     * @return The created {@link AbstractDisasterDataGenerator}.
     */
    protected abstract AbstractDisasterDataGenerator createGenerator();

    /**
     * Creates the {@link DTNHost}s existing in the {@link World} used for testing.
     *
     * @return The created {@link DTNHost}s.
     */
    protected abstract List<DTNHost> createHosts();

    @Test
    public void testNextEventCreatesEventOfTypeDisasterDataCreateEvent() {
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
    public abstract void testSeedGetsUsed();

    /**
     * Creates the next {@link DisasterDataCreateEvent} via the {@link DisasterDataGenerator}, processes it, and
     * returns the resulting {@link DisasterData} object.
     * @return A newly generated {@link DisasterData} object.
     */
    protected DisasterData createNextData() {
        DisasterDataCreateEvent event = (DisasterDataCreateEvent)this.generator.nextEvent();
        event.processEvent(this.world);
        return this.recorder.getLastData();
    }
}
