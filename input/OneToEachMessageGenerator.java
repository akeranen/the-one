/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import core.Settings;
import core.SettingsError;

/**
 * Message creation -external events generator. Creates one message from
 * source node/nodes (defined with {@link MessageEventGenerator#HOST_RANGE_S})
 * to all destination nodes (defined with
 * {@link MessageEventGenerator#TO_HOST_RANGE_S}). 
 * The message size, first messages time and the intervals between creating 
 * messages can be configured like with {@link MessageEventGenerator}. End
 * time is not respected, but messages are created until there's a message for
 * every destination node.
 * @see MessageEventGenerator
 */
public class OneToEachMessageGenerator extends MessageEventGenerator {
	private List<Integer> toIds;
	
	public OneToEachMessageGenerator(Settings s) {
		super(s);
		this.toIds = new ArrayList<Integer>();
		
		if (toHostRange == null) {
			throw new SettingsError("Destination host (" + TO_HOST_RANGE_S + 
					") must be defined");
		}
		for (int i = toHostRange[0]; i < toHostRange[1]; i++) {
			toIds.add(i);
		}
		Collections.shuffle(toIds, rng);
	}
	
	/** 
	 * Returns the next message creation event
	 * @see input.EventQueue#nextEvent()
	 */
	public ExternalEvent nextEvent() {
		int responseSize = 0; /* no responses requested */
		int from;
		int to;
		
		from = drawHostAddress(hostRange);	
		to = this.toIds.remove(0);
		
		if (to == from) { /* skip self */
			if (this.toIds.size() == 0) { /* oops, no more from addresses */
				this.nextEventsTime = Double.MAX_VALUE;
				return new ExternalEvent(Double.MAX_VALUE);
			} else {
				to = this.toIds.remove(0);
			}
		}

		if (this.toIds.size() == 0) {
			this.nextEventsTime = Double.MAX_VALUE; /* no messages left */
		} else {
			this.nextEventsTime += drawNextEventTimeDiff();
		}
				
		MessageCreateEvent mce = new MessageCreateEvent(from, to, getID(), 
				drawMessageSize(), responseSize, this.nextEventsTime);
		
		return mce;
	}

}