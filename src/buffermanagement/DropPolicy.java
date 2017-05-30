/*
 * Copyright 2016 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

package buffermanagement;

import core.Message;
import core.Settings;
import routing.ActiveRouter;


/**
 * Defines the basic structure for implementing a message drop policy. These policies can be used by the hosts
 * when their buffers haven't enough space to receive an incoming message.
 */
public abstract class DropPolicy {
	
	/**
	 * Drop message being sent -setting id ({@value}). The configuration that defines
	 * if messages being sent can be dropped.
	 */
	private static final String DROP_MSG_BEING_SENT = "dropMsgBeingSent";
	
	/**
	 * Defines if messages being sent can be dropped.
	 */
	protected boolean dropMsgBeingSent = true;
	
	/**
	 * Constructor with the signature required to be instantiated by the simulator.
	 * @param s
	 */
	public DropPolicy(Settings s) {
		if (s.contains(DROP_MSG_BEING_SENT)) {
			this.dropMsgBeingSent = s.getBoolean(DROP_MSG_BEING_SENT);
		}
	}
	
	/**
	 * Try to remove messages from the buffer until enough space for receive the incoming message is freed.
	 * @param router A reference to the receiver router.
	 * @param incomingMessage The incoming message.
	 * @return True if it was possible to freed enough space, false otherwise.
	 */
	public abstract boolean makeRoomForMessage(ActiveRouter router, Message incomingMessage);
}
