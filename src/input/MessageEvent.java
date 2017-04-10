/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

/**
 * A message related external event
 */
public abstract class MessageEvent extends ExternalEvent {
    /** fixed priority for one-to-one messages */
    public static final int MESSAGE_PRIORITY = 0;
	/** address of the node the message is from */
	protected int fromAddr;
	/** address of the node the message is to */
	protected int toAddr;
	/** identifier of the message */
	protected String id;
	/** priority of the message */
	protected int priority;

	/**
	 * Creates a message event.
	 * @param from Where the message comes from
	 * @param to Who the message goes to
	 * @param id ID of the message
	 * @param time Time when the message event occurs
	 * @param prio Priority of the message
	 */
	public MessageEvent(int from, int to, String id, double time, int prio) {
		super(time);
		this.fromAddr = from;
		this.toAddr= to;
		this.id = id;
		this.priority = prio;
	}

	@Override
	public String toString() {
		return "MSG @" + this.time + " " + id;
	}
}
