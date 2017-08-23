/*
 * Copyright 2017 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 * Written by Renan Greca as an extension to ONE.
 */

package report;

import java.util.HashMap;
import java.util.List;

import core.ConnectionListener;
import core.Coord;
import core.DTNHost;

/**
 * Adjency snapshot report. Reports all pairs of nodes with active connections
 * every configurable-amount-of-seconds (see {@link SnapshotReport#GRANULARITY}).
 * Based on SnapshotReport, AdjencyGraphvizReport and LocationSnapshotReport.
 */
public class AdjencySnapshotReport extends SnapshotReport implements ConnectionListener {

	private String HOST_DELIM = "<->"; // used in toString()
	private HashMap<String, ConnectionInfo> cons;

	/**
	 * Constructor.
	 */
	public AdjencySnapshotReport() {
		// this.allHosts = null;
		init();
	}

	protected void init() {
		super.init();
		this.cons = new HashMap<String, ConnectionInfo>();
	}

	// Adds a connection to the list.
	public void hostsConnected(DTNHost host1, DTNHost host2) {
		if (isWarmup()) {
			return;
		}

		newEvent();
		ConnectionInfo ci = cons.get(host1+HOST_DELIM+host2);

		if (ci == null) {
			cons.put(host1+HOST_DELIM+host2, new ConnectionInfo(host1,host2));
		}
		else {
			ci.nrofConnections++;
		}
	}

	// Removes a connection from the list.
	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		if (isWarmup()) {
			return;
		}

		newEvent();
		cons.remove(host1+HOST_DELIM+host2);
	}

	// Do nothing. We're interested in the edges, not the nodes.
	@Override 
	protected void writeSnapshot(DTNHost host) {}

	// Outputs the list of connections at each timestamp.
	@Override
	protected void createSnapshot(List<DTNHost> hosts) {
		write ("[" + (int)getSimTime() + "]");

		for (ConnectionInfo ci : cons.values()) {
			write(ci.h1 + " " + ci.h2);
		}
	}

	/**
	 * Private class stores information of the connected hosts
	 * and nrof times they have connected.
	 */
	private class ConnectionInfo {
		private DTNHost h1;
		private DTNHost h2;
		private int nrofConnections;

		public ConnectionInfo(DTNHost h1, DTNHost h2) {
			this.h1 = h1;
			this.h2 = h2;
			this.nrofConnections = 1;
		}

		public boolean equals(Object o) {
			if (o == null) return false;
			return o.toString().equals(this.toString());
		}

		public int hashCode() {
			return toString().hashCode();
		}

		public String toString() {
			return h1+HOST_DELIM+h2;
		}

		public int compareTo(Object o) {
			return nrofConnections - ((ConnectionInfo)o).nrofConnections;
		}
	}

}
