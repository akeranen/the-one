/* 
 * Copyright 2011 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import routing.util.RoutingInfo;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

/**
 * Epidemic-like message router making waves of messages.
 * Work in progress.
 */
public class WaveRouter extends ActiveRouter {
	
	/** 
	 * Immunity time -setting id ({@value}). Defines how long time a node
	 * will reject incoming messages it has already received 
	 */
	public static final String IMMUNITY_S = "immunityTime";
	/** 
	 * Custody fraction -setting id ({@value}). Defines how long (compared to
	 * immunity time) nodes accept custody for new incoming messages. 
	 */
	public static final String CUSTODY_S = "custodyFraction";
	private double immunityTime;
	private double custodyFraction;
	/** map of recently received messages and their receive times */
	private Map<String, Double> recentMessages;	
	/** IDs of the messages this host has custody for */
	private Map<String, Double> custodyMessages;
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public WaveRouter(Settings s) {
		super(s);
		this.immunityTime = s.getDouble(IMMUNITY_S);
		this.custodyFraction = s.getDouble(CUSTODY_S);
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected WaveRouter(WaveRouter r) {
		super(r);
		recentMessages = new HashMap<String, Double>();
		this.immunityTime = r.immunityTime;
		this.custodyFraction = r.custodyFraction;
		this.custodyMessages = new HashMap<String, Double>();
	}

	@Override
	protected int checkReceiving(Message m, DTNHost from) {
		Double lastTime = this.recentMessages.get(m.getId());
			
		if (lastTime != null) {
			if (lastTime + this.immunityTime > SimClock.getTime()) {
				return DENIED_POLICY; /* still immune to the message */
			} else {
				/* immunity has passed; remove from recent */
				this.recentMessages.remove(m.getId()); 
			}
		}

		/* no last time or immunity passed; receive based on other checks */
		return super.checkReceiving(m, from);
	}
	
	/**
	 * Returns the oldest message that has been already sent forward 
	 */
	@Override
	protected Message getNextMessageToRemove(boolean excludeMsgBeingSent) {
		Collection<Message> messages = this.getMessageCollection();
		Message oldest = null;
		
		for (Message m : messages) {
			Double custodyStartTime = this.custodyMessages.get(m.getId());
			if (custodyStartTime != null) {
				if (SimClock.getTime() > 
					custodyStartTime + immunityTime * custodyFraction) {
					this.custodyMessages.remove(m.getId()); /* time passed */
				} else {
					continue; /* skip messages that still have custody */					
				}
			}
				
			
			if (excludeMsgBeingSent && isSending(m.getId())) {
				continue; /* skip the message(s) that router is sending */
			}
			
			if (oldest == null ) {
				oldest = m;
			}
			else if (oldest.getReceiveTime() > m.getReceiveTime()) {
				oldest = m;
			}
		}
		
		return oldest;
	}
	
	@Override
	public void update() {
		super.update();
		
		if (isTransferring() || !canStartTransfer()) {
			return; /* transferring, don't try other connections yet */
		}
		
		/* Try first the messages that can be delivered to final recipient */
		if (exchangeDeliverableMessages() != null) {
			return; 
		}		
		this.tryAllMessagesToAllConnections();
	}
	
	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);
		/* store received message IDs for immunity */
		this.recentMessages.put(m.getId(), new Double(SimClock.getTime()));
		this.custodyMessages.put(id, SimClock.getTime());
		return m;
	}
	
	@Override
	protected void transferDone(Connection con) { 
		/* remove from custody messages (if it was there) */
		this.custodyMessages.remove(con.getMessage().getId()); 
	}
	
	@Override
	public RoutingInfo getRoutingInfo() {
		RoutingInfo ri = super.getRoutingInfo();
		RoutingInfo immunity = new RoutingInfo("Immune to " + 
				this.recentMessages.size() + " messages");
		
		for (String id : recentMessages.keySet()) {
			RoutingInfo m = new RoutingInfo(id + " until " + 
					String.format("%.2f", 
							recentMessages.get(id) + this.immunityTime));
			immunity.addMoreInfo(m);
		}		
		ri.addMoreInfo(immunity);
		
		return ri;
	}
	
	@Override
	public WaveRouter replicate() {
		return new WaveRouter(this);
	}

}