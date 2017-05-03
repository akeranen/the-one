package test;

import core.Coord;
import core.DTNHost;
import core.DisasterData;
import core.LocalDatabase;
import core.SimClock;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.Tuple;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains tests for the {@link core.LocalDatabase} class.
 *
 * Created by Britta Heymann on 09.04.2017.
 */
public class LocalDatabaseTest {
    private static final int DB_SIZE = 100;

    /* Used location for all DB operations. */
    private static final Coord CURR_LOCATION = new Coord(300, 400);
    /* The current time. */
    private static final double CURR_TIME = 1800;

    private static final Coord ORIGIN = new Coord(0,0);

    /* Some utility values used in tests. */
    private static final double IMPOSSIBLE_HIGH_UTILITY = 1.1;
    /* Rounded down utility values for different data types with data created at time 0 at origin. */
    private static final double APPROXIMATE_ORIGIN_SKILL_UTILITY = 0.91;
    private static final double APPROXIMATE_ORIGIN_MARKER_UTILITY = 0.77;
    private static final double APPROXIMATE_ORIGIN_RESOURCE_UTILITY = 0.74;

    private static final String UNEXPECTED_UTILITY = "Expected different utility.";
    private static final double UTILITY_EXACTNESS = 0.01;

    /* Numbers from 1 to 3 data items that are expected to be returned in certain tests. */
    private static final int SINGLE_ITEM = 1;
    private static final int TWO_ITEMS = 2;
    private static final int ALL_ITEMS = 3;

    /* The database used in tests. */
    private LocalDatabase database;
    private DTNHost owner;

    public LocalDatabaseTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    public void setUp() {
        // Create database.
        this.owner = new TestDTNHost(new ArrayList<>(), null, new TestSettings());
        this.database = new LocalDatabase(this.owner, DB_SIZE);

        // Set owner's location to current location.
        this.owner.setLocation(CURR_LOCATION);

        // Set time to current time.
        SimClock.getInstance().setTime(CURR_TIME);
    }

    @After
    public void resetSimClock() {
        SimClock.reset();
    }

    @Test
    public void testGetTotalSize() {
        TestCase.assertEquals("Expected different database size.", DB_SIZE, this.database.getTotalSize());
    }

    /**
     * Tests that storing data that is larger than the database's size is not possible.
     */
    @Test
    public void testAddDataLargerThanDatabase() {
        DisasterData largeData = new DisasterData(DisasterData.DataType.MAP, DB_SIZE + 1, CURR_TIME, CURR_LOCATION);

        this.database.add(largeData);
        TestCase.assertEquals("Did not expect data in database.", 0, this.getAllData().size());
    }

    /**
     * Tests that adding data that is almost as large as the database's size works.
     */
    @Test
    public void testAddDataSmallerThanDatabase() {
        DisasterData fittingData =
                new DisasterData(DisasterData.DataType.MARKER, DB_SIZE - 1, CURR_TIME, CURR_LOCATION);
        this.database.add(fittingData);

        List<DisasterData> allData = this.getAllData();
        TestCase.assertEquals("There should be data in the database.", 1, allData.size());
        TestCase.assertEquals("Expected added data in database.", fittingData, allData.get(0));
    }

    /**
     * Tests that adding data may delete data that is evaluated as less useful.
     */
    @Test
    public void testAddDataDeletesDataOnSmallSpace() {
        DisasterData usefulData = new DisasterData(DisasterData.DataType.MAP, DB_SIZE - 1, CURR_TIME, CURR_LOCATION);
        DisasterData lessUsefulData = new DisasterData(DisasterData.DataType.MAP, 0, 0, ORIGIN);

        this.database.add(lessUsefulData);
        TestCase.assertEquals("Expected not that useful data to be in the DB for now.", 1, this.getAllData().size());

        this.database.add(usefulData);
        List<DisasterData> allData = this.getAllData();
        TestCase.assertEquals("Expected data deletion.", 1, allData.size());
        TestCase.assertEquals("Expected useful data not to be deleted.", usefulData, allData.get(0));
    }

    /**
     * Test adding the same data a second time does not store it a second time.
     */
    @Test
    public void testAddSameDataTwice() {
        DisasterData data = new DisasterData(DisasterData.DataType.MAP, 0, CURR_TIME, CURR_LOCATION);
        this.database.add(data);
        this.database.add(data);

        TestCase.assertEquals("Data should only have been added once.", 1, this.getAllData().size());
    }

