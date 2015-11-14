/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package gui.playfield;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

import core.Connection;
import core.Coord;
import core.DTNHost;
import core.NetworkInterface;

/**
 * Visualization of a DTN Node
 *
 */
public class NodeGraphic extends PlayFieldGraphic {
	private static boolean drawCoverage;
	private static boolean drawNodeName;
	private static boolean drawConnections;
	private static boolean drawBuffer;
	private static List<DTNHost> highlightedNodes;

	private static Color rangeColor = Color.GREEN;
	private static Color conColor = Color.BLACK;
	private static Color hostColor = Color.BLUE;
	private static Color hostNameColor = Color.BLUE;
	private static Color msgColor1 = Color.BLUE;
	private static Color msgColor2 = Color.GREEN;
	private static Color msgColor3 = Color.RED;

	private static Color highlightedNodeColor = Color.MAGENTA;

	private DTNHost node;

	public NodeGraphic(DTNHost node) {
		this.node = node;
	}

	@Override
	public void draw(Graphics2D g2) {
		drawHost(g2);
		if (drawBuffer) {
			drawMessages(g2);
		}
	}

	/**
	 * @return true if the node this graphic represents should be highlighted
	 */
	private boolean isHighlighted() {
		if (highlightedNodes == null) {
			return false;
		} else {
			return highlightedNodes.contains(node);
		}
	}

	/**
	 * Visualize node's location, radio ranges and connections
	 * @param g2 The graphic context to draw to
	 */
	private void drawHost(Graphics2D g2) {
		Coord loc = node.getLocation();

		if (drawCoverage && node.isRadioActive()) {
			ArrayList<NetworkInterface> interfaces =
				new ArrayList<NetworkInterface>();
			interfaces.addAll(node.getInterfaces());
			for (NetworkInterface ni : interfaces) {
				double range = ni.getTransmitRange();
				Ellipse2D.Double coverage;

				coverage = new Ellipse2D.Double(scale(loc.getX()-range),
						scale(loc.getY()-range), scale(range * 2),
						scale(range * 2));

				// draw the "range" circle
				g2.setColor(rangeColor);
				g2.draw(coverage);
			}
		}

		if (drawConnections) {
			g2.setColor(conColor);
			Coord c1 = node.getLocation();
			ArrayList<Connection> conList = new ArrayList<Connection>();
			// create a copy to prevent concurrent modification exceptions
			conList.addAll(node.getConnections());
			for (Connection c : conList) {
				DTNHost otherNode = c.getOtherNode(node);
				Coord c2;

				if (otherNode == null) {
					continue; /* disconnected before drawn */
				}
				c2 = otherNode.getLocation();
				g2.drawLine(scale(c1.getX()), scale(c1.getY()),
						scale(c2.getX()), scale(c2.getY()));
			}
		}


		/* draw node rectangle */
		g2.setColor(hostColor);
		g2.drawRect(scale(loc.getX()-1),scale(loc.getY()-1),
		scale(2),scale(2));

		if (isHighlighted()) {
			g2.setColor(highlightedNodeColor);
			g2.fillRect(scale(loc.getX()) - 3 ,scale(loc.getY()) - 3, 6, 6);
		}

		if (drawNodeName) {
			g2.setColor(hostNameColor);
			// Draw node's address next to it
			g2.drawString(node.toString(), scale(loc.getX()),
					scale(loc.getY()));
		}
	}

	/**
	 * Sets whether radio coverage of nodes should be drawn
	 * @param draw If true, radio coverage is drawn
	 */
	public static void setDrawCoverage(boolean draw) {
		drawCoverage = draw;
	}

	/**
	 * Sets whether node's name should be displayed
	 * @param draw If true, node's name is displayed
	 */
	public static void setDrawNodeName(boolean draw) {
		drawNodeName = draw;
	}

	/**
	 * Sets whether node's connections to other nodes should be drawn
	 * @param draw If true, node's connections to other nodes is drawn
	 */
	public static void setDrawConnections(boolean draw) {
		drawConnections = draw;
	}

	/**
	 * Sets whether node's message buffer is shown
	 * @param draw If true, node's message buffer is drawn
	 */
	public static void setDrawBuffer(boolean draw) {
		drawBuffer = draw;
	}

	public static void setHighlightedNodes(List<DTNHost> nodes) {
		highlightedNodes = nodes;
	}

	/**
	 * Visualize the messages this node is carrying
	 * @param g2 The graphic context to draw to
	 */
	private void drawMessages(Graphics2D g2) {
		int nrofMessages = node.getNrofMessages();
		Coord loc = node.getLocation();

		drawBar(g2,loc, nrofMessages % 10, 1);
		drawBar(g2,loc, nrofMessages / 10, 2);
	}

	/**
	 * Draws a bar (stack of squares) next to a location
	 * @param g2 The graphic context to draw to
	 * @param loc The location where to draw
	 * @param nrof How many squares in the stack
	 * @param col Which column
	 */
	private void drawBar(Graphics2D g2, Coord loc, int nrof, int col) {
		final int BAR_HEIGHT = 5;
		final int BAR_WIDTH = 5;
		final int BAR_DISPLACEMENT = 2;

		// draws a stack of squares next loc
		for (int i=1; i <= nrof; i++) {
			if (i%2 == 0) { // use different color for every other msg
				g2.setColor(msgColor1);
			}
			else {
				if (col > 1) {
					g2.setColor(msgColor3);
				}
				else {
					g2.setColor(msgColor2);
				}
			}

			g2.fillRect(scale(loc.getX()-BAR_DISPLACEMENT-(BAR_WIDTH*col)),
					scale(loc.getY()- BAR_DISPLACEMENT- i* BAR_HEIGHT),
					scale(BAR_WIDTH), scale(BAR_HEIGHT));
		}

	}

}
