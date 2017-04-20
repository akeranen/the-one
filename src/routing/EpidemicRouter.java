/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

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
		//find data messages
		List<Tuple<Message, Connection>> dataMessages =
				DatabaseApplicationUtil.createDataMessages(this, this.getHost(), this.getConnections());
		// try to sending data messages
		tryMessagesForConnected(dataMessages);
	}

	private boolean hasDatabase() {
		return DatabaseApplicationUtil.findDatabaseApplication(this) != null;
	}


	@Override
	public EpidemicRouter replicate() {
		return new EpidemicRouter(this);
	}

}
