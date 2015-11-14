/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import java.util.List;

import core.ConnectionListener;
import core.DTNHost;
import core.UpdateListener;

/**
 * UniqueEncountersReport class creates a report of the distribution of how
 * many promilles of the other nodes a node has encountered.
 *
 * @author Frans Ekman
 */
public class UniqueEncountersReport extends Report implements
	ConnectionListener, UpdateListener {

	private int[][] nodeRelationships;

	public UniqueEncountersReport() {

	}

	public void hostsConnected(DTNHost host1, DTNHost host2) {
		if (nodeRelationships == null) {
			return;
		}
		nodeRelationships[host1.getAddress()][host2.getAddress()]++;
		nodeRelationships[host2.getAddress()][host1.getAddress()]++;
	}

	public void hostsDisconnected(DTNHost host1, DTNHost host2) {}

	public void updated(List<DTNHost> hosts) {
		if (nodeRelationships == null) {
			nodeRelationships = new int[hosts.size()][hosts.size()];
		}
	}

	@Override
	public void done() {
		int[] distribution = new int[1000];

		for (int i=0; i<nodeRelationships.length; i++) {
			int count = 0;
			for (int j=0; j<nodeRelationships.length; j++) {
				if (nodeRelationships[i][j] > 0) {
					count++;
				}
			}

			int promille = (count * 1000)/nodeRelationships.length;
			distribution[promille]++;
		}

		// print distribution
		for (int i=0; i<distribution.length; i++) {
			write(i + " " + distribution[i]);
		}

		super.done();
	}

	public int[][] getNodeRelationships() {
		return nodeRelationships;
	}

	public void setNodeRelationships(int[][] nodeRelationships) {
		this.nodeRelationships = nodeRelationships;
	}

}
