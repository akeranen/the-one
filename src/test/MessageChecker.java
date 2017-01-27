/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import java.util.ArrayList;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Message event checker for tests.
 */
public class MessageChecker implements MessageListener {
	private Message lastMsg;
	private DTNHost lastFrom;
	private DTNHost lastTo;
	private Boolean lastDropped;
	private Boolean lastFirstDelivery;
	private String lastType;
	private ArrayList<MsgCheckerEvent> queue;

	public final String TYPE_NONE = "none";
	public final String TYPE_DELETE = "delete";
	public final String TYPE_ABORT = "abort";
	public final String TYPE_RELAY = "relay";
	public final String TYPE_CREATE = "create";
	public final String TYPE_START = "start";

	public MessageChecker() {
		reset();
	}

	public void reset() {
		this.queue = new ArrayList<MsgCheckerEvent>();
		this.lastType = TYPE_NONE;
		this.lastMsg = null;
		this.lastFrom = null;
		this.lastTo = null;
		this.lastDropped = null;
		this.lastFirstDelivery = null;
	}

	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		this.add(m, where, null, TYPE_DELETE, dropped, null);
	}

	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		this.add(m, from, to, TYPE_ABORT, null, null);
	}

	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean firstDelivery) {
		this.add(m, from, to, TYPE_RELAY, null, firstDelivery);
	}

	public void newMessage(Message m) {
		this.add(m, m.getFrom(), m.getTo(), TYPE_CREATE, null, null);
	}


	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		this.add(m, from, to, TYPE_START, null, null);
	}

	public boolean next() {
		if (this.queue.size() == 0) {
			return false;
		}

		MsgCheckerEvent e = this.queue.remove(0);

		this.lastMsg = e.msg;
		this.lastFrom = e.from;
		this.lastTo = e.to;
		this.lastType = e.type;
		this.lastFirstDelivery = e.delivered;
		this.lastDropped = e.dropped;
		return true;

	}

	private void add(Message m, DTNHost from, DTNHost to, String type, Boolean
			dropped, Boolean delivered) {
		this.queue.add(new MsgCheckerEvent(m,from,to,type,dropped,delivered));
	}

	/**
	 * @return the lastFirstDelivery
	 */
	public Boolean getLastFirstDelivery() {
		return lastFirstDelivery;
	}

	/**
	 * @return the lastDropped
	 */
	public Boolean getLastDropped() {
		return lastDropped;
	}

	/**
	 * @return the lastFrom
	 */
	public DTNHost getLastFrom() {
		return lastFrom;
	}

	/**
	 * @return the lastMsg
	 */
	public Message getLastMsg() {
		return lastMsg;
	}

	/**
	 * @return the lastTo
	 */
	public DTNHost getLastTo() {
		return lastTo;
	}

	/**
	 * @return the lastType
	 */
	public String getLastType() {
		return lastType;
	}

	public String toString() {
		return this.queue.size() + " event(s) : " + this.queue;
	}

	private class MsgCheckerEvent {
		private Message msg;
		private DTNHost from;
		private DTNHost to;
		private Boolean dropped;
		private Boolean delivered;
		private String type;

		public MsgCheckerEvent(Message m, DTNHost from, DTNHost to,
				String type, Boolean dropped, Boolean delivered) {
			this.msg = m;
			this.from = from;
			this.to = to;
			this.type = type;
			this.dropped = dropped;
			this.delivered = delivered;
		}

		public String toString() {
			return this.type + " (" + this.from + "->" + this.to+") " +
			this.msg;
		}
	}
}
