/*
 * Copyright 2014 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package interfaces;

import java.util.Collection;
import core.Connection;
import core.NetworkInterface;
import core.Settings;
import core.VBRConnection;

/**
 * A Network Interface that that takes in to account he distance from the
 * other (connected) interface when determining the capacity of the links.
 * The distance-dependent transmission speeds are given as comma-separated
 * values using setting {@link this#TRANSMIT_SPEEDS_S}.
 */
public class DistanceCapacityInterface extends NetworkInterface {

	/**
	 * Comma-separated list of speed values -setting id ({@value} ). The first
	 * value is the speed at distance 0 and the following are speeds at equal
	 * steps until the last one is the speed at the end of the transmit range (
	 * {@link NetworkInterface#TRANSMIT_RANGE_S}). The speed between the steps
	 * is linearly interpolated.
	 */
	public static final String TRANSMIT_SPEEDS_S = "transmitSpeeds";

	protected final int[] transmitSpeeds;

	/**
	 * Reads the interface settings from the Settings file
	 */
	public DistanceCapacityInterface(Settings s)	{
		super(s);
		transmitSpeeds = s.getCsvInts(TRANSMIT_SPEEDS_S);
	}

	/**
	 * Copy constructor
	 * @param ni the copied network interface object
	 */
	public DistanceCapacityInterface(DistanceCapacityInterface ni) {
		super(ni);
		transmitSpeeds = ni.transmitSpeeds;
	}

	public NetworkInterface replicate()	{
		return new DistanceCapacityInterface(this);
	}

	/**
	 * Tries to connect this host to another host. The other host must be
	 * active and within range of this host for the connection to succeed.
	 * @param anotherInterface The interface to connect to
	 */
	public void connect(NetworkInterface anotherInterface) {
		if (isScanning()
				&& anotherInterface.getHost().isRadioActive()
				&& isWithinRange(anotherInterface)
				&& !isConnected(anotherInterface)
				&& (this != anotherInterface)) {

			Connection con = new VBRConnection(this.host, this,
					anotherInterface.getHost(), anotherInterface);
			connect(con,anotherInterface);
		}
	}

	/**
	 * Updates the state of current connections (i.e. tears down connections
	 * that are out of range and creates new ones).
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
			}
			else {
				i++;
			}
		}
		// Then find new possible connections
		Collection<NetworkInterface> interfaces =
			optimizer.getNearInterfaces(this);
		for (NetworkInterface i : interfaces) {
			connect(i);
		}

		/* update all connections */
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
			Connection con = new VBRConnection(this.host, this,
					anotherInterface.getHost(), anotherInterface);
			connect(con,anotherInterface);
		}
	}

	/**
	 * Returns the transmit speed to another interface based on the
	 * distance to this interface
	 * @param ni The other network interface
	 */
	@Override
	public int getTransmitSpeed(NetworkInterface ni) {
		double distance;
		double fractionIndex;
		double decimal;
		double speed;
		int index;

		/* distance to the other interface */
		distance = ni.getLocation().distance(this.getLocation());

		if (distance >= this.transmitRange) {
			return 0;
		}

		/* interpolate between the two speeds */
		fractionIndex = (distance / this.transmitRange) *
				(this.transmitSpeeds.length - 1);
		index = (int)(fractionIndex);
		decimal = fractionIndex - index;

		speed = this.transmitSpeeds[index] * (1-decimal) +
				this.transmitSpeeds[index + 1] * decimal;

		return (int)speed;
	}

	/**
	 * Returns a string representation of the object.
	 * @return a string representation of the object.
	 */
	public String toString() {
		return "DistanceCapacityInterface " + super.toString();
	}

}
