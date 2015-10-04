/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import input.StandardEventsReader;

/**
 * Report that creates same output as the GUI's event log panel but formatted
 * like {@link input.StandardEventsReader} input. Message relying event has
 * extra one-letter identifier to tell whether that message was delivered to
 * final destination, delivered there again, or just normally relayed 
 * (see the public constants).
 */
public class EventLogReport extends Report 
	implements ConnectionListener, MessageListener {

	/** Extra info for message relayed event ("relayed"): {@value} */
	public static final String MESSAGE_TRANS_RELAYED = "R";
	/** Extra info for message relayed event ("delivered"): {@value} */
	public static final String MESSAGE_TRANS_DELIVERED = "D";
	/** Extra info for message relayed event ("delivered again"): {@value} */
	public static final String MESSAGE_TRANS_DELIVERED_AGAIN = "A";
	
	/**
	 * Processes a log event by writing a line to the report file
	 * @param action The action as a string
	 * @param host1 First host involved in the event (if any, or null)
	 * @param host2 Second host involved in the event (if any, or null)
	 * @param message The message involved in the event (if any, or null)
	 * @param extra Extra info to append in the end of line (if any, or null)
	 */
	private void processEvent(final String action, final DTNHost host1, 
			final DTNHost host2, final Message message, final String extra) {
		write(getSimTime() + " " + action + " " + (host1 != null ? host1 : "")
				+ (host2 != null ? (" " + host2) : "")
				+ (message != null ? " " + message : "")
				+ (extra != null ? " " + extra : ""));
	}
	
	public void hostsConnected(DTNHost host1, DTNHost host2) {
		processEvent(StandardEventsReader.CONNECTION, host1, host2, null,
				StandardEventsReader.CONNECTION_UP);
	}

	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		processEvent(StandardEventsReader.CONNECTION, host1, host2, null,
				StandardEventsReader.CONNECTION_DOWN);
	}

	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		processEvent((dropped ? StandardEventsReader.DROP : 
			StandardEventsReader.REMOVE), where, null, m, null);
	}

	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean firstDelivery) {
		String extra;
		if (firstDelivery) {
			extra = MESSAGE_TRANS_DELIVERED;
		}
		else if (to == m.getTo()) {
			extra = MESSAGE_TRANS_DELIVERED_AGAIN;
		}
		else {
			extra = MESSAGE_TRANS_RELAYED;
		}
		
		processEvent(StandardEventsReader.DELIVERED, from, to, m, extra);
	}

	public void newMessage(Message m) {
		processEvent(StandardEventsReader.CREATE, m.getFrom(), null, m, null);
	}
	
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		processEvent(StandardEventsReader.ABORT, from, to, m, null);
	}
	
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		processEvent(StandardEventsReader.SEND, from, to, m, null);		
	}
}