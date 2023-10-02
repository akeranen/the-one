/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package interfaces;

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
	public boolean areWithinRange(NetworkInterface a, NetworkInterface b) {
		double range = Math.min(a.getTransmitRange(), b.getTransmitRange());
		return a.getLocation().distanceSquared(b.getLocation()) <= range * range;
	}

	/**
	 * Detects interfaces which are in range/no longer in range of each other
	 * Issues LinkUp/LinkDown events to the corresponding interfaces
	 */
	abstract public void detectConnectivity();
}
