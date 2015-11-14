/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import core.ModuleCommunicationBus;
import core.ModuleCommunicationListener;

public class ModuleCommunicationBusTest extends TestCase {

	private ModuleCommunicationBus b;
	private static final String TST_VAL = "test-value";
	private String notifyKey;
	private Object notifyValue;
	private ModuleCommunicationListener mcl;

	@Before
	public void setUp() throws Exception {
		b = new ModuleCommunicationBus();
		this.notifyKey = null;
		this.notifyValue = null;

		this.mcl = new ModuleCommunicationListener() {
			public void moduleValueChanged(String key, Object newValue) {
				notifyKey = key;
				notifyValue = newValue;
			}
		};

	}

	@Test
	public void testGetProperty() {
		assertNull(b.getProperty("test"));
		b.addProperty("test", TST_VAL);
		assertEquals(TST_VAL, b.getProperty("test").toString());
		assertNull(b.getProperty("invalidValue"));

		b.addProperty("test2", "value2");
		assertEquals("value2", b.getProperty("test2".toString()));
		assertEquals(TST_VAL, b.getProperty("test").toString());
	}

	@Test
	public void testUpdateProperty() {
		b.addProperty("test", TST_VAL);
		assertEquals(TST_VAL, b.getProperty("test").toString());
		b.updateProperty("test", "new value");
		assertEquals("new value", b.getProperty("test").toString());
	}

	@Test
	public void testSubscribe() {
		String key = "subtst";

		b.addProperty(key, "test");
		b.subscribe(key, mcl);
		assertNull(notifyKey);
		assertNull(notifyValue);

		b.updateProperty(key, "test2");
		assertEquals(key, notifyKey);
		assertEquals("test2", notifyValue.toString());

		b.updateProperty(key, "newTest");
		assertEquals(key, notifyKey);
		assertEquals("newTest", notifyValue.toString());
	}

	@Test
	public void testUnsubscribe() {
		String key = "unsubtst";
		String tstVal = "unsubtstvalue";
		b.subscribe(key, mcl);
		b.updateProperty(key, tstVal);
		b.unsubscribe(key, mcl);

		b.updateProperty(key, "newvalue");
		assertEquals("newvalue", b.getProperty(key).toString());
		assertEquals(key, notifyKey);
		assertEquals(tstVal, notifyValue.toString());
	}

	@Test
	public void testUpdateDouble() {
		String key = "doubletst";
		Double val = 15.5;

		b.addProperty(key, val);
		assertEquals(16.5, b.updateDouble(key, 1.0));
		assertEquals(16.5, b.getDouble(key, -1.0));

		assertEquals(13.3, b.updateDouble(key, -3.2));
		assertEquals(13.3, b.getDouble(key, -1.0));

		assertEquals(-16.7, b.updateDouble(key, -30));
		assertEquals(-16.7, b.getDouble(key, -1.0));
	}

}
