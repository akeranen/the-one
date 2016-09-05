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
package routing.centrality;

import core.DTNHost;
import core.Settings;
import core.SimClock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import routing.community.Duration;

/**
 * CWindow centrality algorithm implementation.
 */
public class CWindowCentrality extends Centrality {

    /**
     * Width of time window into which to group past history -setting id
     * {@value}
     */
    public static final String CENTRALITY_WINDOW_SETTING = "timeWindow";
    /**
     * Interval between successive updates to centrality values -setting id
     * {@value}
     */
    public static final String COMPUTATION_INTERVAL_SETTING = "computeInterval";

    /**
     * Time to wait before recomputing centrality values (node degree)
     */
    protected static int COMPUTE_INTERVAL = 600; // seconds, i.e. 10 minutes
    /**
     * Width of each time interval in which to count the node's degree
     */
    protected static int CENTRALITY_TIME_WINDOW = 86400; // 24 hrs, from literature

    /**
     * Saved global centrality from last computation
     */
    protected double globalCentrality;
    /**
     * Saved local centrality from last computation
     */
    protected double localCentrality;

    /**
     * timestamp of last global centrality computation
     */
    protected int lastGlobalComputationTime;
    /**
     * timestamp of last local centrality computation
     */
    protected int lastLocalComputationTime;

    /**
     * Constructor that receives specific settings.
     * @param s A reference to simulation settings.
     */
    public CWindowCentrality(Settings s) {
        if (s.contains(CENTRALITY_WINDOW_SETTING)) {
            CENTRALITY_TIME_WINDOW = s.getInt(CENTRALITY_WINDOW_SETTING);
        }

        if (s.contains(COMPUTATION_INTERVAL_SETTING)) {
            COMPUTE_INTERVAL = s.getInt(COMPUTATION_INTERVAL_SETTING);
        }
    }

    /**
     * Copy constructor.
     * @param proto Prototype.
     */
    public CWindowCentrality(CWindowCentrality proto) {
        // set these back in time (negative values) to do one computation at the 
        // start of the sim
        this.lastGlobalComputationTime = this.lastLocalComputationTime
                = -COMPUTE_INTERVAL;
    }

    @Override
    public double getGlobalCentrality(Map<DTNHost, List<Duration>> connHistory) {
        if (SimClock.getIntTime() - this.lastGlobalComputationTime < COMPUTE_INTERVAL) {
            return globalCentrality;
        }

        // initialize
        int epochCount = SimClock.getIntTime() / CENTRALITY_TIME_WINDOW;
        int[] centralities = new int[epochCount];
        int epoch, timeNow = SimClock.getIntTime();
        Map<Integer, Set<DTNHost>> nodesCountedInEpoch
                = new HashMap<Integer, Set<DTNHost>>();

        for (int i = 0; i < epochCount; i++) {
            nodesCountedInEpoch.put(i, new HashSet<DTNHost>());
        }

        /*
         * For each node, loop through connection history until we crossed all
         * the epochs we need to cover
         */
        for (Map.Entry<DTNHost, List<Duration>> entry : connHistory.entrySet()) {
            DTNHost h = entry.getKey();
            for (Duration d : entry.getValue()) {
                int timePassed = (int) (timeNow - d.end);

                // if we reached the end of the last epoch, we're done with this node
                if (timePassed >= CENTRALITY_TIME_WINDOW * epochCount) {
                    break;
                }

                // compute the epoch this contact belongs to
                epoch = timePassed / CENTRALITY_TIME_WINDOW;

                // Only consider each node once per epoch
                Set<DTNHost> nodesAlreadyCounted = nodesCountedInEpoch.get(epoch);
                if (nodesAlreadyCounted.contains(h)) {
                    continue;
                }

                // increment the degree for the given epoch
                centralities[epoch]++;
                nodesAlreadyCounted.add(h);
            }
        }

        // compute and return average node degree
        int sum = 0;
        for (int i = 0; i < epochCount; i++) {
            sum += centralities[i];
        }
        this.globalCentrality = ((double) sum) / epochCount;

        this.lastGlobalComputationTime = SimClock.getIntTime();

        return this.globalCentrality;
    }

    @Override
    public double getLocalCentrality(Map<DTNHost, List<Duration>> connHistory, Set<DTNHost> community) {
        if (SimClock.getIntTime() - this.lastLocalComputationTime < COMPUTE_INTERVAL) {
            return localCentrality;
        }

        // centralities will hold the count of unique encounters in each epoch
        int epochCount = SimClock.getIntTime() / CENTRALITY_TIME_WINDOW;
        int[] centralities = new int[epochCount];
        int epoch, timeNow = SimClock.getIntTime();
        Map<Integer, Set<DTNHost>> nodesCountedInEpoch
                = new HashMap<Integer, Set<DTNHost>>();

        for (int i = 0; i < epochCount; i++) {
            nodesCountedInEpoch.put(i, new HashSet<DTNHost>());
        }

        /*
         * For each node, loop through connection history until we crossed all
         * the epochs we need to cover
         */
        for (Map.Entry<DTNHost, List<Duration>> entry : connHistory.entrySet()) {
            DTNHost h = entry.getKey();

            // if the host isn't in the local community, we don't consider it
            if (!community.contains(h)) {
                continue;
            }

            for (Duration d : entry.getValue()) {
                int timePassed = (int) (timeNow - d.end);

                // if we reached the end of the last epoch, we're done with this node
                if (timePassed >= CENTRALITY_TIME_WINDOW * epochCount) {
                    break;
                }

                // compute the epoch this contact belongs to
                epoch = timePassed / CENTRALITY_TIME_WINDOW;

                // Only consider each node once per epoch
                Set<DTNHost> nodesAlreadyCounted = nodesCountedInEpoch.get(epoch);
                if (nodesAlreadyCounted.contains(h)) {
                    continue;
                }

                // increment the degree for the given epoch
                centralities[epoch]++;
                nodesAlreadyCounted.add(h);
            }
        }

        // compute and return average node degree
        int sum = 0;
        for (int i = 0; i < epochCount; i++) {
            sum += centralities[i];
        }
        this.localCentrality = ((double) sum) / epochCount;

        this.lastLocalComputationTime = SimClock.getIntTime();

        return this.localCentrality;
    }

    @Override
    public CWindowCentrality replicate() {
        return new CWindowCentrality(this);
    }
}
