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
 * 
 * 
 * This version tries to estimate a good value of protocol parameters from
 * a timescale parameter given by the user, and from the encounters the node
 * sees during simulation.
 * Refer to Karvo and Ott, <I>Time Scales and Delay-Tolerant Routing 
 * Protocols</I> Chants, 2008 
 * 
 */
public class ProphetRouterWithEstimation extends ActiveRouter {
	/** delivery predictability initialization constant*/
	public static final double P_INIT = 0.75;
	/** delivery predictability transitivity scaling constant default value */
	public static final double DEFAULT_BETA = 0.25;
	/** delivery predictability aging constant */
	public static final double GAMMA = 0.98;
	/** default P target */
	public static final double DEFAULT_PTARGET = .2;

	/** Prophet router's setting namespace ({@value})*/ 
	public static final String PROPHET_NS = "ProphetRouterWithEstimation";
	/**
	 * Number of seconds in time scale.*/
	public static final String TIME_SCALE_S ="timeScale";
	/**
	 * Target P_avg
	 * 
	 */
	public static final String P_AVG_TARGET_S = "targetPavg";

	/**
	 * Transitivity scaling constant (beta) -setting id ({@value}).
	 * Default value for setting is {@link #DEFAULT_BETA}.
	 */
	public static final String BETA_S = "beta";

	/** values of parameter settings */
	private double beta;
	private double gamma;
	private double pinit;

	/** value of time scale variable */
	private int timescale;
	private double ptavg;

	/** delivery predictabilities */
	private Map<DTNHost, Double> preds;

	/** last meeting time with a node */
	private Map<DTNHost, Double> meetings;
	private int nrofSamples;
	private double meanIET;

	/** last delivery predictability update (sim)time */
	private double lastAgeUpdate;


	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public ProphetRouterWithEstimation(Settings s) {
		super(s);
		Settings prophetSettings = new Settings(PROPHET_NS);
		timescale = prophetSettings.getInt(TIME_SCALE_S);
		if (prophetSettings.contains(P_AVG_TARGET_S)) {
			ptavg = prophetSettings.getDouble(P_AVG_TARGET_S);
		} else {
			ptavg = DEFAULT_PTARGET;
		}
		if (prophetSettings.contains(BETA_S)) {
			beta = prophetSettings.getDouble(BETA_S);
		} else {
			beta = DEFAULT_BETA;
		}
		gamma = GAMMA;
		pinit = P_INIT;

		initPreds();
		initMeetings();
	}

	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected ProphetRouterWithEstimation(ProphetRouterWithEstimation r) {
		super(r);
		this.timescale = r.timescale;
		this.ptavg = r.ptavg;
		this.beta = r.beta;
		initPreds();
		initMeetings();
	}

	/**
	 * Initializes predictability hash
	 */
	private void initPreds() {
		this.preds = new HashMap<DTNHost, Double>();
	}

