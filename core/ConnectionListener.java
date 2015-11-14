/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

/**
 * Interface for classes that want to be informed about connections
 * between hosts.
 */
public interface ConnectionListener {

	/**
	 * Method is called when two hosts are connected.
	 * @param host1 Host that initiated the connection
	 * @param host2 Host that was connected to
	 */
	public void hostsConnected(DTNHost host1, DTNHost host2);

	/**
	 * Method is called when connection between hosts is disconnected.
	 * @param host1 Host that initiated the disconnection
	 * @param host2 Host at the other end of the connection
	 */
	public void hostsDisconnected(DTNHost host1, DTNHost host2);

}
