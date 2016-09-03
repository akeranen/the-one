/*
 * Copyright 2016 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

package buffermanagement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import core.Message;
import core.Settings;
import core.SimClock;
import routing.ActiveRouter;

/**
 * Implementation of the Fair-Drop message drop policy. This drop policy uses a metric called 
 * fairness composed of buffer time, forward count and TTL of the messages combining the ideas of
 * FIFO, MOFO and SHLI. 
 */
public class FairDropPolicy extends DropPolicy {
	
	/**
	 * The Fair Drop namespace for specific configurations.
	 */
	private static final String FAIR_DROP_NS = "FairDropPolicy";

	/**
	 * Enable relative fairness -setting id ({@value}). The configuration that defines
	 * if the relative fairness must be evaluated when there are only messages with fairness
	 * equals 0 in the buffer and there isn't enough space in the buffer to receive the incoming message.
	 */
	private static final String ENABLE_RELATIVE_FAIRNESS_S = "enableRelativeFairness";
	
	/**
	 * Defines if the relative fairness must be evaluated.
	 */
	protected boolean enableRelativeFairness = false;
	
	
	public FairDropPolicy(Settings s) {
		super(s);
		
		Settings set = new Settings(FAIR_DROP_NS);
		
		// Load some specific settings
		if (set.contains(ENABLE_RELATIVE_FAIRNESS_S)) {
			this.enableRelativeFairness = set.getBoolean(ENABLE_RELATIVE_FAIRNESS_S);
		}
		
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
		
		// Sort the messages by fairness
		ArrayList<Message> messages = new ArrayList<Message>(router.getMessageCollection());
		FairDropComparator comparator = new FairDropComparator();
		Collections.sort(messages, comparator);
		
		// Remove messages with fairness equals 0
		while (messages.size() > 0 && comparator.getFairnessCache().get(messages.get(0)) == 0) {
			messages.remove(0);
		}
		
		/* Delete messages from the buffer until there is enough space */
		while (freeBuffer < size) {
			
			// Check if the queue of evaluated messages is empty.
			if (messages.size() == 0) {	
				
				// If enableRelativeFairness is active we need to evaluate messages with fairness equals 0
				if (this.enableRelativeFairness && router.getMessageCollection().size() > 0) {
					return checkRelativeFairness(router, incomingMessage);
				}
				
				return false; // couldn't remove more messages
			}
			
			// Get the message with the maximum fairness
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
	
	
	/**
	 * Try to free space comparing the incoming message relative fairness with the 
	 * relative fairness of the buffer messages. This method should be called only when
	 * enableRelativeFairness is true and there isn't enough space to receive the incoming
	 * messages and all buffer messages have fairness equals zero.
	 * @param router The router evaluated.
	 * @param incomingMessage The message that needs to be stored in the buffer.
	 * @return True if it was possible to freed enough space to receive the incoming message, otherwise false.
	 */
	private boolean checkRelativeFairness(ActiveRouter router, Message incomingMessage) {
		int size = incomingMessage == null ? 0 : incomingMessage.getSize();
		
		long freeBuffer = router.getFreeBufferSize();
		
		// Sort the messages by relative fairness
		ArrayList<Message> messages = new ArrayList<Message>(router.getMessageCollection());
		FairDropComparator comparator = new FairDropComparator(true);
		Collections.sort(messages, comparator);
		
		// Get the incoming message relative fairness
		double incomingFairness = comparator.getFairness(incomingMessage);
		
		/** Delete messages from the buffer until the incoming message has relative fairness lesser 
		 * than the messages in the buffer or the buffer is empty.
		 */
		while (freeBuffer < size) {
			
			// check if the buffer is empty
			if (messages.size() == 0) {
				return false;
			}
			
			// Get the message with maximum fairness from the buffer
			Message msg = messages.remove(messages.size() - 1);
			double msgFairness = comparator.getFairnessCache().get(msg);
			
			// Check if the message removed from the buffer can be dropped
			if (this.dropMsgBeingSent || router.isSending(msg.getId())) {
				
				// If the incoming message has fairness greater or equals the buffer message
				// there are no messages that can be dropped
				if (incomingFairness >= msgFairness) {
					return false; // couldn't drop more messages
				}
				else {
					// Delete message and send signal drop
					router.deleteMessage(msg.getId(), true);
					freeBuffer += msg.getSize();
				}
			}
			
		}
		return true;
	}
	
	/**
	 * A comparator to sort the messages by their fairness value.
	 */
	private class FairDropComparator implements Comparator<Message> {
		
		/**
		 * A simple cache of the fairness values.
		 */
		private Map<Message, Double> fairnessCache;
		
		/**
		 * Defines if the comparator must use the relative fairness (ignoring forward count).
		 */
		private boolean useRelativeFairness;
		
		/**
		 * Default constructor. Assumes that the relative fairness must not be used.
		 */
		public FairDropComparator() {
			this(false);
		}
		
		/**
		 * Constructor.
		 * @param useRelativeFairness Defines if the relative that must be used in the comparison
		 * method.
		 */
		public FairDropComparator(boolean useRelativeFairness) {
			// Initialize the fairness cache
			this.fairnessCache = new HashMap<Message, Double>();
			this.useRelativeFairness = useRelativeFairness;
		}
		
		/**
		 * Expose the last evaluated fairness values.
		 * @return The fairness cache.
		 */
		public Map<Message, Double> getFairnessCache() {
			return this.fairnessCache;
		}
		
		/**
		 * Compute the fairness of a message.
		 * @param msg The message being evaluated.
		 * @return The fairness value of the message.
		 */
		public double getFairness(Message msg) {
			double now = SimClock.getTime();
			double bufferTime = now - msg.getReceiveTime();
			
			// If useRelativeFairness is true, forward count defaults to 1
			int forwardCount = this.useRelativeFairness ? 1 : msg.getForwardCount();
			
			return (bufferTime * forwardCount) / msg.getTtl();
		}

		@Override
		public int compare(Message msg1, Message msg2) {
			// Checks if the fairness of the messages was computed before
			if (!this.fairnessCache.containsKey(msg1)) {
				this.fairnessCache.put(msg1, this.getFairness(msg1));
			}
			if (!this.fairnessCache.containsKey(msg2)) {
				this.fairnessCache.put(msg2, this.getFairness(msg2));
			}
			
			return this.fairnessCache.get(msg1).compareTo(this.fairnessCache.get(msg2));
			
		}
		
	}

}
