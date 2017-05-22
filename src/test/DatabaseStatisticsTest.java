package test;

import applications.DatabaseApplication;

import core.Coord;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.Group;
import core.SimClock;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.Map;

/**
 * Tests statistics function in {@link DatabaseApplication}
 * Created by melanie on 22.05.17.
 */
public class DatabaseStatisticsTest {


    /* Properties of the application. */
    private static final long BIGGEST_DB_SIZE = 100L;
    private static final long SMALLEST_DB_SIZE = 100L;
    private static final double MIN_UTILITY = 0.5;
    private static final double MAP_SENDING_INTERVAL = 43.2;
    private static final int SEED = 0;

    /* The current time and times relevant to it*/
    private static final double CURR_TIME = 1800;
    private static final double FIVE_MINS_AGO=1500;
    private static final double HALF_AN_HOUR_LATER =3600;

    /* Used locations for all DB operations. */
    private static final Coord CURR_LOCATION = new Coord(300, 400);
    private static final Coord CLOSE_TO_CURR_LOCATION = new Coord(400, 400);
    private static final Coord ORIGIN = new Coord(0,0);

    /* Sizes for data items */
    private static final int SMALL_ITEM_SIZE=20;
    private static final int BIG_ITEM_SIZE=55;

    /* Data utility value used in tests */
    private static final double HIGH_UTILITY=0.8;

    /* Factors if something is computed for a fraction of the data items */
    private static final double HALF_THE_DATA=0.5;
    private static final double A_THIRD_OF_DATA=0.333;
    private static final double A_FOURTH_OF_DATA = 0.25;

    /* Margin of error used for floating point comparisons */
    private static final double DOUBLE_COMPARISON_EXACTNESS = 0.01;

    /* Error messages */
    private static final String WRONG_USED_MEM_PERCENTAGE="The percentage of used memory was not computed correctly.";
    private static final String WRONG_AVG_DISTANCE="The average data distance was not computed correctly.";
    private static final String WRONG_MAX_DISTANCE ="The maximum data distance was not computed correctly." ;
    private static final String WRONG_AVG_AGE ="The average data age was not computed correctly." ;
    private static final String WRONG_MAX_AGE ="The maximum data age was not computed correctly." ;
    private static final String STATISTICS_SHOULD_NOT_BE_NULL = "The statistics should be empty, but not null.";
    private static final String STATISTICS_SHOULD_BE_EMPTY = "There should be no values in the statistics.";

    private TestSettings settings = new TestSettings();
    private DatabaseApplication app;
    private DTNHost hostAttachedToApp;

    public DatabaseStatisticsTest() {
        // Empty constructor for "Classes and enums with private members should have a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    public void setUp() {
        /* Add settings for database application */
        this.settings.putSetting(DatabaseApplication.UTILITY_THRESHOLD, Double.toString(MIN_UTILITY));
        this.settings.putSetting(DatabaseApplication.SIZE_RANDOMIZER_SEED, Integer.toString(SEED));
        this.settings.putSetting(
                DatabaseApplication.DATABASE_SIZE_RANGE, String.format("%d,%d", SMALLEST_DB_SIZE, BIGGEST_DB_SIZE));
        this.settings.putSetting(DatabaseApplication.MIN_INTERVAL_MAP_SENDING, Double.toString(MAP_SENDING_INTERVAL));

        /* Create test utils. */
        TestUtils utils = new TestUtils(new ArrayList<>(), new ArrayList<>(), this.settings);

        /* Create and initialize database application. */
        DatabaseApplication prototype = new DatabaseApplication(this.settings);
        this.app = new DatabaseApplication(prototype);
        this.hostAttachedToApp = utils.createHost();
        this.app.update(this.hostAttachedToApp);

        // Set owner's location to current location.
        this.hostAttachedToApp.setLocation(CURR_LOCATION);

        // Set time to current time.
        SimClock.getInstance().setTime(CURR_TIME);
    }

    @After
    public void cleanUp() {
        SimClock.reset();
        Group.clearGroups();
        DTNHost.reset();
    }

