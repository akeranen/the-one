/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import core.ConnectionListener;
import core.DTNHost;
import core.UpdateListener;

/**
 * The number of contacts during an inter-contact time metric is similar to 
 * the inter-contact times metric, except that instead of measuring the time 
 * until a node meets again, we count the number of other nodes both of the 
 * nodes meet separately.  In contrast to the inter-contact times, the number 
 * of contacts during an inter-contact is not symmetric, i.e. during an 
 * inter-contact both nodes wait the exact same time but will meet a different 
 * number of nodes. 
 * 
 * @author Frans Ekman
 */
public class ContactsDuringAnICTReport extends Report 
	implements ConnectionListener, UpdateListener {

	private boolean[][] areDisconnected;
	private int[][] contactCount;
	private LinkedList<Integer> contactsDuringIC;
	
	private boolean updateHasBeenCalled;
	
	public ContactsDuringAnICTReport() {
		super();
		init();
	}
	
	
	@Override
	protected void init() {
		super.init();
		contactsDuringIC = new LinkedList<Integer>();
	}
	
	
	public void hostsConnected(DTNHost host1, DTNHost host2) {	
		if (!updateHasBeenCalled) {
			return;
		}
		int id1 = host1.getAddress();
		int id2 = host2.getAddress();
		if (areDisconnected[id1][id2]) {
			areDisconnected[id1][id2] = false;
			areDisconnected[id2][id1] = false;
			contactsDuringIC.add(new Integer(contactCount[id1][id2]));
			contactsDuringIC.add(new Integer(contactCount[id2][id1]));
			contactCount[id1][id2] = 0;
			contactCount[id2][id1] = 0;
		} 
			
		incContactForAllDisconnectedNodes(host1);
		incContactForAllDisconnectedNodes(host2);
		
	}
	
	private void incContactForAllDisconnectedNodes(DTNHost host) {
		int id = host.getAddress();
		for (int i=0; i<contactCount[id].length; i++) {
			if (areDisconnected[id][i]) {
				contactCount[id][i]++;
			} 
		}
	}

	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		if (!updateHasBeenCalled) {
			return;
		}
		areDisconnected[host1.getAddress()][host2.getAddress()] = true;
		areDisconnected[host2.getAddress()][host1.getAddress()] = true;
	}

	public void updated(List<DTNHost> hosts) {
		if (areDisconnected == null || contactCount == null) {	
			areDisconnected = new boolean[hosts.size()][hosts.size()];
			contactCount = new int[hosts.size()][hosts.size()];
		}
		updateHasBeenCalled = true;
	}
	
	@Override
	public void done() {
		Integer[] contacts = (Integer[])contactsDuringIC.toArray(new Integer[0]);
		Arrays.sort(contacts);
				
		int count = 0;
		int last = 0;
		for (int i=0; i<contacts.length; i++) {
			if (contacts[i].intValue() == last) {
				count++;
				if (i == contacts.length -1) {
					write(last + "\t" + count);
				}
			} else {
				write(last + "\t" + count);
				last++;
				i--;
				count = 0;
			}
		}
		super.done();
	}	
}
