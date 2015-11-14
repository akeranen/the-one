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
 * The total- vs. the unique encounters for each node
 *
 * @author Frans Ekman
 */
public class EncountersVSUniqueEncountersReport extends Report
	implements ConnectionListener, UpdateListener {

	private TotalEncountersReport totalEncountersReport;
	private UniqueEncountersReport uniqueEncountersReport;

	public EncountersVSUniqueEncountersReport() {
		totalEncountersReport = new TotalEncountersReport();
		uniqueEncountersReport = new UniqueEncountersReport();
	}

	public void hostsConnected(DTNHost host1, DTNHost host2) {
		totalEncountersReport.hostsConnected(host1, host2);
		uniqueEncountersReport.hostsConnected(host1, host2);
	}

	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		totalEncountersReport.hostsDisconnected(host1, host2);
		uniqueEncountersReport.hostsDisconnected(host1, host2);
	}

	public void updated(List<DTNHost> hosts) {
		totalEncountersReport.updated(hosts);
		uniqueEncountersReport.updated(hosts);
	}

	@Override
	public void done() {
		int[] totalEncounters = totalEncountersReport.getEncounters();
		int[][] nodeRelationships = uniqueEncountersReport.getNodeRelationships();

		for (int i=0; i<totalEncounters.length; i++) {
			String row = "";
			row += i + "\t";
			row += totalEncounters[i] + "\t";

			int count = 0;
			for (int j=0; j<nodeRelationships.length; j++) {
				if (nodeRelationships[i][j] > 0) {
					count++;
				}
			}
			row += count;
			write(row);
		}

		super.done();
	}
}
