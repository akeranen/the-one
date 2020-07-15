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
import routing.community.ReportCommunity;

/**
 * 
 * Report the all the nodes' communities.
 * 
 */
public class CommunityReport extends Report implements UpdateListener {

	private int simulationDuration;

	/**
	 * 
	 * Constructor.
	 * 
	 */
	public CommunityReport() {
		init();
		simulationDuration = (int) Math.floor(SimScenario.getInstance().getEndTime());
	}

	@Override
	public void updated(List<DTNHost> hosts) {
		int curTime = SimClock.getIntTime();

		// If it is the end of the simulation it is time to write the
		// communities
		if (curTime % simulationDuration == 0) {
			for (DTNHost host : hosts) {
				MessageRouter router = host.getRouter();
				if (router instanceof ReportCommunity) {
					ReportCommunity report = (ReportCommunity) router;
					StringBuilder strBuilder = new StringBuilder();
					strBuilder.append("" + host.getAddress() + " ");
					for (DTNHost h : report.getCommunity()) {
						strBuilder.append("" + h.getAddress() + " ");
					}
					this.write(strBuilder.toString());
				}
			}
		}
	}
}