/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import core.CBRConnection;
import core.DTNHost;
import core.Message;
import core.NetworkInterface;
import core.ModuleCommunicationBus;
import core.SimClock;

/**
 * Some tests for the Connection class.
 */
public class ConnectionTest extends TestCase {
	public static final double START_TIME = 10.0;
	private TestDTNHost h[];
	private CBRConnection c[];
	private Message m[];
	private int speed[] = {50, 50, 100, 200, 100};
	private int size[] = {50, 75, 100, 200, 1000};
	private int nrof = 5;
	private int index;
	private SimClock clock = SimClock.getInstance();
	private int conCount;

	protected void setUp() throws Exception {
		super.setUp();
		SimClock.reset();
		clock.setTime(START_TIME);
		TestSettings testSettings = new TestSettings();
		testSettings.setNameSpace(TestUtils.IFACE_NS);
		testSettings.putSetting(NetworkInterface.TRANSMIT_RANGE_S, "1.0");
		testSettings.putSetting(NetworkInterface.TRANSMIT_SPEED_S, "1");

		h = new TestDTNHost[nrof];
		c = new CBRConnection[nrof];
		m = new Message[nrof];

		for (int i=0; i< nrof; i++) {
			NetworkInterface ni = new TestInterface(testSettings);
			List<NetworkInterface> li = new ArrayList<NetworkInterface>();
			li.add(ni);

			ModuleCommunicationBus comBus = new ModuleCommunicationBus();
			h[i] = new TestDTNHost(li,comBus, testSettings);
			m[i] = new Message(h[0], h[i],""+i, size[i]);
		}

		con(h[0], h[1]);
		con(h[0], h[2]);
		con(h[1], h[3]);
		con(h[2], h[4]);
		con(h[3], h[4]);

		c[0].startTransfer(h[0], m[0]);
		c[1].startTransfer(h[0], m[1]);
		c[2].startTransfer(h[1], m[2]);
		conCount = 3;
	}

	private void con(DTNHost from, DTNHost to) {
		c[index] = new CBRConnection(from, from.getInterfaces().get(0), to, to.getInterfaces().get(0), speed[index]);
		index++;
	}

	public void testIsInitiator() {
		assertTrue(c[0].isInitiator(h[0]));
		assertFalse(c[0].isInitiator(h[1]));
		assertFalse(c[0].isInitiator(h[2]));
		assertTrue(c[3].isInitiator(h[2]));
	}

	public void testStartTransfer() {
		assertTrue(h[1].recvFrom == h[0]);
		assertTrue(h[1].recvMessage.getId().equals(m[0].getId()));
		assertTrue(h[2].recvFrom == h[0]);
		assertTrue(h[2].recvMessage.getId().equals(m[1].getId()));
	}

	public void testAbortTransfer() {
		assertTrue(h[1].abortedId == null);
		assertFalse(c[0].isMessageTransferred());

		c[0].abortTransfer();

		assertTrue(h[1].abortedId != null);
		assertTrue(h[1].abortedId.equals(m[0].getId()));
		assertTrue(c[0].isMessageTransferred());
	}

	public void testGetTransferDoneTime() {
		double doneTime;

		doneTime = START_TIME + (1.0 * m[0].getSize()) / speed[0];
		assertEquals(doneTime, c[0].getTransferDoneTime());

		doneTime = START_TIME + (1.0 * m[1].getSize()) / speed[1];
		assertEquals(doneTime, c[1].getTransferDoneTime());
	}

	public void testGetRemainingByteCount() {
		double STEP = 0.1;
		int transferred;

		assertEquals(size[0], c[0].getRemainingByteCount());
		assertEquals(size[1], c[1].getRemainingByteCount());

		clock.setTime(START_TIME + STEP);
		for (int i=0; i<this.conCount; i++) {
			transferred = (int)Math.ceil(STEP * speed[i]);
			assertEquals("index " + i,
					size[i] - transferred, c[i].getRemainingByteCount());
		}

		clock.setTime(START_TIME + STEP * 5);
		for (int i=0; i<this.conCount; i++) {
			transferred = (int)Math.ceil(STEP * 5 * speed[i]);
			assertEquals("index " + i,
					size[i] - transferred,
					c[i].getRemainingByteCount());
		}
	}

	public void testIsMessageTransferred() {
		assertFalse(c[0].isMessageTransferred()); /* takes 1.0 seconds */
		assertFalse(c[1].isMessageTransferred()); /* takes 1.5 seconds */

		clock.advance(0.9);
		assertFalse(c[0].isMessageTransferred());
		assertFalse(c[1].isMessageTransferred());

		clock.advance(0.1); /* at 1.0 */
		assertTrue(c[0].isMessageTransferred());
		assertFalse(c[1].isMessageTransferred());

		clock.advance(0.1);
		assertFalse(c[1].isMessageTransferred());
		clock.advance(0.3); /* at 1.4 */
		assertFalse(c[1].isMessageTransferred());

		clock.advance(0.19); /* at 1.49 */
		assertTrue(c[1].isMessageTransferred());

		clock.advance(0.01); /* at 1.5 */
		assertTrue(c[1].isMessageTransferred());
	}


	public void testFinalizeTransfer() {
		assertFalse(c[0].isMessageTransferred());
		c[0].finalizeTransfer(); /* this doesn't check time */
		assertTrue(c[0].isMessageTransferred());
	}

	public void testIsReadyForTransfer() {
		assertFalse(c[0].isReadyForTransfer());
		assertFalse(c[1].isReadyForTransfer());
		assertTrue(c[3].isReadyForTransfer());

		c[0].finalizeTransfer();
		assertTrue(c[0].isReadyForTransfer());
	}

	public void testGetTotalBytesTransferred() {
		int count = 0;

		for (int i=0; i<10; i++) {
			assertEquals("at " + SimClock.getTime(),
					count, c[0].getTotalBytesTransferred());
			clock.advance(0.1);
			count += (int)(speed[0] * 0.1);
		}

		/* transfer should be ready now */
		clock.advance(0.1);
		assertEquals("at " + SimClock.getTime(),
				count, c[0].getTotalBytesTransferred());

		clock.advance(5.3);
		assertEquals("at " + SimClock.getTime(),
				count, c[0].getTotalBytesTransferred());

		c[0].finalizeTransfer(); /* should not affect */
		assertEquals(count, c[0].getTotalBytesTransferred());

		c[0].startTransfer(h[0],  new Message(h[0], h[0],"tst", 50000));

		clock.advance(345.67); /* just some value */
		count += (int)(speed[0] * 345.67);
		/* 1 byte difference is OK (due rounding) */
		assertEquals(count, c[0].getTotalBytesTransferred(), 1);
	}

	public void testGetOtherNode() {
		assertEquals(h[1], c[0].getOtherNode(h[0]));
		assertEquals(h[0], c[0].getOtherNode(h[1]));
	}

}
