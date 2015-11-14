/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import input.ScheduledUpdatesQueue;
import junit.framework.TestCase;
import core.SimClock;

/**
 * Tests for the ScheduledUpdatesQueue
 */
public class ScheduledUpdatesQueueTest extends TestCase {
	private static double MAX = Double.MAX_VALUE;
	private ScheduledUpdatesQueue suq;
	private SimClock sc = SimClock.getInstance();

	protected void setUp() throws Exception {
		super.setUp();
		SimClock.reset();
		suq = new ScheduledUpdatesQueue();
	}

	public void testUpdates() {
		assertEquals(MAX, suq.nextEventsTime());
		suq.addUpdate(1);
		suq.addUpdate(1.5);
		suq.addUpdate(20);
		suq.addUpdate(3);
		suq.addUpdate(0);
		suq.addUpdate(5.3);

		assertEquals(0.0, suq.nextEventsTime());
		assertEquals(0.0, suq.nextEvent().getTime());

		assertEquals(1.0, suq.nextEventsTime());
		assertEquals(1.0, suq.nextEventsTime()); // twice the same request
		assertEquals(1.0, suq.nextEvent().getTime());

		assertEquals(1.5, suq.nextEvent().getTime());
		assertEquals(3.0, suq.nextEvent().getTime());
		assertEquals(5.3, suq.nextEvent().getTime());
		assertEquals(20.0, suq.nextEvent().getTime());

		assertEquals(MAX, suq.nextEventsTime());
		assertEquals(MAX, suq.nextEvent().getTime());
	}

	public void testInterlavedRequests() {
		suq.addUpdate(4);
		suq.addUpdate(7);
		suq.addUpdate(9);

		sc.setTime(1.0);
		assertEquals(4.0, suq.nextEvent().getTime());

		suq.addUpdate(8.5);

		suq.addUpdate(3); // to the top
		assertEquals(3.0, suq.nextEvent().getTime());

		suq.addUpdate(10); // to the bottom
		sc.setTime(4.0);
		assertEquals(7.0, suq.nextEvent().getTime());

		sc.setTime(7.5);
		assertEquals(8.5, suq.nextEvent().getTime());
		sc.setTime(8.8);
		assertEquals(9.0, suq.nextEvent().getTime());
		sc.setTime(9.8);
		assertEquals(10.0, suq.nextEvent().getTime());
		sc.setTime(15);
		assertEquals(MAX, suq.nextEvent().getTime());
	}

	public void testNegativeAndZeroValues() {
		suq.addUpdate(3.2);
		suq.addUpdate(-2.1);
		suq.addUpdate(0);
		suq.addUpdate(15);
		suq.addUpdate(-4);
		suq.addUpdate(0.1);

		sc.setTime(-5);

		assertEquals(-4.0, suq.nextEvent().getTime());
		assertEquals(-2.1, suq.nextEvent().getTime());
		assertEquals(0.0, suq.nextEvent().getTime());
		assertEquals(0.1, suq.nextEvent().getTime());
		assertEquals(3.2, suq.nextEvent().getTime());
		assertEquals(15.0, suq.nextEvent().getTime());
		assertEquals(MAX, suq.nextEvent().getTime());
	}

	public void testDuplicateValues() {
		suq.addUpdate(4.0);
		suq.addUpdate(5.0);
		suq.addUpdate(4.0); // these should be merged to the first value
		suq.addUpdate(4.0);
		suq.addUpdate(1.0);
		suq.addUpdate(1.0);
		suq.addUpdate(8.0);

		assertEquals(1.0, suq.nextEvent().getTime());
		assertEquals(4.0, suq.nextEvent().getTime());
		assertEquals(5.0, suq.nextEvent().getTime());
		assertEquals(8.0, suq.nextEvent().getTime());
	}
}
