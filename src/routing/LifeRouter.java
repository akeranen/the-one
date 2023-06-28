/*
 * Copyright 2011 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import java.util.Vector;

import core.DTNHost;
import core.Message;
import core.Settings;
import core.Connection;

/**
 * Router module mimicking the game-of-life behavior
 */
public class LifeRouter extends ActiveRouter {

	/**
	 * Neighboring message count -setting id ({@value}). Two comma
	 * separated values: min and max. Only if the amount of connected nodes
	 * with the given message is between the min and max value, the message
	 * is accepted for transfer and kept in the buffer.
	 */
	public static final String NM_COUNT_S = "nmcount";
	private int countRange[];

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public LifeRouter(Settings s) {
		super(s);
		countRange = s.getCsvInts(NM_COUNT_S, 2);
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected LifeRouter(LifeRouter r) {
		super(r);
		this.countRange = r.countRange;
	}

	/**
	 * Counts how many of the connected peers have the given message
	 * @param m The message to check
	 * @return Amount of connected peers with the message
	 */
	private int getPeerMessageCount(Message m) {
		DTNHost me = getHost();
		String id = m.getId();
		int peerMsgCount = 0;

		for (Connection c : getConnections()) {
			if (c.getOtherNode(me).getRouter().hasMessage(id)) {
				peerMsgCount++;
			}
		}

		return peerMsgCount;
	}

	@Override
	protected int checkReceiving(Message m, DTNHost from) {
		int peerMsgCount = getPeerMessageCount(m);

		if (peerMsgCount < this.countRange[0] ||
				peerMsgCount > this.countRange[1]) {
			return DENIED_POLICY;
		}

		/* peer message count check OK; receive based on other checks */
		return super.checkReceiving(m, from);
	}

	@Override
	public void update() {
		int peerMsgCount;
		Vector<String> messagesToDelete = new Vector<String>();
		super.update();

		if (!canStartTransfer()) {
			return; /* transferring, don't try other connections yet */
		}

		/* Try first the messages that can be delivered to final recipient */
		if (exchangeDeliverableMessages() != null) {
			return;
		}
		this.tryAllMessagesToAllConnections();

		/* see if need to drop some messages... */
		for (Message m : getMessageCollection()) {
			peerMsgCount = getPeerMessageCount(m);
			if (peerMsgCount < this.countRange[0] ||
					peerMsgCount > this.countRange[1]) {
				messagesToDelete.add(m.getId());
			}
		}
		for (String id : messagesToDelete) { /* ...and drop them */
			this.deleteMessage(id, true);
		}

	}


	@Override
	public LifeRouter replicate() {
		return new LifeRouter(this);
	}

}
