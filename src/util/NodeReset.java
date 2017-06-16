package util;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import movement.*;
import movement.map.MapNode;
import core.*;
import routing.*;
import routing.util.*;

public class NodeReset {

	public static void resetNode(DTNHost host) {
		Random rnd = new Random();
		
		int a;
		
		// Node Reset should only be done within the VoluntaryHelperMovement Model
		if (host.getMovement().getClass() != VoluntaryHelperMovement.class)
			return;
		
		List<MapNode> nodes = ((VoluntaryHelperMovement)host.getMovement()).getMap().getNodes();
		int index = host.getAddress();
		//int index = rnd.nextInt(nodes.size());
		
		// Forces node to get a new path
		host.setLocation(nodes.get(index).getLocation());
		((VoluntaryHelperMovement)host.getMovement()).getCarMM().setLastMapNode(nodes.get(index));
		
		if (((VoluntaryHelperMovement)host.getMovement()).getMode() == VoluntaryHelperMovement.movementMode.LOCAL_HELP_MODE)
			a = index;
		
		//((VoluntaryHelperMovement)(host.getMovement())).getlevyWalkMM().setLocation(nodes.get(index).getLocation());
		
		//if (((VoluntaryHelperMovement)(host.getMovement())).getMode() != VoluntaryHelperMovement.movementMode.RANDOM_MAP_BASED_MODE) {
			((VoluntaryHelperMovement)(host.getMovement())).setMovementAsForcefullySwitched();
		//((VoluntaryHelperMovement)(host.getMovement())).startOver();
			((VoluntaryHelperMovement)(host.getMovement())).chooseRandomMapBasedMode();
		//}
		
		
		
		// Set a random energy level between 0.1 and 1.0
		
		double[] range = {0.9,0.9};
		
		((ActiveRouter)(host.getRouter())).getEnergy().setEnergy(range);
		//host.getComBus().updateProperty(EnergyModel.ENERGY_VALUE_ID , 0.8 + rnd.nextDouble()*0.2);
		
	}
}
