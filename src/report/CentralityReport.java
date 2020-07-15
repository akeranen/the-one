/*
 * Copyright 2016, Michael D. Silva (micdoug.silva@gmail.com)
 * Released under GPLv3. See LICENSE.txt for details.
 */

package report;

import core.DTNHost;
import core.SimClock;
import core.SimScenario;
import core.UpdateListener;
import java.util.List;
import routing.MessageRouter;
import routing.centrality.ReportCentrality;
import routing.community.ReportCommunity;

/**
 * Reports the local and global centrality values of the nodes.
 */
public class CentralityReport extends Report implements UpdateListener{

    private int simulationDuration; 
    
    /**
     * Constructor.
     */
    public CentralityReport() {
        init();
        simulationDuration = (int)Math.floor(SimScenario.getInstance().getEndTime());
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        int curTime = SimClock.getIntTime();
        // If it is the end of the simulation it is time to write the communities
        if (curTime % simulationDuration == 0) {
            for (DTNHost host : hosts) {
                MessageRouter router = host.getRouter();
                if (router instanceof ReportCentrality) {
                    ReportCentrality report = (ReportCentrality) router;
                    this.write("" + host.getAddress() + " " 
                            + report.getLocalCentrality() + " " 
                            + report.getGlobalCentrality());
                } 
            }
        }
    }
}