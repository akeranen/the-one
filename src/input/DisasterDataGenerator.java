package input;

import core.Coord;
import core.DisasterData;
import core.Settings;

/**
 * Class generating new {@link core.DisasterData} objects.
 *
 * Created by Britta Heymann on 05.04.2017.
 */
public class DisasterDataGenerator extends AbstractDisasterDataGenerator {
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

    /** Possible types of the generated data objects. */
    private static final DisasterData.DataType[] possibleDataTypes = new DisasterData.DataType[] {
            DisasterData.DataType.MARKER,
            DisasterData.DataType.RESOURCE,
            DisasterData.DataType.SKILL
    };

    /** Time of the next event in simulation time (seconds). */
    private double nextEventsTime;

    /* Minimum and maximum location offset from the creating host. */
    private int minOffset;
    private int maxOffset;

    /* Minimum and maximum number of seconds between two generations. */
    private double minTimeDiff;
    private double maxTimeDiff;

    /**
     * Initializes a new instance of the {@link DisasterDataGenerator} class.
     */
    public DisasterDataGenerator(Settings s) {
        super(s);

        /* Read parameters from settings. */
        int[] minAndMaxOffset = s.getCsvInts(DATA_LOCATION_OFFSET, Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE);
        this.minOffset = minAndMaxOffset[0];
        this.maxOffset = minAndMaxOffset[1];

        double[] minAndMaxTimeDiff = s.getCsvDoubles(DATA_TIME_DIFFERENCE, Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE);
        this.minTimeDiff = minAndMaxTimeDiff[0];
        this.maxTimeDiff = minAndMaxTimeDiff[1];

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
                this.selectRandomHost(),
                this.selectRandomType(),
                this.selectRandomSize(),
                this.selectRandomOffset(),
                this.nextEventsTime);

        /* Determine next event time. */
        this.nextEventsTime += this.selectTimeDiff();

        /* Return created event. */
        return event;
    }

    /**
     * Selects a random {@link DisasterData.DataType} from all possible data types.
     *
     * @return The selected {@link DisasterData.DataType}.
     */
    private DisasterData.DataType selectRandomType() {
        return possibleDataTypes[random.nextInt(possibleDataTypes.length)];
    }

    /**
     * Selects a random {@link Coord} created by using valid offset values for both coordinates.
     *
     * @return The selected {@link Coord}.
     */
    private Coord selectRandomOffset() {
        return new Coord(
                this.getRandomInt(this.minOffset, this.maxOffset), this.getRandomInt(this.minOffset, this.maxOffset));
    }

    /**
     * Randomly selects a valid time difference between created data items.
     *
     * @return The selected time difference.
     */
    private double selectTimeDiff() {
        return this.minTimeDiff + this.random.nextDouble() * (this.maxTimeDiff - this.minTimeDiff);
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
