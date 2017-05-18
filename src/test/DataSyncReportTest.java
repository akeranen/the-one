package test;

import applications.DatabaseApplication;
import core.Coord;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.SimClock;
import core.SimScenario;
import report.DataSyncReport;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import report.SamplingReport;

import static org.junit.Assert.*;



/**
 * Contains tests for the {@link report.DataSyncReport} class
 *
 * @author melanie
 */
public class DataSyncReportTest extends AbstractReportTest{

    /* Properties of the database application. */
    private static final long BIGGEST_DB_SIZE = 3_000_000_000L;
    private static final long SMALLEST_DB_SIZE = 2_000_000_000L;
    private static final double MIN_UTILITY = 0.5;
    private static final double MAP_SENDING_INTERVAL = 43.2;
    private static final int SEED = 0;

    /* String arrays containing all the expected metrics and ratios to check whether everything necessary is contained*/
    private static final String[] EXPECTED_METRICS = new String[]{"avg_used_mem", "max_used_mem", "med_avg_data_util",
            "avg_data_util", "med_avg_data_age", "avg_data_age", "med_max_data_age", "med_avg_data_dist",
            "avg_data_dist", "med_max_data_dist"};
    private static final String[] EXPECTED_RATIOS= new String[]{"avg_ratio_map", "avg_ratio_marker",
            "avg_ratio_skill", "avg_ratio_res"};

    private static final String EXPECTED_FIRST_LINE="Data sync stats for scenario TEST-Scenario";
    private static final String COMMENT_LINE_WRONG="Comment line is not as expected.";

    private static final double TIME_COMPARISON_EXACTNESS = 0.1;

    /* Time values used to check whether reports should be printed */
    private static final double REPORT_INTERVAL = 30;
    private static final double HALF_THE_REPORT_INTERVAL= REPORT_INTERVAL/2;
    private static final int TIME_FOR_A_REPORT = 31;

    /* Properties for DisasterData and DataMessages */
    private static final int SMALL_ITEM_SIZE = 20;
    private static final Coord ORIGIN = new Coord(0,0);

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
        TestSettings.addSettingsToEnableSimScenario(settings);
        this.settings.putSetting(getReportClass().getSimpleName() + "." + SamplingReport.SAMPLE_INTERVAL_SETTING,
                Double.toString(REPORT_INTERVAL));

        // Set locale for periods instead of commas in doubles.
        java.util.Locale.setDefault(java.util.Locale.US);

        /* Create and initialize database application. */
        addDatabaseApplicationSettings();
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
     * Adds all necessary settings the database application needs to function
     */
    private void addDatabaseApplicationSettings() {
        this.settings.putSetting(DatabaseApplication.UTILITY_THRESHOLD, Double.toString(MIN_UTILITY));
        this.settings.putSetting(DatabaseApplication.SIZE_RANDOMIZER_SEED, Integer.toString(SEED));
        this.settings.putSetting(
                DatabaseApplication.DATABASE_SIZE_RANGE, String.format("%d,%d", SMALLEST_DB_SIZE, BIGGEST_DB_SIZE));
        this.settings.putSetting(DatabaseApplication.MIN_INTERVAL_MAP_SENDING, Double.toString(MAP_SENDING_INTERVAL));
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
            line = reader.readLine();
            assertTrue("There should not be anything but a comment", line==null);
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
            line = reader.readLine();
            assertTrue("There should some statistics now", !line.isEmpty());
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
            line = reader.readLine();
            assertEquals("The first output should be at sim_time", WARM_UP_TIME+1,
                    getSimTimeValueFrom(line), TIME_COMPARISON_EXACTNESS);
            //The second line still belongs to the same time
            line = reader.readLine();
            line = reader.readLine();
            line = reader.readLine();
            assertEquals("The second output should be at sim_time", WARM_UP_TIME+REPORT_INTERVAL+1,
                    getSimTimeValueFrom(line), TIME_COMPARISON_EXACTNESS);
            //The line still belongs to the same time
            line = reader.readLine();
            line = reader.readLine();
            line = reader.readLine();
            assertTrue("There should only be two outputs", line==null);

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


    /**
     * SimScenario was called by DataSyncReport and therefore initiated with this specific test's settings.
     * Cleanup to make further tests with other settings possible.
     */
    @After
    public void cleanUp() {
        SimScenario.reset();
        SimClock.reset();
    }

    private void sendDataMessageToHostWithApp(){
        DTNHost sendingHost = utils.createHost(ORIGIN,"hostWithoutDB");
        allHosts.add(sendingHost);
        DisasterData data = new DisasterData(DisasterData.DataType.MAP, SMALL_ITEM_SIZE, 0, ORIGIN);
        DataMessage msg = new DataMessage(sendingHost, hostAttachedToApp, "d1", data, 1, 1);
        sendingHost.createNewMessage(msg);
        sendingHost.sendMessage("d1", hostAttachedToApp);
        hostAttachedToApp.messageTransferred("d1", sendingHost);
        app.handle(msg, hostAttachedToApp);
    }

    private static double getSimTimeValueFrom(String line) {
        //If we do not have sime time, return a value that we never expect
        if (!line.contains("sim_time")){
            return -1;
        }
        final int extraChars=2;
        int firstIndex = line.indexOf(':')+extraChars;
        int lastIndex = line.indexOf(',');
        String numberString = line.substring(firstIndex,lastIndex);
        return Double.parseDouble(numberString);
    }
}
