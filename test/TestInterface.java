/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package test;

import core.CBRConnection;
import core.Connection;
import core.DTNHost;
import core.NetworkInterface;
import core.Settings;

public class TestInterface extends NetworkInterface {
	
	public TestInterface(Settings s) {
		super(s);
	}
	
	public TestInterface(TestInterface ti) {
		super(ti);
	}
	
	/**
	 * Replication function
	 */
	public NetworkInterface replicate() {
		return new TestInterface(this);
	}
	
	/**
	 * Gives the currentTransmit Speed
	 */
	public int getTransmitSpeed() {
		return transmitSpeed;
	}

	/**
	 * Gives the currentTransmit Range
	 */
	public double getTransmitRange() {
		return transmitRange;
	}
	
	/**
	 * Connects the interface to another interface.
	 * 
	 * Overload this in a derived class.  Check the requirements for
	 * the connection to work in the derived class, then call 
	 * connect(Connection, NetworkInterface) for the actual connection.
	 * @param anotherInterface The host to connect to
	 */
	public void connect(NetworkInterface anotherInterface) {
		Connection con = new CBRConnection(this.getHost(),this,
				anotherInterface.getHost(),anotherInterface, transmitSpeed);
		this.connect(con, anotherInterface);
	}

	/**
	 * Updates the state of current connections (ie tears down connections
	 * that are out of range, recalculates transmission speeds etc.).
	 */
	public void update() {
		for (int i=0; i<this.connections.size(); ) {
			Connection con = this.connections.get(i);
			NetworkInterface anotherInterface = con.getOtherInterface(this);

			// all connections should be up at this stage
			assert con.isUp() : "Connection " + con + " was down!";

			if (!isWithinRange(anotherInterface)) {
				disconnect(con,anotherInterface);
				connections.remove(i);
			}
			else {
				i++;
			}
		}
	}

	/** 
	 * Creates a connection to another host. This method does not do any checks
	 * on whether the other node is in range or active 
	 * (cf. {@link #connect(DTNHost)}).
	 * @param anotherHost The host to create the connection to
	 */
	public void createConnection(NetworkInterface anotherInterface) {
		connect(anotherInterface);
	}

}
