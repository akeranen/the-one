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
 * Drop the messages that arrived first, i.e., the messages that have the minimum
 * receive time.
 */
public class FIFODropPolicy extends DropPolicy{

	public FIFODropPolicy(Settings s) {
		super(s);
		// It doesn't need specific settings.
	}

	@Override
	public boolean makeRoomForMessage(ActiveRouter router, Message incomingMessage) {
		
		int size = incomingMessage == null ? 0 : incomingMessage.getSize();
		
		// Check if the incoming message size exceeds the buffer capacity
		if (size > router.getBufferSize()) {
			return false;
		}
		
		long freeBuffer = router.getFreeBufferSize();
		
		// Check if there is enough space to receive the message before sorting the buffer
		if (freeBuffer >= size) {
			return true;
		}
		
		// Sort the messages by receive time
		ArrayList<Message> messages = new ArrayList<Message>(router.getMessageCollection());
		Collections.sort(messages, new FIFOComparator());
		
		/* Delete messages from the buffer until there is enough space */
		while (freeBuffer < size) {
			
			if (messages.size() == 0) {
				return false; // couldn't remove more messages
			}
			
			// Get the message that was received first
			Message msg = messages.remove(0);
			
			// Check if the router is sending this message
			if (this.dropMsgBeingSent || !router.isSending(msg.getId())) {
				// Delete the message and send signal "drop"
				router.deleteMessage(msg.getId(), true);
				freeBuffer += msg.getSize();
			}
		}
		
		return true;
	}
	
	private class FIFOComparator implements Comparator<Message> {

		@Override
		public int compare(Message m1, Message m2) {
			return ((Double)m1.getReceiveTime()).compareTo(m2.getReceiveTime());
		}
		
	}

}
