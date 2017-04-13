package input;

import core.Coord;
import core.DisasterData;
import core.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * A generator to create {@link core.DisasterData} of type map at the beginning of the simulation.
 *
 * Created by Britta Heymann on 07.04.2017.
 */
public class MapDataGenerator extends AbstractDisasterDataGenerator {
    /**
     * Number of map data per host -setting id ({@value}).
     * A range [min, max] of uniformly distributed random integer values.
     * Defines the number of map data items per host.
     */
    public static final String MAPS_PER_HOST = "numMaps";

    /* All data locations will be equal to the hosts' locations. */
    private static final Coord NO_OFFSET = new Coord(0, 0);

    /* Minimum and maximum map data items per host. */
    private int minNumMaps;
    private int maxNumMaps;

    /** All events to return. */
    private List<DisasterDataCreateEvent> events;

    /** Current event index. */
    private int nextEventIdx;

    /**
     * Initializes a new instance of the {@link MapDataGenerator} class.
     * @param s Settings to use.
     */
    public MapDataGenerator(Settings s) {
        super(s);

        /* Read parameters from settings. */
        int[] minAndMaxNumMaps = s.getCsvInts(MAPS_PER_HOST, Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE);
        this.minNumMaps = minAndMaxNumMaps[0];
        this.maxNumMaps = minAndMaxNumMaps[1];

        /* Create events to return one after the other. */
        this.events = this.createAllEvents();
        this.nextEventIdx = 0;
    }

    /**
     * Creates a list of {@link DisasterDataCreateEvent}s to return as events one by one.
     * Each host randomly creates a valid number of map items that have random valid sizes.
     *
     * @return The created list.
     */
    private List<DisasterDataCreateEvent> createAllEvents() {
        List<DisasterDataCreateEvent> eventList = new ArrayList<>();
        for (int creatorAddress = this.minHostId; creatorAddress <= this.maxHostId; creatorAddress++) {
            int numMaps = this.selectNumOfMaps();
            for (int j = 0; j < numMaps; j++) {
                eventList.add(new DisasterDataCreateEvent(
                        creatorAddress, DisasterData.DataType.MAP, this.selectRandomSize(), NO_OFFSET, 0));
            }
        }
        return eventList;
    }

    /**
     * Returns the next event in the queue or ExternalEvent with time of
     * double.MAX_VALUE if there are no events left.
     *
     * @return The next event
     */
    @Override
    public ExternalEvent nextEvent() {
        ExternalEvent event = this.events.get(this.nextEventIdx);
        this.nextEventIdx++;
        return event;
    }

    /**
     * Randomly selects the number of map data items for a host to create.
     *
     * @return The selected number.
     */
    private int selectNumOfMaps() {
        return this.getRandomInt(this.minNumMaps, this.maxNumMaps);
    }

    /**
     * Returns next event's time or Double.MAX_VALUE if there are no
     * events left in the queue.
     *
     * @return Next event's time
     */
    @Override
    public double nextEventsTime() {
        // All events are at the start of the simulation.
        if (this.nextEventIdx < this.events.size()) {
            return 0;
        } else {
            return Double.MAX_VALUE;
        }
    }
}
