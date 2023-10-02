package interfaces;

import core.NetworkInterface;
import core.SettingsError;
import input.MSIMConnector;
import movement.MovementEngine;

import java.util.List;

/**
 * Provides GPU accelerated connectivity detection as an extension to an MSIMMovementEngine.
 * Attention: Connectivity detection currently only supports a single interface type and only 1 interface per host!
 */
public class MSIMConnectivityOptimizer extends ConnectivityOptimizer {
	/** Class name */
	public static final String NAME = "MSIMConnectivityOptimizer";

	/** Connector for IPC */
	private MSIMConnector connector = null;
	/** List of managed interfaces */
	private List<NetworkInterface> interfaces = null;

	public MSIMConnectivityOptimizer(MSIMConnector connector, List<NetworkInterface> interfaces) {
		this.connector = connector;
		this.interfaces = interfaces;

		// Sanity checks
		for (int i = 0; i < interfaces.size(); i++) {
			if (interfaces.get(i).getHost().getID() != i) {
				throw new SettingsError("MSIMConnectivityOptimizer requires one interface the same type for every host!");
			}
		}
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
		// Run connectivity detection

		// TODO
		boolean disableLinkEvents = false;
		if (disableLinkEvents) {
			/*// Get connectivity via list of collisions
			connector.writeHeader(MSIMConnector.Header.CollisionDetection);
			connector.flushOutput();

			// Receive collisions
			int collisionsCount = connector.readInt();
			for (int i = 0; i < collisionsCount; i++) {
				int ID0 = connector.readInt();
				int ID1 = connector.readInt();
				applyLinkUpEvent(ID0, ID1);
			}*/
		} else {
			// Get connectivity via link up/link down events
			long startPass = System.nanoTime();
			connector.writeHeader(MSIMConnector.Header.ConnectivityDetection);
			connector.flushOutput();

			// Receive link down events
			int linkDownEventCount = connector.readInt();
			System.out.printf(" %d:  connectivity_detection.pass = %s\n", MovementEngine.getCurrentTick(), toHumanTime(System.nanoTime() - startPass));
			long startRecv = System.nanoTime();
			for (int i = 0; i < linkDownEventCount; i++) {
				int ID0 = connector.readInt();
				int ID1 = connector.readInt();
				NetworkInterface ni0 = interfaces.get(ID0);
				NetworkInterface ni1 = interfaces.get(ID1);
				ni0.linkDown(ni1);
			}
			System.out.printf(" %d:  connectivity_detection.recv_link_down = %s\n", MovementEngine.getCurrentTick(), toHumanTime(System.nanoTime() - startRecv));

			// Receive link up events
			int linkUpEventCount = connector.readInt();
			long startSend = System.nanoTime();
			for (int i = 0; i < linkUpEventCount; i++) {
				int ID0 = connector.readInt();
				int ID1 = connector.readInt();
				NetworkInterface ni0 = interfaces.get(ID0);
				NetworkInterface ni1 = interfaces.get(ID1);
				ni0.linkUp(ni1);
			}
			System.out.printf(" %d:  connectivity_detection.recv_link_up = %s\n", MovementEngine.getCurrentTick(), toHumanTime(System.nanoTime() - startSend));
		}
	}

}
