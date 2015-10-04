/* 
 * Copyright 2011 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing.util;

import java.util.Random;

import core.*;

/**
 * Energy model for routing modules. Handles power use from scanning (device
 * discovery), scan responses, and data transmission. If scanning is done more 
 * often than 1/s, constant scanning is assumed (and power consumption does not
 * increase from {@link #scanEnergy} value).
 */
public class EnergyModel implements ModuleCommunicationListener {
	/** Initial units of energy -setting id ({@value}). Can be either a 
	 * single value, or a range of two values. In the latter case, the used
	 * value is a uniformly distributed random value between the two values. */
	public static final String INIT_ENERGY_S = "initialEnergy";
	
	/** Energy usage per scanning (device discovery) -setting id ({@value}). */
	public static final String SCAN_ENERGY_S = "scanEnergy";
	
	/** Energy usage per scanning (device discovery) response -setting id 
	 * ({@value}). */
	public static final String SCAN_RSP_ENERGY_S = "scanResponseEnergy";
	
	/** Energy usage per second when sending -setting id ({@value}). */
	public static final String TRANSMIT_ENERGY_S = "transmitEnergy";
	
	/** Base energy usage per second -setting id ({@value}). */
	public static final String BASE_ENERGY_S = "baseEnergy";
	
	/** Energy update warmup period -setting id ({@value}). Defines the 
	 * simulation time after which the energy level starts to decrease due to 
	 * scanning, transmissions, etc. Default value = 0. If value of "-1" is 
	 * defined, uses the value from the report warmup setting 
	 * {@link report.Report#WARMUP_S} from the namespace 
	 * {@value report.Report#REPORT_NS}. */
	public static final String WARMUP_S = "energyWarmup";

	/** {@link ModuleCommunicationBus} identifier for the "current amount of 
	 * energy left" variable. Value type: double */
	public static final String ENERGY_VALUE_ID = "Energy.value";
	
	/** Initial energy levels from the settings */
	private final double[] initEnergy;
	private double warmupTime;
	/** current energy level */
	private double currentEnergy;
	/** energy usage per scan */
	private double scanEnergy;
	/** energy usage per transmitted byte */
	private double transmitEnergy;
	/** energy usage per device discovery response */
	private double scanResponseEnergy;
	/** sim time of the last energy updated */
	private double lastUpdate;
	private ModuleCommunicationBus comBus;
	private static Random rng = null;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public EnergyModel(Settings s) {
		this.initEnergy = s.getCsvDoubles(INIT_ENERGY_S);
		
		if (this.initEnergy.length != 1 && this.initEnergy.length != 2) {
			throw new SettingsError(INIT_ENERGY_S + " setting must have " + 
					"either a single value or two comma separated values");
		}
		
		this.scanEnergy = s.getDouble(SCAN_ENERGY_S);
		this.transmitEnergy = s.getDouble(TRANSMIT_ENERGY_S);
		this.scanResponseEnergy = s.getDouble(SCAN_RSP_ENERGY_S);
	
		if (s.contains(WARMUP_S)) {
			this.warmupTime = s.getInt(WARMUP_S);
			if (this.warmupTime == -1) {
				this.warmupTime = new Settings(report.Report.REPORT_NS).
					getInt(report.Report.WARMUP_S);
			}
		}
		else {
			this.warmupTime = 0;
		}
	}
	
	/**
	 * Copy constructor.
	 * @param proto The model prototype where setting values are copied from
	 */
	protected EnergyModel(EnergyModel proto) {
		this.initEnergy = proto.initEnergy;
		setEnergy(this.initEnergy);
		this.scanEnergy = proto.scanEnergy;
		this.transmitEnergy = proto.transmitEnergy;
		this.warmupTime  = proto.warmupTime;
		this.scanResponseEnergy = proto.scanResponseEnergy;
		this.comBus = null;
		this.lastUpdate = 0;
	}
	
	public EnergyModel replicate() {
		return new EnergyModel(this);
	}
	
	/**
	 * Sets the current energy level into the given range using uniform 
	 * random distribution.
	 * @param range The min and max values of the range, or if only one value
	 * is given, that is used as the energy level
	 */
	protected void setEnergy(double range[]) {
		if (range.length == 1) {
			this.currentEnergy = range[0];
		}
		else {
			if (rng == null) {
				rng = new Random((int)(range[0] + range[1]));
			}
			this.currentEnergy = range[0] + 
				rng.nextDouble() * (range[1] - range[0]);
		}
	}
	
	/**
	 * Returns the current energy level
	 * @return the current energy level
	 */
	public double getEnergy() {
		return this.currentEnergy;
	}
	
	/**
	 * Updates the current energy so that the given amount is reduced from it.
	 * If the energy level goes below zero, sets the level to zero.
	 * Does nothing if the warmup time has not passed.
	 * @param amount The amount of energy to reduce
	 */
	protected void reduceEnergy(double amount) {
		if (SimClock.getTime() < this.warmupTime) {
			return;
		}
		
		if (comBus == null) {
			return; /* model not initialized (via update) yet */
		}
		
		if (amount >= this.currentEnergy) {
			comBus.updateProperty(ENERGY_VALUE_ID, 0.0);
		} else {
			comBus.updateDouble(ENERGY_VALUE_ID, -amount);
		}
		
	}
	
	/**
	 * Reduces the energy reserve for the amount that is used when another
	 * host connects (does device discovery)
	 */
	public void reduceDiscoveryEnergy() {
		reduceEnergy(this.scanResponseEnergy);
	}
	
	/**
	 * Reduces the energy reserve for the amount that is used by sending data
	 * and scanning for the other nodes. 
	 */
	public void update(NetworkInterface iface, ModuleCommunicationBus comBus) {
		double simTime = SimClock.getTime();
		double delta = simTime - this.lastUpdate;
		
		if (this.comBus == null) {
			this.comBus = comBus;
			this.comBus.addProperty(ENERGY_VALUE_ID, this.currentEnergy);
			this.comBus.subscribe(ENERGY_VALUE_ID, this);
		}
		
		if (simTime > this.lastUpdate && iface.isTransferring()) {
			/* sending or receiving data */
			reduceEnergy(delta * this.transmitEnergy);
		}
		this.lastUpdate = simTime;
		
		if (iface.isScanning()) {
			/* scanning at this update round */
			if (iface.getTransmitRange() > 0) {
				if (delta < 1) {
					reduceEnergy(this.scanEnergy * delta);
				} else {
					reduceEnergy(this.scanEnergy);
				}
			}
		}
	}
		
	/**
	 * Called by the combus if the energy value is changed
	 * @param key The energy ID
	 * @param newValue The new energy value
	 */
	public void moduleValueChanged(String key, Object newValue) {
		this.currentEnergy = (Double)newValue;
	}
	
}