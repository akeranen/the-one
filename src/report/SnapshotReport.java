/*
 * Copyright 2016 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import java.util.HashSet;
import java.util.List;

import core.DTNHost;
import core.Settings;
import core.UpdateListener;

/**
 * Node snapshot report superclass. Reports some characteristic of all 
 * (or only some, see {@link #REPORTED_NODES}) nodes every 
 * configurable-amount-of seconds (see {@link #GRANULARITY}).
 */
public abstract class SnapshotReport extends Report implements UpdateListener {
	/** Reporting granularity -setting id ({@value}).
	 * Defines the interval how often (seconds) a new snapshot is created */
	public static final String GRANULARITY = "granularity";
	/** Optional reported nodes (comma separated list of network addresses).
	 * By default all nodes are reported. */
	public static final String REPORTED_NODES = "nodes";
	/** value of the granularity setting */
	protected final int granularity;
	/** time of last update*/
	protected double lastUpdate;
	/** Networks addresses (integers) of the nodes which are reported */
	protected HashSet<Integer> reportedNodes;

	/**
	 * Constructor. Reads the settings and initializes the report module.
	 */
	public SnapshotReport() {
		Settings settings = getSettings();
		this.lastUpdate = 0;
		this.granularity = settings.getInt(GRANULARITY);

		if (settings.contains(REPORTED_NODES)) {
			this.reportedNodes = new HashSet<Integer>();
			for (Integer nodeId : settings.getCsvInts(REPORTED_NODES)) {
				this.reportedNodes.add(nodeId);
			}
		}
		else {
			this.reportedNodes = null;
		}

		init();
	}

	/**
	 * Initiates a new snapshot if "granularity" seconds have passed since the 
	 * last snapshot.
	 * @param hosts All the hosts in the world
	 */
	public void updated(List<DTNHost> hosts) {
		double simTime = getSimTime();
		if (isWarmup()) {
			return; /* warmup period is on */
		}
		/* one snapshot once every granularity seconds */
		if (simTime - lastUpdate >= granularity) {
			createSnapshot(hosts);
			this.lastUpdate = simTime - simTime % granularity;
		}
	}
	
	
	/**
	 * Writes the snapshot information of one single host
	 * @param host
	 */
	abstract protected void writeSnapshot(DTNHost host);

	/**
	 * Creates a snapshot of all hosts by writing time stamp and calling 
	 * {@link #writeSnapshot(DTNHost)} for each host that is in the list of
	 * requested nodes
	 * @param hosts The list of hosts in the world
	 */
	protected void createSnapshot(List<DTNHost> hosts) {
		write ("[" + (int)getSimTime() + "]"); /* simulation time stamp */
		for (DTNHost h : hosts) {
			if (this.reportedNodes != null &&
				!this.reportedNodes.contains(h.getAddress())) {
				continue; /* node not in the list */
			}
			writeSnapshot(h);
		}
	}

}
