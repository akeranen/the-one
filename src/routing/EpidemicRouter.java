/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import applications.DatabaseApplication;
import core.Connection;
import core.Message;
import core.Settings;
import routing.util.DatabaseApplicationUtil;
import util.Tuple;

import java.util.List;

/**
 * Epidemic message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class EpidemicRouter extends ActiveRouter {

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
		if (isTransferring() || (!canStartTransfer() && !hasDatabase())) {
			return; // transferring, don't try other connections yet
		}

		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}

		// then try any/all message to any/all connection
		this.tryAllMessagesToAllConnections();
		this.tryDataMessages();
	}

	private boolean hasDatabase() {
		return DatabaseApplicationUtil.findDatabaseApplication(this) != null;
	}

	private Tuple<Message, Connection> tryDataMessages() {

		DatabaseApplication application = DatabaseApplicationUtil.findDatabaseApplication(this);
		if (application == null) {
			return null;
		}
		List<Tuple<Message, Connection>> messages =
				DatabaseApplicationUtil.createDataMessages(this, this.getHost(), this.getConnections());

		if (messages.isEmpty()) {
			return null;
		}
		// try to send messages
		return tryMessagesForConnected(messages);
	}


	@Override
	public EpidemicRouter replicate() {
		return new EpidemicRouter(this);
	}

}
