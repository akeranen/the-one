/*
 * Copyright 2016 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

package buffermanagement;

import core.Message;
import core.Settings;
import routing.ActiveRouter;

/**
 * This implementation always refuses to drop a message. It can be used for specific tests.
 */
public class PassiveDropPolicy extends DropPolicy{

	public PassiveDropPolicy(Settings s) {
		super(s);
		// It doesn't need specific settings.
	}

	@Override
	public boolean makeRoomForMessage(ActiveRouter router, Message incomingMessage) {
		
		// Get the incoming message size.
		int size = incomingMessage == null ? 0 : incomingMessage.getSize();
		
		// Get the available space.
		long freeBuffer = router.getFreeBufferSize();
		
		// Return if it is possible to receive the incoming message, it does not drop any messages
		return size <= freeBuffer;
	}
	
}
