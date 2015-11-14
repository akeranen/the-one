/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import core.DTNHost;
import core.World;

/**
 * External event for all the stages of relaying a message between two
 * hosts (start and possible abort or delivery).
 */
public class MessageRelayEvent extends MessageEvent {
	private int stage;

	/** Message relay stage constant for start of sending */
	public static final int SENDING = 1;
	/** Message relay stage constant for ready delivery */
	public static final int TRANSFERRED = 2;
	/** Message relay stage constant for aborted delivery */
	public static final int ABORTED = 3;
	/** Stage constant -> String representation mapping */
	public static final String[] STAGE_STRINGS = {"SENDING",
		"TRANSFERRED", "ABORTED"};

	/**
	 * Creates a message relaying event
	 * @param from Where the message comes from (at this hop)
	 * @param to Who the message goes to (at this hop)
	 * @param id ID of the message
	 * @param time Time when this event happens
	 * @param stage The stage of the event (SENDING, TRANSFERRED, or ABORTED)
	 */
	public MessageRelayEvent(int from, int to, String id, double time,
			int stage) {
		super(from, to, id, time);
		this.stage = stage;
	}

	/**
	 * Relays the message
	 */
	public void processEvent(World world) {
		// get DTNHosts and pass messages between them
		DTNHost from = world.getNodeByAddress(this.fromAddr);
		DTNHost to = world.getNodeByAddress(this.toAddr);

		switch(stage) {
		case SENDING:
			from.sendMessage(id, to);
			break;
		case TRANSFERRED:
			to.messageTransferred(id, from);
			break;
		case ABORTED:
			to.messageAborted(id, from, -1);
			break;
		default:
			assert false : "Invalid stage (" + stage + ") for " + this;
		}
	}

	@Override
	public String toString() {
		return super.toString() + " [" + fromAddr + "->" + toAddr + "] " +
		STAGE_STRINGS[stage-1];
	}

}
