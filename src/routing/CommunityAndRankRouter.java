/*
 * Copyright (C) 2016 Michael Dougras da Silva
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package routing;

import core.Connection;
import core.Coord;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.MovementListener;
import core.Settings;
import core.SimClock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import routing.community.CommunityDetection;
import routing.community.Duration;
import util.Tuple;

/**
 * The base implementation for protocols based on communities and global/local
 * rank.
 */
public abstract class CommunityAndRankRouter extends ActiveRouter {

    /**
     * Community algorithm -setting id ({@value}).
     */
    public static final String COMMUNITY_ALG_S = "communityAlg";
    /**
     * The default class to use for community detection.
     */
    private static final String DEFAULT_COMMUNITY_ALG = "DistributedKCliqueCommunityDetection";

    /**
     * Instance of community detection algorithm.
     */
    protected CommunityDetection comdetect;

    /**
     * Used to check if the routing data exchange is done.
     */
    private Set<Connection> conStates;

    /**
     * Store the current connections start time.
     */
    private Map<DTNHost, Double> startedConnections;

    /**
     * Track all the connection history between this node and others.
     */
    private Map<DTNHost, List<Duration>> conHistory;

    /**
     * Constructor.
     *
     * @param set The settings object.
     * @param namespace The base namespace for router configurations. 
     */
    public CommunityAndRankRouter(Settings set, String namespace) {
        super(set);

        Settings settings = new Settings(namespace);

        String communityAlg = settings.getSetting(COMMUNITY_ALG_S, DEFAULT_COMMUNITY_ALG);
        this.comdetect = (CommunityDetection) settings.createIntializedObject("routing.community." + communityAlg);
        this.conStates = new HashSet<Connection>();
        this.conHistory = new HashMap<DTNHost, List<Duration>>();
        this.startedConnections = new HashMap<DTNHost, Double>();
    }

    /**
     * Copy constructor.
     * @param prot Prototype.
     */
    public CommunityAndRankRouter(CommunityAndRankRouter prot) {
        super(prot);

        this.comdetect = prot.comdetect.replicate();
        this.conStates = new HashSet<Connection>();
        this.conHistory = new HashMap<DTNHost, List<Duration>>();
        this.startedConnections = new HashMap<DTNHost, Double>();
    }

    @Override
    public void init(DTNHost host, List<MessageListener> mListeners) {
        super.init(host, mListeners);

        // Set the host reference in the community detection instance.
        this.comdetect.setHost(host);
    }

    /**
     * Return the global rank value of the node.
     */
    public abstract double getGlobalRank();

    /**
     * Return the local rank value of the node.
     */
    public abstract double getLocalRank();

