/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import core.ConnectionListener;
import core.DTNHost;

/**
 * Link connectivity report generator for ONE StandardEventsReader input.
 * Connections that start during the warm up period are ignored.
 */
public class ConnectivityONEReport extends Report 
	implements ConnectionListener {	
	
	/**
	 * Constructor.
	 */
	public ConnectivityONEReport() {
		init();
	}
		
	public void hostsConnected(DTNHost h1, DTNHost h2) {
		if (isWarmup()) {
			addWarmupID(connectionString(h1, h2));
			return;
		}
		newEvent();
		write(createTimeStamp() + " CONN " + connectionString(h1, h2) + " up");
	}
	
	public void hostsDisconnected(DTNHost h1, DTNHost h2) {
		String conString = connectionString(h1, h2);
		
		if (isWarmup() || isWarmupID(conString)) {
			removeWarmupID(conString);
			return;
		} 
		 		
		write(createTimeStamp() + " CONN " + conString + " down");
	}
	
	/**
	 * Creates and returns a "@" prefixed time stamp of the current simulation
	 * time
	 * @return time stamp of the current simulation time
	 */
	private String createTimeStamp() {
		return String.format("%.2f", getSimTime());
	}
	
	/**
	 * Creates and returns a String presentation of the connection where the
	 * node with the lower network address is first
	 * @param h1 The other node of the connection
	 * @param h2 The other node of the connection
	 * @return String presentation of the connection
	 */
	private String connectionString(DTNHost h1, DTNHost h2) {
		if (h1.getAddress() < h2.getAddress()) {
		    return h1.getAddress() + " " + h2.getAddress();
		}
		else {
		    return h2.getAddress() + " " + h1.getAddress();
		}
	}
	
}
