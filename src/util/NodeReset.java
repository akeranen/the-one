package util;

import java.util.List;
import java.util.Random;
import movement.*;
import movement.map.MapNode;
import core.*;
import routing.*;

public class NodeReset {
	/**
	 * Factor used for seeding the pseudo-random number generator that decides the node's new location.
	 * Without this factor, hosts with similar addresses would get similar new locations.
	 */
	private static final int SEED_VARIETY_FACTOR = 7079;

	public static void resetNode(DTNHost host) {
		// Node Reset should only be done within the VoluntaryHelperMovement Model
		if (host.getMovement().getClass() != VoluntaryHelperMovement.class)
			return;

		Random rnd = new Random((SEED_VARIETY_FACTOR * host.getAddress()) ^ SimClock.getIntTime());
		// List of all map nodes
		List<MapNode> nodes = ((VoluntaryHelperMovement)host.getMovement()).getOkMapNodes();
		
		// Node that the host is reset on
		int index = rnd.nextInt(nodes.size());
		
		// Forces node to get a new path
		((VoluntaryHelperMovement)(host.getMovement())).setMovementAsForcefullySwitched();
		host.setLocation(nodes.get(index).getLocation());
		((VoluntaryHelperMovement)(host.getMovement())).startOver();
		
		
		
		// Set a random energy level between 0.1 and 1.0
		double[] range = {0.1,1.0};
		
		((ActiveRouter)(host.getRouter())).getEnergy().setEnergy(range);
		
	}
}
