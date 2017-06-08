package util;

import java.util.Collection;
import java.util.Random;

import core.*;
import routing.*;
import routing.util.*;

public class NodeReset {

	public static void resetNode(DTNHost host) {
		ActiveRouter router = (ActiveRouter)host.getRouter();
		EnergyModel energy = router.getEnergy();
		
		// Forces node to get a new path
		host.interruptMovement();
		
		if (host.getInterfaces().size() > 0) {
			// Close sending connections
			router.update();
			// Delete connections
			host.disconnectAll();
		}
		
		
		router.clearAllMessageLists();
		
		double[] energyRange = {0.1, 1.0};
		energy.setEnergy(energyRange);
		
	}
}
