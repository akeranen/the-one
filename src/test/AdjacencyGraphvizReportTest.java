/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import junit.framework.TestCase;
import report.AdjacencyGraphvizReport;
import core.ConnectionListener;
import core.Coord;
import core.DTNHost;

public class AdjacencyGraphvizReportTest extends TestCase {
	private File outFile;
	private TestUtils utils;
	private AdjacencyGraphvizReport r;

	private static final int NROF = 3;

	public void setUp() throws IOException {
		TestSettings ts = new TestSettings();
		outFile = File.createTempFile("adjgvtest", ".tmp");
		outFile.deleteOnExit();

		ts.putSetting("AdjacencyGraphvizReport.output", outFile.getAbsolutePath());
		ts.putSetting("AdjacencyGraphvizReport.interval" , "");
		r = new AdjacencyGraphvizReport();
		Vector<ConnectionListener> cl = new Vector<ConnectionListener>();
		cl.add(r);

		utils = new TestUtils(cl, null, ts);
	}

	private void generateConnections() {
		Coord c1 = new Coord(0,0);
		Coord c2 = new Coord(1,0);
		Coord c3 = new Coord(2,0);
		Coord c4 = new Coord(0,2);

		utils.setTransmitRange(2);
		DTNHost h1 = utils.createHost(c1,"h1");
		DTNHost h2 = utils.createHost(c2,"h2");
		DTNHost h3 = utils.createHost(c3,"h3");
		DTNHost h4 = utils.createHost(c4,"h4");

		h1.connect(h2);
		h2.connect(h3);
		// h1--h2--h3

		h2.setLocation(new Coord(1,10));
		h2.update(true);
		//       h2
		// h1          h3

		h2.setLocation(c2);
		h2.connect(h3); // reconnect
		// h1  h2--h3

		h1.connect(h4);

		c4.translate(-5, 0);
		h1.update(true); // disconnect h1-h4
		c4.translate(5, 0);
		h1.connect(h4); // reconnect h1-h4
	}

	public void testDone() throws IOException {
		BufferedReader reader;
		List<String> lines = new ArrayList<String>();

		generateConnections();
		r.done();

		reader = new BufferedReader(new FileReader(outFile));

		// check the first line of output
		assertEquals("graph " + AdjacencyGraphvizReport.GRAPH_NAME +
				" {", reader.readLine());

		for (int i=0; i<NROF; i++) {
			lines.add(reader.readLine());
		}

		// check end out output
		assertEquals("}",reader.readLine());

		// sort the result lines because their ordering is not deterministic
		Collections.sort(lines);

		assertEquals("\th1--h2 [weight=1];",lines.get(0));
		assertEquals("\th1--h4 [weight=2];",lines.get(1));
		assertEquals("\th2--h3 [weight=2];",lines.get(2));

		reader.close();
	}

}
