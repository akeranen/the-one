package test;

import applications.DatabaseApplication;
import core.Coord;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.SimClock;
import core.SimScenario;
import report.DataSyncReport;
import report.SamplingReport;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.Tuple;

import static org.junit.Assert.*;

/**
 * Contains tests for the {@link report.DataSyncReport} class
 *
 * @author melanie
 */
public class DataSyncReportTest extends AbstractReportTest{

    /* Properties of the database application. */
    private static final long BIGGEST_DB_SIZE = 300L;
    private static final long SMALLEST_DB_SIZE = 200L;

    /* String arrays containing all the expected metrics and ratios to check whether everything necessary is contained*/
    private static final String[] EXPECTED_METRICS = new String[]{"avg_used_mem", "max_used_mem", "med_avg_data_util",
            "avg_data_util", "med_avg_data_age", "avg_data_age", "med_max_data_age", "med_avg_data_dist",
            "avg_data_dist", "med_max_data_dist"};
    private static final String[] EXPECTED_RATIOS= new String[]{"avg_ratio_map", "avg_ratio_marker",
            "avg_ratio_skill", "avg_ratio_res"};

    /* Expected lines */
    private static final String EXPECTED_FIRST_LINE="Data sync stats for scenario TEST-Scenario";
    private static final String[] EXPECTED_OUTPUTS= new String []{
            "sim_time: 51.0, avg_used_mem: 0.0%, max_used_mem: 0.0%, med_avg_data_util: NaN, avg_data_util: NaN, " +
                    "med_avg_data_age: NaN, avg_data_age: NaN, med_max_data_age: NaN, med_avg_data_dist: NaN, " +
                    "avg_data_dist: NaN, med_max_data_dist: NaN",
            "avg_ratio_map: NaN%, avg_ratio_marker: NaN%, avg_ratio_skill: NaN%, avg_ratio_res: NaN%",
            "sim_time: 81.0, avg_used_mem: 3.7%, max_used_mem: 7.3%, med_avg_data_util: 1.0, avg_data_util: 1.0, " +
                    "med_avg_data_age: NaN, avg_data_age: NaN, med_max_data_age: NaN, med_avg_data_dist: 0.0, " +
                    "avg_data_dist: 0.0, med_max_data_dist: 0.0",
            "avg_ratio_map: 100.0%, avg_ratio_marker: 0.0%, avg_ratio_skill: 0.0%, avg_ratio_res: 0.0%",
            "sim_time: 111.0, avg_used_mem: 8.1%, max_used_mem: 8.8%, med_avg_data_util: 1.0, avg_data_util: 1.0, " +
                    "med_avg_data_age: 91.0, avg_data_age: 91.0, med_max_data_age: 91.0, med_avg_data_dist: 565.7, " +
                    "avg_data_dist: 282.8, med_max_data_dist: 565.7",
            "avg_ratio_map: 50.0%, avg_ratio_marker: 0.0%, avg_ratio_skill: 50.0%, avg_ratio_res: 0.0%",
            "sim_time: 141.0, avg_used_mem: 22.7%, max_used_mem: 30.8%, med_avg_data_util: 1.0, avg_data_util: 0.9, " +
                    "med_avg_data_age: 121.0, avg_data_age: 90.8, med_max_data_age: 121.0, med_avg_data_dist: 848.5, " +
                    "avg_data_dist: 565.7, med_max_data_dist: 1131.4",
            "avg_ratio_map: 25.0%, avg_ratio_marker: 25.0%, avg_ratio_skill: 50.0%, avg_ratio_res: 0.0%"
    };

    /* Error messages for failing tests */
    private static final String COMMENT_LINE_WRONG="Comment line is not as expected.";
    private static final String EMPTY_LINE_EXPECTED="There should be an empty line between outputs";

    private static final double TIME_COMPARISON_EXACTNESS = 0.1;
    private static final int DECIMAL_PLACES=1;

