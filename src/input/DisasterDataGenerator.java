package input;

import core.Coord;
import core.DisasterData;
import core.Settings;

import java.util.Random;

/**
 * Class generating new {@link core.DisasterData} objects.
 *
 * Created by Britta Heymann on 05.04.2017.
 */
public class DisasterDataGenerator implements EventQueue {
    /**
     * Data size range -setting id ({@value}).
     * A range [min, max] of uniformly distributed random integer values.
     * Defines the data size (bytes).
     */
    public static final String DATA_SIZE = "size";
    /**
     * Data location offset -setting id ({@value}).
     * A range [min, max] of uniformly distributed random integer values.
     * Defines the distance between the created data item and the creator (meters).
     */
    public static final String DATA_LOCATION_OFFSET = "location_offset";
    /**
     * Data creation interval range -setting id ({@value}).
     * A range [min, max) of uniformly distributed random continuous values.
     * Defines the inter-data creation interval (seconds).
     */
    public static final String DATA_TIME_DIFFERENCE = "interval";
    /**
     * Pseudo-random number generator seed -setting id ({@value}).
     * An integer.
     * Defines the seed for the pseudo-random number generator used for this generator.
     */
    public static final String SEED = "seed";

    /** Possible types of the generated data objects. */
    private static final DisasterData.DataType[] possibleDataTypes = new DisasterData.DataType[] {
            DisasterData.DataType.MARKER,
            DisasterData.DataType.RESOURCE,
            DisasterData.DataType.SKILL
    };

    /** Time of the next event in simulation time (seconds). */
    private double nextEventsTime;

    /* Minimum and maximum size of the data objects. */
    private int minSize;
    private int maxSize;

    /* Minimum and maximum location offset from the creating host. */
    private int minOffset;
    private int maxOffset;

    /* Minimum and maximum number of seconds between two generations. */
    private double minTimeDiff;
    private double maxTimeDiff;

    private Random random;

    /**
     * Initializes a new instance of the {@link DisasterDataGenerator} class.
     */
    public DisasterDataGenerator(Settings s) {
        /* Read parameters from settings. */
        int[] minAndMaxSize = s.getCsvInts(DATA_SIZE, Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE);
        this.minSize = minAndMaxSize[0];
        this.maxSize = minAndMaxSize[1];

        int[] minAndMaxOffset = s.getCsvInts(DATA_LOCATION_OFFSET, Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE);
        this.minOffset = minAndMaxOffset[0];
        this.maxOffset = minAndMaxOffset[1];

        double[] minAndMaxTimeDiff = s.getCsvDoubles(DATA_TIME_DIFFERENCE, Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE);
        this.minTimeDiff = minAndMaxTimeDiff[0];
        this.maxTimeDiff = minAndMaxTimeDiff[1];

        int seed = s.getInt(SEED);

        /* Initialize pseudo random number generator. */
        this.random = new Random(seed);

        /* Don't start first creation directly, but after random time diff. */
        this.nextEventsTime = this.selectTimeDiff();
    }

    /**
     * Returns the next event in the queue or ExternalEvent with time of
     * double.MAX_VALUE if there are no events left.
     *
     * @return The next event
     */
    @Override
    public ExternalEvent nextEvent() {
        /* Create event. */
        ExternalEvent event = new DisasterDataCreateEvent(
                this.selectRandomType(),
                this.selectRandomSize(),
                this.selectRandomOffset(),
                this.nextEventsTime,
                this.random);

        /* Determine next event time. */
        this.nextEventsTime += this.selectTimeDiff();

        /* Return created event. */
        return event;
    }

    /**
     * Selects a random {@link DisasterData.DataType} from all possible data types.
     * @return The selected {@link DisasterData.DataType}.
     */
    private DisasterData.DataType selectRandomType() {
        return possibleDataTypes[random.nextInt(possibleDataTypes.length)];
    }

    /**
     * Selects a random data size from the valid range.
     * @return Size for a data item.
     */
    private int selectRandomSize() {
        return this.getRandomInt(this.minSize, this.maxSize);
    }

    private Coord selectRandomOffset() {
        return new Coord(
                this.getRandomInt(this.minOffset, this.maxOffset), this.getRandomInt(this.minOffset, this.maxOffset));
    }

    private double selectTimeDiff() {
        return this.minTimeDiff + this.random.nextDouble() * (this.maxTimeDiff - this.minTimeDiff);
    }

    private int getRandomInt(int min, int max) {
        return min + this.random.nextInt(max - min + 1);
    }

    /**
     * Returns next event's time or Double.MAX_VALUE if there are no
     * events left in the queue.
     *
     * @return Next event's time
     */
    @Override
    public double nextEventsTime() {
        return this.nextEventsTime;
    }
}
