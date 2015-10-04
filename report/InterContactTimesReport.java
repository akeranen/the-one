/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import core.DTNHost;

/**
 * Reports the inter-contact time (i.e., the time between the end of previous
 * contact and the beginning of a new contact between two hosts) distribution.
 * The syntax of the report file is the same as in {@link ContactTimesReport}.
 */
public class InterContactTimesReport extends ContactTimesReport {
	
	@Override
	public void hostsConnected(DTNHost host1, DTNHost host2) {
		ConnectionInfo ci = this.removeConnection(host1, host2);
		
		if (ci != null) { // connected again
			newEvent();
			ci.connectionEnd();			
			increaseTimeCount(ci.getConnectionTime());
		}		
	}
	
	@Override
	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		if (isWarmup()) {
			return;
		}
		// start counting time to next connection
		this.addConnection(host1, host2);
	}
}
