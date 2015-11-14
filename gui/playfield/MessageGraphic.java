/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package gui.playfield;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;

import core.DTNHost;

/**
 * Visualization of a message
 *
 */
public class MessageGraphic extends PlayFieldGraphic {
	private Color msgColor = Color.RED;

	private DTNHost from;
	private DTNHost to;

	public MessageGraphic(DTNHost from, DTNHost to) {
		this.to = to;
		this.from = from;
	}

	@Override
	public void draw(Graphics2D g2) {
		g2.setColor(msgColor);

		int fromX = scale(from.getLocation().getX());
		int fromY = scale(from.getLocation().getY());
		int toX = scale(to.getLocation().getX());
		int toY = scale(to.getLocation().getY());

		// line from "from host" to "to host"
		Polygon p = new Polygon(new int[] {fromX, toX},
				new int[] {fromY,toY}, 2);

		g2.draw(p);
	}
}
