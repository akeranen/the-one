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