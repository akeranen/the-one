/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package interfaces;

import java.util.Collection;

import core.NetworkInterface;

/**
 * A superclass for schemes for optimizing the location of possible contacts
 * with network interfaces of a specific range
 */
abstract public class ConnectivityOptimizer {

	/**
	 * Returns true if both interfaces are within radio range of each other.
	 * @param a The first interface
	 * @param b The second interface
	 * @return True if the interfaces are within range, false if not
	 */
	abstract public boolean areWithinRange(NetworkInterface a, NetworkInterface b);

	/**
	 * Finds all network interfaces that might be located so that they can be
	 * connected with the network interface
	 *
	 * @param ni network interface that needs to be connected
	 * @return A collection of network interfaces within proximity
	 */
	abstract public Collection<NetworkInterface> getInterfacesInRange(NetworkInterface ni);

}
