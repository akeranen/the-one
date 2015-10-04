/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement.map;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import core.Coord;

/**
 * A simulation map for node movement.
 */
public class SimMap implements Serializable {
	private Coord minBound;
	private Coord maxBound;
	/** list representation of the map for efficient list-returning */
	private ArrayList<MapNode> nodes;
	/** hash map presentation of the map for efficient finding node by coord */
	private Map<Coord, MapNode> nodesMap;
	/** offset of map translations */
	private Coord offset;
	/** is this map data mirrored after reading */
	private boolean isMirrored;
	
	/** is re-hash needed before using hash mode (some coordinates changed) */
	private boolean needsRehash = false;
	
	public SimMap(Map<Coord, MapNode> nodes) {
		this.offset = new Coord(0,0);
		this.nodes = new ArrayList<MapNode>(nodes.values());
		this.nodesMap = nodes;
		this.isMirrored = false;
		setBounds();
	}
	
	/**
	 * Returns all the map nodes in a list
	 * @return all the map nodes in a list
	 */
	public List<MapNode> getNodes() {
		return this.nodes;
	}
	
	/**
	 * Returns a MapNode at given coordinates or null if there's no MapNode
	 * in the location of the coordinate
	 * @param c The coordinate
	 * @return The map node in that location or null if it doesn't exist
	 */
	public MapNode getNodeByCoord(Coord c) {
		if (needsRehash) { // some coordinates have changed after creating hash
			nodesMap.clear();
			for (MapNode node : getNodes()) {
				nodesMap.put(node.getLocation(), node); // re-hash
			}
		}
	
		return nodesMap.get(c);
	}
	
	/**
	 * Returns the upper left corner coordinate of the map
	 * @return the upper left corner coordinate of the map
	 */
	public Coord getMinBound() {
		return this.minBound;
	}

	/**
	 * Returns the lower right corner coordinate of the map
	 * @return the lower right corner coordinate of the map
	 */
	public Coord getMaxBound() {
		return this.maxBound;
	}

	/**
	 * Returns the offset that has been caused by translates made to 
	 * this map (does NOT take into account mirroring).
	 * @return The current offset
	 */
	public Coord getOffset() {
		return this.offset;
	}
	
	/**
	 * Returns true if this map has been mirrored after reading
	 * @return true if this map has been mirrored after reading
	 * @see #mirror()
	 */
	public boolean isMirrored() {
		return this.isMirrored;
	}
	
	/**
	 * Translate whole map by dx and dy
	 * @param dx The amount to translate X coordinates
	 * @param dy the amount to translate Y coordinates
	 */
	public void translate(double dx, double dy) {
		for (MapNode n : nodes) {
			n.getLocation().translate(dx, dy);
		}
		
		minBound.translate(dx, dy);
		maxBound.translate(dx, dy);
		offset.translate(dx, dy);
		
		needsRehash = true;
	}
	
	/**
	 * Mirrors all map coordinates around X axis (x'=x, y'=-y). 
	 */
	public void mirror() {
		assert !isMirrored : "Map data already mirrored";
	
		Coord c;
		for (MapNode n : nodes) {
			c=n.getLocation();
			c.setLocation(c.getX(), -c.getY());
		}
		setBounds();
		this.isMirrored = true;
		needsRehash = true;
	}
	
	/**
	 * Updates the min & max bounds to conform to the values of the map nodes.
	 */
	private void setBounds() {
		double minX, minY, maxX, maxY;
		Coord c;
		minX = minY = Double.MAX_VALUE;
		maxX = maxY = -Double.MAX_VALUE;
		
		for (MapNode n : nodes) {
			c = n.getLocation();
			if (c.getX() < minX) {
				minX = c.getX();
			}
			if (c.getX() > maxX) {
				maxX = c.getX();
			}
			if (c.getY() < minY) {
				minY = c.getY();
			}
			if (c.getY() > maxY) {
				maxY = c.getY();
			}
		}
		minBound = new Coord(minX, minY);
		maxBound = new Coord(maxX, maxY);
	}
	
	/**
	 * Returns a String representation of the map
	 * @return a String representation of the map
	 */
	public String toString() {
		return this.nodes.toString();
	}
}