/*
 * Copyright 2016, Michael D. Silva (micdoug.silva@gmail.com)
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing.centrality;

/**
 * Used by the CentralityReport to get the global and local centrality from routers.
 */
public interface ReportCentrality {
    /**
     * Get the node's local centrality value.
     */
    public double getLocalCentrality();
    
    /**
     * Get the node's global centrality value.
     */
    public double getGlobalCentrality();
}
