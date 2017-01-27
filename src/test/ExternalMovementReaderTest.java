/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import input.ExternalMovementReader;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import util.Tuple;

import junit.framework.TestCase;
import core.Coord;

public class ExternalMovementReaderTest extends TestCase {
	private ExternalMovementReader r;
	private static final String INPUT =
		"0 0 0 0 0 0\n"+
		"10 1 10 10\n"+
		"10 2 10 20 1010\n" +
		"10 3 10 30\n" +
		"20 1 20 10 dummyData\n" +
		"20 2 20 20\n" +
		"\n"+
		"20 3 30 30\n" +
		"30 1 30 20\n" +
		"30 2 30 30\n" +
		"30 3 40 30";
	private static final String [] ids = {"1","2","3"};
	private static final double [] times = {10,20,30};
	private static final Coord [][] coords =
		{ {new Coord(10,10), new Coord(10,20), new Coord(10,30)},
		  {new Coord(20,10), new Coord(20,20), new Coord(30,30)},
		  {new Coord(30,20), new Coord(30,30), new Coord(40,30)} };


	protected void setUp() throws Exception {
		super.setUp();

		File tmpFile = File.createTempFile("EMRTest","tmp");
		tmpFile.deleteOnExit();

		PrintWriter pw = new PrintWriter(tmpFile);
		pw.println(INPUT);
		pw.close();

		r = new ExternalMovementReader(tmpFile.getAbsolutePath());
	}

	public void testReader() {
		List<Tuple<String, Coord>> list;

		for (int i=0; i<times.length; i++) {
			list = r.readNextMovements();
			checkTuples(list, ids, coords[i]);
			assertEquals(times[i], r.getLastTimeStamp());
		}

		list = r.readNextMovements();
		assertEquals(0, list.size());
	}

	private void checkTuples(List<Tuple<String, Coord>> list, String[] ids,
			Coord[] coords) {

		assertEquals(ids.length, list.size());

		for (int i=0; i<ids.length; i++) {
			assertEquals(ids[i], list.get(i).getKey());
			assertEquals(coords[i], list.get(i).getValue());
		}

	}
}
