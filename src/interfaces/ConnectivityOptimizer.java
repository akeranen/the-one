/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package interfaces;

import java.util.Set;

import core.NetworkInterface;

/**
 * A superclass for schemes for optimizing the location of possible contacts
 * with network interfaces of a specific range
 */
abstract public class ConnectivityOptimizer {

	/**
	 * Updates a network interface's location
	 */
	abstract public void updateLocation(NetworkInterface ni);

	/**
	 * Finds all network interfaces that might be located so that they can be
	 * connected with the network interface
	 *
	 * @param ni network interface that needs to be connected
	 * @return A collection of network interfaces within proximity
	 */
	abstract public Set<NetworkInterface> getInterfacesInRange(NetworkInterface ni);

}
