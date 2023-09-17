package interfaces;

import core.DTNHost;
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
	/** List of all hosts in the simulation */
	private List<DTNHost> hosts = null;
	/** Map of Interfaces within range of each other */
	private List<Set<NetworkInterface>> interfacesInRange = null;

	/**
	 * Creates a new MSIMConnectivityOptimizer based on a Settings object's settings.
	 * @param s The Settings object where the settings are read from
	 */
	public MSIMConnectivityOptimizer(Settings s) {}

	public void init(List<DTNHost> hosts) {
		this.hosts = hosts;

		this.interfacesInRange = new ArrayList<>(hosts.size());
		for (int i = 0; i < hosts.size(); i++) {
			this.interfacesInRange.add(i, new HashSet<>());
		}
	}

	@Override
	public boolean areWithinRange(NetworkInterface a, NetworkInterface b) {
		double range = Math.min(a.getTransmitRange(), b.getTransmitRange());
		return a.getLocation().distanceSquared(b.getLocation()) <= range * range;
	}

	/**
	 * Finds all network interfaces that might be located so that they can be
	 * connected with the network interface (corresponds to MSIM link up events)
	 *
	 * @param ni network interface that needs to be connected
	 * @return A collection of network interfaces within proximity
	 */
	public Collection<NetworkInterface> getInterfacesInRange(NetworkInterface ni) {
		return interfacesInRange.get(NI_to_ID(ni));
	}

	// TODO rem
	public void resetEvents() {
		for(Set<NetworkInterface> s : interfacesInRange) {
			s.clear();
		}
	}

	public void applyLinkUpEvent(int ID0, int ID1) {
		interfacesInRange.get(ID0).add(ID_to_NI(ID1));
	}

	public void applyLinkUpEvents() {
		// TODO
	}

	// Maps a NetworkInterface to its unique ID
	private int NI_to_ID(NetworkInterface ni) {
		return ni.getHost().getID();
	}

	// Maps a NetworkInterfaceID to its Interface class
	private NetworkInterface ID_to_NI(int id) {
		return hosts.get(id).getInterfaces().get(0);
	}

}