    @Test
    public void testGetAllNonMapDataWithMinimumUtilityReturnsAllNonMapForZero() {
        /* Create data with widely differing utility values. Size is always 0 s.t. nothing gets deleted. */
        DisasterData[] data = new DisasterData[] {
                new DisasterData(DisasterData.DataType.SKILL, 0, CURR_TIME, CURR_LOCATION),
                new DisasterData(DisasterData.DataType.SKILL, 0, 0.0, CURR_LOCATION),
                new DisasterData(DisasterData.DataType.RESOURCE, 0, 0.0, ORIGIN),
                new DisasterData(DisasterData.DataType.MARKER, 0, CURR_TIME, ORIGIN)
        };

        /* Add it to database. */
        for (DisasterData dataItem : data) {
            this.database.add(dataItem);
        }

        /* Query database. */
        List<Tuple<DisasterData, Double>> allData = this.database.getAllNonMapDataWithMinimumUtility(0);
        TestCase.assertEquals("Expected all data to be returned.", data.length, allData.size());
    }

    @Test
    public void testGetAllNonMapDataWithMinimumUtilityDoesNotReturnMaps() {
        this.database.add(new DisasterData(DisasterData.DataType.MAP, 0, CURR_TIME, CURR_LOCATION));
        List<Tuple<DisasterData, Double>> nonMapData = this.database.getAllNonMapDataWithMinimumUtility(0);
        TestCase.assertEquals("Map was returned as non-map data.", 0, nonMapData.size());
    }

    @Test
    public void testGetAllNonMapDataWithMinimumUtilityReturnsNoneForUtilityAbove1() {
        /* Add super useful data. */
        this.database.add(new DisasterData(DisasterData.DataType.MARKER, 0, CURR_TIME, CURR_LOCATION));

        /* Query database. */
        List<Tuple<DisasterData, Double>> highUtilityData =
                this.database.getAllNonMapDataWithMinimumUtility(IMPOSSIBLE_HIGH_UTILITY);
        TestCase.assertEquals("Expected nothing to be returned.", 0, highUtilityData.size());
    }

    @Test
    public void testGetAllNonMapDataWithMinimumUtilityOnlyReturnsDataAboveThreshold() {
        /* Add data with different usefulness. */
        DisasterData usefulData = new DisasterData(DisasterData.DataType.MARKER, 0, CURR_TIME, CURR_LOCATION);
        DisasterData lessUsefulData = new DisasterData(DisasterData.DataType.MARKER, 0, 0, ORIGIN);
        this.database.add(usefulData);
        this.database.add(lessUsefulData);

        /* Query database with high threshold. */
        List<Tuple<DisasterData, Double>> highUtilityData = this.database.getAllNonMapDataWithMinimumUtility(1);
        TestCase.assertEquals("Expected a single high utility data item.", 1, highUtilityData.size());
        TestCase.assertEquals(
                "Expected the useful data item to be returned.", usefulData, highUtilityData.get(0).getKey());
    }

    @Test
    public void testGetMapDataReturnsAllMaps() {
        DisasterData map1 = new DisasterData(DisasterData.DataType.MAP, 0, CURR_TIME, CURR_LOCATION);
        DisasterData map2 = new DisasterData(DisasterData.DataType.MAP, 0, 0, ORIGIN);
        this.database.add(map1);
        this.database.add(map2);

        List<DisasterData> maps = this.database.getMapData();
        TestCase.assertEquals("Expected different number of maps.", TWO_ITEMS, maps.size());
        TestCase.assertTrue("Map 1 was not returned.", maps.contains(map1));
        TestCase.assertTrue("Map 2 was not returned.", maps.contains(map2));
    }

    @Test
    public void testGetMapDataDoesNotReturnNonMapData() {
        /* Create non-map data of all different types.. */
        DisasterData[] data = new DisasterData[] {
                new DisasterData(DisasterData.DataType.SKILL, 0, 0.0, ORIGIN),
                new DisasterData(DisasterData.DataType.RESOURCE, 0, 0.0, ORIGIN),
                new DisasterData(DisasterData.DataType.MARKER, 0, 0.0, ORIGIN)
        };

        /* Add it to database. */
        for (DisasterData dataItem : data) {
            this.database.add(dataItem);
        }

        /* Check it's not returned. */
        List<DisasterData> mapData = this.database.getMapData();
        TestCase.assertEquals("Returned non-map data.", 0, mapData.size());
    }

