/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package test;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import core.DTNHost;
import core.Message;
import core.SimClock;

public class MessageTest extends TestCase {

	private Message msg;
	private DTNHost from;
	private DTNHost to;
	private SimClock sc;
	
	@Before
	public void setUp() throws Exception {
		sc = SimClock.getInstance();
		sc.setTime(10);
		
		msg = new Message(from, to, "M", 100);
		msg.setTtl(10);
		
	}

	@Test
	public void testGetTtl() {
		assertEquals(10, msg.getTtl());
		
		sc.advance(50);
		assertEquals(9, msg.getTtl());
		
		sc.advance(120);
		assertEquals(7, msg.getTtl());
		
		sc.advance(180);
		assertEquals(4, msg.getTtl());
		
		sc.advance(240);
		assertEquals(0, msg.getTtl());


	}

	@Test
	public void testAddProperty() {
		String value1 = "value1";
		String value2 = "value2";
		msg.addProperty("foo", value1);
		msg.addProperty("bar", value2);
		
		assertEquals(value1, msg.getProperty("foo"));
		assertEquals(value2, msg.getProperty("bar"));
	}


}
