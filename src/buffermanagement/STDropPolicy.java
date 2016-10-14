/*
 * Copyright 2016 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

package buffermanagement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import routing.ActiveRouter;
import routing.centrality.ExternalCentrality;
import routing.mobility.ExternalMobility;

/**
 * Implementation of the ST-Drop message drop policy. This drop policy uses a metric called 
 * space-time coefficient composed of buffer time, forward count and TTL of the messages combining the ideas of
 * FIFO, MOFO and SHLI. 
 */
public class STDropPolicy extends DropPolicy {
	
	/**
	 * The ST-Drop namespace for specific configurations.
	 */
	private static final String ST_DROP_NS = "STDropPolicy";
	
	/**
	 * Used to configure the initial value of forward count. When configured to 0, a message
	 * that hasn't be sent yet will never be dropped. When configured to a positive number
	 * the algorithm will always drop a message.
	 */
	private double initialForwardCount = 0;
	
	/**
	 * Constructor. Receives a settings reference to initialize internal configuration.
	 * @param s A simulator settings reference.
	 */
	public STDropPolicy(Settings s) {
		super(s);
		
		// Load specific settings. Not useful yet.
		
	}

	@Override
	public boolean makeRoomForMessage(ActiveRouter router, Message incomingMessage) {
		
		try {
			int size = incomingMessage == null ? 0 : incomingMessage.getSize();
			
			// Check if the incoming message size exceeds the buffer capacity
			if (size > router.getBufferSize()) {
				return false;   // It's not possible to free enough space.
			}
			
			// Get the available space
			long freeBuffer = router.getFreeBufferSize();
			
			// Check if there is enough space to receive the message before sorting the buffer
			if (freeBuffer >= size) {
				return true;
			}
			
			// Sort the messages by space-time coefficient
			ArrayList<Message> messages = new ArrayList<Message>(router.getMessageCollection());
			STDropComparator comparator = new STDropComparator();
			Collections.sort(messages, comparator);
			
			// Remove messages with space-time coefficient equals 0
			while (messages.size() > 0 && comparator.getSTCache().get(messages.get(0)) == 0) {
				messages.remove(0);
			}
			
			/* Delete messages from the buffer until there is enough space */
			while (freeBuffer < size) {
				
				// Check if the queue of evaluated messages is empty.
				if (messages.size() == 0) {	
					if (router.getMessageCollection().size() > 0 &&  // The is still not empty
							incomingMessage != null &&  // There is an incoming message, it is not a corrective action
							incomingMessage.getTo().getAddress() == router.getHost().getAddress()) {  // This host is the final destiny of the message	
						// It will drop a message even if the message was not fowarded yet, in order to receive a message destined to this host
						this.initialForwardCount = 1;	
						return makeRoomForMessage(router, incomingMessage);
					}
					return false; // couldn't remove more messages
				}
				
				// Get the message with the maximum space-time coefficient
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
		finally {
			// Adjust the initial forward count always
			this.initialForwardCount = 0;
		}
		
	}
	
	/**
	 * A comparator to sort the messages by their space-time coefficient value.
	 */
	private class STDropComparator implements Comparator<Message> {
		
		/**
		 * A simple cache of the fairness values.
		 */
		private Map<Message, Double> stCache;

		/**
		 * Constructor.
		 */
		public STDropComparator() {
			// Initialize the ST-cache
			this.stCache = new HashMap<Message, Double>();
		}
		
		/**
		 * Expose the last evaluated space-time coefficient values.
		 * @return The st cache.
		 */
		public Map<Message, Double> getSTCache() {
			return this.stCache;
		}
		
		/**
		 * Compute the space-time coefficient of a message.
		 * @param msg The message being evaluated.
		 * @return The space-time coefficient value of the message.
		 */
		public double getST(Message msg) {
			
			double bufferTime = SimClock.getTime() - msg.getReceiveTime();
			int fc = initialForwardCount == 1 ? 1 : msg.getForwardCount();
			return msg.getTtl() == 0 ?  Double.MAX_VALUE : bufferTime * fc / msg.getTtl();
		}

		@Override
		public int compare(Message msg1, Message msg2) {
			// Checks if the fairness of the messages was computed before
			if (!this.stCache.containsKey(msg1)) {
				this.stCache.put(msg1, this.getST(msg1));
			}
			if (!this.stCache.containsKey(msg2)) {
				this.stCache.put(msg2, this.getST(msg2));
			}
			
			return this.stCache.get(msg1).compareTo(this.stCache.get(msg2));
			
		}
		
	}

}
