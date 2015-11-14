/*
 * Copyright 2011 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SettingsError;

/**
 * Reports which messages are available (either in the buffer or at one
 * of the connected hosts' buffer) for certain, randomly selected,
 * tracked hosts. Supports the same settings as the
 * {@link MessageLocationReport}
 */
public class MessageAvailabilityReport extends MessageLocationReport {

	/** Number of tracked hosts -setting id ({@value}). Defines how many
	 * hosts are selected for sampling message availability */
	public static final String NROF_HOSTS_S = "nrofHosts";

	private int nrofHosts;
	private Set<DTNHost> trackedHosts;
	private Random rng;

	public MessageAvailabilityReport() {
		super();
		Settings s = getSettings();
		nrofHosts = s.getInt(NROF_HOSTS_S, -1);
		this.rng = new Random(nrofHosts);

		this.trackedHosts = null;
	}

	/**
	 * Randomly selects the hosts to track
	 * @param allHosts All hosts in the scenario
	 * @return The set of tracked hosts
	 */
	private Set<DTNHost> selectTrackedHosts(List<DTNHost> allHosts) {
		Set<DTNHost> trackedHosts = new HashSet<DTNHost>();

		if (this.nrofHosts > allHosts.size()) {
			throw new SettingsError("Can't use more hosts than there are " +
					"in the simulation scenario");
		}


		for (int i=0; i<nrofHosts; i++) {
			DTNHost nextHost = allHosts.get(rng.nextInt(allHosts.size()));
			if (trackedHosts.contains(nextHost)) {
				i--;
			} else {
				trackedHosts.add(nextHost);
			}
		}

		return trackedHosts;
	}

	/**
	 * Creates a snapshot of message availability
	 * @param trackedHosts The list of hosts in the world
	 */
	@Override
	protected void createSnapshot(List<DTNHost> hosts) {
		write("[" + (int) getSimTime() + "]"); /* write sim time stamp */

		if (this.trackedHosts == null) {
			this.trackedHosts = selectTrackedHosts(hosts);
		}

		for (DTNHost host : hosts) {
			Set<String> msgIds = null;
			String idString = "";

			if (! this.trackedHosts.contains(host)) {
				continue;
			}

			msgIds = new HashSet<String>();

			/* add own messages */
			for (Message m : host.getMessageCollection()) {
				if (!isTracked(m)) {
					continue;
				}
				msgIds.add(m.getId());
			}
			/* add all peer messages */
			for (Connection c : host.getConnections()) {
				DTNHost peer = c.getOtherNode(host);
				for (Message m : peer.getMessageCollection()) {
					if (!isTracked(m)) {
						continue;
					}
					msgIds.add(m.getId());
				}
			}

			for (String id : msgIds) {
				idString += " " + id;
			}

			write(host + idString);
		}
	}
}
