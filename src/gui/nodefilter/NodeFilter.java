/*
 * Copyright 2011 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

package gui.nodefilter;

import core.DTNHost;

/**
 * Interface for node filtering classes
 */
public interface NodeFilter {

	/**
	 * Returns true if the given node should be included in the filtered set
	 * @param node The node to check
	 * @return true if the node should be included, false if not
	 */
	public boolean filterNode(DTNHost node);

	/**
	 * Returns a String presentations of the filter
	 */
	public String toString();
}
