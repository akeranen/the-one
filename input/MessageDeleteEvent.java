/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

import java.util.ArrayList;
import java.util.List;

import core.DTNHost;
import core.Message;
import core.World;

/**
 * External event for deleting a message.
 */

public class MessageDeleteEvent extends MessageEvent {
	/** is the delete caused by a drop (not "normal" removing) */
	private boolean drop; 
	
	/**
	 * Creates a message delete event
	 * @param host Where to delete the message
	 * @param id ID of the message
	 * @param time Time when the message is deleted
	 */
	public MessageDeleteEvent(int host, String id, double time, 
			boolean drop) {
		super(host, host, id, time);
		this.drop = drop;
	}
	
	/**
	 * Deletes the message
	 */
	@Override
	public void processEvent(World world) {
		DTNHost host = world.getNodeByAddress(this.fromAddr);
		
		if (id.equals(StandardEventsReader.ALL_MESSAGES_ID)) {
			List<String> ids = new ArrayList<String>();
			for (Message m : host.getMessageCollection()) {
				ids.add(m.getId());
			}
			for (String nextId : ids) {
				host.deleteMessage(nextId, drop);
			}
		} else {
			host.deleteMessage(id, drop);
		}
	}

	@Override
	public String toString() {
		return super.toString() + " [" + fromAddr + "] DELETE";
	}

}
