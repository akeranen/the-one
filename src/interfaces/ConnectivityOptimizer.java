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
	 * Adds a network interface to the optimizer (unless it is already present)
	 */
	abstract public void addInterface(NetworkInterface ni);

	/**
	 * Adds a collection of network interfaces to the optimizer (except of those
	 * already added
	 */
	abstract public void addInterfaces(Collection<NetworkInterface> interfaces);

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
	abstract public Collection<NetworkInterface> getNearInterfaces(
			NetworkInterface ni);

	/**
	 * Finds all other interfaces that are registered to the
	 * ConnectivityOptimizer
	 */
	abstract public Collection<NetworkInterface> getAllInterfaces();
}