	/**
	 * Initializes inter-encounter time estimator
	 */
	private void initMeetings() {
		this.meetings = new HashMap<DTNHost, Double>();
		this.meanIET = 0;
		this.nrofSamples = 0;
	}

	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);
		
		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			if (updateIET(otherHost)) {
				updateParams();
			} 
			updateDeliveryPredFor(otherHost);
			updateTransitivePreds(otherHost);
		}
	}

	/**
	 * Updates the interencounter time estimates
	 * @param host
	 */
	private boolean updateIET(DTNHost host) {		
		/* First estimate the mean InterEncounter Time */
		double currentTime = SimClock.getTime();
		if (meetings.containsKey(host)) {
			double timeDiff = currentTime - meetings.get(host);
			// System.out.printf("current time: %f\t last time:  %f\n",currentTime,meetings.get(host));

			nrofSamples++;
			meanIET = (((double)nrofSamples -1) / (double)nrofSamples) * meanIET
			+ (1 / (double)nrofSamples) * timeDiff;
			meetings.put(host, currentTime);
			return true;
		} else {
			/* nothing to update */
			meetings.put(host,currentTime);
			return false;
		}		
	}

	/**
	 * update PROPHET parameters
	 *
	 */
	private void updateParams()
	{
		double b;
		double zeta;
		double err;
		boolean cond;
		int ntarg;
		double zetadiff;
		int ozeta;
		double pstable;
		double pavg;		
		double ee;
		double bdiff;
		int ob;
		int zcount;
		boolean bcheck;
		double pnzero;
		double pnone;
		double eezero;
		double eeone;

		/*
		 * the estimation algorith does not work for timescales
		 * shorter than the mean IET - so use defaults
		 */
		if (meanIET > (double)timescale) {
			System.out.printf("meanIET %f > %d timescale\n",meanIET,timescale);
			return;
		}

		if (meanIET == 0) {
			System.out.printf("Mean IET == 0\n");
			return;
		}			

		System.out.printf("prophetfindparams(%d,%f,%f);\n",timescale,ptavg,meanIET);
		b = 1e-5;
		zeta = .9;
		err = 0.005;
		zetadiff = .1;
		ozeta = 0;
		cond = false;
		ntarg = (int)Math.ceil((double)timescale/(double)meanIET);
		while (cond == false) {
			pstable = (1-zeta)/(Math.exp(b*meanIET)-zeta);
			pavg = (1/(b*meanIET)) * (1-zeta*(1-pstable)) *
			(1- Math.exp( -b*meanIET));

			if (Double.isNaN(pavg)) {
				pavg = 1;
			}

			if (pavg > ptavg) {
				//System.out.printf("PAVG %f > %f PTAVG\n", pavg,ptavg);
				if (ozeta == 2) {
					zetadiff = zetadiff / 2.0;
				}
				ozeta = 1;
				zeta = zeta + zetadiff;
				if (zeta >= 1) {
					zeta = 1-zetadiff;
					zetadiff = zetadiff / 2.0;
					ozeta = 0;
				}
			} else {
				if (pavg < ptavg * (1-err)) {
					//	System.out.printf("PAVG %f < %f PTAVG\n", pavg,ptavg);
					if (ozeta == 1) {
						zetadiff = zetadiff / 2.0;
					}
					ozeta = 2;
					zeta = zeta-zetadiff;
					if (zeta <= 0) {
						zeta = 0 + zetadiff;
						zetadiff = zetadiff / 2.0;
						ozeta = 0;
					}
				} else {
					cond = true;
				}
			}

			//System.out.printf("Zeta: %f Zetadiff: %f\n",zeta,zetadiff);
			ee = 1;
			bdiff = .1;			
			ob = 0;
			zcount = 0; // if 100 iterations won't help, lets increase zeta...
			bcheck = false;
			while (bcheck == false) {

				pstable = (1-zeta)/(Math.exp(b*meanIET)-zeta);
				pnzero = Math.exp(-b*meanIET) * (1-zeta) *
				((1-Math.pow(zeta*Math.exp(-b*meanIET),ntarg-1))/
						(1-zeta*Math.exp(-b*meanIET)));
				pnone = Math.pow(zeta*Math.exp(-b*meanIET),ntarg) + pnzero;
				eezero = Math.abs(pnzero-pstable);
				eeone  = Math.abs(pnone -pstable);
				ee = Math.max(eezero,eeone);

//				System.out.printf("Zeta: %f\n", zeta);
				//			System.out.printf("Ptarget: %f \t Pstable: %f\n",ptavg,pstable);
				//		System.out.printf("Pnzero: %f \tPnone: %f\n", pnzero,pnone);
				//	System.out.printf("eezero: %f\t eeone: %f\n", eezero, eeone);

				if (ee > err) {
					if (ob == 2) {
						bdiff = bdiff / 2.0;
					}
					ob = 1;
					b = b+bdiff;
				} else {
					if (ee < (err*(1-err))) {
						if (ob == 1) {
							bdiff = bdiff / 2.0;
						}
						ob = 2;
						b = b-bdiff;
						if (b <= 0) {
							b = 0 + bdiff;
							bdiff = bdiff / 1.5;
							ob = 0;
						}
					} else {
						bcheck = true;
//						System.out.println("******");
					}
				}

//				System.out.printf("EE: %f B: %f Bdiff: %f\n",ee,b,bdiff);
				zcount = zcount + 1;
				if (zcount > 100) {
					bcheck = true;
					ozeta = 0;
				}
			}
		}
		gamma = Math.exp(-b);
		pinit = 1-zeta;
	}

	/**
	 * Updates delivery predictions for a host.
	 * <CODE>P(a,b) = P(a,b)_old + (1 - P(a,b)_old) * P_INIT</CODE>
	 * @param host The host we just met
	 */
	private void updateDeliveryPredFor(DTNHost host) {
		double oldValue = getPredFor(host);
		double newValue = oldValue + (1 - oldValue) * pinit;
		preds.put(host, newValue);
	}

	/**
	 * Returns the current prediction (P) value for a host or 0 if entry for
	 * the host doesn't exist.
	 * @param host The host to look the P for
	 * @return the current P value
	 */
	public double getPredFor(DTNHost host) {
		ageDeliveryPreds(); // make sure preds are updated before getting
		if (preds.containsKey(host)) {
			return preds.get(host);
		}
		else {
			return 0;
		}
	}

	/**
	 * Updates transitive (A->B->C) delivery predictions.
	 * <CODE>P(a,c) = P(a,c)_old + (1 - P(a,c)_old) * P(a,b) * P(b,c) * BETA
	 * </CODE>
	 * @param host The B host who we just met
	 */
	private void updateTransitivePreds(DTNHost host) {
		MessageRouter otherRouter = host.getRouter();
		assert otherRouter instanceof ProphetRouterWithEstimation : "PRoPHET only works " + 
		" with other routers of same type";

		double pForHost = getPredFor(host); // P(a,b)
		Map<DTNHost, Double> othersPreds = 
			((ProphetRouterWithEstimation)otherRouter).getDeliveryPreds();

		for (Map.Entry<DTNHost, Double> e : othersPreds.entrySet()) {
			if (e.getKey() == getHost()) {
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
		double timeDiff = (SimClock.getTime() - this.lastAgeUpdate);

		if (timeDiff == 0) {
			return;
		}

		double mult = Math.pow(gamma, timeDiff);
		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			e.setValue(e.getValue()*mult);
		}

		this.lastAgeUpdate = SimClock.getTime();
	}

	/**
	 * Returns a map of this router's delivery predictions
	 * @return a map of this router's delivery predictions
	 */
	private Map<DTNHost, Double> getDeliveryPreds() {
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
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			ProphetRouterWithEstimation othRouter = (ProphetRouterWithEstimation)other.getRouter();

			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}

			for (Message m : msgCollection) {
				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}
				if (othRouter.getPredFor(m.getTo()) > getPredFor(m.getTo())) {
					// the other node has higher probability of delivery
					messages.add(new Tuple<Message, Connection>(m,con));
				}
			}			
		}

		if (messages.size() == 0) {
			return null;
		}

		// sort the message-connection tuples
		Collections.sort(messages, new TupleComparator());
		return tryMessagesForConnected(messages);	// try to send messages
	}

	/**
	 * Comparator for Message-Connection-Tuples that orders the tuples by
	 * their delivery probability by the host on the other side of the 
	 * connection (GRTRMax)
	 */
	private class TupleComparator implements Comparator 
	<Tuple<Message, Connection>> {

		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {
			// delivery probability of tuple1's message with tuple1's connection
			double p1 = ((ProphetRouterWithEstimation)tuple1.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
							tuple1.getKey().getTo());
			// -"- tuple2...
			double p2 = ((ProphetRouterWithEstimation)tuple2.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
							tuple2.getKey().getTo());

			// bigger probability should come first
			if (p2-p1 == 0) {
				/* equal probabilities -> let queue mode decide */
				return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
			}
			else if (p2-p1 < 0) {
				return -1;
			}
			else {
				return 1;
			}
		}
	}

	@Override
	public RoutingInfo getRoutingInfo() {
		ageDeliveryPreds();
		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo(preds.size() + 
		" delivery prediction(s)");

		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			DTNHost host = e.getKey();
			Double value = e.getValue();

			ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f", 
					host, value)));
		}

		ri.addMoreInfo(new RoutingInfo(String.format("meanIET: %f\t from %d samples",meanIET,nrofSamples)));
		ri.addMoreInfo(new RoutingInfo(String.format("current gamma: %f",gamma)));
		ri.addMoreInfo(new RoutingInfo(String.format("current Pinit: %f",pinit)));

		top.addMoreInfo(ri);
		return top;
	}

	@Override
	public MessageRouter replicate() {
		ProphetRouterWithEstimation r = new ProphetRouterWithEstimation(this);
		return r;
	}

}
