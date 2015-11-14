/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package gui.playfield;

import java.awt.Graphics2D;

/**
 * Superclass for all graphics to be drawn on the "play field".
 */
public abstract class PlayFieldGraphic {
	/** Common scaling factor for all playfield graphics.
	 * @see #setScale(double)
	 */
	protected static double scale = 1;

	/**
	 * Set the zooming factor of the graphics to be drawn
	 * @param newScale New scale
	 */
	public static void setScale(double newScale) {
		scale = newScale;
	}

	/**
	 * Returns the currently used scaling factor
	 * @return The scaling factor
	 */
	public static double getScale() {
		return scale;
	}

	/**
	 * Draws the graphic component to the graphics context g2
	 * @param g2 The context to draw the graphics to
	 */
	public abstract void draw(Graphics2D g2);


	/**
	 * Scales the value according to current zoom level
	 * @param value Value to scale
	 * @return Scaled value bit-truncated (casted) to an integer
	 */
	public static int scale(double value) {
		return (int)Math.round(scale * value);
	}

	/**
	 * Scales the value according to current zoom level
	 * @param value Value to scale
	 * @return Scaled value bit-truncated (casted) to an integer
	 */
	public static int scale(int value) {
		return (int)Math.round(scale * value);
	}

	/**
	 * Performs an inverse of the scaling procedure with current scale.
	 * NOTE: invScale(scale(value)) != value because of rounding to integer
	 * at scale() -methods
	 * @param value The value to inverse-scale
	 * @return Inverse-scaled value
	 * @see #scale(double)
	 */
	public static double invScale(double value) {
		return value/scale;
	}


}
