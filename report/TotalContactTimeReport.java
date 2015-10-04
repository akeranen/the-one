/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.List;

import core.DTNHost;
import core.UpdateListener;

/**
 * Report for total amount of contact times among hosts. Reports how long all
 * nodes have been in contact with some other node. Supports 
 * {@link ContactTimesReport#GRANULARITY} setting. If update interval is
 * smaller than 1.0 seconds, time stamps may start to drift. Reported values
 * still correspond to reported times. Connections that started during the
 * warmup period are ignored.
 */
public class TotalContactTimeReport extends ContactTimesReport implements
		UpdateListener {
	
	/** The header of every report file */
	public static final String HEADER = "# time totalContactTime";
	/** cumulative contact times of all disconnected contacts */
	private double oldContactTimes;
	/** sim time of last report writing */
	private double lastWrite;
	/** last reported time count (to suppress duplicates) */
	private double lastReportedTime;
	
	public void init() {
		super.init();
		write(HEADER);
		this.oldContactTimes = 0;
		this.lastReportedTime = 0;
		this.lastWrite = getSimTime();
	}

	@Override
	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		newEvent();
		ConnectionInfo ci = removeConnection(host1, host2);
		
		if (ci == null) {
			return; // connection started during the warm up period
		}
		
		oldContactTimes += ci.getConnectionTime();		
	}

	/**
	 * Reports total contact time if more time than defined with setting
	 * {@link ContactTimesReport#GRANULARITY} has passed. Method is called
	 * on every update cycle.
	 */
	public void updated(List<DTNHost> hosts) {
		double simTime = getSimTime();
		if (simTime - lastWrite < granularity || isWarmup()) {
			return; // shouldn't report yet
		}
		lastWrite = simTime;
		
		// count also the times for connections that are still up
		double othersTime = 0;
		for (ConnectionInfo oth : this.connections.values()) {
			othersTime += oth.getConnectionTime();
		}
		
		double totalTime = oldContactTimes + othersTime;
		
		if (lastReportedTime == totalTime) {
			return; // don't report duplicate times
		}
		
		write(format(simTime) + " " + format(totalTime));
		lastReportedTime = totalTime;
	}
}
