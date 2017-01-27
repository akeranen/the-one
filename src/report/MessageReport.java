/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Reports delivered messages
 * report:
 *  message_id creation_time deliver_time (duplicate)
 */
public class MessageReport extends Report implements MessageListener {
	public static final String HEADER =
	    "# messages: ID, start time, end time";
	/** all message delays */

	/**
	 * Constructor.
	 */
	public MessageReport() {
		init();
	}

	@Override
	public void init() {
		super.init();
		write(HEADER);
	}

	public void newMessage(Message m) {}

	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean firstDelivery) {
		if (firstDelivery) {
			write(m.getId() + " "
					+ format(m.getCreationTime()) + " "
					+ format(getSimTime()));
		} else {
			if (to.getAddress() == m.getTo().getAddress()) {
				write(m.getId() + " "
						+ format(m.getCreationTime()) + " "
						+ format(getSimTime()) + " duplicate");
			}
		}
	}

	@Override
	public void done() {
		super.done();
	}

	// nothing to implement for the rest
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}

}
