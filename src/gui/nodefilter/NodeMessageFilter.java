/*
 * Copyright 2011 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

package gui.nodefilter;

import core.DTNHost;

/**
 * Node filter that filters nodes by the messages they have in their buffer
 */
public class NodeMessageFilter implements NodeFilter {
	private String messageId;

	/**
	 * Creates a new filter with the given message ID
	 * @param messageId The message ID used for filtering
	 */
	public NodeMessageFilter(String messageId) {
		this.messageId = messageId;
	}

	public boolean filterNode(DTNHost node) {
		return node.getRouter().hasMessage(messageId);
	}

	@Override
	public String toString() {
		return "Filters nodes with message ID " + messageId;
	}

}
