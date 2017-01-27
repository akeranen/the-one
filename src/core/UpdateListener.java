/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

import java.util.List;

/**
 * Interface for classes that want to be informed about every single update
 * call to the World object. <BR>
 * <b>NOTE:</b> if update interval is large (if, e.g., no movement or
 * connection simulation is needed), update listeners may not get called at all
 * during the simulation.
 */
public interface UpdateListener {

	/**
	 * Method is called on every update cycle.
	 * @param hosts A list of all hosts in the world
	 */
	public void updated(List<DTNHost> hosts);

}
