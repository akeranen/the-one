/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package test;

import util.ActivenessHandler;
import junit.framework.TestCase;
import core.SimClock;

public class ActivenessHandlerTest extends TestCase {
	private ActivenessHandler ah;
	private SimClock clock;
	private double in = 3.0;
	private double out = 7.0;
	
	private String moreTimes = ", 100,110, 210,220, 350,400";
	
	protected void setUp() throws Exception {
		super.setUp();
		TestSettings ts = new TestSettings();
		
		ts.putSetting(ActivenessHandler.ACTIVE_TIMES_S, in + "," + out + 
				moreTimes);
		ah = new ActivenessHandler(ts);
		clock = SimClock.getInstance();
		SimClock.reset();
	}

	public void testIsActive() {
		assertFalse(ah.isActive());
		clock.setTime(in);
		assertTrue(ah.isActive());
		clock.setTime(in + 0.1);
		assertTrue(ah.isActive());
		clock.advance(1.0);
		assertTrue(ah.isActive());
		clock.advance(10.0);
		assertFalse(ah.isActive());
	}
	
	
	public void testMoreTimes() {
		// test second value tuple
		clock.setTime(98);
		assertFalse(ah.isActive());
		clock.setTime(100);
		assertTrue(ah.isActive());
		clock.setTime(105);
		assertTrue(ah.isActive());
		clock.setTime(112);
		assertFalse(ah.isActive());

		
		// test zero ease
		clock.setTime(209.9);
		assertFalse(ah.isActive());
		clock.setTime(210);
		assertTrue(ah.isActive());
		clock.setTime(220);
		assertTrue(ah.isActive());
		clock.setTime(220.1);
		assertFalse(ah.isActive());

		// test last tuple
		clock.setTime(360);
		assertTrue(ah.isActive());
		clock.setTime(450);
		assertFalse(ah.isActive());

	}
	
}

