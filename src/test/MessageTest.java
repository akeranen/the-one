/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import java.util.ArrayList;

import core.*;
import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import test.TestUtils;
import test.TestSettings;

public class MessageTest extends TestCase {

    private TestUtils utils;

	private Message msg;
	private DTNHost from;
	private DTNHost to;
	private SimClock sc;

	@Before
	public void setUp() throws Exception {
		sc = SimClock.getInstance();
		sc.setTime(10);

		this.utils = new TestUtils(
				new ArrayList<ConnectionListener>(),
				new ArrayList<MessageListener>(),
				new TestSettings());
		this.to = this.utils.createHost();

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

	@Test
	public void testGetTo() {
		assertEquals(this.to, this.msg.getTo());
    }

    @Test
    public void testIsFinalRecipientReturnsTrueForSingleRecipient() {
		assertTrue(this.msg.isFinalRecipient(this.to));
    }

    @Test
    public void testIsFinalRecipientReturnsFalseForHostDifferentFromRecipient() {
		DTNHost otherHost = this.utils.createHost();
		assertFalse(this.msg.isFinalRecipient(otherHost));
    }

    @Test
    public void testCompletesDeliveryReturnsTrueForSingleRecipient() {
	    assertTrue(this.msg.completesDelivery(this.to));
    }

    @Test
    public void testCompletesDeliveryReturnsFalseForHostDifferentFromRecipient() {
	    DTNHost otherHost = this.utils.createHost();
	    assertFalse(this.msg.completesDelivery(otherHost));
    }

    @Test
    public void testGetTypeReturnsOneToOne() {
	    assertEquals(Message.MessageType.ONE_TO_ONE, this.msg.getType());
    }
}
