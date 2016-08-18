/*
 * Copyright 2016 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

package buffermanagement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import core.Message;
import core.Settings;
import routing.ActiveRouter;

public class SHLIDropPolicy extends DropPolicy {

	public SHLIDropPolicy(Settings s) {
		super(s);
		// It doesn't need specific settings.
	}

	@Override
	public boolean makeRoomForMessage(ActiveRouter router, Message incomingMessage) {
		
		int size = incomingMessage == null ? 0 : incomingMessage.getSize();
		
		if (size > router.getBufferSize()) {
			return false; // Message too big for the buffer
		}
		
		long freeBuffer = router.getFreeBufferSize();
		
		// Check if there is enough space to receive the message before sorting the buffer
		if (freeBuffer >= size) {
			return true;
		}
		
		// Sort the messages by ttl
		ArrayList<Message> messages = new ArrayList<Message>(router.getMessageCollection());
		Collections.sort(messages, new SHLIComparator());
		
		/* delete messages from the buffer until there's enough space */
		while (freeBuffer < size) {
			
			if (messages.size() == 0) {
				return false; // Couldn't remove more messages
			}
			
			// Get the message with minimum ttl
			Message msg = messages.remove(0);
			
			// Check if the router is sending this message
			if (this.dropMsgBeingSent || !router.isSending(msg.getId())) {
				router.deleteMessage(msg.getId(), true);
				freeBuffer += msg.getSize();
			}
		}
		return true;
		
	}
	
	private class SHLIComparator implements Comparator<Message> {

		@Override
		public int compare(Message msg0, Message msg1) {
			return ((Integer)msg0.getTtl()).compareTo(msg1.getTtl());
		}
		
	}
	
	

}
