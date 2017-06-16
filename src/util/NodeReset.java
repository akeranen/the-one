package util;

import java.util.List;
import java.util.Random;
import movement.*;
import movement.map.MapNode;
import core.*;
import routing.*;

public class NodeReset {

	public static void resetNode(DTNHost host) {
		Random rnd = new Random(host.getAddress());
		
		// Node Reset should only be done within the VoluntaryHelperMovement Model
		if (host.getMovement().getClass() != VoluntaryHelperMovement.class)
			return;
		
		// List of all map nodes
		List<MapNode> nodes = ((VoluntaryHelperMovement)host.getMovement()).getMap().getNodes();
		
		// Node that the host is reset on
		int index = rnd.nextInt(nodes.size());
		
		// Forces node to get a new path
		host.setLocation(nodes.get(index).getLocation());
		((VoluntaryHelperMovement)host.getMovement()).getCarMM().setLastMapNode(nodes.get(index));
		
		// Switch to Random Map Based Movement
		((VoluntaryHelperMovement)(host.getMovement())).setMovementAsForcefullySwitched();
		((VoluntaryHelperMovement)(host.getMovement())).chooseRandomMapBasedMode();
		
		
		
		// Set a random energy level between 0.1 and 1.0
		double[] range = {0.1,1.0};
		
		((ActiveRouter)(host.getRouter())).getEnergy().setEnergy(range);
		
	}
}
