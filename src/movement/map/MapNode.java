/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement.map;

import java.util.List;
import java.util.Vector;

import core.Coord;
import core.SettingsError;

/**
 * A node in a SimMap. Node has a location, 0-n neighbors that it is
 * connected to and possibly a type identifier.
 */
public class MapNode implements Comparable<MapNode> {
	/** Smallest valid type of a node: {@value}*/
	public static final int MIN_TYPE = 1;
	/** Biggest valid type of a node: {@value} */
	public static final int MAX_TYPE = 31;


	private Coord location;
	private Vector<MapNode> neighbors;
	// bit mask of map node's types or 0 if no type's are defined
	private int type;

	/**
	 * Constructor. Creates a map node to a location.
	 * @param location The location of the node.
	 */
	public MapNode(Coord location) {
		this.location = location;
		this.neighbors = new Vector<MapNode>();
		type = 0;
	}

	/**
	 * Adds a type indicator to this node
	 * @param type An integer from range [{@value MIN_TYPE}, {@value MAX_TYPE}]
	 */
	public void addType(int type) {
		this.type |= typeToBitMask(type);
	}

	/**
	 * Returns true if this node is of given type, false if none of node's
	 * type(s) match to given type or node doesn't have type at all
	 * @param type The type (integer from range [{@value MIN_TYPE},
	 * {@value MAX_TYPE}])
	 * @return True if this node is of given type
	 */
	public boolean isType(int type) {
		if (this.type == 0) {
			return false;
		}

		return (this.type & typeToBitMask(type)) != 0;
	}

	/**
	 * Returns true if the node's types match any of the given types
	 * @param types The types to check (array of values in range
	 * [{@value MIN_TYPE}, {@value MAX_TYPE}])
	 * @return True if at least one of the types matched, false if none of the
	 * types matched
	 * @see #isType(int)
	 */
	public boolean isType(int[] types) {
		for (int type : types) {
			if (isType(type)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Converts type integer to a bit mask for setting & checking type
	 * @param type The type to convert
	 * @return A bit mask for the given type
	 * @throws SettingsError if the type is out of range
	 */
	private int typeToBitMask(int type) {
		assert type >= MIN_TYPE && type <= MAX_TYPE : "Invalid node type "+type;
		return 1 << type; // create the mask by bitwise shift
	}

	/**
	 * Adds the node as this node's neighbour (unless the node is null)
	 * @param node The node to add or null for no action
	 */
	public void addNeighbor(MapNode node) {
		if (node == null) {
			return;
		}

		addToList(node);		// add the node to list
	}

	/**
	 * Adds the node to list of neighbours unless it is already there or
	 * "neighbour" is this node
	 * @param node
	 */
	private void addToList(MapNode node) {
		if (!this.neighbors.contains(node) && node != this) {
			this.neighbors.add(node);
		}
	}

	/**
	 * Returns the location of the node
	 * @return the location of the node
	 */
	public Coord getLocation() {
		return location;
	}

	/**
	 * Returns the neighbors of this node.
	 * @return the neighbors in a list
	 */
	public List<MapNode> getNeighbors() {
		return neighbors;
	}

	/**
	 * Returns a String representation of the map node
	 * @return a String representation of the map node
	 */
	public String toString() {
		return "N" + (type != 0 ? "t"+type : "") + "@"+this.location.toString();
	}

	/**
	 * Compares two map nodes by their coordinates
	 * @param o The other MapNode
	 */
	public int compareTo(MapNode o) {
		return this.getLocation().compareTo((o).getLocation());
	}

}
