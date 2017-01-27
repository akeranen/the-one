/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import java.util.Random;

import util.ActivenessHandler;

import core.Coord;
import core.DTNHost;
import core.DTNSim;
import core.ModuleCommunicationBus;
import core.Settings;
import core.SimClock;
import core.SimError;

/**
 * <P>Superclass for all movement models. All subclasses must contain at least a
 * constructor with one {@link Settings} parameter and also a copy-constructor.
 * They must also implement the {@link #replicate()} method, which should return
 * an instance of the movement model class with same parameters as the object
 * (immutable fields can be shared, but mutable fields must be copied).</P>
 * <P>To make a new movement model do something useful, also at least
 * {@link #getInitialLocation()} and {@link #getPath()} are worthwhile to
 * override.</P>
 */
public abstract class MovementModel {
	/** node's speed CSV (min, max) -setting id ({@value})*/
	public static final String SPEED = "speed";
	/** node's wait time CSV (min, max) -setting id ({@value})*/
	public static final String WAIT_TIME = "waitTime";

	/** default setting for speed distribution */
	public static final double[] DEF_SPEEDS = {1,1};
	/** default setting for wait time distribution */
	public static final double[] DEF_WAIT_TIMES = {0,0};

	/** MovementModel namespace (where world size and rng seed settings
	 * are looked from ({@value})*/
	public static final String MOVEMENT_MODEL_NS = "MovementModel";
	/** world's size CSV (width, height) -setting id ({@value})*/
	public static final String WORLD_SIZE = "worldSize";
	/** movement models' rng seed -setting id ({@value})*/
	public static final String RNG_SEED = "rngSeed";

	/** common rng for all movement models in the simulation */
	protected static Random rng;

	/** DTNHost to which this movement model is attached */
	protected DTNHost host;

	private ActivenessHandler ah;

	protected double minSpeed;
	protected double maxSpeed;
	protected double minWaitTime;
	protected double maxWaitTime;

	private int maxX;
	private int maxY;

	protected ModuleCommunicationBus comBus;

	// static initialization of all movement models' random number generator
	static {
		DTNSim.registerForReset(MovementModel.class.getCanonicalName());
		reset();
	}

	/**
	 * Checks that the minimum setting is not bigger than the maximum and
	 * that both are positive
	 * @param name Name of the setting
	 * @param min The minimum setting
	 * @param max The maximum setting
	 */
	private static void checkMinAndMaxSetting(String name,
		double min, double max) {
		if (min < 0 || max < 0) {
			throw new SimError("MovementModel." + name + " (in Settings)" +
					" has a value less than zero ("+min+", "+max+")");
		}
		if (min > max) {
			throw new SimError("MovementModel." + name + " (in Settings)" +
					" min is bigger than max ("+min+", "+max+")");
		}
	}

	/**
	 * Empty constructor for testing purposes.
	 */
	public MovementModel() {
		super();
	}

	/**
	 * Creates a new MovementModel based on a Settings object's settings.
	 * @param settings The Settings object where the settings are read from
	 */
	public MovementModel(Settings settings) {
		double[] speeds;
		double[] times;

		ah = new ActivenessHandler(settings);

		if (settings.contains(SPEED)) {
			speeds = settings.getCsvDoubles(SPEED, 2);
		}
		else {
			speeds = DEF_SPEEDS;
		}

		minSpeed = speeds[0];
		maxSpeed = speeds[1];
		checkMinAndMaxSetting(SPEED,minSpeed,maxSpeed);

		if(settings.contains(WAIT_TIME)) {
			times = settings.getCsvDoubles(WAIT_TIME, 2);
		}
		else {
			times = DEF_WAIT_TIMES;
		}

		minWaitTime = times[0];
		maxWaitTime = times[1];
		checkMinAndMaxSetting(WAIT_TIME,minWaitTime,maxWaitTime);

		settings.setNameSpace(MOVEMENT_MODEL_NS);
		int [] worldSize = settings.getCsvInts(WORLD_SIZE,2);
		this.maxX = worldSize[0];
		this.maxY = worldSize[1];

		settings.restoreNameSpace();
	}

