/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import movement.map.MapNode;
import movement.map.SimMap;
import core.Coord;

/**
 * "Well-known text syntax" map data reader.<BR>
 * <STRONG>Note</STRONG>: Understands only <CODE>LINESTRING</CODE>s and
 * <CODE>MULTILINESTRING</CODE>s. Skips all <CODE>POINT</CODE> data.
 * Other data causes IOException.
 */
public class WKTMapReader extends WKTReader {
	private Hashtable<Coord, MapNode> nodes;
	/** are all paths bidirectional */
	private boolean bidirectionalPaths = true;
	private int nodeType = -1;

	/**
	 * Constructor. Creates a new WKT reader ready for addPaths() calls.
	 * @param bidi If true, all read paths are set bidirectional (i.e. if node A
	 * is a neighbor of node B, node B is also a neighbor of node A).
	 */
	public WKTMapReader(boolean bidi) {
		this.bidirectionalPaths = bidi;
		this.nodes = new Hashtable<Coord, MapNode>();
	}

	/**
	 * Sets bidirectional paths on/off.
	 * @param bidi If true, all paths are set bidirectional (false -> not)
	 */
	public void setBidirectional(boolean bidi) {
		this.bidirectionalPaths = bidi;
	}

	/**
	 * Returns the map nodes that were read in a collection
	 * @return the map nodes that were read in a collection
	 */
	public Collection<MapNode> getNodes() {
		return this.nodes.values();
	}

	/**
	 * Returns the original Map object that was used to read the map
	 * @return the original Map object that was used to read the map
	 */
	public Map<Coord, MapNode> getNodesHash() {
		return this.nodes;
	}

	/**
	 * Returns new a SimMap that is based on the read map
	 * @return new a SimMap that is based on the read map
	 */
	public SimMap getMap() {
		return new SimMap(this.nodes);
	}

	/**
	 * Adds paths to the map and adds given type to all nodes' type.
	 * @param file The file where the WKT data is read from
	 * @param type The type to use (integer value, see class {@link MapNode}))
	 * @throws IOException If something went wrong while reading the file
	 */
	public void addPaths(File file, int type) throws IOException {
		addPaths(new FileReader(file), type);
	}


	/**
	 * Add paths to current path set. Adding paths multiple times
	 * has the same result as concatenating the data before adding it.
	 * @param input Reader where the WKT data is read from
	 * @param nodeType The type to use (integer value, see class
	 * {@link MapNode}))
	 * @throws IOException if something went wrong with reading from the input
	 */
	public void addPaths(Reader input, int nodeType) throws IOException {
		this.nodeType = nodeType;
		String type;
		String contents;

		init(input);

		while((type = nextType()) != null) {
			if (type.equals(LINESTRING)) {
				contents = readNestedContents();
				updateMap(parseLineString(contents));
			}
			else if (type.equals(MULTILINESTRING)) {
				for (List<Coord> list : parseMultilinestring()) {
					updateMap(list);
				}
			}
			else {
				// known type but not interesting -> skip
				readNestedContents();
			}
		}
	}

	/**
	 * Updates simulation map with coordinates in the list
	 * @param coords The list of coordinates
	 */
	private void updateMap(List<Coord> coords) {
		MapNode previousNode = null;
		for (Coord c : coords) {
			previousNode = createOrUpdateNode(c, previousNode);
		}
	}

	/**
	 * Creates or updates a node that is in location c and next to
	 * node previous
	 * @param c The location coordinates of the node
	 * @param previous Previous node whose neighbor node at c is
	 * @return The created/updated node
	 */
	private MapNode createOrUpdateNode(Coord c, MapNode previous) {
		MapNode n = null;

		n = nodes.get(c);	// try to get the node at that location

		if (n == null) { 	// no node in that location -> create new
			n = new MapNode(c);
			nodes.put(c, n);
		}

		if (previous != null) {
			n.addNeighbor(previous);
			if (bidirectionalPaths) {
				previous.addNeighbor(n);
			}
		}

		if (nodeType != -1) {
			n.addType(nodeType);
		}

		return n;
	}

}
