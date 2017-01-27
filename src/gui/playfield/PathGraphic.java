/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package gui.playfield;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import movement.Path;
import core.Coord;

/**
 * Visualization of a Path
 *
 */
public class PathGraphic extends PlayFieldGraphic {
	private final static Color PATH_COLOR = Color.RED;
	private List<Coord> coords;

	public PathGraphic(Path path) {
		if (path == null) {
			this.coords = null;
		}
		else {
			this.coords = path.getCoords();
			assert this.coords != null && this.coords.size() > 0 :
			"No coordinates in the path (" + path + ")";
		}
	}

	/**
	 * Draws a line trough all path's coordinates.
	 * @param g2 The graphics context to draw to
	 */
	@Override
	public void draw(Graphics2D g2) {
		if (coords == null) {
			return;
		}

		g2.setColor(PATH_COLOR);
		Coord prev = coords.get(0);

		for (int i=1, n=coords.size(); i < n; i++) {
			Coord next = coords.get(i);
			g2.drawLine(scale(prev.getX()), scale(prev.getY()),
					scale(next.getX()), scale(next.getY()));
			prev = next;
		}
	}

}
