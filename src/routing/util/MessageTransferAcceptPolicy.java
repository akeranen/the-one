/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing.util;

import java.util.ArrayList;

import util.Range;
import util.Tuple;

import core.ArithmeticCondition;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.ModuleCommunicationBus;
import core.Settings;

/**
 * <P> Message transfer accepting policy module. Can be used to decide whether
 * certain messages should be accepted or not. Shared by a whole node group, but
 * uses the module communication bus of each node in question.</P>
 * <P> Supports 3 different modes: "simple policy", Hop Count, and
 * ModuleCommunicationBus (MCB) values. With simple policy, hosts that are
 * accepted as the source of a message (i.e., the original message sender) when
 * receiving a message are listed (comma separated values) using the
 * {@link #FROM_RPOLICY_S} setting
 * (when sending, using {@link #FROM_SPOLICY_S}) and hosts that are accepted as
 * the destination when receiving are listed using {@link #TO_RPOLICY_S}
 * (and when sending, using {@link #TO_SPOLICY_S}). By default, any message is
 * accepted. </P> <P>
 * With ModuleCommunicationBus values, the amount of conditions is first defined
 * with {@link #NROF_MCBCS_S} and sending/receiving conditions are defined
 * as {@link ArithmeticCondition} with {@link #MCBACS_S} or {@link #MCBACR_S}
 * and the ModuleCommuncationBus IDs where to get values from to use with the
 * condition with {@link #MCBCVS_S} and {@link #MCBCVR_S}.</P>
 * <P>The MCB conditions are checked first, and if none of them match,
 * simple policy conditions are checked. If they don't exists,
 * or one of them matches, hop count policy is checked. If that doesn't exist
 * or matches to message's hop count, transfer is accepted.  Otherwise transfer
 * is denied.
 * </P>
 * @author Ari
 */
public class MessageTransferAcceptPolicy {

	/** Namespace for all "Message Transfer Accept policy" settings ({@value})*/
	public static final String MTA_POLICY_NS = "mtaPolicy";

	/** Number of Module Communication Bus Conditions -setting id ({@value}).
	 * Two comma separated values. Defines the number of receiving and number of
	 * sending conditions to read from the settings. */
	public static final String NROF_MCBCS_S = "nrofMCBACs";

	/** Module Communication Bus Arithmetic Condition for Receiving -setting id
	 * ({@value}). {@link ArithmeticCondition}. Defines one arithmetic condition
	 * to use for receiving messages. */
	public static final String MCBACR_S = "MCBRcondition";
	/** Module Communication Bus Arithmetic Condition for Sending -setting id
	 * ({@value}). {@link ArithmeticCondition}. */
	public static final String MCBACS_S = "MCBScondition";

	/** Module Communication Bus Condition Value for Receiving -setting id
	 * ({@value}). String. Defines the ID to use with the receiving
	 * condition. */
	public static final String MCBCVR_S = "MCBRvalue";
	/** Module Communication Bus Condition Value for Sending -setting id
	 * ({@value}). String. Defines the ID to use with the sending
	 * condition. */
	public static final String MCBCVS_S = "MCBSvalue";

	/** The valued used in to-policy to refer to this host ({@value}) */
	public static final int TO_ME_VALUE = -1;

	/** Simple-policy accept-to -setting id ({@value}). Integer list.
	 * Defines the addresses of the hosts accepted as the destination of a
	 * message when receiving messages. Special value {@link #TO_ME_VALUE}
	 * refers to this host. */
	public static final String TO_RPOLICY_S = "toReceivePolicy";

	/** Simple-policy accept-from -setting id ({@value}). Integer list.
	 * Defines the addresses of the hosts accepted as the source of a
	 * message when receiving messages. Special value {@link #TO_ME_VALUE}
	 * refers to this host. */
	public static final String FROM_RPOLICY_S = "fromReceivePolicy";

	/** Simple-policy accept-to -setting id ({@value}). Integer list.
	 * Defines the addresses of the hosts accepted as the destination of a
	 * message when sending messages. Special value {@link #TO_ME_VALUE} refers
	 * to this host (but doesn't usually make much sense here). */
	public static final String TO_SPOLICY_S = "toSendPolicy";

