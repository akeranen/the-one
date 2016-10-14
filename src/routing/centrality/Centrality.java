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
