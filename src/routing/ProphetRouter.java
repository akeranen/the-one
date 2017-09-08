/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.BroadcastMessage;
import core.DataMessage;
import core.MulticastMessage;
import routing.util.DatabaseApplicationUtil;
import routing.util.RoutingInfo;

import util.Tuple;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

/**
 * Implementation of PRoPHET router as described in
 * <I>Probabilistic routing in intermittently connected networks</I> by
 * Anders Lindgren et al.
 */
public class ProphetRouter extends ActiveRouter {
	/** delivery predictability initialization constant*/
	public static final double P_INIT = 0.75;
	/** delivery predictability transitivity scaling constant default value */
	public static final double DEFAULT_BETA = 0.25;
	/** delivery predictability aging constant */
	public static final double GAMMA = 0.98;

	/** Prophet router's setting namespace ({@value})*/
	public static final String PROPHET_NS = "ProphetRouter";
	/**
	 * Number of seconds in time unit -setting id ({@value}).
	 * How many seconds one time unit is when calculating aging of
	 * delivery predictions. Should be tweaked for the scenario.*/
	public static final String SECONDS_IN_UNIT_S ="secondsInTimeUnit";

	/**
	 * Transitivity scaling constant (beta) -setting id ({@value}).
	 * Default value for setting is {@link #DEFAULT_BETA}.
	 */
	public static final String BETA_S = "beta";

	/** the value of nrof seconds in time unit -setting */
	private int secondsInTimeUnit;
	/** value of beta setting */
	private double beta;

	/** delivery predictabilities */
	private Map<Integer, Double> preds;
	/** last delivery predictability update (sim)time */
	private double lastAgeUpdate;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public ProphetRouter(Settings s) {
		super(s);
		Settings prophetSettings = new Settings(PROPHET_NS);
		secondsInTimeUnit = prophetSettings.getInt(SECONDS_IN_UNIT_S);
		if (prophetSettings.contains(BETA_S)) {
			beta = prophetSettings.getDouble(BETA_S);
		}
		else {
			beta = DEFAULT_BETA;
		}

		initPreds();
	}

	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected ProphetRouter(ProphetRouter r) {
		super(r);
		this.secondsInTimeUnit = r.secondsInTimeUnit;
		this.beta = r.beta;
		initPreds();
	}

