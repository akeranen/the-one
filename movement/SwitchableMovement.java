/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import core.Coord;

/**

 * Movement models to be used by ExtendedMovementModels should implement this
 * interface
 * 
 * @author Frans Ekman
 */
public interface SwitchableMovement {

	/**
	 * Tell the movement model what its current location is
	 * @param lastWaypoint
	 */
	public void setLocation(Coord lastWaypoint);
	
	/**
	 * Get the last location the getPath() of this movement model has returned 
	 * @return the last location
	 */
	public Coord getLastLocation();
	
	/**
	 * Checks if the movement model is finished doing its task and it's time to 
	 * switch to the next movement model. The method should be called between 
	 * getPath() calls.
	 * @return true if ready
	 */
	public boolean isReady();
}
