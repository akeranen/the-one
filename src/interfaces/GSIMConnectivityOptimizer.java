package interfaces;

import core.Connection;
import core.NetworkInterface;
import core.Settings;
import core.SettingsError;
import input.GSIMConnector;
import movement.GSIMMovementEngine;
import movement.MovementEngine;

import java.util.List;

/**
 * Provides GPU accelerated connectivity detection as an extension to an GSIMMovementEngine.
 * Attention: Connectivity detection currently only supports a single interface type and only 1 interface per host!
 */
public class GSIMConnectivityOptimizer extends ConnectivityOptimizer {
	/** Class name */
	public static final String NAME = "GSIMConnectivityOptimizer";
	/** Disable gpu link events, link events -setting id ({@value})*/
	public static final String DISABLE_GPU_LINK_EVENTS_S = "disableGPULinkEvents";

	/** Connector for IPC */
	private GSIMConnector connector = null;
	/** List of managed interfaces */
	private List<NetworkInterface> interfaces = null;
	/** This disables link event filtering on the GPU, copies all collisions and filters locally */
	private boolean disableGPULinkEvents = false;

	public GSIMConnectivityOptimizer(GSIMConnector connector, List<NetworkInterface> interfaces) {
		this.connector = connector;
		this.interfaces = interfaces;

		// Sanity checks
		for (int i = 0; i < interfaces.size(); i++) {
			if (interfaces.get(i).getHost().getID() != i) {
				throw new SettingsError("GSIMConnectivityOptimizer requires one interface the same type for every host!");
			}
		}

		Settings s = new Settings(GSIMMovementEngine.NAME);
		disableGPULinkEvents = s.getBoolean(DISABLE_GPU_LINK_EVENTS_S, false);

	}

	// TODO move to debug_utils class
	private String toHumanTime(long nanos) {
		if (nanos > 1000000000) {
			// at least one second
			return (nanos / 1000000000) + " s";
		}
		if (nanos > 1000000) {
			// less than a second, but at least one millisecond
			return (nanos / 1000000) + " ms";
		}
		if (nanos > 1000) {
			// less than a millisecond, but at least one microsecond
			return (nanos / 1000) + " us";
		}
		// less than a microsecond
		return nanos + " ns";
	}

	/**
	 * Detects interfaces which are in range/no longer in range of each other
	 * Issues LinkUp/LinkDown events to the corresponding interfaces
	 */
	@Override
	public void detectConnectivity() {
		if (disableGPULinkEvents) {
			// Detect link events
			for (NetworkInterface ni : interfaces) {
				// Issue LinkDown Events
				List<Connection> connections = ni.getConnections();
				for (int i = 0; i < connections.size(); ) {
					Connection con = connections.get(i);
					NetworkInterface other = con.getOtherInterface(ni);

					if (!areWithinRange(ni, other)) {
						ni.linkDown(other);
					}
					else {
						i++;
					}
				}
			}

			// Get connectivity via list of collisions
			connector.writeHeader(GSIMConnector.Header.CollisionDetection);
			connector.flushOutput();

			// Receive collisions
			int collisionsCount = connector.readInt();
			for (int i = 0; i < collisionsCount; i++) {
				int ID0 = connector.readInt();
				int ID1 = connector.readInt();
				NetworkInterface ni0 = interfaces.get(ID0);
				NetworkInterface ni1 = interfaces.get(ID1);

				// Issue LinkUp Events
				if (!ni0.isConnected(ni1)) {
					ni0.linkUp(ni1);
				}
			}
		} else {
			// Get connectivity via link up/link down events
			connector.writeHeader(GSIMConnector.Header.ConnectivityDetection);
			connector.flushOutput();

			// Receive link down events
			int linkDownEventCount = connector.readInt();
			for (int i = 0; i < linkDownEventCount; i++) {
				int ID0 = connector.readInt();
				int ID1 = connector.readInt();
				NetworkInterface ni0 = interfaces.get(ID0);
				NetworkInterface ni1 = interfaces.get(ID1);
				ni0.linkDown(ni1);
			}

			// Receive link up events
			int linkUpEventCount = connector.readInt();
			for (int i = 0; i < linkUpEventCount; i++) {
				int ID0 = connector.readInt();
				int ID1 = connector.readInt();
				NetworkInterface ni0 = interfaces.get(ID0);
				NetworkInterface ni1 = interfaces.get(ID1);
				ni0.linkUp(ni1);
			}
		}
	}

}
