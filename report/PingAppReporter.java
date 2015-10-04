/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */

package report;

import applications.PingApplication;
import core.Application;
import core.ApplicationListener;
import core.DTNHost;

/**
 * Reporter for the <code>PingApplication</code>. Counts the number of pings
 * and pongs sent and received. Calculates success probabilities.
 * 
 * @author teemuk
 */
public class PingAppReporter extends Report implements ApplicationListener {
	
	private int pingsSent=0, pingsReceived=0;
	private int pongsSent=0, pongsReceived=0;
	
	public void gotEvent(String event, Object params, Application app,
			DTNHost host) {
		// Check that the event is sent by correct application type
		if (!(app instanceof PingApplication)) return;
		
		// Increment the counters based on the event type
		if (event.equalsIgnoreCase("GotPing")) {
			pingsReceived++;
		}
		if (event.equalsIgnoreCase("SentPong")) {
			pongsSent++;
		}
		if (event.equalsIgnoreCase("GotPong")) {
			pongsReceived++;
		}
		if (event.equalsIgnoreCase("SentPing")) {
			pingsSent++;
		}
		
	}

	
	@Override
	public void done() {
		write("Ping stats for scenario " + getScenarioName() + 
				"\nsim_time: " + format(getSimTime()));
		double pingProb = 0; // ping probability
		double pongProb = 0; // pong probability
		double successProb = 0;	// success probability
		
		if (this.pingsSent > 0) {
			pingProb = (1.0 * this.pingsReceived) / this.pingsSent;
		}
		if (this.pongsSent > 0) {
			pongProb = (1.0 * this.pongsReceived) / this.pongsSent;
		}
		if (this.pingsSent > 0) {
			successProb = (1.0 * this.pongsReceived) / this.pingsSent;
		}
		
		String statsText = "pings sent: " + this.pingsSent + 
			"\npings received: " + this.pingsReceived + 
			"\npongs sent: " + this.pongsSent +
			"\npongs received: " + this.pongsReceived +
			"\nping delivery prob: " + format(pingProb) +
			"\npong delivery prob: " + format(pongProb) + 
			"\nping/pong success prob: " + format(successProb)
			;
		
		write(statsText);
		super.done();
	}
}
