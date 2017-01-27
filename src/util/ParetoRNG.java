/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package util;

import java.util.Random;

/**
 * A random number generator for a Pareto distribution
 * @author Frans Ekman
 */
public class ParetoRNG {
	private Random rng;
	private double xm; // min value (Xm)
	private double k; // coefficient
	private double maxValue;

	/**
	 * Creates a new Pareto random number generator that makes use of a normal
	 * random number generator
	 * @param rng
	 * @param k
	 * @param minValue
	 * @param maxValue
	 */
	public ParetoRNG(Random rng, double k, double minValue, double maxValue) {
		this.rng = rng;
		this.xm = minValue;
		this.k = k;
		if (maxValue == -1) {
			this.maxValue = Double.POSITIVE_INFINITY;
		} else {
			this.maxValue = maxValue;
		}
	}

	/**
	 * Returns a Pareto distributed double value
	 * @return a Pareto distributed double value
	 */
	public double getDouble() {
		if (xm == -1) {
			return Double.POSITIVE_INFINITY;
		}
		double x;
		do {
			x = xm * Math.pow((1 - rng.nextDouble()), (-1/k));
		} while (x > maxValue);
		return x;
	}

}
