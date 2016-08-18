/*
 * Copyright 2016 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

package buffermanagement;

import java.util.Iterator;
import core.Message;
import core.Settings;
import routing.ActiveRouter;

/**
 * Implementation of the E-Drop policy as proposed in the paper 
 * "E-DROP: An Effective Drop Buffer Management Policy for DTN Routing Protocols"
 * that can be found at 
 * http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.206.3719&rep=rep1&type=pdf
 */
public class EDropPolicy extends DropPolicy {

	/**
	 * Store a settings reference to pass to FIFO constructor when needed.
	 */
	private Settings settings;
	
	public EDropPolicy(Settings s) {
		super(s);
		this.settings = s;
		
		// It doesn't need specific settings.
	}

	@Override
	public boolean makeRoomForMessage(ActiveRouter router, Message incomingMessage) {
		
		int size = incomingMessage == null ? 0 : incomingMessage.getSize();
		
		if (size > router.getBufferSize()) {
			return false; // message too big for the buffer
		}
		
		long freeBuffer = router.getFreeBufferSize();
		
		while (freeBuffer < size) {
			Iterator<Message> iter = router.getMessageCollection().iterator();
			
			if (!iter.hasNext()) {
				return false; // There is no message that can be dropped
			}
			
			// Try to find a message with size greater or equals to the incoming message size
			Message msg = null;
			while (iter.hasNext()) {
				Message temp = iter.next();
				if (temp.getSize() >= size && (this.dropMsgBeingSent || !router.isSending(temp.getId()))) {
					msg = temp;
					break;
				}
			}
			
			// If there is a message to drop
			if (msg != null) {
				router.deleteMessage(msg.getId(), true);
				freeBuffer += msg.getSize();
			}
			else {
				// If there aren't messages with size equals or greater than the incoming, works like FIFO
				FIFODropPolicy fifo = new FIFODropPolicy(this.settings);
				return fifo.makeRoomForMessage(router, incomingMessage);
			}
		}
		return true;
	}
	

}
