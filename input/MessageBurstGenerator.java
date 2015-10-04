/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

import core.Settings;

/**
 * Message creation -external events generator. Creates bursts of messages where
 * every source node (defined with {@link MessageEventGenerator#HOST_RANGE_S})
 * creates a new message to every destination node (defined with 
 * {@link MessageEventGenerator#TO_HOST_RANGE_S})on every interval. 
 * The message size, burst times, and inter-burst intervals can be configured 
 * like with {@link MessageEventGenerator}.
 * @see MessageEventGenerator
 */
public class MessageBurstGenerator extends MessageEventGenerator {
	/** next index to use from the "from" range */
	private int nextFromOffset;
	private int nextToOffset;

	public MessageBurstGenerator(Settings s) {
		super(s);
		this.nextFromOffset = 0;
		this.nextToOffset = 0;
		
		if (this.toHostRange == null) {
			this.toHostRange = this.hostRange;
		}
	}
	
	/** 
	 * Returns the next message creation event
	 * @see input.EventQueue#nextEvent()
	 */
	public ExternalEvent nextEvent() {
		int responseSize = 0; /* no responses requested */
		int msgSize;
		int interval;
		int from;
		int to;
		boolean nextBurst = false;
		
		from = this.hostRange[0] + nextFromOffset;	
		to = this.toHostRange[0] + nextToOffset;
		
		if (to == from) { /* skip self */
			to = this.toHostRange[0] + (++nextToOffset);
		}
		
		msgSize = drawMessageSize();		
		MessageCreateEvent mce = new MessageCreateEvent(from, to, getID(), 
				msgSize, responseSize, this.nextEventsTime);

		if (to < this.toHostRange[1] - 1) {
			this.nextToOffset++;
		} else {
			if (from < this.hostRange[1] - 1) {
				this.nextFromOffset++;
				this.nextToOffset = 0;
			} else { 
				nextBurst = true;
			}
		}
		
		if (this.hostRange[0] + nextFromOffset == 
			this.toHostRange[0] + nextToOffset) {
			/* to and from would be same for next event */
			nextToOffset++;
			if (nextToOffset >= toHostRange[1]) {
				/* TODO: doesn't work correctly with non-aligned ranges */
				nextBurst = true;			
			}
		}
		
		if (nextBurst) {
			interval = drawNextEventTimeDiff();
			this.nextEventsTime += interval;
			this.nextFromOffset = 0;
			this.nextToOffset = 0;
		}
		
		if (this.msgTime != null && this.nextEventsTime > this.msgTime[1]) {
			/* next event would be later than the end time */
			this.nextEventsTime = Double.MAX_VALUE;
		}
		
		return mce;
	}

}