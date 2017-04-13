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
    private static final Coord CURR_LOCATION = new Coord(3, 4);
    /* The current time. */
    private static final double CURR_TIME = 10;

    private static final Coord ORIGIN = new Coord(0,0);

    /* Some utility values used in tests. */
    private static final double IMPOSSIBLE_HIGH_UTILITY = 1.1;
    /* Rounded down utility values for different data types with data created at time 0 at origin. */
    private static final double APPROXIMATE_ORIGIN_SKILL_UTILITY = 0.32;
    private static final double APPROXIMATE_ORIGIN_MAP_UTILITY = 0.17;
    private static final double APPROXIMATE_ORIGIN_MARKER_UTILITY = 0.08;
    private static final double APPROXIMATE_ORIGIN_RESOURCE_UTILITY = 0.05;

    /* Numbers from 1 to 4 data items that are expected to be returned in a certain test. */
    private static final int SINGLE_ITEM = 1;
    private static final int TWO_ITEMS = 2;
    private static final int ONE_ITEM_MISSING = 3;
    private static final int ALL_ITEMS = 4;

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
    public void testGetAllDataWithMinimumUtilityReturnsAllForZero() {
        /* Create data with widely differing utility values. Size is always 0 s.t. nothing gets deleted. */
        DisasterData[] data = new DisasterData[] {
                new DisasterData(DisasterData.DataType.MAP, 0, CURR_TIME, CURR_LOCATION),
                new DisasterData(DisasterData.DataType.SKILL, 0, 0.0, CURR_LOCATION),
                new DisasterData(DisasterData.DataType.RESOURCE, 0, 0.0, ORIGIN),
                new DisasterData(DisasterData.DataType.MARKER, 0, CURR_TIME, ORIGIN)
        };

        /* Add it to database. */
        for (DisasterData dataItem : data) {
            this.database.add(dataItem);
        }

        /* Query database. */
        List<DisasterData> allData = this.database.getAllDataWithMinimumUtility(0);
        TestCase.assertEquals("Expected all data to be returned.", data.length, allData.size());
    }

    @Test
    public void testGetAllDataWithMinimumUtilityReturnsNoneForUtilityAbove1() {
        /* Add super useful data. */
        this.database.add(new DisasterData(DisasterData.DataType.MARKER, 0, CURR_TIME, CURR_LOCATION));

        /* Query database. */
        List<DisasterData> highUtilityData = this.database.getAllDataWithMinimumUtility(IMPOSSIBLE_HIGH_UTILITY);
        TestCase.assertEquals("Expected nothing to be returned.", 0, highUtilityData.size());
    }

    @Test
    public void testGetAllDataWithMinimumUtilityOnlyReturnsDataAboveThreshold() {
        /* Add data with different usefulness. */
        DisasterData usefulData = new DisasterData(DisasterData.DataType.MAP, 0, CURR_TIME, CURR_LOCATION);
        DisasterData lessUsefulData = new DisasterData(DisasterData.DataType.MAP, 0, 0, ORIGIN);
        this.database.add(usefulData);
        this.database.add(lessUsefulData);

        /* Query database with high threshold. */
        List<DisasterData> highUtilityData = this.database.getAllDataWithMinimumUtility(1);
        TestCase.assertEquals("Expected a single high utility data item.", 1, highUtilityData.size());
        TestCase.assertEquals("Expected the useful data item to be returned.", usefulData, highUtilityData.get(0));
    }

    @Test
    public void testUtilityValues() {
        /* Create data with different types. Size is always 0 s.t. nothing gets deleted. */
        DisasterData map = new DisasterData(DisasterData.DataType.MAP, 0, 0.0, ORIGIN);
        DisasterData skill = new DisasterData(DisasterData.DataType.SKILL, 0, 0.0, ORIGIN);
        DisasterData marker = new DisasterData(DisasterData.DataType.MARKER, 0, 0.0, ORIGIN);
        DisasterData resource = new DisasterData(DisasterData.DataType.RESOURCE, 0, 0.0, ORIGIN);

        /* Add it to database. */
        this.database.add(map);
        this.database.add(skill);
        this.database.add(marker);
        this.database.add(resource);

        /* The different types should have different utility values.
        Test getting data items slightly below these values one after the other. */
        List<DisasterData> highestUtility =
                this.database.getAllDataWithMinimumUtility(APPROXIMATE_ORIGIN_SKILL_UTILITY);
        TestCase.assertEquals("Expected only one data item to be returned.", SINGLE_ITEM, highestUtility.size());
        TestCase.assertEquals("Expected the skill as highest utility item.", skill, highestUtility.get(0));

        List<DisasterData> highUtility =
                this.database.getAllDataWithMinimumUtility(APPROXIMATE_ORIGIN_MAP_UTILITY);
        TestCase.assertEquals("Expected two data items to be returned.", TWO_ITEMS, highUtility.size());
        TestCase.assertTrue("Expected the map to be returned.", highUtility.contains(map));

        List<DisasterData> mediumUtility =
                this.database.getAllDataWithMinimumUtility(APPROXIMATE_ORIGIN_MARKER_UTILITY);
        TestCase.assertEquals("Expected three data items to be returned.", ONE_ITEM_MISSING, mediumUtility.size());
        TestCase.assertTrue("Expected the marker to be returned.", mediumUtility.contains(marker));

        List<DisasterData> lowUtility = this.database.getAllDataWithMinimumUtility(APPROXIMATE_ORIGIN_RESOURCE_UTILITY);
        TestCase.assertEquals("Expected all data items to be returned.", ALL_ITEMS, lowUtility.size());
    }

    /**
     * Tests that utility values decrease if time advances.
     */
    @Test
    public void testCurrentTimeInfluencesUtilityValue() {
        /* Create data at current location and place. */
        DisasterData data = new DisasterData(DisasterData.DataType.MARKER, 0, CURR_TIME, CURR_LOCATION);
        this.database.add(data);

        /* Make sure it has maximum utility value. */
        TestCase.assertTrue(
                "Expected data item to have maximum utility.",
                this.database.getAllDataWithMinimumUtility(1).contains(data));

        /* Advance time. */
        SimClock.getInstance().advance(CURR_TIME);

        /* It should not have maximum utility value now. */
        TestCase.assertFalse(
                "Utility value should decrease with time.",
                this.database.getAllDataWithMinimumUtility(1).contains(data));
    }

    /**
     * Tests that utility values decrease if the distance between location and the data item increases.
     */
    @Test
    public void testCurrentLocationInfluencesUtilityValue() {
        /* Create data at current location and place. */
        DisasterData data = new DisasterData(DisasterData.DataType.MARKER, 0, CURR_TIME, CURR_LOCATION);
        this.database.add(data);

        /* Make sure it has maximum utility value. */
        TestCase.assertTrue(
                "Expected data item to have maximum utility.",
                this.database.getAllDataWithMinimumUtility(1).contains(data));

        /* Change location. */
        this.owner.setLocation(ORIGIN);

        /* It should not have maximum utility value now. */
        TestCase.assertFalse(
                "Utility value should decrease with increasing distance.",
                this.database.getAllDataWithMinimumUtility(1).contains(data));
    }

    /**
     * Gets all data that is stored in the database.
     *
     * @return All data stored in the database.
     */
    private List<DisasterData> getAllData() {
        /* Returns all data as the minimum possible utility value is 0. */
        return this.database.getAllDataWithMinimumUtility(0);
    }
}
