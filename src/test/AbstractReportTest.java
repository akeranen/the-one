package test;

import core.Group;
import core.SimClock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import report.Report;
import ui.DTNSimUI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * A base class for all tests testing reports.
 *
 * Created by Britta Heymann on 08.03.2017.
 */
public abstract class AbstractReportTest {
    protected static final int WARM_UP_TIME = 50;

    protected File outputFile;
    protected TestSettings settings;

    /**
     * Sets up a report using a temporary file to write to.
     * @throws IOException
     */
    @Before
    public void setUp() throws IOException {
        // Set locale for periods instead of commas in doubles.
        java.util.Locale.setDefault(java.util.Locale.US);

        this.outputFile = File.createTempFile("reportTest", ".tmp");

        String reportName = this.getReportClass().getSimpleName();

        settings = new TestSettings();
        settings.putSetting(DTNSimUI.NROF_REPORT_S, "1");
        settings.putSetting(Report.REPORTDIR_SETTING, "test");
        settings.putSetting("Report.report1", reportName);
        settings.setNameSpace(reportName);
        settings.putSetting(Report.OUTPUT_SETTING, outputFile.getAbsolutePath());
        this.settings.putSetting(Report.WARMUP_S, Integer.toString(WARM_UP_TIME));
        settings.restoreNameSpace();
    }

    @After
    public void deleteFile() {
        this.outputFile.delete();
    }

    @After
    public void resetGroups() {
        Group.clearGroups();
    }

    @After
    public void resetSimClock() {
        SimClock.reset();
    }

    /**
     * Checks that the report correctly handles the warm up time as set by the {@link Report#WARMUP_S} setting.
     */
    @Test
    public abstract void testReportCorrectlyHandlesWarmUpTime() throws IOException;

    /**
     * Gets the report class to test.
     * @return The report class to test.
     */
    protected abstract Class getReportClass();

    /**
     * Sets the simulator time to one second after warm up.
     */
    protected void skipWarmUpTime() {
        SimClock.getInstance().setTime(WARM_UP_TIME + 1.0);
    }

    /**
     * Create a buffered reader that assumes the output file was written using UTF8 encoding.
     * @return The buffered reader.
     * @throws IOException
     */
    protected BufferedReader createBufferedReader() throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(this.outputFile), StandardCharsets.UTF_8));
    }
}
