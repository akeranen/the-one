/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.Settings;

/**
 * Router that will deliver messages only to the final recipient.
 */
public class DirectDeliveryRouter extends ActiveRouter {

	public DirectDeliveryRouter(Settings s) {
		super(s);
	}

	protected DirectDeliveryRouter(DirectDeliveryRouter r) {
		super(r);
	}

	@Override
	public void update() {
		super.update();
		if (!canStartTransfer()) {
			return; // can't start a new transfer
		}

		// Try only the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer
		}
	}

	@Override
	public DirectDeliveryRouter replicate() {
		return new DirectDeliveryRouter(this);
	}
}
