/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package ui;

import core.SimClock;

/**
 * Simple text-based user interface.
 */
public class DTNSimTextUI extends DTNSimUI {
	/** runtime object, used to gather information on memory usage **/
    private Runtime runtime = Runtime.getRuntime();
	// as the runtime returns memory information in bytes,
	// we need to divide by 1024*1024 = 1048576 to receive megabytes
	/** constant for the conversion from bytes to megabytes **/
    private static final int BYTES_PER_MEGABYTE = 1_048_576;
	private long lastUpdateRt;	// real time of last ui update
	private long startTime; // simulation start time
	/** How often the UI view is updated (milliseconds) */
	public static final long UI_UP_INTERVAL = 60000;

	@Override
	protected void runSim() {
		double simTime = SimClock.getTime();
		double endTime = scen.getEndTime();

		print("Running simulation '" + scen.getName()+"'");
		//description of output format/meaning
		print("output description:");
        print("elapsed_real_seconds simulated_seconds current_simulation_rate " +
              "used_memory free_memory allocated_memory maximum_allocatable_memory");
		startTime = System.currentTimeMillis();
		lastUpdateRt = startTime;

		while (simTime < endTime && !simCancelled){
			try {
				world.update();
			} catch (AssertionError e) {
				e.printStackTrace();
				done();
				return;
			}
			simTime = SimClock.getTime();
			this.update(false);
		}

		double duration = (System.currentTimeMillis() - startTime)/1000.0;

		simDone = true;
		done();
		this.update(true); // force final UI update

		print("Simulation done in " + String.format("%.2f", duration) + "s");

	}

	/**
	 * Updates user interface if the long enough (real)time (update interval)
	 * has passed from the previous update.
	 * @param forced If true, the update is done even if the next update
	 * interval hasn't been reached.
	 */
	private void update(boolean forced) {
		long now = System.currentTimeMillis();
		long diff = now - this.lastUpdateRt;
		double dur = (now - startTime)/1000.0;
		if (forced || (diff > UI_UP_INTERVAL)) {
			// simulated seconds/second calc
			double ssps = ((SimClock.getTime() - lastUpdate)*1000) / diff;
			// print out debug data in a format that is usable in a csv file
			// columns: elapsed real seconds, simulated seconds, current simulation rate,
			// used memory, free memory, allocated memory, maximum allocatable memory
			print(dur + " " + SimClock.getTime() + " " + ssps + " "
					+ (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MEGABYTE + " "
					+ runtime.freeMemory() / BYTES_PER_MEGABYTE + " "
					+ runtime.totalMemory() / BYTES_PER_MEGABYTE + " "
					+ runtime.maxMemory() / BYTES_PER_MEGABYTE);

			this.lastUpdateRt = System.currentTimeMillis();
			this.lastUpdate = SimClock.getTime();
		}
	}

	private void print(String txt) {
		System.out.println(txt);
	}

}
