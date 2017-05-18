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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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

    private static final int SMALL_ITEM_SIZE = 20;

    private static final Coord ORIGIN = new Coord(0,0);

    private DataSyncReport report;
    private TestUtils utils;

    private DatabaseApplication app;
    private DTNHost hostAttachedToApp;

    public DataSyncReportTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
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

        // Set locale for periods instead of commas in doubles.
        java.util.Locale.setDefault(java.util.Locale.US);

        /* Add settings for database application */
        this.settings.putSetting(DatabaseApplication.UTILITY_THRESHOLD, Double.toString(MIN_UTILITY));
        this.settings.putSetting(DatabaseApplication.SIZE_RANDOMIZER_SEED, Integer.toString(SEED));
        this.settings.putSetting(
                DatabaseApplication.DATABASE_SIZE_RANGE, String.format("%d,%d", SMALLEST_DB_SIZE, BIGGEST_DB_SIZE));
        this.settings.putSetting(DatabaseApplication.MIN_INTERVAL_MAP_SENDING, Double.toString(MAP_SENDING_INTERVAL));

        //Create Test Utils for host creation
        this.utils = new TestUtils(new ArrayList<>(), new ArrayList<>(), settings);

        /* Create and initialize database application. */
        this.app = new DatabaseApplication(this.settings);
        this.hostAttachedToApp = this.utils.createHost();
        this.app.update(this.hostAttachedToApp);

        // Create report and set it as an update listener.
        this.report = new DataSyncReport();
        SimScenario.getInstance().addUpdateListener(this.report);
    }

    @Test
    @Override
    public void testReportCorrectlyHandlesWarmUpTime() throws IOException {
        //Send data to host with the app during warm up time
        DTNHost sendingHost = utils.createHost(ORIGIN);
        DisasterData data = new DisasterData(DisasterData.DataType.MAP, SMALL_ITEM_SIZE, 0, ORIGIN);
        DataMessage msg = new DataMessage(sendingHost, hostAttachedToApp, "d1", data, 1, 1);
        sendingHost.createNewMessage(msg);
        sendingHost.sendMessage("d1", hostAttachedToApp);
        hostAttachedToApp.messageTransferred("d1", sendingHost);
        app.update(hostAttachedToApp);

        //Finish report and check whether there is nothing but the comment
        this.report.done();
        try (BufferedReader reader = this.createBufferedReader()) {
            String line = reader.readLine();
            assertEquals("First comment line is not as expected.",
                    "Data sync stats for scenario TEST-Scenario", line);
            line = reader.readLine();
            assertTrue("There should not be anything but a comment", line.isEmpty());

        }
    }

    /**
     * Tests that message events creating and delivery are counted correctly.
     *
     * @throws IOException If the temporary file cannot be opened, read or closed.
     */
    @Test
    public void testDoneCorrectlyCountsMessageEvents() throws IOException {
        this.skipWarmUpTime();

        //TODO
        this.report.done();

        try (BufferedReader reader = this.createBufferedReader()) {
            assertEquals("First comment line is not as expected.",
                    "Data sync stats for scenario TEST-Scenario", reader.readLine());
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

    @After
    /**
     * SimScenario was called by DataSyncReport and therefore initiated with this specific test's settings.
     * Cleanup to make further tests with other settings possible.
     */
    public void cleanUp() {
        SimScenario.reset();
        SimClock.reset();
    }

}
