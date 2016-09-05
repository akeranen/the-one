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
import core.Settings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the Distributed K-Clique community detection algorithm as
 * proposed in the paper "Distributed Community Detection in Delay Tolerant Networks".
 */
public class DistributedKCliqueCommunityDetection extends CommunityDetection {

	/**
	 * Configuration id for setting the parameter K in the algorithm.
	 */
    public static final String K_SETTING = "k";
    /**
     * Configuration id for setting the parameter familiar threshold 
     * in the algorithm.
     */
    public static final String FAMILIAR_SETTING = "familiarThreshold";

    /**
     * Stores the familiar set of hosts.
     */
    protected Set<DTNHost> familiarSet;
    /**
     * Stores the current local community.
     */
    protected Set<DTNHost> localCommunity;
    /**
     * Stores references to the familiar sets of
     * the hosts in the local community.
     */
    protected Map<DTNHost, Set<DTNHost>> familiarsOfMyCommunity;

    /**
     * The parameter k.
     */
    protected double k;
    /**
     * The parameter familiar threshold.
     */
    protected double familiarThreshold;

    /**
     * Basic constructor that loads the specific configurations.
     * @param s Settings reference.
     */
    public DistributedKCliqueCommunityDetection(Settings s) {
        this.k = s.getDouble(K_SETTING);
        this.familiarThreshold = s.getDouble(FAMILIAR_SETTING);
        this.familiarSet = new HashSet<DTNHost>();
        this.localCommunity = new HashSet<DTNHost>();
        this.familiarsOfMyCommunity = new HashMap<DTNHost, Set<DTNHost>>();
    }

    /**
     * Copy constructor.
     * @param proto The origin of the copy.
     */
    public DistributedKCliqueCommunityDetection(DistributedKCliqueCommunityDetection proto) {
        this.k = proto.k;
        this.familiarThreshold = proto.familiarThreshold;
        // Initializes all the sets without copying 
        familiarSet = new HashSet<DTNHost>();
        localCommunity = new HashSet<DTNHost>();
        this.familiarsOfMyCommunity = new HashMap<DTNHost, Set<DTNHost>>();
    }

    @Override
    public void setHost(DTNHost host) {
        super.setHost(host); 
        // Add the host to its own community.
        this.localCommunity.add(host);
    }

    @Override
    public Set<DTNHost> getFamiliarSet() {
        return this.familiarSet;
    }

    @Override
    public Map<DTNHost, Set<DTNHost>> getCommunityFamiliarSet() {
        return this.familiarsOfMyCommunity;
    }

    @Override
    public void startContact(DTNHost otherHost, Set<DTNHost> otherCommunity,
            Set<DTNHost> otherFamiliarSet, Map<DTNHost, Set<DTNHost>> otherFSOfC) {
        if (!this.localCommunity.contains(otherHost)) {
            
            // Get the intersection between my local community and the other host familiar set
            HashSet<DTNHost> intersection = new HashSet<DTNHost>(this.localCommunity);
            intersection.retainAll(otherFamiliarSet);
            if (intersection.size() >= (this.k - 1)) {
                this.localCommunity.add(otherHost);
                this.familiarsOfMyCommunity.put(otherHost, otherFamiliarSet);
                this.wasAdded(otherFSOfC);
            }
        }
    }
    
    /**
     * Analyze the similarity between my local community and the 
     * familiar set of the other host in the familiar set of the
     * added host.
     * @param otherFSOfC The familiar set of the added host.
     */
    private void wasAdded(Map<DTNHost, Set<DTNHost>> otherFSOfC) {
        for (DTNHost host: otherFSOfC.keySet()) {
            if (!this.localCommunity.contains(host)) {
                // Get the intersection between my local community and the other host familiar set
                HashSet<DTNHost> intersection = new HashSet<DTNHost>(this.localCommunity);
                intersection.retainAll(otherFSOfC.get(host));
                if (intersection.size() >= (this.k - 1)) {
                    this.localCommunity.add(host);
                    this.familiarsOfMyCommunity.put(host, otherFSOfC.get(host));
                }
            }
        }
    }

    @Override
    public void endContact(DTNHost otherHost, Set<DTNHost> otherFamiliarSet, Map<DTNHost, Set<DTNHost>> otherFSOfC, List<Duration> connHistory) {
        if (this.familiarSet.contains(otherHost)) {
            return;
        }
        
        // Compute cumulative contact duration with this peer
        Iterator<Duration> i = connHistory.iterator();
        double time = 0;
        while (i.hasNext()) {
            Duration d = i.next();
            time += d.end - d.start;
        }
        
        // If cumulative duration is greater or equals than threshold, add
        if (time >= this.familiarThreshold) {
            this.familiarSet.add(otherHost);
            this.localCommunity.add(otherHost);
            this.familiarsOfMyCommunity.put(otherHost, otherFamiliarSet);
            this.wasAdded(otherFSOfC);
        }
    }

    @Override
    public DistributedKCliqueCommunityDetection replicate() {
        return new DistributedKCliqueCommunityDetection(this);
    }

    @Override
    public Set<DTNHost> getCommunity() {
        return this.localCommunity;
    }
}
