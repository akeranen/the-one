/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Intermodule communication bus. Works as a blackboard where modules can
 * post data, subscribe to data changes and also poll for data values.
 * This is fairly similar to Message class' property interface, but these
 * values are shared for a node instead of message.
 */
public class ModuleCommunicationBus {
	/** Initial capacity for the listener lists (instead of 10) */
	private static int INIT_CAPACITY = 3;
	/** The values in the blackboard (or null if none)*/
	private HashMap<String, Object> values;
	/** Subscribed listeners (or null if none)*/
	private HashMap<String, List<ModuleCommunicationListener>> listeners;

	/**
	 * Constructor.
	 */
	public ModuleCommunicationBus() {
		this.values = null; /* use lazy creation  */
		this.listeners = null;
	}

	/**
	 * Adds a new property for this node. The key can be any string but
	 * it should be such that no other class accidently uses the same value.
	 * Note that, unless the value is immutable, it can be changed by any
	 * object that can call {@link #getProperty}.
	 * @param key The key which is used to lookup the value
	 * @param value The value to store
	 * @throws SimError if there is already a value for the given key
	 */
	public void addProperty(String key, Object value) throws SimError {
		if (this.values != null && this.values.containsKey(key)) {
			/* check to prevent accidental name space collisions */
			throw new SimError("A value for the key " + key +
					" already exists");
		}

		this.updateProperty(key, value);
	}

	/**
	 * Returns an object that was stored using the given key. If such object
	 * is not found, null is returned.
	 * @param key The key used to lookup the object
	 * @return The stored object or null if it isn't found
	 */
	public Object getProperty(String key) {
		if (this.values == null) {
			return null;
		}
		return this.values.get(key);
	}

	/**
	 * Returns true if the bus contains a value for the given key
	 * @param key The key for which a value's existence is checked
	 * @return true if the value exists, false if not
	 */
	public boolean containsProperty(String key) {
		if (this.values == null) {
			return false;
		}
		return this.values.containsKey(key);
	}

	/**
	 * Updates a value for an existing property. For storing the value first
	 * time, {@link #addProperty(String, Object)} should be used which
	 * checks for name space clashes.
	 * @param key The key which is used to lookup the value
	 * @param value The new value to store
	 */
	public void updateProperty(String key, Object value) throws SimError {
		if (this.values == null) {
			/* lazy creation to prevent performance overhead for classes
			   that don't use the property feature  */
			this.values = new HashMap<String, Object>();
		}

		this.values.put(key, value);
		notifyListeners(key, value);
	}

	/**
	 * Changes the Double value with given key with the value delta
	 * @param key The key of variable to update
	 * @param delta Value added to the old value
	 * @return The new value
	 * @throws SimError if the value with the given key was not a Double
	 */
	public double updateDouble(String key, double delta) throws SimError {
		double current;
		try {
			current = (Double)getProperty(key);
			updateProperty(key, current + delta);
		}
		catch (ClassCastException cce) {
			throw new SimError("No Double value for key " + key);
		}
		catch (NullPointerException npe) {
			throw new SimError("No value for key " + key);
		}

		return current + delta;
	}

	/**
	 * Returns a double value from the communication bus.
	 * @param key The key of the variable
	 * @param naValue The value to return if there is no value for the key
	 * @return The value of the key, or the naValue if they key was not found
	 * @throws SimError if the value with the given key was not a Double
	 */
	public double getDouble(String key, double naValue) throws SimError {
		Object value = this.getProperty(key);
		if (value == null) {
			return naValue;
		}
		try {
			return (Double)value;
		}
		catch (ClassCastException cce) {
			throw new SimError("No Double value for key " + key);
		}
	}

	/**
	 * Returns an integer value from the communication bus.
	 * @param key The key of the variable
	 * @param naValue The value to return if there is no value for the key
	 * @return The value of the key, or the naValue if they key was not found
	 * @throws SimError if the value with the given key was not an Integer
	 */
	public int getInt(String key, int naValue) throws SimError {
		Object value = this.getProperty(key);
		if (value == null) {
			return naValue;
		}
		try {
			return (Integer)value;
		}
		catch (ClassCastException cce) {
			throw new SimError("No Integer value for key " + key);
		}
	}

	/**
	 * Subscribes a module to changes of a certain value.
	 * @param key The key of the value whose changes the module is interested of
	 * @param module The module to subscribe.
	 */
	public void subscribe(String key, ModuleCommunicationListener module) {
		if (this.listeners == null) {
			/* first listener for the whole node */
			this.listeners =
				new HashMap<String, List<ModuleCommunicationListener>>();
		}

		List<ModuleCommunicationListener> list = this.listeners.get(key);
		if (list == null) {
			/* first listener for this key */
			list = new ArrayList<ModuleCommunicationListener>(INIT_CAPACITY);
			this.listeners.put(key, list);
		}

		list.add(module);
	}

	/**
	 * Removes a notification subscription
	 * @param key The key for which the subscription should be removed
	 * @param module The module to whose subscription is removed
	 */
	public void unsubscribe(String key, ModuleCommunicationListener module) {
		List<ModuleCommunicationListener> list;

		if (this.listeners == null) {
			return; /* no subscriptions */
		}

		list = this.listeners.get(key);
		if (list == null) {
			return; /* no subscriptions for the key */
		}

		list.remove(module);
	}


	/**
	 * Notifies all listeners that have subscribed to the given key
	 * @param key The key which got new value
	 * @param newValue The new value for the key
	 */
	private void notifyListeners(String key, Object newValue) {
		List<ModuleCommunicationListener> list;

		if (this.listeners == null) {
			return;
		}
		list = this.listeners.get(key);

		if (list == null) {
			return;
		}

		for (ModuleCommunicationListener mcl : list) {
			mcl.moduleValueChanged(key, newValue);
		}
	}


	@Override
	public String toString() {
		return "ComBus with mapping: " + (this.values != null ?
				this.values.toString() : "n/a");
	}
}