    /**
     * Tests whether the statistics about {@link DisasterData} are computed correctly.
     * The statistics are about the distance of the data item's location to the host location.
     */
    @Test
    public void testGetDataDistanceStatistics(){
        //Test statistics for empty database
        DoubleSummaryStatistics statistics = app.getDataDistanceStatistics();
        TestCase.assertTrue(STATISTICS_SHOULD_NOT_BE_NULL, statistics != null);
        TestCase.assertTrue(STATISTICS_SHOULD_BE_EMPTY, statistics.getCount()==0);
        //Add a single item that was created at the current time and location
        //It should be very useful. All statistics should just be about this item.
        giveDataToHost(DisasterData.DataType.SKILL, SMALL_ITEM_SIZE, CURR_TIME, CURR_LOCATION);
        TestCase.assertEquals(WRONG_AVG_DISTANCE,
                0, app.getDataDistanceStatistics().getAverage(), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals(WRONG_MAX_DISTANCE,
                0, app.getDataDistanceStatistics().getMax(), DOUBLE_COMPARISON_EXACTNESS);
        //Add a second item that has positive age and distance. All statistics should be about the existing two items
        giveDataToHost(DisasterData.DataType.MAP, SMALL_ITEM_SIZE, 0, ORIGIN);
        TestCase.assertEquals(WRONG_AVG_DISTANCE,
                CURR_LOCATION.distance(ORIGIN)*HALF_THE_DATA,
                app.getDataDistanceStatistics().getAverage(), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals(WRONG_MAX_DISTANCE,
                CURR_LOCATION.distance(ORIGIN),
                app.getDataDistanceStatistics().getMax(), DOUBLE_COMPARISON_EXACTNESS);
        //Add a third data item. As it is big and very useful, it will lead to the removal of the other map data
        //So all statistics refer to bigMapDataItem and skillItem.
        giveDataToHost(DisasterData.DataType.MAP, BIG_ITEM_SIZE, FIVE_MINS_AGO, CLOSE_TO_CURR_LOCATION);
        TestCase.assertEquals(WRONG_AVG_DISTANCE,
                CLOSE_TO_CURR_LOCATION.distance(CURR_LOCATION)*HALF_THE_DATA,
                app.getDataDistanceStatistics().getAverage(), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals(WRONG_MAX_DISTANCE,
                CLOSE_TO_CURR_LOCATION.distance(CURR_LOCATION),
                app.getDataDistanceStatistics().getMax(), DOUBLE_COMPARISON_EXACTNESS);
    }

    /**
     * Tests whether the statistics about {@link DisasterData} are computed correctly.
     * The statistics are about the age of the data items, i.e., the current time minus their creation time.
     */
    @Test
    public void testGetDataAgeStatistics(){
        //Test statistics for empty database
        DoubleSummaryStatistics statistics = app.getDataAgeStatistics();
        TestCase.assertTrue(STATISTICS_SHOULD_NOT_BE_NULL, statistics != null);
        TestCase.assertTrue(STATISTICS_SHOULD_BE_EMPTY, statistics.getCount()==0);
        //Add a single item that was created at the current time and location
        //It should be very useful. All statistics should just be about this item.
        giveDataToHost(DisasterData.DataType.SKILL, SMALL_ITEM_SIZE, CURR_TIME, CURR_LOCATION);
        TestCase.assertEquals(WRONG_AVG_AGE,
                0, app.getDataAgeStatistics().getAverage(), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals(WRONG_MAX_AGE,
                0, app.getDataAgeStatistics().getMax(), DOUBLE_COMPARISON_EXACTNESS);
        //Add a second item that has positive age and distance. All statistics should be about the existing two items
        giveDataToHost(DisasterData.DataType.RESOURCE, SMALL_ITEM_SIZE, 0, ORIGIN);
        TestCase.assertEquals(WRONG_AVG_AGE, CURR_TIME * HALF_THE_DATA,
                app.getDataAgeStatistics().getAverage(), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals(WRONG_MAX_AGE, CURR_TIME,
                app.getDataAgeStatistics().getMax(), DOUBLE_COMPARISON_EXACTNESS);
        //Add a third data item with moderate age
        giveDataToHost(DisasterData.DataType.MARKER, SMALL_ITEM_SIZE,
                FIVE_MINS_AGO, CLOSE_TO_CURR_LOCATION);
        TestCase.assertEquals(WRONG_AVG_AGE, A_THIRD_OF_DATA*CURR_TIME+A_THIRD_OF_DATA*(CURR_TIME-FIVE_MINS_AGO),
                app.getDataAgeStatistics().getAverage(), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals(WRONG_MAX_AGE,
                CURR_TIME, app.getDataAgeStatistics().getMax(), DOUBLE_COMPARISON_EXACTNESS);
    }

    /**
     * Tests whether age statistics do not take map data items into account.
     * As the utility function for map data does not include age, we exclude map data from
     * age statistics. Map data would not be deleted when it gets old, hence it will likely
     * get older and older during the simulation and would throw off our results if we included it.
     */
    @Test
    public void testGetDataAgeStatisticsIgnoresMapData(){
        giveDataToHost(DisasterData.DataType.MAP, SMALL_ITEM_SIZE, FIVE_MINS_AGO, CLOSE_TO_CURR_LOCATION);
        DoubleSummaryStatistics statistics = app.getDataAgeStatistics();
        TestCase.assertTrue("Map data should be ignored by age statistics.", statistics.getCount()==0);
    }

    /**
     * Tests whether the utility statistics about {@link DisasterData} are computed correctly.
     * The statistics are about the utility of the data items, which is computed from the
     * distance of the data items to the host location and the age of the data items.
     */
    @Test
    public void testGetDataUtilityStatistics(){
        //Test statistics for empty database
        DoubleSummaryStatistics statistics = app.getDataUtilityStatistics();
        TestCase.assertTrue(STATISTICS_SHOULD_NOT_BE_NULL, statistics != null);
        TestCase.assertTrue(STATISTICS_SHOULD_BE_EMPTY, statistics.getCount()==0);
        //Add a single item that was created at the current time and location
        //It should be very useful. All statistics should just be about this item.
        giveDataToHost(DisasterData.DataType.SKILL, SMALL_ITEM_SIZE, CURR_TIME, CURR_LOCATION);
        TestCase.assertEquals("The average and max utility should be equal for a single data item",
                app.getDataUtilityStatistics().getAverage(), app.getDataUtilityStatistics().getMax(),
                DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertTrue("Utility for close, recent data items should be high.",
                app.getDataUtilityStatistics().getAverage() > HIGH_UTILITY);
        //Add a second item that has positive age and distance. All statistics should be about the existing two items
        giveDataToHost(DisasterData.DataType.MAP, SMALL_ITEM_SIZE, 0, ORIGIN);
        TestCase.assertTrue("Maximal utility should be high.",
                app.getDataUtilityStatistics().getMax() > HIGH_UTILITY);
        TestCase.assertTrue("The average and max utility should not be equal for two differently useful data items.",
                app.getDataUtilityStatistics().getAverage() < app.getDataUtilityStatistics().getMax());
        //Add a third data item. As it is big and very useful, it will lead to the removal of the other map data
        //So all statistics refer to bigMapDataItem and skillItem.
        giveDataToHost(DisasterData.DataType.MARKER, BIG_ITEM_SIZE, FIVE_MINS_AGO, CLOSE_TO_CURR_LOCATION);
        double maxUtility =app.getDataUtilityStatistics().getMax();
        TestCase.assertTrue("Maximal utility should be high.", maxUtility > HIGH_UTILITY);
        double avgUtility = app.getDataUtilityStatistics().getAverage();
        TestCase.assertTrue("Average utility should be high.", avgUtility > HIGH_UTILITY);
        TestCase.assertTrue("The average and max utility should not be equal for two differently useful data items.",
                avgUtility < maxUtility);
        //If time passes the utilities should be recomputed when accessing the statistics, even if nothing was added
        //to the database.
        SimClock.getInstance().setTime(HALF_AN_HOUR_LATER);
        double maxUtilityLater = app.getDataUtilityStatistics().getMax();
        double avgUtilityLater = app.getDataUtilityStatistics().getAverage();
        TestCase.assertTrue("Average utility should have decreased.", avgUtilityLater < avgUtility);
        TestCase.assertTrue("Maximum utility should have decreased.", maxUtilityLater < maxUtility);
        TestCase.assertTrue("Average and max utility should still be different.", avgUtilityLater < maxUtilityLater);
    }

    /**
     * Tests whether the used memory percentage is computed correctly. The used memory
     * percentage is needed for deletion from the database and for reports.
     */
    @Test
    public void testGetUsedMemoryPercentage(){
        giveDataToHost(DisasterData.DataType.MAP, SMALL_ITEM_SIZE, CURR_TIME, CURR_LOCATION);
        TestCase.assertEquals(WRONG_USED_MEM_PERCENTAGE,
                SMALL_ITEM_SIZE/(double)app.getDatabaseSize(),
                app.getUsedMemoryPercentage(),
                DOUBLE_COMPARISON_EXACTNESS);
        giveDataToHost(DisasterData.DataType.MARKER, SMALL_ITEM_SIZE, 0, ORIGIN);
        TestCase.assertEquals(WRONG_USED_MEM_PERCENTAGE,
                SMALL_ITEM_SIZE+SMALL_ITEM_SIZE/(double)app.getDatabaseSize(),
                app.getUsedMemoryPercentage(),
                DOUBLE_COMPARISON_EXACTNESS);
        giveDataToHost(DisasterData.DataType.MARKER, BIG_ITEM_SIZE, CURR_TIME, CURR_LOCATION);
        //After adding a big useful item, the item with the lower utility should be deleted
        TestCase.assertEquals(WRONG_USED_MEM_PERCENTAGE,
                SMALL_ITEM_SIZE+BIG_ITEM_SIZE/(double)app.getDatabaseSize(),
                app.getUsedMemoryPercentage(),
                DOUBLE_COMPARISON_EXACTNESS);
    }

    /**
     * Tests whether the ratio of items of a certain {@link DisasterData.DataType} type is
     * computed correctly.
     */
    @Test
    public void testGetRatioOfDataItemsPerType(){
        //Add a single item, so we just have skill data
        giveDataToHost(DisasterData.DataType.SKILL, SMALL_ITEM_SIZE, CURR_TIME, CURR_LOCATION);
        Map<DisasterData.DataType, Double> ratioPerType = app.getRatioOfItemsPerDataType();
        TestCase.assertEquals("All data should be skill data.",
                1, ratioPerType.get(DisasterData.DataType.SKILL), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("No markers should be in the database.",
                0, ratioPerType.get(DisasterData.DataType.MARKER), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("No maps should be in the database.",
                0, ratioPerType.get(DisasterData.DataType.MAP), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("No resources should be in the database.",
                0, ratioPerType.get(DisasterData.DataType.RESOURCE), DOUBLE_COMPARISON_EXACTNESS);
        //Add a second data item, which is map data. It is not large enough for anything to
        //get deleted. This item is not that useful
        giveDataToHost(DisasterData.DataType.MAP, SMALL_ITEM_SIZE, 0, ORIGIN);
        ratioPerType = app.getRatioOfItemsPerDataType();
        TestCase.assertEquals("Half the data should be map data.",
                HALF_THE_DATA, ratioPerType.get(DisasterData.DataType.MAP), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("No markers should be in the database",
                0, ratioPerType.get(DisasterData.DataType.MARKER), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("Half the data should be skill data.",
                HALF_THE_DATA, ratioPerType.get(DisasterData.DataType.SKILL), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("No resources should be in the database",
                0, ratioPerType.get(DisasterData.DataType.RESOURCE), DOUBLE_COMPARISON_EXACTNESS);
        //Add a third item, which is also Map data and very useful. The smaller, less useful
        //map data item will be deleted, so we are again left with one skill data and one map data item
        giveDataToHost(DisasterData.DataType.MAP, BIG_ITEM_SIZE, CURR_TIME, CURR_LOCATION);
        ratioPerType = app.getRatioOfItemsPerDataType();
        TestCase.assertEquals("Half the data should be map data.",
                HALF_THE_DATA, ratioPerType.get(DisasterData.DataType.MAP), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("No markers should be in the database.",
                0, ratioPerType.get(DisasterData.DataType.MARKER), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("Half the data should be skill data.",
                HALF_THE_DATA, ratioPerType.get(DisasterData.DataType.SKILL), DOUBLE_COMPARISON_EXACTNESS);
        TestCase.assertEquals("No resources should be in the database.",
                0, ratioPerType.get(DisasterData.DataType.RESOURCE), DOUBLE_COMPARISON_EXACTNESS);
    }

    /**
     * Tests whether statistics about what fraction of all {@link DisasterData} items in a host's database
     * is of a certain {@link DisasterData.DataType} are computed correctly.
     */
    @Test
    public void testRatioOfDataItemsCanIncludeAllDataTypes(){
        for (DisasterData.DataType type : DisasterData.DataType.values()){
            giveDataToHost(type, SMALL_ITEM_SIZE, CURR_TIME, CURR_LOCATION);
        }
        Map<DisasterData.DataType, Double> ratioPerType = app.getRatioOfItemsPerDataType();

        for (DisasterData.DataType type : DisasterData.DataType.values()){
            TestCase.assertEquals("There should be exactly one item of each DataType.", A_FOURTH_OF_DATA,
                    ratioPerType.get(type));
        }
    }

    private void giveDataToHost(DisasterData.DataType type, int size, double creation, Coord location){
        DisasterData data = new DisasterData(type, size, creation, location);
        DataMessage msg1 = new DataMessage(null, hostAttachedToApp, "d1", data, 1, 1);
        app.handle(msg1, hostAttachedToApp);
    }
}
