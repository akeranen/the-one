/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package gui.playfield;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Reference scale bar graphic. This is the small reference scale
 * on the upper left corner of the playfield.
 */
public class ScaleReferenceGraphic extends PlayFieldGraphic {
	/** minimum length of the reference bar (pixels) */
	private final int MIN_LENGTH = 30;

	/** x position of the left end of the bar (pixels) */
	private final int X_POS = 20;
	/** y position of the left end of the bar (pixels) */
	private final int Y_POS = 20;
	/** height of the bar (pixels) */
	private final int SIZE = 8;
	/** size of the font */
	private final int FONT_SIZE = 10;
	/** color of the bar */
	private final Color REF_COLOR = Color.BLACK;

	@Override
	public void draw(Graphics2D g2) {
		int meterLen = 1;
		String scaleUnit = "m";
		double pixelLen = meterLen * scale;
		int endX;
		int h = SIZE/2;

		while (pixelLen < MIN_LENGTH) {
			meterLen *= 10;
			pixelLen = meterLen * scale;
		}
		if (meterLen >= 1000) {
			scaleUnit = "km";
			meterLen /= 1000;
		}
		endX = X_POS + (int)pixelLen;

		g2.setFont(new Font(null, Font.PLAIN, FONT_SIZE));

		g2.setColor(REF_COLOR);
		g2.drawLine(X_POS, Y_POS-h, X_POS, Y_POS+h); // left end
		g2.drawLine(X_POS, Y_POS, endX, Y_POS); // horizontal line
		g2.drawLine(endX, Y_POS-h, endX, Y_POS+h);

		g2.drawString(meterLen + scaleUnit, X_POS + 10, Y_POS - 1);
	}

}
