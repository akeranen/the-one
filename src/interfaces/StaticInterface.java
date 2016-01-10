/*
 * Copyright 2016 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package interfaces;

import java.util.ArrayList;

import core.CBRConnection;
import core.Connection;
import core.NetworkInterface;
import core.Settings;
import core.SimError;

/**
 * A simple static Network Interface with constant bit-rate connections. 
 * This interface does not do connectivity simulation but can be used
 * e.g., to create backbone network with (external) connectivity events.
 */
public class StaticInterface extends NetworkInterface {

	/**
	 * Reads the interface settings from the Settings file
	 */
	public StaticInterface(Settings s)	{
		this.interfacetype = s.getNameSpace();
		this.connections = new ArrayList<Connection>();

		this.transmitRange = 0;
		this.transmitSpeed = s.getInt(TRANSMIT_SPEED_S);
		s.ensurePositiveValue(transmitRange, TRANSMIT_RANGE_S);
		s.ensurePositiveValue(transmitSpeed, TRANSMIT_SPEED_S);
	}

	/**
	 * Copy constructor
	 * @param ni the copied network interface object
	 */
	public StaticInterface(StaticInterface ni) {
		super(ni);
	}

	public NetworkInterface replicate()	{
		return new StaticInterface(this);
	}

	/**
	 * This method is not used by StaticInterface
	 */
	public void connect(NetworkInterface anotherInterface) {
		throw new SimError("Not in use");
	}

	/**
	 * Does nothing (this interface does not do simulation)
	 */
	public void update() {
		return; 
	}

	/**
	 * Creates a connection to another host. This method does not do any checks
	 * on whether the other node is in range or active
	 * @param anotherInterface The interface to create the connection to
	 */
	public void createConnection(NetworkInterface anotherInterface) {
		if (!isConnected(anotherInterface) && (this != anotherInterface)) {
			// connection speed is the lower one of the two speeds
			int conSpeed = anotherInterface.getTransmitSpeed(this);
			if (conSpeed > this.transmitSpeed) {
				conSpeed = this.transmitSpeed;
			}

			Connection con = new CBRConnection(this.host, this,
					anotherInterface.getHost(), anotherInterface, conSpeed);
			connect(con,anotherInterface);
		}
	}

	/**
	 * Returns a string representation of the object.
	 * @return a string representation of the object.
	 */
	public String toString() {
		return "StaticInterface " + super.toString();
	}

}
