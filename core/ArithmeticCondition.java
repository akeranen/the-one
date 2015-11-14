/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

/**
 * This class presents a simple arithmetic condition: is value smaller than,
 * bigger than, or equal to another value. The condition is given in text
 * form, e.g., "< 42", and then different values can be matched against that
 * condition.
 * @author Ari
 */
public class ArithmeticCondition {

	private static final String VALID_OPERATORS = "><=";
	private char operator;
	private double number;

	/**
	 * Creates a new condition based on the given string.
	 * @param cond The condition string. Must consist of one operator
	 * ("<", ">", or "=") and one double-precision floating point number.
	 * @throws SettingsError if the given string is not a valid condition
	 */
	public ArithmeticCondition(String cond) {
		String value;
		int multiplier = 1;

		if (cond.length() < 2) {
			throw new SettingsError("Invalid condition \"" + cond + "\"");
		}

		operator = cond.charAt(0);
		value = cond.substring(1);

		/* handle kilo and Mega suffixes for the value */
		if (value.endsWith("k")) {
			multiplier = 1000;
		} else if (value.endsWith("M")) {
			multiplier = 1000000;
		}
		if (multiplier > 1) { /* remove suffix */
			value = value.substring(0, value.length() - 1);
		}

		if (VALID_OPERATORS.indexOf(operator) == -1) {
			throw new SettingsError("Invalid operator in condition \"" + cond +
					"\" valid operators: " + VALID_OPERATORS);
		}

		try {
			number = Double.parseDouble(value);
		} catch (NumberFormatException e) {
			throw new SettingsError("Invalid numeric value in condition \"" +
					cond + "\"");
		}

		number *= multiplier;

	}

	/**
	 * Returns true if the given value satisfies "V X N" where V is the given
	 * value, X is the operator (from the settings), and N is the numeric value
	 * given after the operator in the settings.
	 * @param value The value to check
	 * @return true if the condition holds for the given value, false otherwise
	 */
	public boolean isTrueFor(double value) {
		switch (operator) {
		case '<': return value < this.number;
		case '>': return value > this.number;
		case '=': return value == this.number; // XXX: == for doubles...
		default: throw new SettingsError("Invalid operator");
		}
	}

	@Override
	public String toString() {
		return "Condition \"" + operator + " " + number + "\"";
	}
}
