/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import input.BinaryEventsReader;
import input.ExternalEvent;
import input.ExternalEventsQueue;
import input.ExternalEventsReader;
import input.MessageCreateEvent;
import input.StandardEventsReader;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import junit.framework.TestCase;

public class ExternalEventsQueueTest extends TestCase {
	private final String[] stdinput = {
"1000.000	C	MSG_365_D_1	p1	p2	100000",
"1533.405	S	MSG_365_D_1	p1	p0",
"1542.000	A	MSG_365_D_1	p1	p0",
"2200.000	C	MSG_746_D_2	p1	p3	100000",
"3095.408	S	MSG_746_D_2	p1	c64",
"3103.000	A	MSG_746_D_2	p1	c64",
"8071.608	DE	MSG_746_D_2	p1	p10",
" ", // empty line
"8091.608	DE	MSG_365_D_1	p1	p10",
"100502.200	DR	MSG_365_D_1	p10",
"# comment line",
"106202.613	R	MSG_10644_D_5	c70"
};

	private final double msgTimes[] = {1000.000, 1533.405, 1542.000,
			2200.000, 3095.408, 3103.000, 8071.608, 8091.608,
			100502.200, 106202.613};

	private ExternalEventsQueue eeq;
	private File tempFile;

	protected void setUp() throws Exception {
		java.util.Locale.setDefault(java.util.Locale.US);
		super.setUp();
		String TMP = ".tmp";
		tempFile = File.createTempFile("eeqTest", TMP);

		PrintWriter out = new PrintWriter(tempFile);

		for (String s : stdinput) {
			out.println(s);
		}
		out.close();
	}


	public void testEEQ() {
		int preload = 10;
		eeq = new ExternalEventsQueue(tempFile.getAbsolutePath(),preload);
		checkEeq(eeq, preload);

		preload = 1;
		eeq = new ExternalEventsQueue(tempFile.getAbsolutePath(),preload);
		checkEeq(eeq, preload);
	}


	public void testBinaryEEQ() throws Exception{
		int preload = 7;
		File tmpBinFile = File.createTempFile("TempBinTest",
				BinaryEventsReader.BINARY_EXT);
		String binFileName = tmpBinFile.getAbsolutePath();
		ExternalEventsReader r = new StandardEventsReader(tempFile);
		List<ExternalEvent> events = r.readEvents(100);
		BinaryEventsReader.storeToBinaryFile(binFileName, events);

		eeq = new ExternalEventsQueue(binFileName, preload);
		checkEeq(eeq, preload);

		assertTrue(tmpBinFile.delete()); // make sure all locks are gone
	}


	private void checkEeq(ExternalEventsQueue eeq, int preloadVal) {
		ExternalEvent ee;
		assertEquals(msgTimes[0],eeq.nextEventsTime());
		assertEquals(preloadVal, eeq.eventsLeftInBuffer());

		ee = eeq.nextEvent();
		assertTrue(ee instanceof MessageCreateEvent);

		for (int i=1; i < msgTimes.length; i++) {
			assertEquals(msgTimes[i], eeq.nextEventsTime());
			ee = eeq.nextEvent();
			assertTrue(ee instanceof ExternalEvent);
			assertEquals(msgTimes[i], ee.getTime());
		}
	}
}
