/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

/**
 * This interface should be implemented by classes that want to be notified
 * of variable value changes in ModuleCommunicationBuses.
 */
public interface ModuleCommunicationListener {

	/**
	 * This method is called whenever a variable, whose changes the module has
	 * registered to, changes.
	 * @param key The name of the variable
	 * @param newValue New value for the variable
	 */
	public void moduleValueChanged(String key, Object newValue);

}
