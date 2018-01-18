package input;

import core.Settings;

import java.util.Random;

/**
 * Abstract class for generators creating {@link DisasterDataCreateEvent}s with variable size.
 *
 * Created by Britta Heymann on 07.04.2017.
 */
public abstract class AbstractDisasterDataGenerator implements EventQueue {
    /**
     * Creator address range -setting id ({@value}).
     * A range [min, max] of valid host addresses.
     * Both bounds are inclusive.
     */
    public static final String HOST_RANGE = "hosts";
    /**
     * Data size range -setting id ({@value}).
     * A range [min, max] of uniformly distributed random integer values.
     * Defines the data size (bytes).
     */
    public static final String DATA_SIZE = "size";
    /**
     * Pseudo-random number generator seed -setting id ({@value}).
     * An integer.
     * Defines the seed for the pseudo-random number generator used for this generator.
     */
    public static final String SEED = "seed";

    /* Minimum and maximum host ID */
    protected int minHostId;
    protected int maxHostId;

    /* Minimum and maximum size of the data objects. */
    private int minSize;
    private int maxSize;

    protected Random random;

    protected AbstractDisasterDataGenerator(Settings s) {
        /* Read parameters from settings. */
        int[] minAndMaxHostId = s.getCsvInts(HOST_RANGE, Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE);
        this.minHostId = minAndMaxHostId[0];
        this.maxHostId = minAndMaxHostId[1];

        int[] minAndMaxSize = s.getCsvInts(DATA_SIZE, Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE);
        this.minSize = minAndMaxSize[0];
        this.maxSize = minAndMaxSize[1];

        int seed = s.getInt(SEED);

        /* Initialize pseudo random number generator. */
        this.random = new Random(seed);
    }

    /**
     * Selects a random host ID from the valid range.
     *
     * @return A host ID.
     */
    protected int selectRandomHost() {
        return this.getRandomInt(this.minHostId, this.maxHostId);
    }

    /**
     * Selects a random data size from the valid range.
     *
     * @return Size for a data item.
     */
    protected int selectRandomSize() {
        return this.getRandomInt(this.minSize, this.maxSize);
    }

    /**
     * Randomly gets an integer value between the two bounds (both inclusive).
     *
     * @param min Inclusive lower bound.
     * @param max Inclusive upper bound.
     * @return A random integer between the bounds.
     */
    protected int getRandomInt(int min, int max) {
        return min + this.random.nextInt(max - min + 1);
    }
}