	/**
	 * Copyconstructor. Creates a new MovementModel based on the given
	 * prototype.
	 * @param mm The MovementModel prototype to base the new object to
	 */
	public MovementModel(MovementModel mm) {
		this.maxSpeed = mm.maxSpeed;
		this.minSpeed = mm.minSpeed;
		this.maxWaitTime = mm.maxWaitTime;
		this.minWaitTime = mm.minWaitTime;
		this.maxX = mm.maxX;
		this.maxY = mm.maxY;
		this.ah = mm.ah;
		this.comBus = null;
	}

	/**
	 * Returns the largest X coordinate value this model uses
	 * @return Maximum of X coordinate values
	 */
	public int getMaxX() {
		return this.maxX;
	}

	/**
	 * Returns the largest Y coordinate value this model uses
	 * @return Maximum of Y coordinate values
	 */
	public int getMaxY() {
		return this.maxY;
	}


	/**
	 * Generates and returns a speed value between min and max of the
	 * {@link #WAIT_TIME} setting.
	 * @return A new speed between min and max values
	 */
	protected double generateSpeed() {
		if (rng == null) {
			return 1;
		}
		return (maxSpeed - minSpeed) * rng.nextDouble() + minSpeed;
	}

	/**
	 * Generates and returns a suitable waiting time at the end of a path.
	 * (i.e. random variable whose value is between min and max of the
	 * {@link #WAIT_TIME} setting).
	 * @return The time as a double
	 */
	protected double generateWaitTime() {
		if (rng == null) {
			return 0;
		}
		return (maxWaitTime - minWaitTime) * rng.nextDouble() +
			minWaitTime;
	}

	/**
	 * Returns a new path by this movement model or null if no new path could
	 * be constructed at the moment (node should wait where it is). A new
	 * path should not be requested before the destination of the previous
	 * path has been reached.
	 * @return A new path or null
	 */
	public abstract Path getPath();

	/**
	 * Returns a new initial placement for a node
	 * @return The initial coordinates for a node
	 */
	public abstract Coord getInitialLocation();

	/**
	 * @return the host
	 */
	public DTNHost getHost() {
		return host;
	}

	/**
	 * @param host the host to set
	 */
	public void setHost(DTNHost host) {
		this.host = host;
	}

	/**
	 * Returns true if this node is active at the moment (false if not)
	 * @return true if this node is active (false if not)
	 */
	public boolean isActive() {
		/* TODO: add offset support */
		return ah.isActive();
	}

	/**
	 * Returns a sim time when the next path is available. This implementation
	 * returns a random time in future that is {@link #WAIT_TIME} from now.
	 * @return The sim time when node should ask the next time for a path
	 */
	public double nextPathAvailable() {
		return SimClock.getTime() + generateWaitTime();
	}

	/**
	 * Sets the module communication bus for this movement model
	 * @param comBus The communications bus to set
	 */
	public void setComBus(ModuleCommunicationBus comBus) {
		this.comBus = comBus;
	}

	/**
	 * Returns the module communication bus of this movement model (if any)
	 * @return The communications bus or null if one is not set
	 */
	public ModuleCommunicationBus getComBus() {
		return this.comBus;
	}

	/**
	 * Returns simply the name of the movement model class
	 * @return the name of the movement model class
	 */
	public String toString() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Creates a replicate of the movement model.
	 * @return A new movement model with the same settings as this model
	 */
	public abstract MovementModel replicate();

	/**
	 * Resets all static fields to default values
	 */
	public static void reset() {
		Settings s = new Settings(MOVEMENT_MODEL_NS);
		if (s.contains(RNG_SEED)) {
			int seed = s.getInt(RNG_SEED);
			rng = new Random(seed);
		}
		else {
			rng = new Random(0);
		}
	}

}
