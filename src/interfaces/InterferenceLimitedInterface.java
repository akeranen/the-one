/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package interfaces;

import java.util.Collection;

import core.Connection;
import core.NetworkInterface;
import core.Settings;
import core.VBRConnection;

/**
 * A simple Network Interface that provides a variable bit-rate service, where
 * the bit-rate depends on the number of other transmitting stations within
 * range The current transmit speed is updated only if there are ongoing
 * transmissions. The configured transmit speed is the maximum obtainable speed.
 */
public class InterferenceLimitedInterface extends NetworkInterface {
	protected int currentTransmitSpeed;
	protected int numberOfTransmissions;

	public InterferenceLimitedInterface(Settings s) {
		super(s);
		this.currentTransmitSpeed = 0;
		this.numberOfTransmissions = 0;
	}

	/**
	 * Copy constructor
	 * @param ni the copied network interface object
	 */
	public InterferenceLimitedInterface(InterferenceLimitedInterface ni) {
		super(ni);
		this.transmitRange = ni.transmitRange;
		this.transmitSpeed = ni.transmitSpeed;
		this.currentTransmitSpeed = 0;
		this.numberOfTransmissions = 0;
	}


	public NetworkInterface replicate() {
		return new InterferenceLimitedInterface(this);
	}

	/**
	 * Returns the transmit speed of this network layer
	 * @return the transmit speed
	 */
	@Override
	public int getTransmitSpeed(NetworkInterface ni) {
		return this.currentTransmitSpeed;
	}

	/**
	 * Tries to connect this host to another host. The other host must be
	 * active and within range of this host for the connection to succeed.
	 * @param anotherInterface The host to connect to
	 */
	public void connect(NetworkInterface anotherInterface) {
		if (isScanning()
				&& anotherInterface.getHost().isRadioActive()
				&& isWithinRange(anotherInterface)
				&& !isConnected(anotherInterface)
				&& (this != anotherInterface)) {
			// new contact within range

			Connection con = new VBRConnection(this.host, this,
					anotherInterface.getHost(), anotherInterface);
			connect(con, anotherInterface);
		}
	}

	/**
	 * Updates the state of current connections (i.e., tears down connections
	 * that are out of range).
	 */
	public void update() {
		if (optimizer == null) {
			return; /* nothing to do */
		}

		// First break the old ones
		optimizer.updateLocation(this);
		for (int i=0; i<this.connections.size(); ) {
			Connection con = this.connections.get(i);
			NetworkInterface anotherInterface = con.getOtherInterface(this);

			// all connections should be up at this stage
			assert con.isUp() : "Connection " + con + " was down!";

			if (!isWithinRange(anotherInterface)) {
				disconnect(con,anotherInterface);
				connections.remove(i);
			} else {
				i++;
			}
		}
		// Then find new possible connections
		Collection<NetworkInterface> interfaces =
			optimizer.getNearInterfaces(this);
		for (NetworkInterface i : interfaces)
			connect(i);

		// Find the current number of transmissions
		// (to calculate the current transmission speed
		numberOfTransmissions = 0;
		int numberOfActive = 1;
		for (Connection con : this.connections) {
			if (con.getMessage() != null) {
				numberOfTransmissions++;
			}
			if (((InterferenceLimitedInterface)con.getOtherInterface(this)).
					isTransferring() == true) {
				numberOfActive++;
			}
		}

		int ntrans = numberOfTransmissions;
		if ( numberOfTransmissions < 1) ntrans = 1;
		if ( numberOfActive <2 ) numberOfActive = 2;

		// Based on the equation of Gupta and Kumar - and the transmission speed
		// is divided equally to all the ongoing transmissions
		currentTransmitSpeed = (int)Math.floor((double)transmitSpeed /
				(Math.sqrt((1.0*numberOfActive) *
						Math.log(1.0*numberOfActive))) /
							ntrans );

		for (Connection con : getConnections()) {
			con.update();
		}
	}

	/**
	 * Creates a connection to another host. This method does not do any checks
	 * on whether the other node is in range or active
	 * @param anotherInterface The interface to create the connection to
	 */
	public void createConnection(NetworkInterface anotherInterface) {
		if (!isConnected(anotherInterface) && (this != anotherInterface)) {
			// new contact within range

			Connection con = new VBRConnection(this.host, this,
					anotherInterface.getHost(), anotherInterface);
			connect(con,anotherInterface);
		}
	}

	/**
	 * Returns true if this interface is actually transmitting data
	 */
	public boolean isTransferring() {
		return (numberOfTransmissions > 0);
	}

	/**
	 * Returns a string representation of the object.
	 * @return a string representation of the object.
	 */
	public String toString() {
		return "InterfaceLimitedInterface " + super.toString();
	}

}
