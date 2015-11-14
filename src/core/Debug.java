/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

import java.io.PrintStream;

/**
 * Debugging info printer with time stamping. This class is not to be actively
 * used but convenient for temporary debugging.
 */
public class Debug {
	private static PrintStream out = System.out;
	private static int debugLevel = 0;
	private static long timingStart = -1;
	private static String timingCause;

	/**
	 * Sets the current debug level (smaller level -> more messages)
	 * @param level The level to set
	 */
	public void setDebugLevel(int level) {
		debugLevel = level;
	}

	/**
	 * Sets print stream of debug output.
	 * @param outStrm The stream
	 */
	public void setPrintStream(PrintStream outStrm) {
		out = outStrm;
	}

	/**
	 * Prints text to output with level 0
	 * @param txt text to print
	 */
	public static void p(String txt) {
		p(txt, 0, false);
	}

	/**
	 * Prints text to output given with level
	 * @param level The debug level
	 * @param txt text to print
	 */
	public static void p(String txt, int level) {
		p(txt,level, false);
	}


	/**
	 * Debug print with a timestamp
	 * @param txt Text to print
	 * @param level Debug level
	 */
	public static void pt(String txt, int level) {
		p(txt,level,true);
	}

	/**
	 * Debug print with a timestamp and 0 level
	 * @param txt Text to print
	 */
	public static void pt(String txt) {
		p(txt,0,true);
	}

	/**
	 * Print text to debug output.
	 * @param txt The text to print
	 * @param level The debug level (only messages with level >= debugLevel
	 * are printed)
	 * @param timestamp If true, text is (sim)timestamped
	 */
	public static void p(String txt, int level, boolean timestamp) {
		String time = "";
		int simTime = SimClock.getIntTime();
		if (level < debugLevel) {
			return;
		}

		if (timestamp) {
			time = "[@"+simTime+"]";
		}
		out.println("D" + time + ": " + txt);
	}

	/**
	 * Start timing an action.
	 * @see #doneTiming()
	 */
	public static void startTiming(String cause) {
		if (timingStart != -1) {
			doneTiming();
		}
		timingCause = cause;
		timingStart = System.currentTimeMillis();
	}

	/**
	 * End timing an action. Information how long the action took is
	 * printed to debug stream.
	 * @see #startTiming(String)
	 */
	public static void doneTiming() {
		long end = System.currentTimeMillis();
		long diff = end-timingStart;
		if (end-timingStart > 0)
			pt(timingCause + " took "+ diff/1000.0 + "s" );

		timingStart = -1;
	}
}
