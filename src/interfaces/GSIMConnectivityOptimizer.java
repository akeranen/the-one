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

	/** Connector for IPC */
	private GSIMConnector connector = null;
	/** List of managed interfaces */
	private List<NetworkInterface> interfaces = null;

	public GSIMConnectivityOptimizer(GSIMConnector connector, List<NetworkInterface> interfaces) {
		this.connector = connector;
		this.interfaces = interfaces;

		// Sanity checks
		for (int i = 0; i < interfaces.size(); i++) {
			if (interfaces.get(i).getHost().getID() != i) {
				throw new SettingsError("GSIMConnectivityOptimizer requires one interface the same type for every host!");
			}
		}

	}

	/**
	 * Detects interfaces which are in range/no longer in range of each other
	 * Issues LinkUp/LinkDown events to the corresponding interfaces
	 */
	@Override
	public void detectConnectivity() {
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
