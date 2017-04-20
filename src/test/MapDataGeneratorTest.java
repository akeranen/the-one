package test;

import core.Coord;
import core.DTNHost;
import core.DisasterData;
import input.AbstractDisasterDataGenerator;
import input.MapDataGenerator;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contains tests for the {@link input.MapDataGenerator} class.
 *
 * Created by Britta Heymann on 07.04.2017.
 */
public class MapDataGeneratorTest extends AbstractDisasterDataGeneratorTest {
    private static final double DOUBLE_COMPARING_DELTA = 0.001;

    private static final int MIN_NR_MAPS = 1;
    private static final int MAX_NR_MAPS = 4;

    /** Many host locations to pass all tests */
    private static final Coord[] HOST_LOCATIONS = new Coord[] {
        new Coord(0, 1),
        new Coord(100, 20),
        new Coord(23, 32.1),
        new Coord(0, 0),
        new Coord(12, 35.6),
        new Coord(12, 35.6),
        new Coord(12, 35.6),
        new Coord(12, 35.6),
        new Coord(12, 35.6),
        new Coord(12, 35.6),
        new Coord(12, 35.6),
        new Coord(12, 35.6),
        new Coord(12, 35.6),
        new Coord(12, 35.6)
    };

    /**
     * Creates the {@link MapDataGenerator} to test.
     *
     * @return The created {@link MapDataGenerator}.
     */
    @Override
    protected AbstractDisasterDataGenerator createGenerator() {
        /* Add MapDataGenerator specific property to settings object: Range of number of maps per host. */
        this.settings.putSetting(
                MapDataGenerator.MAPS_PER_HOST,
                String.format("%d,%d", MIN_NR_MAPS, MAX_NR_MAPS));

        /* Create generator with those settings. */
        return new MapDataGenerator(this.settings);
    }

    /**
     * Creates the {@link DTNHost}s existing in the {@link core.World} used for testing.
     *
     * @return The created {@link DTNHost}s.
     */
    @Override
    protected List<DTNHost> createHosts() {
        TestUtils utils = new TestUtils(null, null, this.settings);
        List<DTNHost> hosts = new ArrayList<>();
        for (int i = 0; i < HOST_LOCATIONS.length; i++) {
            hosts.add(utils.createHost(HOST_LOCATIONS[i]));
        }

        return hosts;
    }

    @Test
    public void testDataIsOfMapType() {
        while (this.nextEventExists()) {
            DisasterData data = this.createNextData();
            TestCase.assertEquals("Expected map data.", DisasterData.DataType.MAP, data.getType());
        }
    }

    @Test
    public void testDataLocationIsHostLocation() {
        while (this.nextEventExists()) {
            DisasterData data = this.createNextData();
            Coord creatorLocation = this.recorder.getLastCreator().getLocation();
            TestCase.assertEquals(
                    "Expected data location to be creator location.", creatorLocation, data.getLocation());
        }
    }

    @Test
    public void testDataCreationIsAtSimulationStart() {
        while (this.nextEventExists()) {
            DisasterData data = this.createNextData();
            TestCase.assertEquals(
                    "Expected data creation time to be at simulation start.",
                    0.0,
                    data.getCreation(),
                    DOUBLE_COMPARING_DELTA);
        }
    }

    @Test
    public void testAllEventsAreAtSimulationStart() {
        while (this.nextEventExists()) {
            TestCase.assertEquals(
                    "Expected event to be at simulation start.",
                    0.0,
                    this.generator.nextEventsTime(),
                    DOUBLE_COMPARING_DELTA);
            this.generator.nextEvent();
        }
    }

    @Test
    public void testEachHostGetsMapData() {
        Set<DTNHost> hosts = new HashSet<>();

        while (this.nextEventExists()) {
            this.createNextData();
            hosts.add(this.recorder.getLastCreator());
        }

        TestCase.assertEquals("Not all hosts have received map data.", HOST_LOCATIONS.length, hosts.size());

    }

    @Test
    public void testNumberOfMapsIsCorrect() {
        Map<DTNHost, Integer> numOfMapsByHost = new HashMap<>(HOST_LOCATIONS.length);

        /* Look at all events... */
        while (this.nextEventExists()) {
            this.createNextData();
            DTNHost host = this.recorder.getLastCreator();
            /* ... and count the number of maps per hosts. */
            if (numOfMapsByHost.containsKey(host)) {
                numOfMapsByHost.put(host, numOfMapsByHost.get(host) + 1);
            } else {
                numOfMapsByHost.put(host, 1);
            }
        }

        /* Then check the numbers: */
        int minNumMaps = Integer.MAX_VALUE;
        int maxNumMaps = Integer.MIN_VALUE;
        for (Integer numberOfMaps : numOfMapsByHost.values()) {
            minNumMaps = Integer.min(minNumMaps, numberOfMaps);
            maxNumMaps = Integer.max(maxNumMaps, numberOfMaps);

            /* (1) Check all numbers of maps per hosts have been valid. */
            TestCase.assertTrue("Too few data items.", numberOfMaps >= MIN_NR_MAPS);
            TestCase.assertTrue("Too many data items.", numberOfMaps <= MAX_NR_MAPS);
        }

        /* (2) Check extremes have been used. */
        TestCase.assertEquals("Minimum number of maps was not used.", MIN_NR_MAPS, minNumMaps);
        TestCase.assertEquals("Maximum number of maps was not used.", MAX_NR_MAPS, maxNumMaps);
    }

    @Override
    public void testSeedGetsUsed() {
        int firstEventSize = this.createNextData().getSize();

        MapDataGenerator secondGenerator = new MapDataGenerator(this.settings);
        secondGenerator.nextEvent().processEvent(this.world);
        int secondEventSize = this.recorder.getLastData().getSize();

        TestCase.assertEquals(
                "Generators with same seed should have the same first event size.",
                firstEventSize,
                secondEventSize);
    }

    /**
     * Checks whether there is a next event for the generator.
     *
     * @return Whether there are still events to process.
     */
    private boolean nextEventExists() {
        return this.generator.nextEventsTime() < Double.MAX_VALUE;
    }

    /**
     * Returns the number of tries in tests using (pseudo) randomness.
     */
    @Override
    protected int getNumberTries() {
        return HOST_LOCATIONS.length;
    }
}
