/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import routing.util.EnergyModel;
import routing.util.MessageTransferAcceptPolicy;
import routing.util.RoutingInfo;
import util.Tuple;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.NetworkInterface;
import core.Settings;
import core.SimClock;

/**
 * Superclass of active routers. Contains convenience methods (e.g.
 * {@link #getNextMessageToRemove(boolean)}) and watching of sending connections (see
 * {@link #update()}).
 */
public abstract class ActiveRouter extends MessageRouter {
	/** Delete delivered messages -setting id ({@value}). Boolean valued.
	 * If set to true and final recipient of a message rejects it because it
	 * already has it, the message is deleted from buffer. Default=false. */
	public static final String DELETE_DELIVERED_S = "deleteDelivered";
	/** should messages that final recipient marks as delivered be deleted
	 * from message buffer */
	protected boolean deleteDelivered;

	/** prefix of all response message IDs */
	public static final String RESPONSE_PREFIX = "R_";
	/** how often TTL check (discarding old messages) is performed */
	public static int TTL_CHECK_INTERVAL = 60;
	/** connection(s) that are currently used for sending */
	protected ArrayList<Connection> sendingConnections;
	/** sim time when the last TTL check was done */
	private double lastTtlCheck;

	private MessageTransferAcceptPolicy policy;
	private EnergyModel energy;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public ActiveRouter(Settings s) {
		super(s);

		this.policy = new MessageTransferAcceptPolicy(s);

		this.deleteDelivered = s.getBoolean(DELETE_DELIVERED_S, false);

		if (s.contains(EnergyModel.INIT_ENERGY_S)) {
			this.energy = new EnergyModel(s);
		} else {
			this.energy = null; /* no energy model */
		}
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected ActiveRouter(ActiveRouter r) {
		super(r);
		this.deleteDelivered = r.deleteDelivered;
		this.policy = r.policy;
		this.energy = (r.energy != null ? r.energy.replicate() : null);
	}

	@Override
	public void init(DTNHost host, List<MessageListener> mListeners) {
		super.init(host, mListeners);
		this.sendingConnections = new ArrayList<Connection>(1);
		this.lastTtlCheck = 0;
	}

	/**
	 * Called when a connection's state changes. If energy modeling is enabled,
	 * and a new connection is created to this node, reduces the energy for the
	 * device discovery (scan response) amount
	 * @param con The connection whose state changed
	 */
	@Override
	public void changedConnection(Connection con) {
		if (this.energy != null && con.isUp() && !con.isInitiator(getHost())) {
			this.energy.reduceDiscoveryEnergy();
		}
	}

	@Override
	public boolean requestDeliverableMessages(Connection con) {
		if (isTransferring()) {
			return false;
		}

		DTNHost other = con.getOtherNode(getHost());
		/* do a copy to avoid concurrent modification exceptions
		 * (startTransfer may remove messages) */
		ArrayList<Message> temp =
			new ArrayList<Message>(this.getMessageCollection());
		for (Message m : temp) {
			if (other == m.getTo()) {
				if (startTransfer(m, con) == RCV_OK) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean createNewMessage(Message m) {
		makeRoomForNewMessage(m.getSize());
		return super.createNewMessage(m);
	}

	@Override
	public int receiveMessage(Message m, DTNHost from) {
		int recvCheck = checkReceiving(m, from);
		if (recvCheck != RCV_OK) {
			return recvCheck;
		}

		// seems OK, start receiving the message
		return super.receiveMessage(m, from);
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);

		/**
		 *  N.B. With application support the following if-block
		 *  becomes obsolete, and the response size should be configured
		 *  to zero.
		 */
		// check if msg was for this host and a response was requested
		if (m.getTo() == getHost() && m.getResponseSize() > 0) {
			// generate a response message
			Message res = new Message(this.getHost(),m.getFrom(),
					RESPONSE_PREFIX+m.getId(), m.getResponseSize());
			this.createNewMessage(res);
			this.getMessage(RESPONSE_PREFIX+m.getId()).setRequest(m);
		}

		return m;
	}

	/**
	 * Returns a list of connections this host currently has with other hosts.
	 * @return a list of connections this host currently has with other hosts
	 */
	protected List<Connection> getConnections() {
		return getHost().getConnections();
	}

	/**
	 * Tries to start a transfer of message using a connection. Is starting
	 * succeeds, the connection is added to the watch list of active connections
	 * @param m The message to transfer
	 * @param con The connection to use
	 * @return the value returned by
	 * {@link Connection#startTransfer(DTNHost, Message)}
	 */
	protected int startTransfer(Message m, Connection con) {
		int retVal;

		if (!con.isReadyForTransfer()) {
			return TRY_LATER_BUSY;
		}

		if (!policy.acceptSending(getHost(),
				con.getOtherNode(getHost()), con, m)) {
			return MessageRouter.DENIED_POLICY;
		}

		retVal = con.startTransfer(getHost(), m);
		if (retVal == RCV_OK) { // started transfer
			addToSendingConnections(con);
		}
		else if (deleteDelivered && retVal == DENIED_OLD &&
				m.getTo() == con.getOtherNode(this.getHost())) {
			/* final recipient has already received the msg -> delete it */
			this.deleteMessage(m.getId(), false);
		}

		return retVal;
	}

	/**
	 * Makes rudimentary checks (that we have at least one message and one
	 * connection) about can this router start transfer.
	 * @return True if router can start transfer, false if not
	 */
	protected boolean canStartTransfer() {
		if (this.getNrofMessages() == 0) {
			return false;
		}
		if (this.getConnections().size() == 0) {
			return false;
		}

		return true;
	}

	/**
	 * Checks if router "wants" to start receiving message (i.e. router
	 * isn't transferring, doesn't have the message and has room for it).
	 * @param m The message to check
	 * @return A return code similar to
	 * {@link MessageRouter#receiveMessage(Message, DTNHost)}, i.e.
	 * {@link MessageRouter#RCV_OK} if receiving seems to be OK,
	 * TRY_LATER_BUSY if router is transferring, DENIED_OLD if the router
	 * is already carrying the message or it has been delivered to
	 * this router (as final recipient), or DENIED_NO_SPACE if the message
	 * does not fit into buffer
	 */
	protected int checkReceiving(Message m, DTNHost from) {
		if (isTransferring()) {
			return TRY_LATER_BUSY; // only one connection at a time
		}

		if ( hasMessage(m.getId()) || isDeliveredMessage(m) ||
				super.isBlacklistedMessage(m.getId())) {
			return DENIED_OLD; // already seen this message -> reject it
		}

		if (m.getTtl() <= 0 && m.getTo() != getHost()) {
			/* TTL has expired and this host is not the final recipient */
			return DENIED_TTL;
		}

		if (energy != null && energy.getEnergy() <= 0) {
			return MessageRouter.DENIED_LOW_RESOURCES;
		}

		if (!policy.acceptReceiving(from, getHost(), m)) {
			return MessageRouter.DENIED_POLICY;
		}

		/* remove oldest messages but not the ones being sent */
		if (!makeRoomForMessage(m.getSize())) {
			return DENIED_NO_SPACE; // couldn't fit into buffer -> reject
		}

		return RCV_OK;
	}

	/**
	 * Removes messages from the buffer (oldest first) until
	 * there's enough space for the new message.
	 * @param size Size of the new message
	 * transferred, the transfer is aborted before message is removed
	 * @return True if enough space could be freed, false if not
	 */
	protected boolean makeRoomForMessage(int size){
		if (size > this.getBufferSize()) {
			return false; // message too big for the buffer
		}

		long freeBuffer = this.getFreeBufferSize();
		/* delete messages from the buffer until there's enough space */
		while (freeBuffer < size) {
			Message m = getNextMessageToRemove(true); // don't remove msgs being sent

			if (m == null) {
				return false; // couldn't remove any more messages
			}

			/* delete message from the buffer as "drop" */
			deleteMessage(m.getId(), true);
			freeBuffer += m.getSize();
		}

		return true;
	}

	/**
	 * Drops messages whose TTL is less than zero.
	 */
	protected void dropExpiredMessages() {
		Message[] messages = getMessageCollection().toArray(new Message[0]);
		for (int i=0; i<messages.length; i++) {
			int ttl = messages[i].getTtl();
			if (ttl <= 0) {
				deleteMessage(messages[i].getId(), true);
			}
		}
	}

	/**
	 * Tries to make room for a new message. Current implementation simply
	 * calls {@link #makeRoomForMessage(int)} and ignores the return value.
	 * Therefore, if the message can't fit into buffer, the buffer is only
	 * cleared from messages that are not being sent.
	 * @param size Size of the new message
	 */
	protected void makeRoomForNewMessage(int size) {
		makeRoomForMessage(size);
	}


	/**
	 * Returns the oldest (by receive time) message in the message buffer
	 * (that is not being sent if excludeMsgBeingSent is true).
	 * @param excludeMsgBeingSent If true, excludes message(s) that are
	 * being sent from the oldest message check (i.e. if oldest message is
	 * being sent, the second oldest message is returned)
	 * @return The oldest message or null if no message could be returned
	 * (no messages in buffer or all messages in buffer are being sent and
	 * exludeMsgBeingSent is true)
	 */
	protected Message getNextMessageToRemove(boolean excludeMsgBeingSent) {
		Collection<Message> messages = this.getMessageCollection();
		Message oldest = null;
		for (Message m : messages) {

			if (excludeMsgBeingSent && isSending(m.getId())) {
				continue; // skip the message(s) that router is sending
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

	/**
	 * Returns a list of message-connections tuples of the messages whose
	 * recipient is some host that we're connected to at the moment.
	 * @return a list of message-connections tuples
	 */
	protected List<Tuple<Message, Connection>> getMessagesForConnected() {
		if (getNrofMessages() == 0 || getConnections().size() == 0) {
			/* no messages -> empty list */
			return new ArrayList<Tuple<Message, Connection>>(0);
		}

		List<Tuple<Message, Connection>> forTuples =
			new ArrayList<Tuple<Message, Connection>>();
		for (Message m : getMessageCollection()) {
			for (Connection con : getConnections()) {
				DTNHost to = con.getOtherNode(getHost());
				if (m.getTo() == to) {
					forTuples.add(new Tuple<Message, Connection>(m,con));
				}
			}
		}

		return forTuples;
	}

	/**
	 * Tries to send messages for the connections that are mentioned
	 * in the Tuples in the order they are in the list until one of
	 * the connections starts transferring or all tuples have been tried.
	 * @param tuples The tuples to try
	 * @return The tuple whose connection accepted the message or null if
	 * none of the connections accepted the message that was meant for them.
	 */
	protected Tuple<Message, Connection> tryMessagesForConnected(
			List<Tuple<Message, Connection>> tuples) {
		if (tuples.size() == 0) {
			return null;
		}

		for (Tuple<Message, Connection> t : tuples) {
			Message m = t.getKey();
			Connection con = t.getValue();
			if (startTransfer(m, con) == RCV_OK) {
				return t;
			}
		}

		return null;
	}

	 /**
	  * Goes trough the messages until the other node accepts one
	  * for receiving (or doesn't accept any). If a transfer is started, the
	  * connection is included in the list of sending connections.
	  * @param con Connection trough which the messages are sent
	  * @param messages A list of messages to try
	  * @return The message whose transfer was started or null if no
	  * transfer was started.
	  */
	protected Message tryAllMessages(Connection con, List<Message> messages) {
		for (Message m : messages) {
			int retVal = startTransfer(m, con);
			if (retVal == RCV_OK) {
				return m;	// accepted a message, don't try others
			}
			else if (retVal > 0) {
				return null; // should try later -> don't bother trying others
			}
		}

		return null; // no message was accepted
	}

	/**
	 * Tries to send all given messages to all given connections. Connections
	 * are first iterated in the order they are in the list and for every
	 * connection, the messages are tried in the order they are in the list.
	 * Once an accepting connection is found, no other connections or messages
	 * are tried.
	 * @param messages The list of Messages to try
	 * @param connections The list of Connections to try
	 * @return The connections that started a transfer or null if no connection
	 * accepted a message.
	 */
	protected Connection tryMessagesToConnections(List<Message> messages,
			List<Connection> connections) {
		for (int i=0, n=connections.size(); i<n; i++) {
			Connection con = connections.get(i);
			Message started = tryAllMessages(con, messages);
			if (started != null) {
				return con;
			}
		}

		return null;
	}

	/**
	 * Tries to send all messages that this router is carrying to all
	 * connections this node has. Messages are ordered using the
	 * {@link MessageRouter#sortByQueueMode(List)}. See
	 * {@link #tryMessagesToConnections(List, List)} for sending details.
	 * @return The connections that started a transfer or null if no connection
	 * accepted a message.
	 */
	protected Connection tryAllMessagesToAllConnections(){
		List<Connection> connections = getConnections();
		if (connections.size() == 0 || this.getNrofMessages() == 0) {
			return null;
		}

		List<Message> messages =
			new ArrayList<Message>(this.getMessageCollection());
		this.sortByQueueMode(messages);

		return tryMessagesToConnections(messages, connections);
	}

	/**
	 * Exchanges deliverable (to final recipient) messages between this host
	 * and all hosts this host is currently connected to. First all messages
	 * from this host are checked and then all other hosts are asked for
	 * messages to this host. If a transfer is started, the search ends.
	 * @return A connection that started a transfer or null if no transfer
	 * was started
	 */
	protected Connection exchangeDeliverableMessages() {
		List<Connection> connections = getConnections();

		if (connections.size() == 0) {
			return null;
		}

		@SuppressWarnings(value = "unchecked")
		Tuple<Message, Connection> t =
			tryMessagesForConnected(sortByQueueMode(getMessagesForConnected()));

		if (t != null) {
			return t.getValue(); // started transfer
		}

		// didn't start transfer to any node -> ask messages from connected
		for (Connection con : connections) {
			if (con.getOtherNode(getHost()).requestDeliverableMessages(con)) {
				return con;
			}
		}

		return null;
	}



	/**
	 * Shuffles a messages list so the messages are in random order.
	 * @param messages The list to sort and shuffle
	 */
	protected void shuffleMessages(List<Message> messages) {
		if (messages.size() <= 1) {
			return; // nothing to shuffle
		}

		Random rng = new Random(SimClock.getIntTime());
		Collections.shuffle(messages, rng);
	}

	/**
	 * Adds a connections to sending connections which are monitored in
	 * the update.
	 * @see #update()
	 * @param con The connection to add
	 */
	protected void addToSendingConnections(Connection con) {
		this.sendingConnections.add(con);
	}

	/**
	 * Returns true if this router is transferring something at the moment or
	 * some transfer has not been finalized.
	 * @return true if this router is transferring something
	 */
	public boolean isTransferring() {
		if (this.sendingConnections.size() > 0) {
			return true; // sending something
		}

		List<Connection> connections = getConnections();

		if (connections.size() == 0) {
			return false; // not connected
		}

		for (int i=0, n=connections.size(); i<n; i++) {
			Connection con = connections.get(i);
			if (!con.isReadyForTransfer()) {
				return true;	// a connection isn't ready for new transfer
			}
		}

		return false;
	}

	/**
	 * Returns true if this router is currently sending a message with
	 * <CODE>msgId</CODE>.
	 * @param msgId The ID of the message
	 * @return True if the message is being sent false if not
	 */
	public boolean isSending(String msgId) {
		for (Connection con : this.sendingConnections) {
			if (con.getMessage() == null) {
				continue; // transmission is finalized
			}
			if (con.getMessage().getId().equals(msgId)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the node has energy left (i.e., energy modeling is
	 * enabled OR (is enabled and model has energy left))
	 * @return has the node energy
	 */
	public boolean hasEnergy() {
		return this.energy == null || this.energy.getEnergy() > 0;
	}

	/**
	 * Checks out all sending connections to finalize the ready ones
	 * and abort those whose connection went down. Also drops messages
	 * whose TTL <= 0 (checking every one simulated minute).
	 * @see #addToSendingConnections(Connection)
	 */
	@Override
	public void update() {
		super.update();

		/* in theory we can have multiple sending connections even though
		  currently all routers allow only one concurrent sending connection */
		for (int i=0; i<this.sendingConnections.size(); ) {
			boolean removeCurrent = false;
			Connection con = sendingConnections.get(i);

			/* finalize ready transfers */
			if (con.isMessageTransferred()) {
				if (con.getMessage() != null) {
					transferDone(con);
					con.finalizeTransfer();
				} /* else: some other entity aborted transfer */
				removeCurrent = true;
			}
			/* remove connections that have gone down */
			else if (!con.isUp()) {
				if (con.getMessage() != null) {
					transferAborted(con);
					con.abortTransfer();
				}
				removeCurrent = true;
			}

			if (removeCurrent) {
				// if the message being sent was holding excess buffer, free it
				if (this.getFreeBufferSize() < 0) {
					this.makeRoomForMessage(0);
				}
				sendingConnections.remove(i);
			}
			else {
				/* index increase needed only if nothing was removed */
				i++;
			}
		}

		/* time to do a TTL check and drop old messages? Only if not sending */
		if (SimClock.getTime() - lastTtlCheck >= TTL_CHECK_INTERVAL &&
				sendingConnections.size() == 0) {
			dropExpiredMessages();
			lastTtlCheck = SimClock.getTime();
		}

		if (energy != null) {
			/* TODO: add support for other interfaces */
			NetworkInterface iface = getHost().getInterface(1);
			energy.update(iface, getHost().getComBus());
		}
	}

	/**
	 * Method is called just before a transfer is aborted at {@link #update()}
	 * due connection going down. This happens on the sending host.
	 * Subclasses that are interested of the event may want to override this.
	 * @param con The connection whose transfer was aborted
	 */
	protected void transferAborted(Connection con) { }

	/**
	 * Method is called just before a transfer is finalized
	 * at {@link #update()}.
	 * Subclasses that are interested of the event may want to override this.
	 * @param con The connection whose transfer was finalized
	 */
	protected void transferDone(Connection con) { }

	@Override
	public RoutingInfo getRoutingInfo() {
		RoutingInfo top = super.getRoutingInfo();
		if (energy != null) {
			top.addMoreInfo(new RoutingInfo("Energy level: " +
					String.format("%.2f", energy.getEnergy())));
		}
		return top;
	}

}
