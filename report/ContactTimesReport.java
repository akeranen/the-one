/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.HashMap;
import java.util.Vector;

import core.ConnectionListener;
import core.DTNHost;
import core.Settings;

/**
 * Reports the node contact time (i.e., how long they were in the range
 * of each other) distribution. Report file contains the count of connections
 * that lasted for certain amount of time. Syntax:<br>
 * <code>time nrofContacts</code>
 */
public class ContactTimesReport extends Report implements ConnectionListener {
	protected HashMap<ConnectionInfo, ConnectionInfo> connections;
	private Vector<Integer> nrofContacts;
	
	/** Granularity -setting id ({@value}). Defines how many simulated seconds
	 * are grouped in one reported interval. */
	public static final String GRANULARITY = "granularity";
	/** How many seconds are grouped in one group */
	protected double granularity;
	
	/**
	 * Constructor.
	 */
	public ContactTimesReport() {
		Settings settings = getSettings();
		if (settings.contains(GRANULARITY)) {
			this.granularity = settings.getDouble(GRANULARITY);
		}
		else {
			this.granularity = 1.0;
		}

		init();
	}
	
	@Override
	protected void init() {
		super.init();
		this.connections = new HashMap<ConnectionInfo,ConnectionInfo>();
		this.nrofContacts = new Vector<Integer>();
	}
		
	public void hostsConnected(DTNHost host1, DTNHost host2) {
		if (isWarmup()) {
			return;
		}
		addConnection(host1, host2);
	}

	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		newEvent();
		ConnectionInfo ci = removeConnection(host1, host2);
		
		if (ci == null) {
			return; /* the connection was started during the warm up period */
		}
		
		ci.connectionEnd();
		increaseTimeCount(ci.getConnectionTime());
	}
	
	protected void addConnection(DTNHost host1, DTNHost host2) {
		ConnectionInfo ci = new ConnectionInfo(host1, host2);
		
		assert !connections.containsKey(ci) : "Already contained "+
			" a connection of " + host1 + " and " + host2;
		
		connections.put(ci,ci);
	}
	
	protected ConnectionInfo removeConnection(DTNHost host1, DTNHost host2) {
		ConnectionInfo ci = new ConnectionInfo(host1, host2);
		ci = connections.remove(ci);
		return ci;
	}
		
	/**
	 * Increases the amount of times a certain time value has been seen.
	 * @param time The time value that was seen
	 */
	protected void increaseTimeCount(double time) {
		int index = (int)(time/this.granularity);
		
		if (index >= this.nrofContacts.size()) {
			/* if biggest index so far, fill array with nulls up to 
			  index+2 to keep the last time count always zero */
			this.nrofContacts.setSize(index + 2);
		}
		
		Integer curValue = this.nrofContacts.get(index); 
		if (curValue == null) { // no value found -> put the first
			this.nrofContacts.set(index, 1); 
		}
		else { // value found -> increase the number by one
			this.nrofContacts.set(index, curValue+1);
		}
	}
	
	@Override
	public void done() {
		
		for (int i=0, n=this.nrofContacts.size(); i<n; i++) {
			Integer contacts = nrofContacts.get(i); 
			if (contacts == null) {
				contacts = 0;
			}
			//     time(interval)             nrofContacts
			write((i*this.granularity) + " " + contacts);
		}
		
		super.done();
	}

	
	/**
	 * Objects of this class store time information about contacts.
	 */
	protected class ConnectionInfo {
		private double startTime;
		private double endTime;
		private DTNHost h1;
		private DTNHost h2;
		
		public ConnectionInfo (DTNHost h1, DTNHost h2){
			this.h1 = h1;
			this.h2 = h2;
			this.startTime = getSimTime();
			this.endTime = -1;
		}
		
		/**
		 * Should be called when the connection ended to record the time.
		 * Otherwise {@link #getConnectionTime()} will use end time as
		 * the time of the request.
		 */
		public void connectionEnd() {
			this.endTime = getSimTime();
		}
		
		/**
		 * Returns the time that passed between creation of this info 
		 * and call to {@link #connectionEnd()}. Unless connectionEnd() is 
		 * called, the difference between start time and current sim time
		 * is returned.
		 * @return The amount of simulated seconds passed between creation of
		 * this info and calling connectionEnd()
		 */
		public double getConnectionTime() {
			if (this.endTime == -1) {
				return getSimTime() - this.startTime;
			}			
			else {
				return this.endTime - this.startTime;
			}
		}
		
		/**
		 * Returns true if the other connection info contains the same hosts. 
		 */
		public boolean equals(Object other) {
			if (!(other instanceof ConnectionInfo)) {
				return false;
			}
			
			ConnectionInfo ci = (ConnectionInfo)other;

			if ((h1 == ci.h1 && h2 == ci.h2)) {
				return true;
			}
			else if ((h1 == ci.h2 && h2 == ci.h1)) {
				// bidirectional connection the other way
				return true;
			}
			return false;
		}
		
		/**
		 * Returns the same hash for ConnectionInfos that have the
		 * same two hosts.
		 * @return Hash code
		 */
		public int hashCode() {
			String hostString;

			if (this.h1.compareTo(this.h2) < 0) {
				hostString = h1.toString() + "-" + h2.toString();
			}
			else {
				hostString = h2.toString() + "-" + h1.toString();
			}
			
			return hostString.hashCode();
		}
		
		/**
		 * Returns a string representation of the info object
		 * @return a string representation of the info object
		 */
		public String toString() {
			return this.h1 + "<->" + this.h2 + " [" + this.startTime
				+"-"+ (this.endTime >0 ? this.endTime : "n/a") + "]";
		}
	}
}
