/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

import core.DTNHost;
import core.World;

/**
 * A connection up/down event.
 */
public class ConnectionEvent extends ExternalEvent {
	/** address of the node the (dis)connection is from */
	protected int fromAddr;
	/** address of the node the (dis)connection is to */
	protected int toAddr;
	/** Is this a "connection up" event*/
	protected boolean isUp;
	/** What is the interface number for this event*/
	protected String interfaceId;
	
	/**
	 * Creates a new connection event
	 * @param from End point of connection
	 * @param to Another end of connection
	 * @param interf The number of interface for the connection
	 * @param up If true, this was a "connection up" event, if false, this
	 *  was a "connection down" event
	 * @param time Time when the Connection event occurs
	 */
	public ConnectionEvent(int from, int to, String interf, boolean up, double time) {
		super(time);
		assert to != from : "Can't self connect";
		this.fromAddr = from;
		this.toAddr= to;
		this.isUp = up;
		this.interfaceId = interf;
	}
	
	@Override
	public void processEvent(World world) {
		DTNHost from = world.getNodeByAddress(this.fromAddr);
		DTNHost to = world.getNodeByAddress(this.toAddr);
		
		from.forceConnection(to, interfaceId, this.isUp);
	}
	
	@Override
	public String toString() {
		return "CONN " + (isUp ? "up" : "down") + " @" + this.time + " " + 
				this.fromAddr+"<->"+this.toAddr;
	}
}
