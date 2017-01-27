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
import report.MessageGraphvizReport;
import core.Coord;
import core.DTNHost;
import core.Message;
import core.MessageListener;

public class MessageGraphvizReportTest extends TestCase {
	private File outFile;
	private MessageGraphvizReport r;
	private TestUtils utils;

	public void setUp() throws IOException {
		TestSettings ts = new TestSettings();
		outFile = File.createTempFile("mgtest", ".tmp");
		outFile.deleteOnExit();

		ts.putSetting("MessageGraphvizReport.output", outFile.getAbsolutePath());
		ts.putSetting("MessageGraphvizReport.interval" , "");

		Vector<MessageListener> ml = new Vector<MessageListener>();
		r = new MessageGraphvizReport();
		ml.add(r);
		utils = new TestUtils(null, ml, ts);
	}

	private void generateMessages() {
		Coord c1 = new Coord(0,0);
		Coord c2 = new Coord(1,0);
		Coord c3 = new Coord(2,0);

		utils.setTransmitRange(2);
		DTNHost h1 = utils.createHost(c1,"h1");
		DTNHost h2 = utils.createHost(c2,"h2");
		DTNHost h3 = utils.createHost(c3,"h3");

		h1.createNewMessage(new Message(h1, h3, "M1", 1));
		h1.sendMessage("M1", h2);
		h2.messageTransferred("M1", h1);
		h2.sendMessage("M1", h3);
		h3.messageTransferred("M1", h2);
		h3.createNewMessage(new Message(h3, h2, "M2", 1));
		h3.sendMessage("M2", h2);
		h2.messageTransferred("M2", h3);
	}

	public void testDone() throws IOException{
		BufferedReader reader;

		generateMessages();
		r.done();

		reader = new BufferedReader(new FileReader(outFile));
		reader.readLine(); // read comment lines
		reader.readLine(); // read comment lines
		assertEquals("digraph " + MessageGraphvizReport.GRAPH_NAME +
				" {", reader.readLine());
		assertEquals("\th1->h2->h3;",reader.readLine());
		assertEquals("\th3->h2;",reader.readLine());
		assertEquals("}",reader.readLine());

		reader.close();
	}

}
