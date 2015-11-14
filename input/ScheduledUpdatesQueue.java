/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import java.util.ArrayList;
import java.util.List;

/**
 * Event queue where simulation objects can request an update to happen
 * at the specified simulation time. Multiple updates at the same time
 * are merged to a single update.
 */
public class ScheduledUpdatesQueue implements EventQueue {
	/** Time of the event (simulated seconds) */
	private ExternalEvent nextEvent;
	private List<ExternalEvent> updates;

	/**
	 * Constructor. Creates an empty update queue.
	 */
	public ScheduledUpdatesQueue(){
		this.nextEvent = new ExternalEvent(Double.MAX_VALUE);
		this.updates = new ArrayList<ExternalEvent>();
	}

	/**
	 * Returns the next scheduled event or event with time Double.MAX_VALUE
	 * if there aren't any.
	 * @return the next scheduled event
	 */
	public ExternalEvent nextEvent() {
		ExternalEvent event = this.nextEvent;

		if (this.updates.size() == 0) {
			this.nextEvent = new ExternalEvent(Double.MAX_VALUE);
		}
		else {
			this.nextEvent = this.updates.remove(0);
		}

		return event;
	}

	/**
	 * Returns the next scheduled event's time or Double.MAX_VALUE if there
	 * aren't any events left
	 * @return the next scheduled event's time
	 */
	public double nextEventsTime() {
		return this.nextEvent.getTime();
	}

	/**
	 * Add a new update request for the given time
	 * @param simTime The time when the update should happen
	 */
	public void addUpdate(double simTime) {
		ExternalEvent ee = new ExternalEvent(simTime);

		if (ee.compareTo(nextEvent) == 0) { // this event is already next
			return;
		}
		else if (this.nextEvent.getTime() > simTime) { // new nextEvent
			putToQueue(this.nextEvent); // put the old nextEvent back to q
			this.nextEvent = ee;
		}
		else { // given event happens later..
			putToQueue(ee);
		}
	}

	/**
	 * Puts a event to the queue in the right place
	 * @param ee The event to put to the queue
	 */
	private void putToQueue(ExternalEvent ee) {
		double eeTime = ee.getTime();

		for (int i=0, n=this.updates.size(); i<n; i++) {
			double time = updates.get(i).getTime();
			if (time == eeTime) {
				return; // update with the given time exists -> no need for new
			}
			else if (eeTime < time) {
				this.updates.add(i, ee);
				return;
			}
		}

		/* all existing updates are earlier -> add to the end of the list */
		this.updates.add(ee);
	}

	public String toString() {
		String times = "updates @ " + this.nextEvent.getTime();

		for (ExternalEvent ee : this.updates) {
			times += ", " + ee.getTime();
		}

		return times;
	}
}
