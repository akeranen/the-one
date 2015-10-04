/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

import junit.framework.TestCase;
import report.Report;
import report.TotalContactTimeReport;
import core.ConnectionListener;
import core.Coord;
import core.DTNHost;
import core.SimClock;

public class TotalContactTimeReportTest extends TestCase {
	private BufferedReader ctReader;
	private File outFile;
	private SimClock clock;
	private TotalContactTimeReport ctr;
	
	private DTNHost h1, h2, h3;
	private Coord c1 = new Coord(0,0);
	private Coord c2 = new Coord(1,0);
	private Coord c3 = new Coord(2,0);
	private Coord away = new Coord(1000,1000);
	
	private final String SET_PREFIX = "TotalContactTimeReport.";
	
	protected void setUp() throws Exception {
		super.setUp();
		SimClock.reset();	
		outFile = File.createTempFile("cttest", ".tmp");
		outFile.deleteOnExit();

		TestSettings ts = new TestSettings();
		ts.putSetting(SET_PREFIX + 
				report.Report.PRECISION_SETTING, "1"); // drop precision to 1
		ts.putSetting(SET_PREFIX + report.ContactTimesReport.GRANULARITY, "5");
		
		ts.putSetting(SET_PREFIX + Report.OUTPUT_SETTING,
				outFile.getAbsolutePath());

		clock = SimClock.getInstance();
		ctr = new TotalContactTimeReport();
		
		Vector<ConnectionListener> cl = new Vector<ConnectionListener>();
		cl.add(ctr);
		TestUtils utils = new TestUtils(cl, null, ts);
		
		utils.setTransmitRange(3); // make sure everyone can connect
		h1 = utils.createHost(c1);
		h2 = utils.createHost(c2);
		h3 = utils.createHost(c3);
	}
	
	private void done() throws Exception {
		ctr.done();
		ctReader = new BufferedReader(new FileReader(outFile));
	}
	
	public void testReport() throws Exception {
		clock.advance(5);
		h1.connect(h2);
		clock.advance(10);
		disc(h2);
		ctr.updated(null);
		h1.connect(h2);
		clock.advance(1);
		ctr.updated(null); // less time than granularity has passed -> suppress
		clock.advance(4);
		ctr.updated(null); // now should report
		
		checkValues(new String[] {"15.0 10.0", "20.0 15.0"});
	}
	
	public void testMultipleTimes() throws Exception {
		clock.advance(10);
		h1.connect(h2);
		clock.advance(10);
		disc(h2);
		ctr.updated(null);
		h2.connect(h3);
		clock.advance(5);
		ctr.updated(null);
		
		checkValues(new String[] {"20.0 10.0", "25.0 15.0"});
	}
	
	public void testOverlappingTimes() throws Exception {
		clock.advance(5);
		h1.connect(h2);
		clock.advance(5);
		ctr.updated(null); // h1-h2 connected for 5s -> @10: 5s
		h2.connect(h3);
		clock.advance(10);
		ctr.updated(null); // h1-h2 for 15s and h2-h3 for 10s -> @20: 25s
		h1.setLocation(away);
		h1.update(true);  // h1-h2 disconnected
		clock.advance(10);
		disc(h3); // h2-h3 connected for 20s + h1-h2 15s -> @30: 35s
		ctr.updated(null);
		clock.advance(10);
		ctr.updated(null); // no more active connections -> should suppress this
		
		h2.connect(h3);
		clock.advance(5);
		ctr.updated(null); // h2-h3 5s -> @45: 40s
		
		checkValues(new String[] {"10.0 5.0", "20.0 25.0", "30.0 35.0",
				"45.0 40.0"});
	}

	private void disc(DTNHost host) {
		Coord loc = host.getLocation();
		host.setLocation(away);
		host.update(true);
		host.setLocation(loc);
	}
	
	private void checkValues(String[] values) throws Exception {
		done();
		// read header line away
		assertTrue(ctReader.ready());
		assertEquals(TotalContactTimeReport.HEADER, ctReader.readLine());
		
		for (String value : values) {
			assertEquals(value,ctReader.readLine());	
		}
		assertEquals(null,ctReader.readLine()); // no more times left
	
	}

}
