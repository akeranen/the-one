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
 * A report of the distribution of how many encounters (contacts) a node has had 
 * 
 * @author Frans Ekman
 */
public class TotalEncountersReport extends Report implements ConnectionListener,
	UpdateListener {

	private int[] encounters;
	
	public TotalEncountersReport() {
		
	}
	
	public void hostsConnected(DTNHost host1, DTNHost host2) {
		if (encounters == null) {
			return;
		}
		encounters[host1.getAddress()]++;
		encounters[host2.getAddress()]++;
	}

	public void hostsDisconnected(DTNHost host1, DTNHost host2) {}

	public void updated(List<DTNHost> hosts) {
		if (encounters == null) {
			encounters = new int[hosts.size()];
		}
	}

	@Override
	public void done() {

		int maxEncounters = -1;
		for (int i=0; i<encounters.length; i++) {
			if (encounters[i] > maxEncounters) {
				maxEncounters = encounters[i];
			}
		}
		
		int[] distribution = new int[maxEncounters + 1];
		
		for (int i=0; i<encounters.length; i++) {
			distribution[encounters[i]]++;
		}
		
		// Print distribution
		for (int i=0; i<distribution.length; i++) {
			write(i + " " + distribution[i]);
		}
		
		super.done();
	}

	public int[] getEncounters() {
		return encounters;
	}

	public void setEncounters(int[] encounters) {
		this.encounters = encounters;
	}
	
}