	/** <P> Simple-policy accept-from -setting id ({@value}). Integer list.
	 * Defines the addresses of the hosts accepted as the source of a
	 * message when sending messages. Special value {@link #TO_ME_VALUE} refers
	 * to this host. </P>
	 * <P> <B>Note:</B> if this setting is defined and the {@link #TO_ME_VALUE}
	 * is NOT listed, the hosts own messages are not sent anywhere. </P>*/
	public static final String FROM_SPOLICY_S = "fromSendPolicy";

	/** Hop count forwarding receive policy -setting id ({@value}).
	 * {@link ArithmeticCondition}. Defines condition for the message hop
	 * count; if the condition does not match, the message is rejected,
	 * unless it is destined to this node. */
	public static final String HOPCOUNT_RPOLICY_S = "hopCountReceivePolicy";
	/** Hop count forwarding send policy -setting id ({@value}).
	 * {@link ArithmeticCondition}. Defines condition for the message hop
	 * count; if the condition does not match, the message is not offered
	 * to other nodes, unless it would be delivered to the final destination. */
	public static final String HOPCOUNT_SPOLICY_S = "hopCountSendPolicy";

	private ArrayList<Tuple<String,ArithmeticCondition>> recvConditions = null;
	private ArrayList<Tuple<String,ArithmeticCondition>> sendConditions = null;

	private Range[] toSendPolicy = null;
	private Range[] fromSendPolicy = null;
	private Range[] toReceivePolicy = null;
	private Range[] fromReceivePolicy = null;
	private ArithmeticCondition hopCountSendPolicy = null;
	private ArithmeticCondition hopCountReceivePolicy = null;

	public MessageTransferAcceptPolicy(Settings nsSettings) {
		Settings s;

		if (! nsSettings.contains(MTA_POLICY_NS)) {
			return; /* no (or "default") policy */
		}

		s = new Settings(nsSettings.getSetting(MTA_POLICY_NS));
		addMCBCs(s);

		if (s.contains(TO_SPOLICY_S)) {
			this.toSendPolicy = s.getCsvRanges(TO_SPOLICY_S);
		}
		if (s.contains(FROM_SPOLICY_S)) {
			this.fromSendPolicy = s.getCsvRanges(FROM_SPOLICY_S);
		}
		if (s.contains(TO_RPOLICY_S)) {
			this.toReceivePolicy = s.getCsvRanges(TO_RPOLICY_S);
		}
		if (s.contains(FROM_RPOLICY_S)) {
			this.fromReceivePolicy = s.getCsvRanges(FROM_RPOLICY_S);
		}
		if (s.contains(HOPCOUNT_SPOLICY_S)) {
			hopCountSendPolicy = s.getCondition(HOPCOUNT_SPOLICY_S);
		}
		if (s.contains(HOPCOUNT_RPOLICY_S)) {
			hopCountReceivePolicy = s.getCondition(HOPCOUNT_RPOLICY_S);
		}
	}

	/**
	 * Adds MessageCommunicationBus Conditions
	 * @param s Settings where the conditions are read from
	 */
	private void addMCBCs(Settings s) {
		if (!s.contains(NROF_MCBCS_S)) {
			return; /* no transfer policy defined */
		}

		int[] nrof = s.getCsvInts(NROF_MCBCS_S);
		if (nrof[0] > 0) { /* create lists only if needed */
			this.recvConditions =
				new ArrayList<Tuple<String,ArithmeticCondition>>();
		}
		if (nrof[1] > 0) {
			this.sendConditions =
				new ArrayList<Tuple<String,ArithmeticCondition>>();
		}

		addConditions(s, MCBACR_S, MCBCVR_S, this.recvConditions,  nrof[0]);
		addConditions(s, MCBACS_S, MCBCVS_S, this.sendConditions,  nrof[1]);
	}

