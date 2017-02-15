/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

/**
 * A message related external event
 */
public abstract class MessageEvent extends ExternalEvent {
    public enum MessageType {
        ONE_TO_ONE,
        BROADCAST
	}

	/** address of the node the message is from */
	protected int fromAddr;
	/** address of the node the message is to */
	protected int toAddr;
	/** identifier of the message */
	protected String id;
	/** message type **/
	protected MessageType type;

	/**
	 * Creates a message  event
	 * @param from Where the message comes from
	 * @param to Who the message goes to
	 * @param id ID of the message
	 * @param time Time when the message event occurs
	 */
	public MessageEvent(int from, int to, String id, double time) {
        this(from, to, id, time, MessageType.ONE_TO_ONE);
	}

	/**
	 * Creates a message event.
	 * @param from Where the message comes from
	 * @param to Who the message goes to
	 * @param id ID of the message
	 * @param time Time when the message event occurs
	 * @param type Type of the message
	 */
	public MessageEvent(int from, int to, String id, double time, MessageType type) {
		super(time);
		this.fromAddr = from;
		this.toAddr= to;
		this.id = id;
		this.type = type;
	}

	@Override
	public String toString() {
		return "MSG @" + this.time + " " + id;
	}
}
