/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import junit.framework.TestCase;
import report.ContactTimesReport;
import report.InterContactTimesReport;
import report.Report;
import core.ConnectionListener;
import core.Coord;
import core.DTNHost;
import core.SimClock;

/**
 * Test cases for ContactTimes & InterContactTimesReports.
 */
public class ContactTimesReportTest extends TestCase {
	private BufferedReader ctReader;
	private BufferedReader ictReader;

	private SimClock clock;
	private final static String SET_PREFIX = "ContactTimesReport.";
	private final static String I_SET_PREFIX = "InterContactTimesReport.";

	private void setUpWithGranularity(double gran) throws IOException {
		ContactTimesReport ctr;
		InterContactTimesReport ictr;
		TestSettings ts = new TestSettings();

		File outFile = File.createTempFile("cttest", ".tmp");
		File iOutFile = File.createTempFile("icttest", ".tmp");
		outFile.deleteOnExit();
		iOutFile.deleteOnExit();

		ts.putSetting(SET_PREFIX + Report.OUTPUT_SETTING,
				outFile.getAbsolutePath());
		ts.putSetting(SET_PREFIX + Report.INTERVAL_SETTING , "");
		ts.putSetting(SET_PREFIX + ContactTimesReport.GRANULARITY, gran+"");

		ts.putSetting(I_SET_PREFIX + Report.OUTPUT_SETTING,
				iOutFile.getAbsolutePath());
		ts.putSetting(I_SET_PREFIX + Report.INTERVAL_SETTING , "");
		ts.putSetting(I_SET_PREFIX + ContactTimesReport.GRANULARITY, gran+"");

		clock = SimClock.getInstance();

		ctr = new ContactTimesReport();
		ictr = new InterContactTimesReport();

		Vector<ConnectionListener> cl = new Vector<ConnectionListener>();
		cl.add(ctr);
		cl.add(ictr);

		TestUtils utils = new TestUtils(cl, null, ts);
		generateConnections(utils);
		ctr.done();
		ictr.done();
		ctReader = new BufferedReader(new FileReader(outFile));
		ictReader = new BufferedReader(new FileReader(iOutFile));

	}

	private void generateConnections(TestUtils utils) {
		Coord c1 = new Coord(0,0);
		Coord c2 = new Coord(1,0);
		Coord c3 = new Coord(2,0);

		utils.setTransmitRange(3); // make sure everyone can connect
		DTNHost h1 = utils.createHost(c1);
		DTNHost h2 = utils.createHost(c2);
		DTNHost h3 = utils.createHost(c3);

		h1.connect(h2);

		clock.advance(1.0);
		h2.connect(h3);

		clock.advance(2.0);
		h2.setLocation(new Coord(10,10));
		h1.update(true); // disconnect h1-h2
		h3.update(true); // disconnect h2-h3 (from h3)

		clock.advance(3.0);
		h2.setLocation(c2);
		h3.connect(h2); // reconnect the other way
		h2.setLocation(new Coord(10,10));
		clock.advance(3.5);
		h3.update(true); // disconnect

		clock.advance(10);
		h3.setLocation(c2);
		h3.connect(h1); // con h3--h1
		clock.advance(6);
		h1.setLocation(new Coord(-10,0));
		h1.update(true); // disconnect 6 sec connection
	}

	public void testReport() throws IOException {
		String[] ctValues = {"0.0 0", "1.0 0", "2.0 1", "3.0 2", "4.0 0",
				"5.0 0", "6.0 1", "7.0 0"};
		String[] ictValues = {"0.0 0", "1.0 0", "2.0 0", "3.0 1",
				"4.0 0"};

		this.setUpWithGranularity(1.0);
		checkValues(ctValues, ictValues);
	}

	private void checkValues(String[] ctValues, String[] ictValues)
		throws IOException {
		for (String value : ctValues) {
			assertEquals(value,ctReader.readLine());
		}
		assertEquals(null,ctReader.readLine()); // no more times left

		for (String value : ictValues) {
			assertEquals(value,ictReader.readLine());
		}
		assertEquals(null,ictReader.readLine());
	}

	public void testGranularity2() throws IOException {
		String[] ctValues = {"0.0 0", "2.0 3", "4.0 0",
				"6.0 1", "8.0 0"};
		String[] ictValues = {"0.0 0", "2.0 1", "4.0 0"};

		this.setUpWithGranularity(2.0);
		checkValues(ctValues, ictValues);
	}


	public void testGranularity10() throws IOException {
		this.setUpWithGranularity(10.0);

		assertEquals("0.0 4",ctReader.readLine());
		assertEquals("10.0 0",ctReader.readLine());
		assertEquals(null,ctReader.readLine());
	}

	public void testGanularity05() throws IOException {
		String[] ctValues = {"0.0 0", "0.5 0", "1.0 0", "1.5 0",
				"2.0 1", "2.5 0", "3.0 1", "3.5 1", "4.0 0",
				"4.5 0", "5.0 0", "5.5 0", "6.0 1", "6.5 0"};
		String[] ictValues = {"0.0 0", "0.5 0", "1.0 0", "1.5 0",
				"2.0 0", "2.5 0", "3.0 1", "3.5 0"};

		this.setUpWithGranularity(0.5);
		checkValues(ctValues, ictValues);
	}

}
