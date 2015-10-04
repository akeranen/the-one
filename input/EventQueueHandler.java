/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

import java.util.ArrayList;
import java.util.List;

import core.Settings;

/**
 * <p>
 * Handler for managing event queues. Supports two different type of event 
 * queues: external event queues and event generator classes.
 * For external event queues, the events are defined in external data
 * file(s) (see e.g. input.StandarEventsReader). Event generator classes
 * define events dynamically. Both type of event queues must implement
 * the input.EventQueue interface.
 * <p>
 * The total number of event queues to load is defined with variable 
 * <code>NROF_SETTING</code>, e.g.<br>
 * <code>Events.nrof = 3</code><br>
 * Separate event queues are configured with syntax 
 * <code>EventsN.variable = value</code> e.g.:<br>
 * <code>Events1.filePath = ee/messages.txt</code><br>
 * or<br>
 * <code>Events2.class = RandomMessageGenerator</code>
 * <p>
 * External event files are used when the variable <code>PATH_SETTING</code>
 * is used to define the path to the event file and event generator class 
 * is loaded when the name of the class is defined with 
 * <code>CLASS_SETTING</code>. 
 */
public class EventQueueHandler  {
	/** Event queue settings main namespace ({@value})*/
	public static final String SETTINGS_NAMESPACE = "Events";
	/** number of event queues -setting id ({@value})*/
	public static final String NROF_SETTING = "nrof";

	/** name of the events class (for class based events) -setting id
	 * ({@value}) */
	public static final String CLASS_SETTING = "class";
	/** name of the package where event generator classes are looked from */
	public static final String CLASS_PACKAGE = "input";
	
	/** number of events to preload from file -setting id ({@value})*/
	public static final String PRELOAD_SETTING = "nrofPreload";
	/** path of external events file -setting id ({@value})*/
	public static final String PATH_SETTING = "filePath";
	
	private List<EventQueue> queues;
	
	/**
	 * Creates a new EventQueueHandler which can be queried for 
	 * event queues.
	 */
	public EventQueueHandler() {
		Settings settings = new Settings(SETTINGS_NAMESPACE);
		int nrof = settings.getInt(NROF_SETTING);
		this.queues = new ArrayList<EventQueue>();

		for (int i=1; i <= nrof; i++) {
			Settings s = new Settings(SETTINGS_NAMESPACE + i);

			if (s.contains(PATH_SETTING)) { // external events file
				int preload = 0;
				String path = "";
				if (s.contains(PRELOAD_SETTING)) {
					preload = s.getInt(PRELOAD_SETTING);
				}
				path = s.getSetting(PATH_SETTING);

				queues.add(new ExternalEventsQueue(path, preload));
			}
			else if (s.contains(CLASS_SETTING)) { // event generator class
				String className = CLASS_PACKAGE + "." + 
					s.getSetting(CLASS_SETTING);
				EventQueue eq = (EventQueue)s.createIntializedObject(className);
				
				queues.add(eq);
			}
		}
	}
	
	/** 
	 * Returns all the loaded event queues
	 * @return all the loaded event queues
	 */
	public List<EventQueue> getEventQueues() {
		return this.queues;
	}

}
