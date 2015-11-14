/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package util;

/**
 * Range of values
 * @author Ari
 */
public class Range {

	private double min;
	private double max;

	/**
	 * Creates a new range
	 * @param min The minimum bound of the range
	 * @param max The maximum bound
	 */
	public Range(double min, double max) {
		this.min = min;
		this.max = max;
		checkRangeValidity(min, max);
	}

	/**
	 * Parses a range from the given string
	 * @param str The string to parse
	 * @throws NumberFormatException if the range didn't contain numeric values
	 */
	public Range(String str) throws NumberFormatException {
		if (str.indexOf("-") != str.lastIndexOf("-")) {
			/* TODO */
			throw new Error("Ranges with negative values not supported");
		}

		if (str.substring(1).contains("-")) {
			/* has "-" but not as the first char -> range */
			String[] vals = str.split("-");
			this.min = Double.parseDouble(vals[0]);
			this.max = Double.parseDouble(vals[1]);
		} else {
			this.min = this.max = Double.parseDouble(str);
		}
		checkRangeValidity(min, max);
	}

	/**
	 * Checks if the given values for a valid range
	 * @param min The minimum value
	 * @param max The maximum value
	 * @throws Error if min > max
	 */
	private void checkRangeValidity(double min, double max) {
		if (min > max) {
			throw new Error("Minimum value is larger than maximum");
		}
	}

	/**
	 * Returns true if the given value is within this range [min, max],
	 * min and max included
	 * @param value The value to check
	 * @return True if the given value is in the range, false if not
	 */
	public boolean isInRange(double value) {
		return (value >= min && value <= max);
	}

	@Override
	public String toString() {
		return "Range [" + min + ", " + max + "]";
	}

}
