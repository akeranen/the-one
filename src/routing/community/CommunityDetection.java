/*
 * Copyright 2016, Michael D. Silva (micdoug.silva@gmail.com)
 * Released under GPLv3. See LICENSE.txt for details.
 */

package routing.community;

import core.DTNHost;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The interface for community detection algorithms. The algorithms can keep track
 * of connection status and are responsible for determining whether a given host
 * is a member of a community or not.
 */
public abstract class CommunityDetection
{
    /**
    * The host attached to this community detection instance.
    */
    protected DTNHost host;

    /**
     * Set the host related to this community detection instance.
     * @param host 
     */
    public void setHost(DTNHost host) {
        this.host = host;
    }
    
    /**
     * Return the familiar set of this node.
     */
    public abstract Set<DTNHost> getFamiliarSet();
    
    /**
     * Return the familiar set of all the community members.
     */
    public abstract Map<DTNHost, Set<DTNHost>> getCommunityFamiliarSet();
    
    /**
     * Called to inform that the node is in contact with another node.
     * @param otherHost A reference to the other node.
     * @param otherCommunity The other node's community.
     * @param otherFamiliarSet The other node's familiar set.
     * @param otherFSOfC The familiar set of the other node's community members.
     */
    public abstract void startContact(DTNHost otherHost, Set<DTNHost> otherCommunity,
            Set<DTNHost> otherFamiliarSet, Map<DTNHost, Set<DTNHost>> otherFSOfC);

    /**
     * Called to inform the object that a contact was lost.
     * 
     * @param otherHost Host that is now disconnected from this object
     * @param otherFamiliarSet The familiar set of the other node.
     * @param otherFSOfC The familiar set ot the other node's community members.
     * @param connHistory Entire connection history between this host and the peer
     */
    public abstract void endContact(DTNHost otherHost, Set<DTNHost> otherFamiliarSet,
            Map<DTNHost, Set<DTNHost>> otherFSOfC, List<Duration> connHistory);

    /**
     * Returns a set of hosts that are members of the local community of this 
     * object. This method is only provided for reporting.
     * 
     * @return the Set representation of the local community
     */
    public abstract Set<DTNHost> getCommunity();

    /**
     * Duplicates this CommunityDetection object.
     * 
     * @return A semantically equal copy of this CommunityDetection object
     */
    public abstract CommunityDetection replicate();
}