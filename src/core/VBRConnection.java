/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

import routing.MessageRouter;

/**
 * A connection between two DTN nodes.  The transmission speed
 * is updated every round from the end point transmission speeds
 */
public class VBRConnection extends Connection {
	private int msgsize;
	private int msgsent;
	private int currentspeed = 0;
	private double lastUpdate = 0;


	/**
	 * Creates a new connection between nodes and sets the connection
	 * state to "up".
	 * @param fromNode The node that initiated the connection
	 * @param fromInterface The interface that initiated the connection
	 * @param toNode The node in the other side of the connection
	 * @param toInterface The interface in the other side of the connection
	 */
   public VBRConnection(DTNHost fromNode, NetworkInterface fromInterface,
		   DTNHost toNode, NetworkInterface toInterface) {
	    super(fromNode, fromInterface, toNode, toInterface);
		this.msgsent = 0;
	}

	/**
	 * Sets a message that this connection is currently transferring. If message
	 * passing is controlled by external events, this method is not needed
	 * (but then e.g. {@link #finalizeTransfer()} and
	 * {@link #isMessageTransferred()} will not work either). Only a one message
	 * at a time can be transferred using one connection.
	 * @param from The host sending the message
	 * @param m The message
	 * @return The value returned by
	 * {@link MessageRouter#receiveMessage(Message, DTNHost)}
	 */
	public int startTransfer(DTNHost from, Message m) {
		assert this.msgOnFly == null : "Already transferring " +
			this.msgOnFly + " from " + this.msgFromNode + " to " +
			this.getOtherNode(this.msgFromNode) + ". Can't "+
			"start transfer of " + m + " from " + from;

		this.msgFromNode = from;
		Message newMessage = m.replicate();
		int retVal = getOtherNode(from).receiveMessage(newMessage, from);

		if (retVal == MessageRouter.RCV_OK) {
			this.msgOnFly = newMessage;
			this.msgsize = m.getSize();
			this.msgsent = 0;
		}

		return retVal;
	}

	/**
	 * Calculate the current transmission speed from the information
	 * given by the interfaces, and calculate the missing data amount.
	 *
	 */
	public void update() {
		currentspeed =  this.fromInterface.getTransmitSpeed(toInterface);
		int othspeed =  this.toInterface.getTransmitSpeed(fromInterface);
		double now = core.SimClock.getTime();

		if (othspeed < currentspeed) {
			currentspeed = othspeed;
		}


		msgsent += currentspeed * (now - this.lastUpdate);
		this.lastUpdate = now;
	}

	/**
	 * returns the current speed of the connection
	 */
	public double getSpeed() {
		return this.currentspeed;
	}

    /**
     * Returns the amount of bytes to be transferred before ongoing transfer
     * is ready or 0 if there's no ongoing transfer or it has finished
     * already
     * @return the amount of bytes to be transferred
     */
    public int getRemainingByteCount() {
	int bytesLeft = msgsize - msgsent;
	return (bytesLeft > 0 ? bytesLeft : 0);
    }

	/**
	 * Returns true if the current message transfer is done.
	 * @return True if the transfer is done, false if not
	 */
	public boolean isMessageTransferred() {
		if (msgsent >= msgsize) {
			return true;
		} else {
			return false;
		}
	}

}
