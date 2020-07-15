/*
 * Copyright 2016, Michael D. Silva (micdoug.silva@gmail.com)
 * Released under GPLv3. See LICENSE.txt for details.
 */

package routing.centrality;

import core.DTNHost;
import java.util.List;
import java.util.Map;
import java.util.Set;
import routing.community.Duration;

/**
 * The base interface for implementing centrality detection algorithms.
 */
public abstract class Centrality {

    /**
     * Store the node's related to this centrality computation algorithm.
     */
    protected DTNHost host;

    /**
     * Returns the computed global centrality based on the connection history
     * passed as an argument. The global centrality measures the centrality
     * of the node taking into account all the nodes all in the simulation.
     *
     * @param connHistory Contact History on which to compute centrality
     * @return Value corresponding to the global centrality
     */
    public abstract double getGlobalCentrality(Map<DTNHost, List<Duration>> connHistory);

    /**
     * Returns the computed local centrality based on the connection history and
     * community detection objects passed as parameters. The local centrality measures 
     * the centrality taking into account only the nodes in the local community.
     *
     * @param connHistory Contact history on which to compute centrality
     * @param community The community of the node.
     * @return Value corresponding to the local centrality
     */
    public abstract double getLocalCentrality(Map<DTNHost, List<Duration>> connHistory,
            Set<DTNHost> community);

    /**
     * Duplicates a Centrality object. This is a convention of the ONE to easily
     * create multiple instances of objects based on defined settings.
     *
     * @return A duplicate Centrality instance
     */
    public abstract Centrality replicate();

    /**
     * Set the host related to this instance of centrality computation.
     *
     * @param host The host related to this instance.
     */
    public void setHost(DTNHost host) {
        this.host = host;
    }
}
