/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import applications.DatabaseApplication;
import core.Connection;
import core.DTNHost;
import core.DataMessage;
import core.Message;
import core.Settings;
import core.SimClock;
import routing.util.DatabaseApplicationUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Epidemic message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class EpidemicRouter extends ActiveRouter {
    private static final double FIFTY_PERCENT = 0.5;

    /**
     * Cache for data messages to send in a certain update interval.
     */
    private List<DataMessage> currentDataMessageCache = new ArrayList<>();

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public EpidemicRouter(Settings s) {
		super(s);
		//TODO: read&use epidemic router specific settings (if any)
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected EpidemicRouter(EpidemicRouter r) {
		super(r);
		//TODO: copy epidemic settings here (if any)
	}

	@Override
	public void update() {
		super.update();
		if (isTransferring() || !this.canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}

		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}

		// then try any/all message to any/all connection
		this.tryAllMessagesToAllConnections();
		// Finally, reset database synchronization message cache.
		this.currentDataMessageCache.clear();
	}

    /**
     * Checks whether this router has anything to send out.
     *
     * @return Whether or not the router has anything to send out.
     */
    @Override
    protected boolean hasNothingToSend() {
        return DatabaseApplicationUtil.hasNoMessagesToSend(this);
    }

    /**
     * Goes through the messages until the other node accepts one
     * for receiving (or doesn't accept any). If a transfer is started, the
     * connection is included in the list of sending connections.
     *
     * In this specific implementation, in addition to the provided messages, database synchronization is tried at
     * random times.
     *
     * @param con      Connection trough which the messages are sent
     * @param messages A list of messages to try
     * @return The message whose transfer was started or null if no
     * transfer was started.
     */
    @Override
    protected Message tryAllMessages(Connection con, List<Message> messages) {
        // Make sure we know which data messages to send.
        if (this.currentDataMessageCache.isEmpty()) {
            this.currentDataMessageCache = this.findCurrentDataMessages();
        }

        // Determine neighbor.
        DTNHost receiver = con.getOtherNode(this.getHost());

        // Go through the messages:
        Iterator<DataMessage> dataMessageIterator = this.currentDataMessageCache.iterator();
        Iterator<Message> messageIterator = messages.iterator();
        Random messageTypeChooser = new Random(SimClock.getIntTime() ^ receiver.getAddress());
        while (messageIterator.hasNext() || dataMessageIterator.hasNext()) {
            // If we still have data messages: Choose whether to send a data message or not.
            // While we still have both kinds of messages, data is sent with a 50% chance.
            boolean tryDataMessage = dataMessageIterator.hasNext()
                    && (!messageIterator.hasNext() || messageTypeChooser.nextDouble() < FIFTY_PERCENT);
            Message messageCandidate;
            if (tryDataMessage) {
                messageCandidate = dataMessageIterator.next().instantiateFor(receiver);
            } else {
                messageCandidate = messageIterator.next();
            }

            // Try to send the message.
            int retVal = this.startTransfer(messageCandidate, con);
            if (retVal == RCV_OK) {
                return messageCandidate;
            } else if (retVal > 0) {
                // Return value indicates that we should try later --> no message will be accepted right now.
                return null;
            }
        }

        // No message was accepted.
        return null;
    }

    /**
     * Builds up data messages prototypes (without receivers) for data the host has stored and believes to be relevant
     * right now.
     * @return A list of those data messages.
     */
    private List<DataMessage> findCurrentDataMessages() {
        // If no database application exists, return empty list.
        DatabaseApplication dbApp = DatabaseApplicationUtil.findDatabaseApplication(this);
        if (dbApp == null) {
            return new ArrayList<>(0);
        }

        // Else return current data messages from relevant existing data.
        return dbApp.createDataMessages(this.getHost());
    }

	@Override
	public EpidemicRouter replicate() {
		return new EpidemicRouter(this);
	}

}
