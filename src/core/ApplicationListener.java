/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

/**
 * <p>
 * Interface for classes that want to be informed about messages
 * between hosts.
 * </p>
 *
 * <p>
 * Report classes wishing to receive application events should implement this
 * interface. Note that the application event names are defined by the
 * applications so any class wishing to interpret them must know the
 * application.
 * </p>
 *
 * @author teemuk
 * @author mjpitka
 */
public interface ApplicationListener {

	/**
	 * Application has generated an event.
	 *
	 * @param event		Event name.
	 * @param params	Additional parameters for the event
	 * @param app		Application instance that generated the event.
	 * @param host		The host this application instance is running on.
	 */
	public void gotEvent(String event, Object params, Application app,
			DTNHost host);
}
