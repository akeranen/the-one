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
 * every source node (defined with {@link MessageEventGenerator#HOST_RANGE_S})
 * to one of the destination nodes (defined with
 * {@link MessageEventGenerator#TO_HOST_RANGE_S}).
 * The message size, first messages time and the intervals between creating
 * messages can be configured like with {@link MessageEventGenerator}. End
 * time is not respected, but messages are created until every from-node has
 * created a message.
 * @see MessageEventGenerator
 */
public class OneFromEachMessageGenerator extends MessageEventGenerator {
	private List<Integer> fromIds;

	public OneFromEachMessageGenerator(Settings s) {
		super(s);
		this.fromIds = new ArrayList<Integer>();

		if (toHostRange == null) {
			throw new SettingsError("Destination host (" + TO_HOST_RANGE_S +
					") must be defined");
		}
		for (int i = hostRange[0]; i < hostRange[1]; i++) {
			fromIds.add(i);
		}
		Collections.shuffle(fromIds, rng);
	}

	/**
	 * Returns the next message creation event
	 * @see input.EventQueue#nextEvent()
	 */
	public ExternalEvent nextEvent() {
		int responseSize = 0; /* no responses requested */
		int from;
		int to;

		from = this.fromIds.remove(0);
		to = drawToAddress(toHostRange, -1);

		if (to == from) { /* skip self */
			if (this.fromIds.size() == 0) { /* oops, no more from addresses */
				this.nextEventsTime = Double.MAX_VALUE;
				return new ExternalEvent(Double.MAX_VALUE);
			} else {
				from = this.fromIds.remove(0);
			}
		}

		if (this.fromIds.size() == 0) {
			this.nextEventsTime = Double.MAX_VALUE; /* no messages left */
		} else {
			this.nextEventsTime += drawNextEventTimeDiff();
		}

		MessageCreateEvent mce = new MessageCreateEvent(from, to, getID(),
				drawMessageSize(), responseSize, this.nextEventsTime);

		return mce;
	}

}
