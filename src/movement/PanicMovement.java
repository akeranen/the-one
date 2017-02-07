package movement;

import java.util.List;

import core.Coord;
import core.Settings;
import movement.map.MapNode;
import movement.map.PanicPointsOfInterest;

public class PanicMovement extends ShortestPathMapBasedMovement implements SwitchableMovement {
    
	private Coord eventLocation;
	private Coord nodeLocation;
	
	public PanicMovement (Settings settings) {
		super(settings);
		// Only temporary solution for testing issues!
		//eventLocation = new Coord(getMaxX() / 4, getMaxY() / 4);
		eventLocation = new Coord(0.0, 0.0);
		nodeLocation = getInitialLocation();
		
		this.pois = new PanicPointsOfInterest(getMap(), getOkMapNodeTypes(),
					settings, rng, nodeLocation, eventLocation);
	}
	
	/**
	 * Copyconstructor.
	 * @param mbm The ShortestPathMapBasedMovement prototype to base
	 * the new object to
	 */
	protected PanicMovement(PanicMovement pm) {
		super(pm);
		this.eventLocation = pm.eventLocation;
		this.nodeLocation = pm.nodeLocation;
	}
	
	@Override
	public PanicMovement replicate() {
		return new PanicMovement(this);
	}

	
}