    /**
     * Return the community of the node.
     */
    public Set<DTNHost> getCommunity() {
        return this.comdetect.getCommunity();
    }

    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con);

        DTNHost otherHost = con.getOtherNode(getHost());
        CommunityAndRankRouter otherRouter = (CommunityAndRankRouter) otherHost.getRouter();

        // Started a new connection
        if (con.isUp()) {
            this.startedConnections.put(otherHost, SimClock.getTime());
            if (!this.conStates.contains(con)) {
                // Add the connection in the track list of the two routers
                this.conStates.add(con);
                otherRouter.conStates.add(con);
                // Compute the community
                // Create copies of familiar sets and communities, to ensure that the two nodes will receive 
                // the old vision (status before update) of the communities and familiar sets.
                Set<DTNHost> myFamiliarSet = new HashSet<DTNHost>(this.comdetect.getFamiliarSet());
                HashSet<DTNHost> otherFamiliarSet = new HashSet<DTNHost>(otherRouter.comdetect.getFamiliarSet());
                HashSet<DTNHost> myCommunity = new HashSet<DTNHost>(this.comdetect.getCommunity());
                HashSet<DTNHost> otherCommunity = new HashSet<DTNHost>(otherRouter.comdetect.getCommunity());

                this.comdetect.startContact(otherHost, otherCommunity, otherFamiliarSet, otherRouter.comdetect.getCommunityFamiliarSet());
                otherRouter.comdetect.startContact(getHost(), myCommunity, myFamiliarSet, this.comdetect.getCommunityFamiliarSet());
            }
        } // A connection was finished
        else {

            // Compute the contact duration and store in the connection history.
            double startTime = this.startedConnections.remove(otherHost);
            double endTime = SimClock.getTime();
            if (!this.conHistory.containsKey(otherHost)) {
                this.getConHistory().put(otherHost, new ArrayList<Duration>());
            }
            this.getConHistory().get(otherHost).add(new Duration(startTime, endTime));

            if (this.conStates.contains(con)) {
                // Remove the connection from the exchange list.
                this.conStates.remove(con);
                otherRouter.conStates.remove(con);

                CommunityDetection othercomdetect = otherRouter.comdetect;
                // Inform the community detection algorithm that the connection was finished
                this.comdetect.endContact(otherHost, othercomdetect.getFamiliarSet(), othercomdetect.getCommunityFamiliarSet(), this.getConHistory().get(otherHost));
                othercomdetect.endContact(getHost(), this.comdetect.getFamiliarSet(), this.comdetect.getCommunityFamiliarSet(), this.getConHistory().get(otherHost));
            }
        }
    }

    @Override
    public void update() {
        super.update();

        if (!canStartTransfer() || isTransferring()) {
            return; // Nothing to transfer or is currently transferring
        }

        // Try messages that could be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return;
        }

        tryOtherMessages();
    }

    /**
     * Process each message using community and rank information to decide 
     * if the message should be forwarded or not. 
     * @return A set of messages and connections to forward messages.
     */
    protected Tuple<Message, Connection> tryOtherMessages() {
        List<Tuple<Message, Connection>> messages = new ArrayList<Tuple<Message, Connection>>();

        Collection<Message> msgCollection = getMessageCollection();

        /**
         * Process all messages
         */
        for (Connection con : getConnections()) {
            // Get the reference to the router of the other node
            DTNHost otherNode = con.getOtherNode(getHost());
            final CommunityAndRankRouter otherRouter = (CommunityAndRankRouter) otherNode.getRouter();
            final double myLocalRank = getLocalRank();
            final double otherLocalRank = otherRouter.getLocalRank();
            final double myGlobalRank = getGlobalRank();
            final double otherGlobalRank = otherRouter.getGlobalRank();

            if (otherRouter.isTransferring()) {
                continue; // skip hosts that are transferring
            }

            for (Message m : msgCollection) {
                if (otherRouter.hasMessage(m.getId())) {
                    continue; // skip messages that the other one has
                }
                final DTNHost destHost = m.getTo();
                Message msg = shouldSend(destHost, otherRouter, myGlobalRank, otherGlobalRank, myLocalRank, otherLocalRank, m);
                if (msg != null) {
                    messages.add(new Tuple<Message, Connection>(msg, con));
                }
            }

        }
        if (messages.isEmpty()) {
            return null;
        }

        return tryMessagesForConnected(messages);
    }

    /**
     * Return the connection history of the node.
     */
    public Map<DTNHost, List<Duration>> getConHistory() {
        return conHistory;
    }

    /**
     * Method that decides whether to send or not the message to the other node.
     *
     * @param destHost The message destination.
     * @param otherRouter The router of the other node.
     * @param myGlobalRank My global rank value.
     * @param otherGlobalRank The global rank value of the other node.
     * @param myLocalRank My local rank value.
     * @param otherLocalRank The local rank value of the other node.
     * @param m The message to be evaluated.
     * @return The message to be sent, null otherwise.
     */
    protected Message shouldSend(DTNHost destHost, CommunityAndRankRouter otherRouter,
            double myGlobalRank, double otherGlobalRank,
            double myLocalRank, double otherLocalRank, Message m) {
        // Bubble Rap specification
        boolean meInCommunity = getCommunity().contains(destHost);
        boolean otherInCommunity = otherRouter.getCommunity().contains(destHost);

        // First case, both aren't in the dest community
        if (!meInCommunity && !otherInCommunity) {
            // use global rank
            if (otherGlobalRank > myGlobalRank) {
                return m;
            }
        } // Second case, both are in the dest community
        if (meInCommunity && otherInCommunity) {
            // use local rank
            if (otherLocalRank > myLocalRank) {
                return m;
            }
        } // Third case, only other node is in the dest community
        if (!meInCommunity && otherInCommunity) {
            return m;
        } 
        // 
        return null;
    }

}
