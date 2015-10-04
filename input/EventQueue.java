/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

/**
 * Interface for event queues. Any class that is not a movement model or a 
 * routing module but wishes to provide events for the simulation (like creating
 * messages) must implement this interface and register itself to the 
 * simulator. See the {@link EventQueueHandler} class for configuration 
 * instructions.
 */
public interface EventQueue {
	
	/**
	 * Returns the next event in the queue or ExternalEvent with time of 
	 * double.MAX_VALUE if there are no events left.
	 * @return The next event
	 */
	public ExternalEvent nextEvent();
	
	/**
	 * Returns next event's time or Double.MAX_VALUE if there are no 
	 * events left in the queue.
	 * @return Next event's time
	 */
	public double nextEventsTime();

}
