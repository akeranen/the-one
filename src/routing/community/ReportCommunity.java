/*
 * Copyright 2016, Michael D. Silva (micdoug.silva@gmail.com)
 * Released under GPLv3. See LICENSE.txt for details.
 */
 
package routing.community;

import core.DTNHost;
import java.util.Set;

/**
 * Interface used by the CommunityReport.
 */
public interface ReportCommunity {
    
    /**
     * Get the node's current community.
     */
    public Set<DTNHost> getCommunity();
}