    /* Time values used to check whether reports should be printed */
    private static final double REPORT_INTERVAL = 30;
    private static final double HALF_THE_REPORT_INTERVAL= REPORT_INTERVAL/2;
    private static final int TIME_FOR_A_REPORT = 31;
    private static final int TIME_CLOSE_TO_SIM_START=20;

    /* Properties for DisasterData and DataMessages */
    private static final int SMALL_ITEM_SIZE = 20;
    private static final int BIG_ITEM_SIZE=50;
    private static final Coord ORIGIN = new Coord(0,0);
    private static final Coord CLOSE_COORD = new Coord(400, 400);
    private static final Coord DISTANT_COORD = new Coord(800, 800);

    private DataSyncReport report;
    private TestUtils utils;

    private DatabaseApplication app;
    private DTNHost hostAttachedToApp;

    /* We will need to manually trigger the updates for UpdateListeners. The method receives a list of all hosts
    so we store it a list of them.
    */
    private List<DTNHost> allHosts = new ArrayList<>();

    public DataSyncReportTest() {
        // Empty constructor for "Classes and enums with private members should have a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    /**
     * Creates the report writing to a temporary file and adds it as message listener.
     */
    @Before
    @Override
    public void setUp() throws IOException {
        // Call generic report set up adding important settings and the temporary file.
        super.setUp();

        // Make SimScenario usage possible.
        SimScenario.reset();
        this.settings.putSetting(getReportClass().getSimpleName() + "." + SamplingReport.SAMPLE_INTERVAL_SETTING,
                Double.toString(REPORT_INTERVAL));
        this.settings.putSetting(getReportClass().getSimpleName() + "." + SamplingReport.PRECISION_SETTING,
                Integer.toString(DECIMAL_PLACES));

        // Set locale for periods instead of commas in doubles.
        java.util.Locale.setDefault(java.util.Locale.US);

        /* Create and initialize database application. */
        DatabaseApplicationTest.addDatabaseApplicationSettings(this.settings);
        this.settings.putSetting(
               DatabaseApplication.DATABASE_SIZE_RANGE, String.format("%d,%d", SMALLEST_DB_SIZE, BIGGEST_DB_SIZE));
        this.app = new DatabaseApplication(this.settings);

        //Create Test Utils for host creation
        this.utils = new TestUtils(new ArrayList<>(), new ArrayList<>(), settings);

        this.hostAttachedToApp = this.utils.createHost("hostWithDB");
        this.app.update(this.hostAttachedToApp);
        hostAttachedToApp.getRouter().addApplication(app);
        allHosts.add(hostAttachedToApp);

        // Create report
        this.report = new DataSyncReport();
    }

    /**
     * Tests that during the warm up time there is no output even if
     * there is data
     *
     * @throws IOException if temporary file could not be read
     */
    @Test
    @Override
    public void testReportCorrectlyHandlesWarmUpTime() throws IOException {
        sendDataMessageToHostWithApp();

        //Finish report and check whether there is nothing but the comment
        SimClock.getInstance().setTime(TIME_FOR_A_REPORT);
        report.updated(allHosts);
        this.report.done();
        try (BufferedReader reader = this.createBufferedReader()) {
            String line = reader.readLine();
            assertEquals(COMMENT_LINE_WRONG, EXPECTED_FIRST_LINE, line);
            line = reader.readLine();
            assertNull("There should not be anything but a comment", line);
        }
    }

    /**
     * Tests that the report includes all relevant statistics and the sim time
     *
     * @throws IOException If the temporary file cannot be opened, read or closed.
     */
    @Test
    public void testIncludesAllNecessaryStatistics() throws IOException {
        this.skipWarmUpTime();
        sendDataMessageToHostWithApp();

        //One host should have one data item now, another none
        report.updated(allHosts);
        this.report.done();

        try (BufferedReader reader = this.createBufferedReader()) {
            String line = reader.readLine();
            assertEquals(COMMENT_LINE_WRONG, EXPECTED_FIRST_LINE, line);
            line = reader.readLine();
            assertFalse("There should some statistics now", line.isEmpty());
            //Check whether the sim time was printed
            assertTrue("The simulation time should be included.", line.contains("sim_time"));
            //Check whether all metrics are in the first line
            for (String metric : EXPECTED_METRICS){
                assertTrue("Metrics should include " + metric + ".",
                        line.contains(metric));
            }
            //Check whether all ratios are contained in the second line
            line = reader.readLine();
            for (String ratio : EXPECTED_RATIOS){
                assertTrue("Metrics should include " + ratio + ".",
                        line.contains(ratio));
            }
        }
    }

    @Test
    public void testReportPrintsInCorrectIntervals() throws IOException {
        this.skipWarmUpTime();
        sendDataMessageToHostWithApp();

        //We should write the statistics for the current time
        report.updated(allHosts);

        //Not time to output statistics yet
        SimClock.getInstance().advance(HALF_THE_REPORT_INTERVAL);
        report.updated(allHosts);

        //Time to output statistics again
        SimClock.getInstance().advance(HALF_THE_REPORT_INTERVAL);
        report.updated(allHosts);

        this.report.done();

        try (BufferedReader reader = this.createBufferedReader()) {
            String line = reader.readLine();
            assertEquals(COMMENT_LINE_WRONG, EXPECTED_FIRST_LINE, line);
            line = reader.readLine();
            assertEquals("The first output should be after the warm up time.", WARM_UP_TIME+1,
                    getSimTimeValueFrom(line), TIME_COMPARISON_EXACTNESS);
            line = reader.readLine();
            assertTrue("This line should include ratios for the first output.", line.contains(EXPECTED_RATIOS[0]));
            line = reader.readLine();
            assertTrue(EMPTY_LINE_EXPECTED, line.isEmpty());
            line = reader.readLine();
            assertEquals("The second output should at one samplingInterval after the first",
                    WARM_UP_TIME+REPORT_INTERVAL+1, getSimTimeValueFrom(line), TIME_COMPARISON_EXACTNESS);
            line = reader.readLine();
            assertTrue("This line should include ratios for the second output.", line.contains(EXPECTED_RATIOS[0]));
            line = reader.readLine();
            assertTrue(EMPTY_LINE_EXPECTED, line.isEmpty());
            line = reader.readLine();
            assertNull("There should only be two outputs", line);
        }
    }

    @Test
    public void testCorrectReportForSmallScenario() throws IOException {
        playScenario();
        this.report.done();
        try (BufferedReader reader = this.createBufferedReader()) {
            String line = reader.readLine();
            final int textLinesPerSimTime = 2;
            assertEquals(COMMENT_LINE_WRONG, EXPECTED_FIRST_LINE, line);
            for (int output=0; output < EXPECTED_OUTPUTS.length; output=output+textLinesPerSimTime){
                line = reader.readLine();
                assertEquals("Metrics were not as expected.", EXPECTED_OUTPUTS[output], line);
                line = reader.readLine();
                assertEquals("Type ratios were not as expected.", EXPECTED_OUTPUTS[output+1], line);
                line = reader.readLine();
                assertTrue(EMPTY_LINE_EXPECTED, line.isEmpty());
            }
            line = reader.readLine();
            assertNull("There should only be four outputs", line);
        }
    }

    /**
     * Gets the report class to test.
     *
     * @return The report class to test.
     */
    @Override
    protected Class getReportClass() {
        return DataSyncReport.class;
    }

    private void sendDataMessageToHostWithApp(){
        DTNHost sendingHost = utils.createHost(ORIGIN,"hostWithoutDB");
        allHosts.add(sendingHost);
        DisasterData data = new DisasterData(DisasterData.DataType.MAP, SMALL_ITEM_SIZE, 0, ORIGIN);
        DataMessage msg =
                new DataMessage(sendingHost, hostAttachedToApp, "d1", Collections.singleton(new Tuple<>(data, 1D)), 1);
        app.handle(msg, hostAttachedToApp);
    }

    private static double getSimTimeValueFrom(String line) {
        //If we do not have sime time, return a value that we never expect
        if (!line.contains("sim_time")){
            return -1;
        }
        int firstIndex = line.indexOf("sim_time: ")+"sim_time: ".length();
        String numberString = line.substring(firstIndex);
        int lastIndex = numberString.indexOf(',');
        numberString = numberString.substring(0,lastIndex);

        return Double.parseDouble(numberString);
    }

    /**
     * Simulates creation of data and exchange of data for two hosts.
     *
     * time   | hostAttachedtoApp       | secondHost                |
     * -------|-------------------------|---------------------------|
     *  51    |0 items                  | 0 items                   |
     * -------|-------------------------|---------------------------|
     *  81    |1 item                   | 0 items                   |
     *        |MAP, 20, 0, (0,0)        |                           |
     * -------|-------------------------|---------------------------|
     * 111    |1 item                   | 1 item                    |
     *        |MAP, 20, 0, (0,0)        |SKILL, 20, 20, (400,400)   |
     * -------|-------------------------|---------------------------|
     * 141    |2 items                  | 2 items                   |
     *        |MAP, 20, 0, (0,0)        |SKILL, 20, 20, (400,400)   |
     *        |SKILL, 20, 20,(400,400)  |MARKER, 50, 141, (800,800) |
     * -------|-------------------------|---------------------------|
     */
    private void playScenario(){
        skipWarmUpTime();

        //Create a second host with a database
        DTNHost secondHost = utils.createHost(ORIGIN,"secondHost");
        allHosts.add(secondHost);
        DatabaseApplication secondApp = new DatabaseApplication(app);
        secondApp.update(secondHost);
        secondHost.getRouter().addApplication(secondApp);

        //First report output while no host has any data
        report.updated(allHosts);

        //Send message with map data to hostAttachedToApp
        SimClock.getInstance().advance(REPORT_INTERVAL);
        DisasterData mapData = new DisasterData(DisasterData.DataType.MAP, SMALL_ITEM_SIZE, 0, ORIGIN);
        DataMessage msg1 = new DataMessage(
                secondHost, hostAttachedToApp, "d1", Collections.singleton(new Tuple<>(mapData, 1D)), 1);
        app.handle(msg1, hostAttachedToApp);

        //Second report output while one host has one data item
        report.updated(allHosts);

        //Send message with skill data to second host, this data was created later
        SimClock.getInstance().advance(REPORT_INTERVAL);
        DisasterData skillData = new DisasterData(DisasterData.DataType.SKILL, SMALL_ITEM_SIZE, TIME_CLOSE_TO_SIM_START,
                CLOSE_COORD);
        DataMessage msg2 = new DataMessage(
                hostAttachedToApp, secondHost, "d2", Collections.singleton(new Tuple<>(skillData, 1D)), 1);
        secondApp.handle(msg2, secondHost);

        //Third report output when each host has a different data item
        report.updated(allHosts);

        //Send a marker Data item that is recent, but further away to the second host
        SimClock.getInstance().advance(REPORT_INTERVAL);
        DisasterData markerData = new DisasterData(DisasterData.DataType.MARKER, BIG_ITEM_SIZE,
                SimClock.getIntTime(), DISTANT_COORD);
        DataMessage msg3 = new DataMessage(
                hostAttachedToApp, secondHost,  "d3", Collections.singleton(new Tuple<>(markerData, 1D)), 1);
        secondApp.handle(msg3, secondHost);

        //Send skill data from second host to first host
        DataMessage msg4 = new DataMessage(
                secondHost, hostAttachedToApp, "d4", Collections.singleton(new Tuple<>(skillData, 1D)), 1);
        app.handle(msg4, hostAttachedToApp);

        //Fourth report output when each host has two database items
        report.updated(allHosts);
    }

    /**
     * SimScenario was called by DataSyncReport and therefore initiated with this specific test's settings.
     * Cleanup to make further tests with other settings possible.
     */
    @After
    public void cleanUp() {
        SimScenario.reset();
        SimClock.reset();
        DTNHost.reset();
    }
}
