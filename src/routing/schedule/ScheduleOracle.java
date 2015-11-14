/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing.schedule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleOracle implements Serializable{
	private static final long serialVersionUID = 42L;

	Map<Integer, List<ScheduleEntry>> schedules;

	public ScheduleOracle() {
		this.schedules = new HashMap<Integer, List<ScheduleEntry>>();
	}

	/**
	 * Adds a new schedule entry to the oracle
	 * @param start Start time
	 * @param from Source of the connection
	 * @param via The node that goes from "from" to "via" (or -1 for n/a)
	 * @param to Destination of the connection
	 * @param duration How long it takes to get to destination
	 */
	public void addEntry(double start, int from, int via, int to,
			double duration) {
		List<ScheduleEntry> list = schedules.get(from);

		if (list == null) { /* first entry for the from */
			list = new ArrayList<ScheduleEntry>();
			schedules.put(from, list);
		}

		list.add(new ScheduleEntry(start, from, via, to, duration));
	}

	/**
	 * Adds a new schedule entry to the oracle
	 * @param start Start time
	 * @param from Source of the connection
	 * @param to Destination of the connection
	 * @param duration How long it takes to get to destination
	 */
	public void addEntry(double start, int from, int to,double duration) {
		addEntry(start, from, -1, to, duration);
	}

	/**
	 * Returns a list of schedule entries for nodes reachable after given time
	 * from the given node
	 * @param from The source node
	 * @param time Time to start
	 * @return List of reachable nodes
	 */
	public List<ScheduleEntry> getConnected(int from, double time) {
		List<ScheduleEntry> connected = new ArrayList<ScheduleEntry>();
		List<ScheduleEntry> all = schedules.get(from);

		if (all == null) {
			return connected;
		}

		for (ScheduleEntry s : all) {
			if (s.getTime() >= time) {
				connected.add(s);
			}
		}

		return connected;
	}

	/**
	 * Returns all schedule entries
	 * @return all schedule entries
	 */
	public List<ScheduleEntry> getEntries() {
		List<ScheduleEntry> entries = new ArrayList<ScheduleEntry>();
		for (List<ScheduleEntry> list : schedules.values()) {
			for (ScheduleEntry se : list) {
				entries.add(se);
			}
		}

		return entries;
	}
}
