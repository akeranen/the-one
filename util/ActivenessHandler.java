/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package util;

import java.util.LinkedList;
import java.util.Queue;

import core.Settings;
import core.SettingsError;
import core.SimClock;

/**
 * Object of this class tell the models when a node belonging
 * to a certain group is active and when not.
 */
public class ActivenessHandler {
	
	/** 
	 * <P>Active times -setting id ({@value})</P>
	 * <P>Syntax: <CODE>start, end</CODE></P>
	 * <P>Defines the simulation time ranges when the node is active. Multiple 
	 * timer ranges can be concatenated by repeating the sequence. Time 
	 * limits must be in order and must not overlap.</P>
	 * <P>Example: 100,200,6000,6500<BR>
	 * Here node is active from 100 to 200 and from 6000 to 6500 simulated 
	 * seconds from the start of the simulation.</P> 
	 */
	public static final String ACTIVE_TIMES_S = "activeTimes";
	
	/** 
	 * <P>Active periods -setting id ({@value}).</P>
	 * <P>Syntax: <CODE>activeTime, inactiveTime</CODE></P>
	 * <P>Defines the activity and inactivity periods. </P>
	 * <P>Example: 2000,500<BR>
	 * Here node is periodically first active for 2000 seconds, followed by 500 
	 * seconds of inactiveness, and 2000 seconds of activeness, 500 seconds of
	 * inactiveness, etc.</P> 
	 */
	public static final String ACTIVE_PERIODS_S = "activePeriods";
	
	/** 
	 * <P>Active periods offset -setting id ({@value}).</P>
	 * <P>Defines how much the activity periods are offset from sim time 0.
	 * Value X means that the first period has been on for X seconds at sim
	 * time 0. Default = 0 (i.e., first active period starts at 0) </P>
	 */
	public static final String ACTIVE_PERIODS_OFFSET_S = "activePeriodsOffset";
	
	private Queue<TimeRange> activeTimes;
	private int [] activePeriods;
	private int activePeriodsOffset;
	
	private TimeRange curRange = null;
	
	public ActivenessHandler(Settings s) {
		this.activeTimes = parseActiveTimes(s);

		if (activeTimes != null) {
			this.curRange = activeTimes.poll();
		} else if (s.contains(ACTIVE_PERIODS_S)){
			this.activePeriods = s.getCsvInts(ACTIVE_PERIODS_S, 2);
			this.activePeriodsOffset = s.getInt(ACTIVE_PERIODS_OFFSET_S, 0);
		} else {
			this.activePeriods = null;
		}
	}
	
	private Queue<TimeRange> parseActiveTimes(Settings s) {
		double [] times;
		String sName = s.getFullPropertyName(ACTIVE_TIMES_S);
		
		if (s.contains(ACTIVE_TIMES_S)) {
			times = s.getCsvDoubles(ACTIVE_TIMES_S);
			if (times.length % 2 != 0) {
				throw new SettingsError("Invalid amount of values (" + 
						times.length + ") for setting " + sName + ". Must " + 
						"be divisable by 2");
			}
		}
		else {
			return null; // no setting -> always active
		}

		Queue<TimeRange> timesList = new LinkedList<TimeRange>(); 
		
		for (int i=0; i<times.length; i+= 2) {
			double start = times[i];
			double end = times[i+1];
			
			if (start > end) {
				throw new SettingsError("Start time (" + start + ") is " + 
						" bigger than end time (" + end + ") in setting " + 
						sName);
			}
			
			timesList.add(new TimeRange(start, end));
		}
		
		return timesList;
	}
	
	/**
	 * Returns true if node should be active at the moment
	 * @return true if node should be active at the moment
	 */
	public boolean isActive() {
		return isActive(0);
	}
	
	/**
	 * Returns true if node should be active after/before offset amount of 
	 * time from now
	 * @param offset The offset 
	 * @return true if node should be active, false if not
	 */
	public boolean isActive(int offset) {
		if (this.activeTimes == null) {
			if (this.activePeriods == null) {
				return true; // no inactive times nor periods -> always active		
			} else {
				/* using active periods mode */
				int timeIndex = 
					(SimClock.getIntTime() + this.activePeriodsOffset + offset)% 
					(this.activePeriods[0] + this.activePeriods[1]);
				if (timeIndex <= this.activePeriods[0]) {
					return true;
				} else {
					return false;
				}
			}
		}
		
		if (curRange == null) {
			return false; // out of active times
		}
		
		double time = SimClock.getTime() + offset;
		
		if (this.curRange.isOut(time)) { // time for the next time range
			this.curRange = activeTimes.poll();
			if (curRange == null) {
				return false; // out of active times
			}
		}
		
		return curRange.isInRange(time);
	}

	/**
	 * Class for handling time ranges
	 */
	private class TimeRange {
		private double start;
		private double end;
		
		/**
		 * Constructor.
		 * @param start The start time
		 * @param end The end time
		 */
		public TimeRange(double start, double end) {
			this.start = start;
			this.end = end;
		}
		
		/**
		 * Returns true if the given time is within start and end time 
		 * (inclusive).
		 * @param time The time to check
		 * @return true if the time is within limits
		 */
		public boolean isInRange(double time) {
			if (time < start || time > end ) {
				return false; // out of range
			}
			return true;			
		}
		
		/**
		 * Returns true if given time is bigger than end the end time
		 * @param time The time to check
		 * @return true if given time is bigger than end 
		 */
		public boolean isOut(double time) {
			return time > end;
		}
	}
}
