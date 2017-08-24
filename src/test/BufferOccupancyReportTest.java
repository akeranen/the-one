package test;

import core.BroadcastMessage;
import core.DTNHost;
import core.Settings;
import core.SimClock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import report.BufferOccupancyReport;
import report.Report;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * Contains tests for the {@link report.BufferOccupancyReport} class.
 * Created by Britta Heymann on 11.08.2017.
 */
public class BufferOccupancyReportTest extends AbstractReportTest {
    /* Time spans needed for tests. */
    private static final int REPORT_INTERVAL = WARM_UP_TIME / 2;
    private static final int SECOND_REPORT_TIME = 2 * REPORT_INTERVAL;
    private static final double SMALL_TIME_DIFFERENCE = 0.01;

    /* Message sizes needed for tests. */
    private static final int SMALL_MESSAGE_SIZE = 10;
    private static final int LARGER_MESSAGE_SIZE = 5000;

    /* Indices of certain values in a report line. */
    private static final int TIME_INDEX = 0;
    private static final int AVERAGE_INDEX = 1;
    private static final int VARIANCE_INDEX = 2;
    private static final int MINIMUM_INDEX = 3;
    private static final int MAXIMUM_INDEX = 4;

    /* Further constants. */
    private static final double EXPONENT_TO_SQUARE = 2;

    private static final String UNEXPECTED_REPORT_TIME = "Expected report line at different simulator time point.";

    /** The accepted difference when comparing doubles for equality. */
    private static final double DOUBLE_COMPARISON_DELTA = 0.0001;

    private BufferOccupancyReport report;
    private SimClock clock = SimClock.getInstance();

    // Hosts with buffers.
    private DTNHost hostWithSmallMessage;
    private DTNHost hostWithLargerMessage;
    private List<DTNHost> allHosts;

    public BufferOccupancyReportTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    @Override
    public void setUp() throws IOException {
        // Let base do the basic report setup.
        super.setUp();

        // Add warm up time.
        this.settings.setNameSpace(this.getReportClass().getSimpleName());
        this.settings.putSetting(Report.WARMUP_S, Integer.toString(WARM_UP_TIME));
        this.settings.putSetting(BufferOccupancyReport.BUFFER_REPORT_INTERVAL, Integer.toString(REPORT_INTERVAL));
        this.settings.restoreNameSpace();

        // Set clock to 0.
        this.clock.setTime(0);

        // Create report.
        this.report = new BufferOccupancyReport();

        // Create hosts.
        TestUtils utils = new TestUtils(null, new ArrayList<>(), this.settings);
        this.hostWithSmallMessage = utils.createHost();
        this.hostWithLargerMessage = utils.createHost();
        this.allHosts = Arrays.asList(this.hostWithLargerMessage, this.hostWithSmallMessage);

        // Make sure hosts have messages in buffer.
        this.hostWithSmallMessage.createNewMessage(
                new BroadcastMessage(this.hostWithSmallMessage, "M", SMALL_MESSAGE_SIZE));
        this.hostWithLargerMessage.createNewMessage(
                new BroadcastMessage(this.hostWithLargerMessage, "M", LARGER_MESSAGE_SIZE));
    }

