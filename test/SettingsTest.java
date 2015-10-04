/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package test;

import java.io.File;
import java.io.PrintWriter;

import junit.framework.TestCase;
import core.Settings;

/**
 * Tests Settings class' different setting getting methods
 */
public class SettingsTest extends TestCase {
	private static final String IRS_S = "invalidRunSetting";
	
	private static final String CSV_RS_S = "csvRunSetting";
	private static final int[] CSV_RS_V = {1,2,3,4};
	
	private static final String TST = "tstSetting";
	private static final String TST_RES = "tst";
	private static final String[] INPUT = {
		"Ns.setting1 = 1",
		"Ns.setting2 = true",
		TST + " = " + TST_RES,
		"tstSetting2 = tst2",
		"double = 1.1",
		"csvDoubles = 1.1,2.2,3.3",
		"csvInts 1,2,3",
		"booleanTrue = true",
		"booleanFalse = false",
		"int = 1",
		"runSetting = [val1 ; val2;val3; val4 ]",
		IRS_S + " = [val1 ; val2",
		CSV_RS_S + " = [" + CSV_RS_V[0]+","+CSV_RS_V[1]+";"+CSV_RS_V[2]+","+CSV_RS_V[3]+"]",
		"Ns.runSetting = [ ; ; 2; ]",
		"DefNs.runSetting = 1"
		
	};
	
	private String RS_S = "runSetting";

	
	private Settings s;
	
	protected void setUp() throws Exception {
		super.setUp();
		File tempFile = File.createTempFile("settingsTest", ".tmp");
		tempFile.deleteOnExit();
		
		PrintWriter out = new PrintWriter(tempFile);
		
		for (String s : INPUT) {
			out.println(s);
		}
		out.close();
		
		Settings.init(tempFile.getAbsolutePath());
		s = new Settings();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		Settings.setRunIndex(0);
	}


	public void testContains() {
		assertTrue(s.contains("Ns.setting1"));
		assertTrue(s.contains("Ns.setting2"));
	}

	public void testGetSetting() {
		assertEquals("1", s.getSetting("Ns.setting1"));
		assertEquals("true", s.getSetting("Ns.setting2"));
	}

	public void testGetDouble() {
		assertEquals(1.1, s.getDouble("double"));
	}

	public void testGetCsvSetting() {
		String[] csv = s.getCsvSetting("csvInts",3);
		assertEquals(csv.length, 3);
		assertEquals("1",csv[0]);
		assertEquals("2",csv[1]);
		assertEquals("3",csv[2]);
	}

	public void testGetCsvDoubles() {
		double[] csv = s.getCsvDoubles("csvDoubles",3);
		assertEquals(csv.length, 3);
		assertEquals(1.1,csv[0]);
		assertEquals(2.2,csv[1]);
		assertEquals(3.3,csv[2]);
	}
	
	public void testGetCsvDoublesUnknownAmount() {
		double[] csv = s.getCsvDoubles("csvDoubles");
		assertEquals(csv.length, 3);
		assertEquals(1.1,csv[0]);
		assertEquals(2.2,csv[1]);
		assertEquals(3.3,csv[2]);
	}

	public void testGetCsvInts() {
		int[] csv = s.getCsvInts("csvInts",3);
		assertEquals(csv.length, 3);
		assertEquals(1,csv[0]);
		assertEquals(2,csv[1]);
		assertEquals(3,csv[2]);
	}
	
	public void testGetCsvIntsUnknownAmount() {
		int[] csv = s.getCsvInts("csvInts");
		assertEquals(csv.length, 3);
		assertEquals(1,csv[0]);
		assertEquals(2,csv[1]);
		assertEquals(3,csv[2]);		
	}

	public void testGetInt() {
		assertEquals(1,s.getInt("int"));
	}

	public void testGetBoolean() {
		assertTrue(s.getBoolean("booleanTrue"));
		assertFalse(s.getBoolean("booleanFalse"));
	}

	public void testCreateIntializedObject() {
		Object o = s.createIntializedObject("movement.RandomWaypoint");
		assertTrue(o instanceof movement.RandomWaypoint);
	}


	public void testValueFillString() {
		String test = "1-%%tstSetting%%-2-%%tstSetting2%%";
		String result = s.valueFillString(test);
		assertEquals("1-tst-2-tst2",result);
		
		result = s.valueFillString("%%"+TST+"%%-aaa");
		assertEquals(TST_RES + "-aaa",result);
		
		result = s.valueFillString("%%"+TST+"%%");
		assertEquals(TST_RES,result);
	}
	
	public void testRunIndex() {
		assertEquals(s.getSetting(RS_S), "val1");
		Settings.setRunIndex(1);
		assertEquals(s.getSetting(RS_S), "val2");
		Settings.setRunIndex(2);
		assertEquals(s.getSetting(RS_S), "val3");
		Settings.setRunIndex(3);
		assertEquals(s.getSetting(RS_S), "val4");
		Settings.setRunIndex(4);
		assertEquals(s.getSetting(RS_S), "val1"); // should wrap around
		Settings.setRunIndex(5);
		assertEquals(s.getSetting(RS_S), "val2");		
	}
	
	public void testRunIndexContains() {
		assertFalse(s.contains("Ns.runSetting"));
		Settings.setRunIndex(2);
		assertTrue(s.contains("Ns.runSetting"));
	}
	
	/**
	 * Test filling empty values of run index from secondary namespace
	 */
	public void testEmptyRunIndex() {
		String rs = "runSetting";
		Settings s = new Settings("Ns");
		s.setSecondaryNamespace("DefNs");
		assertEquals(s.getInt(rs), 1);
		Settings.setRunIndex(1);
		assertEquals(s.getInt(rs), 1);
		
		Settings.setRunIndex(2);
		assertEquals(s.getInt(rs), 2); // the only defined value
		
		Settings.setRunIndex(3);
		assertEquals(s.getInt(rs), 1);
	}
	
	public void testRunIndexCSVs() {
		// test CSVs
		int [] vals = s.getCsvInts(CSV_RS_S, 2);
		assertEquals(CSV_RS_V[0],vals[0]);
		assertEquals(CSV_RS_V[1],vals[1]);

		Settings.setRunIndex(1);
		vals = s.getCsvInts(CSV_RS_S, 2);
		assertEquals(CSV_RS_V[2],vals[0]);
		assertEquals(CSV_RS_V[3],vals[1]);

		Settings.setRunIndex(2); // wrap around
		vals = s.getCsvInts(CSV_RS_S, 2);
		assertEquals(CSV_RS_V[0],vals[0]);
		assertEquals(CSV_RS_V[1],vals[1]);
	}
	
	public void testInvalidRunIndex() {
		assertEquals("[val1 ; val2",s.getSetting(IRS_S));
	}
	
	/**
	 * Tests disabled run-specific variables
	 */
	public void testNoRun() {
		Settings.setRunIndex(-1);
		assertEquals("[val1 ; val2;val3; val4 ]", s.getSetting(RS_S));
	}

}
