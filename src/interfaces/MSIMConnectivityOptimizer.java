package interfaces;

import core.NetworkInterface;
import core.Settings;

import java.util.*;

/**
 * Provides GPU accelerated connectivity detection as an extension to an MSIMMovementEngine.
 * Attention: Connectivity detection currently only supports a single interface type and only 1 interface per host!
 */
public class MSIMConnectivityOptimizer extends ConnectivityOptimizer {
	/** Class name */
	public static final String NAME = "MSIMConnectivityOptimizer";
	/** NetworkInterface-to-ID mapping */
	private HashMap<NetworkInterface, Integer> NI2ID = null;
	/** NetworkID-to-Interface mapping */
	private HashMap<Integer, NetworkInterface> ID2NI = null;
	/** Near NetworkIDs mapping */
	private HashMap<Integer, List<Integer>> nearInterfaces = null;

	/**
	 * Creates a new MSIMConnectivityOptimizer based on a Settings object's settings.
	 * @param s The Settings object where the settings are read from
	 */
	public MSIMConnectivityOptimizer(Settings s) {}

	/**
	 * Updates a network interface's location
	 */
	public void updateLocation(NetworkInterface ni) {
		// Does nothing; This optimizer does not need to access host locations
	}

	/**
	 * Finds all network interfaces that might be located so that they can be
	 * connected with the network interface (corresponds to MSIM link up events)
	 *
	 * @param ni network interface that needs to be connected
	 * @return A collection of network interfaces within proximity
	 */
	public Collection<NetworkInterface> getInterfacesInRange(NetworkInterface ni) {
		List<Integer> ids = nearInterfaces.get(NI_to_ID(ni));
		if (ids == null) {
			return Collections.emptyList();
		}

		boolean deterministic = true; // TODO add setting
		if (deterministic) {
			Collections.sort(ids);
		}

		ArrayList<NetworkInterface> interfaces = new ArrayList<>(ids.size());
		for (int id : ids) {
			interfaces.add(ID_to_NI(id));
		}

		return interfaces;
	}

	/** Sets the nearInterfaces mapping */
	public void setNearInterfaces(HashMap<Integer, List<Integer>> nearInterfaces) {
		this.nearInterfaces = nearInterfaces;
	}

	/** Sets the NetworkInterface-to-ID mapping */
	public void setNI2ID(HashMap<NetworkInterface, Integer> NI2ID) {
		this.NI2ID = NI2ID;
	}

	/** Sets the NetworkID-to-Interface mapping */
	public void setID2NI(HashMap<Integer, NetworkInterface> ID2NI) {
		this.ID2NI = ID2NI;
	}

	/** Maps a NetworkInterface to its unique ID */
	private int NI_to_ID(NetworkInterface ni) {
		return NI2ID.get(ni);
	}

	/** Maps a NetworkInterfaceID to its Interface class */
	private NetworkInterface ID_to_NI(int id) {
		return ID2NI.get(id);
	}
}
