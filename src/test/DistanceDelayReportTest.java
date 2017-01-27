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
import report.DistanceDelayReport;
import core.Coord;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimClock;

public class DistanceDelayReportTest extends TestCase {
	private SimClock clock;
	File outFile;

	private Vector<MessageListener> ml;
	private DistanceDelayReport r;
	private TestUtils utils;

	public void setUp() throws IOException {
		final String NS = "DistanceDelayReport.";
		TestSettings ts = new TestSettings();
		outFile = File.createTempFile("ddrtest", ".tmp");
		outFile.deleteOnExit();

		ts.putSetting(NS + "output", outFile.getAbsolutePath());
		ts.putSetting(NS + report.Report.PRECISION_SETTING, "1");
		clock = SimClock.getInstance();
		r = new DistanceDelayReport();
		ml = new Vector<MessageListener>();
		ml.add(r);
		this.utils = new TestUtils(null, ml, ts);
	}


	public void testMessageTransferred() throws IOException {
		DTNHost h1 = utils.createHost(new Coord(0,0));
		DTNHost h2 = utils.createHost(new Coord(2,0));
		DTNHost h3 = utils.createHost(new Coord(0,5));
		BufferedReader reader;

		Message m1 = new Message(h1, h2, "tst1", 1);
		h1.createNewMessage(m1);
		clock.advance(1.5);
		h1.sendMessage("tst1", h2);
		h2.messageTransferred("tst1", h1);

		Message m2 = new Message(h2,h1, "tst2", 1);
		h2.createNewMessage(m2);
		clock.advance(0.5);
		h2.sendMessage("tst2", h1);
		h1.messageTransferred("tst2", h2);

		Message m3 = new Message(h1,h3, "tst3", 1);
		h1.createNewMessage(m3);
		clock.advance(1.0);
		h1.sendMessage("tst3", h2);
		h2.messageTransferred("tst3", h1);
		h2.sendMessage("tst3", h3);
		h3.messageTransferred("tst3", h2);

		r.done();

		reader = new BufferedReader(new FileReader(outFile));

		reader.readLine(); // skip headers
		reader.readLine(); // skip headers
		assertEquals("2.0 1.5 1 tst1",reader.readLine());
		assertEquals("2.0 0.5 1 tst2",reader.readLine());
		assertEquals("5.0 1.0 2 tst3",reader.readLine());

		reader.close();
	}


}