    /**
     * Checks that the report correctly handles the warm up time as set by the {@link Report#WARMUP_S} setting.
     */
    @Override
    public void testReportCorrectlyHandlesWarmUpTime() throws IOException {
        Assert.assertTrue("Report interval should be smaller than warm up time for test that makes sense.",
                REPORT_INTERVAL < WARM_UP_TIME);

        // Check report works even before warm up time expired.
        this.clock.setTime(REPORT_INTERVAL);
        this.report.updated(this.allHosts);
        this.report.done();
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(
                    UNEXPECTED_REPORT_TIME,
                    REPORT_INTERVAL,
                    BufferOccupancyReportTest.getTimeFromLine(reader.readLine()),
                    DOUBLE_COMPARISON_DELTA);
            Assert.assertNull("Expected only one line.", reader.readLine());
        }
    }

    /**
     * Checks that the {@link BufferOccupancyReport} correctly prints all statistics.
     * @throws IOException
     */
    @Test
    public void testBufferStatisticsAreCorrect() throws IOException {
        // Write report.
        this.clock.setTime(REPORT_INTERVAL);
        this.report.updated(Arrays.asList(this.hostWithSmallMessage, this.hostWithLargerMessage));
        this.report.done();

        // Check statistics.
        DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();
        statistics.accept(this.hostWithLargerMessage.getBufferOccupancy());
        statistics.accept(this.hostWithSmallMessage.getBufferOccupancy());
        double variance = 0;
        variance += Math.pow(
                this.hostWithLargerMessage.getBufferOccupancy() - statistics.getAverage(), EXPONENT_TO_SQUARE);
        variance += Math.pow(
                this.hostWithSmallMessage.getBufferOccupancy() - statistics.getAverage(), EXPONENT_TO_SQUARE);
        try(BufferedReader reader = this.createBufferedReader()) {
            String line = reader.readLine();
            Assert.assertEquals(
                    UNEXPECTED_REPORT_TIME,
                    REPORT_INTERVAL, BufferOccupancyReportTest.getTimeFromLine(line),
                    DOUBLE_COMPARISON_DELTA);
            Assert.assertEquals(
                    "Expected different value as average.",
                    statistics.getAverage(), BufferOccupancyReportTest.getAverageFromLine(line),
                    DOUBLE_COMPARISON_DELTA);
            Assert.assertEquals(
                    "Expected different value as variance.",
                    variance, BufferOccupancyReportTest.getVarianceFromLine(line),
                    DOUBLE_COMPARISON_DELTA);
            Assert.assertEquals(
                    "Expected different value as minimum.",
                    statistics.getMin(), BufferOccupancyReportTest.getMinimumFromLine(line),
                    DOUBLE_COMPARISON_DELTA);
            Assert.assertEquals(
                    "Expected different value as average.",
                    statistics.getMax(), BufferOccupancyReportTest.getMaximumFromLine(line),
                    DOUBLE_COMPARISON_DELTA);
        }
    }

    @Test
    public void testCorrectReportIntervalGetsUsed() throws IOException {
        // Update report at multiple time points.
        this.clock.setTime(REPORT_INTERVAL - SMALL_TIME_DIFFERENCE);
        this.report.updated(this.allHosts);
        this.clock.setTime(REPORT_INTERVAL);
        this.report.updated(this.allHosts);
        this.clock.setTime(SECOND_REPORT_TIME);
        this.report.updated(this.allHosts);

        // Finish report.
        this.report.done();

        // Check it was printed at correct times.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(
                    UNEXPECTED_REPORT_TIME,
                    REPORT_INTERVAL,
                    BufferOccupancyReportTest.getTimeFromLine(reader.readLine()),
                    DOUBLE_COMPARISON_DELTA);
            Assert.assertEquals(
                    UNEXPECTED_REPORT_TIME,
                    SECOND_REPORT_TIME,
                    BufferOccupancyReportTest.getTimeFromLine(reader.readLine()),
                    DOUBLE_COMPARISON_DELTA);
            Assert.assertNull("Expected only two lines.", reader.readLine());
        }
    }

    @Test
    public void testDefaultReportIntervalIsUsedIfNonSpecified() throws IOException {
        // Use report without any special settings.
        Settings.init(null);
        super.setUp();
        this.report = new BufferOccupancyReport();

        // Update report after default time.
        this.clock.setTime(BufferOccupancyReport.DEFAULT_BUFFER_REPORT_INTERVAL);
        this.report.updated(this.allHosts);
        this.report.done();

        // Check line was written at correct time.
        try(BufferedReader reader = this.createBufferedReader()) {
            Assert.assertEquals(
                    UNEXPECTED_REPORT_TIME,
                    BufferOccupancyReport.DEFAULT_BUFFER_REPORT_INTERVAL,
                    BufferOccupancyReportTest.getTimeFromLine(reader.readLine()),
                    DOUBLE_COMPARISON_DELTA);
            Assert.assertNull("Expected only one line.", reader.readLine());
        }
    }

    /**
     * Gets the report class to test.
     *
     * @return The report class to test.
     */
    @Override
    protected Class getReportClass() {
        return BufferOccupancyReport.class;
    }

    /**
     * Gets the time from a line with format "TIME something else".
     * @param line The line to get the time from.
     * @return The parsed time.
     */
    private static double getTimeFromLine(String line) {
        return BufferOccupancyReportTest.parseWordFromLineAsDouble(line, TIME_INDEX);
    }

    /**
     * Gets the reported average from a line with format "something AVERAGE something else".
     * @param line The line to get the average from.
     * @return The parsed average.
     */
    private static double getAverageFromLine(String line) {
        return BufferOccupancyReportTest.parseWordFromLineAsDouble(line, AVERAGE_INDEX);
    }

    /**
     * Gets the reported variance from a line with format "something something VARIANCE something else".
     * @param line The line to get the variance from.
     * @return The parsed variance.
     */
    private static double getVarianceFromLine(String line) {
        return BufferOccupancyReportTest.parseWordFromLineAsDouble(line, VARIANCE_INDEX);
    }

    /**
     * Gets the reported minimum from a line with format "something something something MINIMUM something else".
     * @param line The line to get the minimum from.
     * @return The parsed minimum.
     */
    private static double getMinimumFromLine(String line) {
        return BufferOccupancyReportTest.parseWordFromLineAsDouble(line, MINIMUM_INDEX);
    }

    /**
     * Gets the reported maximum from a line with format "something something something something MAXIMUM".
     * @param line The line to get the maximum from.
     * @return The parsed maximum.
     */
    private static double getMaximumFromLine(String line) {
        return BufferOccupancyReportTest.parseWordFromLineAsDouble(line, MAXIMUM_INDEX);
    }

    /**
     * Reads the word with the provided index from the provided line and parses it as a double.
     * @param line Line to read the value from.
     * @param index Index of word to parse.
     * @return The parsed double.
     */
    private static double parseWordFromLineAsDouble(String line, int index) {
        return Double.parseDouble(line.split(" ")[index]);
    }
}
