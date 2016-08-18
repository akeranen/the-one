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

/**
 * Drop the messages with the maximum number of transmissions first, i.e., the most forwarded 
 * messages.
 */
public class MOFODropPolicy extends DropPolicy {


	public MOFODropPolicy(Settings s) {
		super(s);
		// It doesn't need specific settings.
	}

	@Override
	public boolean makeRoomForMessage(ActiveRouter router, Message incomingMessage) {
		
		int size = incomingMessage == null ? 0 : incomingMessage.getSize();
		
		if (size > router.getBufferSize()) {
			return false; // message too big for the buffer
		}
		
		long freeBuffer = router.getFreeBufferSize();
		
		// Check if there is enough space to receive the message before sorting the buffer
		if (freeBuffer >= size) {
			return true;
		}
		
		// Sort the messages by forward count
		ArrayList<Message> messages = new ArrayList<Message>(router.getMessageCollection());
		Collections.sort(messages, new MOFOComparator());
		
		/* Delete messages from the buffer until there is enough space */
		while (freeBuffer < size) {
			
			if (messages.size() == 0) {
				return false; // couldn't remove more messages
			}
			
			// Get the message that was most forwarded
			Message msg = messages.remove(messages.size()-1);
			
			// Check if the router is sending this message
			if (this.dropMsgBeingSent || !router.isSending(msg.getId())) {
				// Delete the message and send signal "drop"
				router.deleteMessage(msg.getId(), true);
				freeBuffer += msg.getSize();
			}
		}
		
		return true;
		
	}
	
	private class MOFOComparator implements Comparator<Message> {

		@Override
		public int compare(Message msg0, Message msg1) {
			return ((Integer)msg0.getForwardCount()).compareTo(msg1.getForwardCount());
		}
		
	}

}
