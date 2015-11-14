/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing.schedule;

import java.io.Serializable;

public class ScheduleEntry implements Serializable {
	private static final long serialVersionUID = 42L;

	private double time;
	private int from;
	private int to;
	private int via;
	private double delta;
	private double duration;
	private int usageCount;

	/**
	 * Constructor of new schedule entry
	 * @param time When the journey from "from" starts
	 * @param from The source
	 * @param via The node that takes us there (or -1 if n/a)
	 * @param to The destination
	 * @param duration Time it takes from the source to destination
	 */
	public ScheduleEntry(double time, int from, int via, int to,
			double duration) {
		this.time = time;
		this.from = from;
		this.via = via;
		this.to = to;
		this.duration = duration;
		this.delta = 0;
		this.usageCount = 0;
	}

	/**
	 * Returns time + delta
	 * @return the time
	 */
	public double getTime() {
		return time + delta;
	}

	/**
	 * @return the destination
	 */
	public int getTo() {
		return to;
	}

	/**
	 * @return the source
	 */
	public int getFrom() {
		return from;
	}

	/**
	 * @return the via
	 */
	public int getVia() {
		return via;
	}

	/**
	 * Return the time it takes to get from source to destination
	 * @return the duration
	 */
	public double getDuration() {
		return duration;
	}

	public double getDestinationTime() {
		return this.getTime() + this.getDuration();
	}

	/**
	 * @return the delta
	 */
	public double getDelta() {
		return delta;
	}

	/**
	 * @param delta the delta to set
	 */
	public void setDelta(double delta) {
		this.delta = delta;
	}

	/**
	 * @return the usageCount
	 */
	public int getUsageCount() {
		return usageCount;
	}

	public void increaseUsageCount() {
		this.usageCount++;
	}

	@Override
	public String toString() {
		return time + "(+" + delta + "): " + from + "->"
				+ (via > 0 ? via + "->" : "") + to + " (" + duration + ")";
	}

}