	/**
	 * Initializes predictability hash
	 */
	private void initPreds() {
        this.preds = new HashMap<>();
	}

	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);

		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			updateDeliveryPredFor(otherHost);
			updateTransitivePreds(otherHost);
		}
	}

	/**
	 * Updates delivery predictions for a host.
	 * <CODE>P(a,b) = P(a,b)_old + (1 - P(a,b)_old) * P_INIT</CODE>
	 * @param host The host we just met
	 */
	private void updateDeliveryPredFor(DTNHost host) {
		double oldValue = getPredFor(host);
		double newValue = oldValue + (1 - oldValue) * P_INIT;
		preds.put(host.getAddress(), newValue);
	}

    /**
     * Returns the current prediction (P) value for a message or 0 if no entry for any recipient exists.
     * @param message Message to find P for. Either a one-to-one message or a multicast.
     * @return The current P value. In case of multicast, the maximum P value of remaining recipients.
     */
    private double getPredFor(Message message) {
        switch (message.getType()) {
            case ONE_TO_ONE:
                return this.getPredFor(message.getTo());
            case MULTICAST:
                MulticastMessage multicast = (MulticastMessage)message;
                return this.getMaxPredFor(multicast.getRemainingRecipients());
            default:
                throw new IllegalArgumentException(
                        "No delivery predictability for messages of type " + message.getType() + " defined!");
        }
    }

    /**
     * Returns the maximum prediction (P) value for all hosts matching the provided addresses. If no such P values
     * exist, returns 0.
     * @param addresses The addresses to check.
     * @return The maximum P value.
     */
    private double getMaxPredFor(Collection<Integer> addresses) {
        // Make sure preds are updated once before getting.
        this.ageDeliveryPreds();

        double maxPred = 0;
        for (int address : addresses) {
            double predForAddress = this.preds.getOrDefault(address, 0D);
            maxPred = Math.max(maxPred, predForAddress);
        }
        return maxPred;
    }

	/**
	 * Returns the current prediction (P) value for a host or 0 if entry for
	 * the host doesn't exist.
	 * @param host The host to look the P for
	 * @return the current P value
	 */
	public double getPredFor(DTNHost host) {
        return this.getPredFor(host.getAddress());
	}

    /**
     * Returns the current prediction (P) value for a host address or 0 if entry for
     * the host doesn't exist.
     * @param address The host address to look the P for
     * @return the current P value
     */
    private double getPredFor(Integer address) {
        // make sure preds are updated before getting
        this.ageDeliveryPreds();
        return preds.getOrDefault(address, 0D);
    }

	/**
	 * Updates transitive (A->B->C) delivery predictions.
	 * <CODE>P(a,c) = P(a,c)_old + (1 - P(a,c)_old) * P(a,b) * P(b,c) * BETA
	 * </CODE>
	 * @param host The B host who we just met
	 */
	private void updateTransitivePreds(DTNHost host) {
		MessageRouter otherRouter = host.getRouter();
		assert otherRouter instanceof ProphetRouter : "PRoPHET only works " +
			" with other routers of same type";

		double pForHost = getPredFor(host); // P(a,b)
		Map<Integer, Double> othersPreds =
			((ProphetRouter)otherRouter).getDeliveryPreds();

		for (Map.Entry<Integer, Double> e : othersPreds.entrySet()) {
			if (e.getKey().equals(getHost().getAddress())) {
				continue; // don't add yourself
			}

			double pOld = getPredFor(e.getKey()); // P(a,c)_old
			double pNew = pOld + ( 1 - pOld) * pForHost * e.getValue() * beta;
			preds.put(e.getKey(), pNew);
		}
	}

	/**
	 * Ages all entries in the delivery predictions.
	 * <CODE>P(a,b) = P(a,b)_old * (GAMMA ^ k)</CODE>, where k is number of
	 * time units that have elapsed since the last time the metric was aged.
	 * @see #SECONDS_IN_UNIT_S
	 */
	private void ageDeliveryPreds() {
		double timeDiff = (SimClock.getTime() - this.lastAgeUpdate) /
			secondsInTimeUnit;

		if (timeDiff == 0) {
			return;
		}

		double mult = Math.pow(GAMMA, timeDiff);
		for (Map.Entry<Integer, Double> e : preds.entrySet()) {
			e.setValue(e.getValue()*mult);
		}

		this.lastAgeUpdate = SimClock.getTime();
	}

	/**
	 * Returns a map of this router's delivery predictions
	 * @return a map of this router's delivery predictions
	 */
	private Map<Integer, Double> getDeliveryPreds() {
		ageDeliveryPreds(); // make sure the aging is done
		return this.preds;
	}

	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring
		}

		// try messages that could be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return;
		}

		tryOtherMessages();
	}

    /**
     * Checks whether this router has anything to send out.
     *
     * @return Whether or not the router has anything to send out.
     */
    @Override
    protected boolean hasNothingToSend() {
        return DatabaseApplicationUtil.hasNoMessagesToSend(this);
	}

	/**
	 * Tries to send all other messages to all connected hosts ordered by
	 * their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	private Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages =
			new ArrayList<Tuple<Message, Connection>>();

		Collection<Message> msgCollection = getMessageCollection();

		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
		List<Connection> availableConnections = new ArrayList<>();
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			ProphetRouter othRouter = (ProphetRouter)other.getRouter();

			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}
			availableConnections.add(con);

			for (Message m : msgCollection) {
				if (othRouter.hasMessage(m.getId()) || m instanceof BroadcastMessage) {
					// Ignore both messages that the other one has and all broadcast messages.
					// (Broadcasts should be sent via exchangeDeliverableMessages.)
					// The latter check may not be caught by the former because of caching (direct messages may be
					// sent belatedly; see explanation at ActiveRouter#cachedMessagesForConnected.).
					continue;
				}
				if (othRouter.getPredFor(m) > getPredFor(m)) {
					// the other node has higher probability of delivery
					messages.add(new Tuple<Message, Connection>(m,con));
				}
			}
		}

		/* For all available connections, add useful data messages. */
		messages.addAll(DatabaseApplicationUtil.wrapUsefulDataIntoMessages(
		        this, this.getHost(), availableConnections));

		if (messages.size() == 0) {
			return null;
		}

		// sort the message-connection tuples
		Collections.sort(messages, new TupleComparator());
		return tryMessagesForConnected(messages);	// try to send messages
	}

	/**
	 * Comparator for Message-Connection-Tuples that orders the tuples by the utility computed by
     * {@link #computeUtility(Tuple)}, higher utilities first.
	 */
	private class TupleComparator implements Comparator
		<Tuple<Message, Connection>> {

		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {
            double utility1 = this.computeUtility(tuple1);
            double utility2 = this.computeUtility(tuple2);

            // bigger utility should come first
            int utilityComparison = (-1) * Double.compare(utility1, utility2);
            if (utilityComparison == 0) {
                /* equal utilities -> let queue mode decide */
				return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
			}
            return utilityComparison;
		}

        /**
         * Computes a utility value for a Message-Connection tuple. This is
         * - either the delivery probability by the host on the other side of the connection (GRTRMax) if the message is
         *   not a data message, or
         * - the data message's utility.
         * @param tuple Tuple to compute utility for.
         * @return The tuple's utility.
         */
        private double computeUtility(Tuple<Message, Connection> tuple) {
            Message message = tuple.getKey();
            if (message instanceof DataMessage) {
                return ((DataMessage) message).getUtility();
            }

            DTNHost neighbor = tuple.getValue().getOtherNode(getHost());
            return ((ProphetRouter)neighbor.getRouter()).getPredFor(message);
        }
    }

	@Override
	public RoutingInfo getRoutingInfo() {
		ageDeliveryPreds();
		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo(preds.size() +
				" delivery prediction(s)");

		for (Map.Entry<Integer, Double> e : preds.entrySet()) {
			Integer hostAddress = e.getKey();
			Double value = e.getValue();

			ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f",
					hostAddress, value)));
		}

		top.addMoreInfo(ri);
		return top;
	}

	@Override
	public MessageRouter replicate() {
		ProphetRouter r = new ProphetRouter(this);
		return r;
	}

}