	/**
	 * Read conditions from the settings and add them to the given list
	 * @param s The settings object
	 * @param cPrefix Condition setting prefix
	 * @param vPrefix Value setting prefix
	 * @param list The list to add conditions
	 * @param nrof The number of settings to read
	 */
	private void addConditions(Settings s, String cPrefix, String vPrefix,
			ArrayList<Tuple<String,ArithmeticCondition>> list,
			int nrof) {
		for (int i=1; i<=nrof; i++) {
			ArithmeticCondition ac = s.getCondition(cPrefix + i);
			String mcbValue = s.getSetting(vPrefix + i);
			list.add(new Tuple<String, ArithmeticCondition>(mcbValue, ac));
		}
	}

	/**
	 * Checks all the Module Communication Bus conditions and returns false
	 * if at least one of them failed.
	 * @param mcb The module communication bus to use
	 * @param receiving Should check using the receiving conditions list;
	 * if false, the sending conditions list is used
	 * @return true if all conditions evaluated to true
	 */
	private boolean checkMcbConditions(ModuleCommunicationBus mcb,
			boolean receiving) {
		ArrayList<Tuple<String,ArithmeticCondition>> list =
			(receiving ? this.recvConditions : this.sendConditions);

		if (list == null) {
			return true;
		}

		for (Tuple<String,ArithmeticCondition> t : list) {
			if (!mcb.containsProperty(t.getKey())) {
				continue; /* no value in the bus; can't fail condition */
			}
			if (t.getValue().isTrueFor(mcb.getDouble(t.getKey(), 0))){
				return false;
			}
		}

		return true;
	}

	/**
	 * Checks if the host's address is contained in the policy list
	 * (or {@value #TO_ME_VALUE} is contained and the address matches to
	 * thisHost parameter)
	 * @param host The hosts whose address to check
	 * @param policy The list of accepted addresses
	 * @param thisHost The address of this host
	 * @return True if the address was in the policy list, or the policy list
	 * was null
	 */
	private boolean checkSimplePolicy(DTNHost host, Range [] policy,
			int thisHost) {
		int address;

		if (policy == null) {
			return true;
		}

		address = host.getAddress();

		for (Range r : policy) {
			if (r.isInRange(TO_ME_VALUE) && address == thisHost) {
				return true;
			}
			 else if (r.isInRange(address)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks the given messages hop count against the given policy arithmetic
	 * condition
	 * @param m The message whose hop count is checked
	 * @param ac The policy arithmetic condition
	 * @return True if the condition is null or the hop count matches to the
	 * condition, false otherwise
	 */
	private boolean checkHopCountPolicy(Message m, ArithmeticCondition ac) {
		if (ac == null) {
			return true;
		} else {
			return ac.isTrueFor(m.getHopCount());
		}
	}

	/**
	 * Returns true if the given message, using the given connection, is OK
	 * to send from "from" to "to" host.
	 * @param from The sending host
	 * @param to The receiving host
	 * @param con The connection used by the hosts
	 * @param m The message to transfer
	 * @return True if the message is OK to transfer, false is not
	 */
	public boolean acceptSending(DTNHost from, DTNHost to, Connection con,
			Message m) {
		if (!checkMcbConditions(from.getComBus(), false)) {
			return false;
		}

		int myAddr = from.getAddress();
		if (! (checkSimplePolicy(m.getTo(), this.toSendPolicy, myAddr) &&
			checkSimplePolicy(m.getFrom(), this.fromSendPolicy,	myAddr)) ) {
			return false;
		}

		if (m.getTo() != to &&
				!checkHopCountPolicy(m, this.hopCountSendPolicy)){
			return false;
		}

		return true;
	}

	/**
	 * Returns true if the given message is OK to receive from "from" to
	 * "to" host.
	 * @param from The sending host
	 * @param to The receiving host
	 * @param m The message to transfer
	 * @return True if the message is OK to transfer, false is not
	 */
	public boolean acceptReceiving(DTNHost from, DTNHost to, Message m) {
		if (! checkMcbConditions(to.getComBus(), true)) {
			return false;
		}

		int myAddr = to.getAddress();
		if (! (checkSimplePolicy(m.getTo(), this.toReceivePolicy,myAddr) &&
			checkSimplePolicy(m.getFrom(), this.fromReceivePolicy, myAddr)) ) {
			return false;
		}

		if (m.getTo() != to &&
				!checkHopCountPolicy(m, this.hopCountReceivePolicy)) {
			return false;
		}

		return true;
	}

}
