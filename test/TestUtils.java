/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package test;

import java.util.ArrayList;
import java.util.List;

import movement.MovementModel;
import routing.MessageRouter;
import routing.PassiveRouter;
import core.ConnectionListener;
import core.Coord;
import core.DTNHost;
import core.MessageListener;
import core.ModuleCommunicationBus;
import core.NetworkInterface;

/**
 * Generic convenience methods for tests.
 */
public class TestUtils {
	
	private List<ConnectionListener> conListeners;
	private List<MessageListener> msgListeners;
	private String groupId = "h";
	private List<DTNHost> allHosts;
	private MessageRouter mr;
	
	private ModuleCommunicationBus comBus;
	private TestSettings settings;
	
	public static String IFACE_NS = "interface";

	/**
	 * Creates a test utils object suitable for creating new hosts.
	 * @param cl Connection listeners for the hosts
	 * @param ml Message -"-
	 * @param settings Setting object given to message router
	 */
	public TestUtils(List<ConnectionListener> cl, List<MessageListener> ml,
			TestSettings settings) {
		this.conListeners = cl;
		this.msgListeners = ml;
		this.allHosts = new ArrayList<DTNHost>();
		this.settings = settings;
		this.mr = new PassiveRouter(settings);

		this.comBus = new ModuleCommunicationBus();
	}
	
	public void setMessageRouterProto(MessageRouter mr) {
		this.mr = mr;
	}
	
	/**
	 * @param conListeners the ConnectionListeners to set
	 */
	public void setConListeners(List<ConnectionListener> conListeners) {
		this.conListeners = conListeners;
	}

	/**
	 * @param groupId the groupId to set
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	/**
	 * @param msgListeners the MessageListeners to set
	 */
	public void setMsgListeners(List<MessageListener> msgListeners) {
		this.msgListeners = msgListeners;
	}

	/**
	 * @param transmitRange the transmitRange to set
	 */
	public void setTransmitRange(double transmitRange) {
		this.comBus.updateProperty(NetworkInterface.RANGE_ID, transmitRange);
	}
	
	/**
	 * Creates a host to a location with stationary movement model and
	 * MessageRouter router.
	 * @param loc The location of the host
	 * @param name Name of the host (or null for default)
	 * @return The new host
	 */
	public DTNHost createHost(Coord loc, String name) {
		MovementModel mmProto = new StationaryMovement(loc);
		return createHost(mmProto, name);
	}
	
	/**
	 * Creates a host with defined movement model
	 * @param mmProto The prototype of the movement model
	 * @param name name of the host
	 * @return the host
	 */
	public DTNHost createHost(MovementModel mmProto, String name) {
		if (settings.getNameSpace() == null) {
			settings.setNameSpace(IFACE_NS);
		}
		if (!this.settings.contains(NetworkInterface.TRANSMIT_RANGE_S)) {
			settings.putSetting(NetworkInterface.TRANSMIT_RANGE_S, "1.0");
			settings.putSetting(NetworkInterface.TRANSMIT_SPEED_S, "1");
		}
		
		NetworkInterface ni = new TestInterface(settings);
		ni.setClisteners(conListeners);
		List<NetworkInterface> li = new ArrayList<NetworkInterface>();
		li.add(ni);
		DTNHost host = new DTNHost(msgListeners, null, groupId,
				li, comBus, mmProto, mr);
		if (name != null) {
			host.setName(name);
		}
		
		this.allHosts.add(host);
		return host;		
	}
		
	/**
	 * Creates a host to a location with stationary movement model and
	 * default name.
	 * @param loc The location of the host
	 * @return The new host
	 */
	public DTNHost createHost(Coord loc) {
		return this.createHost(loc, null);
	}
	
	/**
	 * Creates a host to location (0,0) with stationary movement model 
	 * and default name.
	 * @return The new host
	 */
	public DTNHost createHost() {
		return this.createHost(new Coord(0,0));
	}
	
	public List<DTNHost> getAllHosts() {
		return this.allHosts;
	}
}