    @Test
    public void testUtilityValuesForNonMapData() {
        /* Create data with different types. Size is always 0 s.t. nothing gets deleted. */
        DisasterData skill = new DisasterData(DisasterData.DataType.SKILL, 0, 0.0, ORIGIN);
        DisasterData marker = new DisasterData(DisasterData.DataType.MARKER, 0, 0.0, ORIGIN);
        DisasterData resource = new DisasterData(DisasterData.DataType.RESOURCE, 0, 0.0, ORIGIN);

        /* Add it to database. */
        this.database.add(skill);
        this.database.add(marker);
        this.database.add(resource);

        /* The different types should have different utility values.
        Test getting data items slightly below these values one after the other. */
        List<Tuple<DisasterData, Double>> highestUtility =
                this.database.getAllNonMapDataWithMinimumUtility(APPROXIMATE_ORIGIN_SKILL_UTILITY);
        TestCase.assertEquals("Expected only one data item to be returned.", SINGLE_ITEM, highestUtility.size());
        TestCase.assertTrue("Expected the skill as highest utility item.", containsData(highestUtility, skill));
        TestCase.assertEquals(
                UNEXPECTED_UTILITY,
                APPROXIMATE_ORIGIN_SKILL_UTILITY,
                getUtility(highestUtility, skill),
                UTILITY_EXACTNESS);

        List<Tuple<DisasterData, Double>> mediumUtility =
                this.database.getAllNonMapDataWithMinimumUtility(APPROXIMATE_ORIGIN_MARKER_UTILITY);
        TestCase.assertEquals("Expected two data items to be returned.", TWO_ITEMS, mediumUtility.size());
        TestCase.assertTrue("Expected the marker to be returned.", containsData(mediumUtility, marker));
        TestCase.assertEquals(
                UNEXPECTED_UTILITY,
                APPROXIMATE_ORIGIN_MARKER_UTILITY,
                getUtility(mediumUtility, marker),
                UTILITY_EXACTNESS);

        List<Tuple<DisasterData, Double>> lowUtility =
                this.database.getAllNonMapDataWithMinimumUtility(APPROXIMATE_ORIGIN_RESOURCE_UTILITY);
        TestCase.assertEquals("Expected all data items to be returned.", ALL_ITEMS, lowUtility.size());
        TestCase.assertEquals(
                UNEXPECTED_UTILITY,
                APPROXIMATE_ORIGIN_RESOURCE_UTILITY,
                getUtility(lowUtility, resource),
                UTILITY_EXACTNESS);
    }

    /**
     * Tests that utility values decrease if time advances.
     */
    @Test
    public void testCurrentTimeInfluencesUtilityValue() {
        /* Create data at current location and place. */
        DisasterData data = new DisasterData(DisasterData.DataType.MARKER, 0, CURR_TIME, CURR_LOCATION);
        this.database.add(data);
        double originalUtility = getUtility(this.database.getAllNonMapDataWithMinimumUtility(0), data);

        /* Advance time. */
        SimClock.getInstance().advance(CURR_TIME);

        /* Check new utility value is smaller than original one. */
        double newUtility = getUtility(this.database.getAllNonMapDataWithMinimumUtility(0), data);
        TestCase.assertTrue(
                "Utility value should decrease with increasing age.",
                newUtility + UTILITY_EXACTNESS < originalUtility);
    }

    /**
     * Tests that utility values decrease if the distance between location and the data item increases.
     */
    @Test
    public void testCurrentLocationInfluencesUtilityValue() {
        /* Create data at current location and place. */
        DisasterData data = new DisasterData(DisasterData.DataType.MARKER, 0, CURR_TIME, CURR_LOCATION);
        this.database.add(data);
        double originalUtility = getUtility(this.database.getAllNonMapDataWithMinimumUtility(0), data);

        /* Change location. */
        this.owner.setLocation(ORIGIN);

        /* Check new utility value is smaller than the original one. */
        double newUtility = getUtility(this.database.getAllNonMapDataWithMinimumUtility(0), data);
        TestCase.assertTrue(
                "Utility value should decrease with increasing distance.",
                newUtility + UTILITY_EXACTNESS < originalUtility);
    }

    /**
     * Gets all data that is stored in the database.
     *
     * @return All data stored in the database.
     */
    private List<DisasterData> getAllData() {
        // Find all non-map data with utility >= 0, i.e. all non-map data overall.
        List<Tuple<DisasterData, Double>> nonMaps = this.database.getAllNonMapDataWithMinimumUtility(0);
        // Get maps.
        List<DisasterData> maps = this.database.getMapData();

        // Add all data items to a common collection.
        List<DisasterData> allData = new ArrayList<>(nonMaps.size() + maps.size());
        for (Tuple<DisasterData, Double> tuple : nonMaps) {
            allData.add(tuple.getKey());
        }
        allData.addAll(maps);

        // Return it.
        return allData;
    }

    /**
     * Checks whether the given list contains the given data item.
     *
     * @param list List to check.
     * @param data Data item to search for.
     * @return Whether the list contains a tuple that has the data item as its key.
     */
    private static boolean containsData(List<Tuple<DisasterData, Double>> list, DisasterData data) {
        return list.stream().anyMatch(dataItem -> dataItem.getKey().equals(data));
    }

    /**
     * Extracts the data item's utility value from the given list.
     *
     * @param list List to take the utility value from.
     * @param data Data item to check the utility for.
     * @return The utility value of the provided data item as defined by the provided list.
     */
    private static double getUtility(List<Tuple<DisasterData, Double>> list, DisasterData data) {
        for (Tuple<DisasterData, Double> dataItem : list) {
            if (dataItem.getKey().equals(data)) {
                return dataItem.getValue();
            }
        }
        throw new IllegalArgumentException("Data " + data + " was not to be found in list.");
    }
}
