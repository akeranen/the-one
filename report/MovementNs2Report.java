/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import core.Coord;
import core.DTNHost;
import core.MovementListener;
import core.Settings;

/**
 * Movement report that generates suitable movement data for ns-2 simulator
 * as described in <A HREF="http://www.isi.edu/nsnam/ns/doc/node174.html">
 * http://www.isi.edu/nsnam/ns/doc/node174.html</A>.
 * This report ignores the warm up settings.
 */
public class MovementNs2Report extends Report implements MovementListener {
	/** node array's name -setting id ({@value})*/
	public static final String NODE_ARR_S = "nodeArray";
	/** ns command -setting id ({@value}) */
	public static final String NS_CMD_S = "nsCmd";
	/** default value for the array name ({@value})*/
	public static final String DEF_NODE_ARRAY = "$node_";
	/** default value for the ns command ({@value})*/
	public static final String DEF_NS_CMD = "$ns_";

	/** a value "close enough" to zero ({@value}). Used for fixing zero values*/
	public static final double EPSILON = 0.00001;
	/** formatting string for coordinate values ({@value})*/
	public static final String COORD_FORMAT = "%.5f";

	private String nodeArray;
	private String nsCmd;

	/**
	 * Constructor. Reads {@link #NODE_ARR_S} and {@link #NS_CMD_S} settings
	 * and uses those values as the name of the node array and ns command.
	 * If the values aren't present, default values of
	 * <CODE>{@value DEF_NODE_ARRAY}</CODE> and
	 * <CODE>{@value DEF_NS_CMD}</CODE> are used.
	 */
	public MovementNs2Report() {
		Settings settings = getSettings();

		if (settings.contains(NODE_ARR_S)) {
			nodeArray = settings.getSetting(NODE_ARR_S);
		}
		else {
			nodeArray = DEF_NODE_ARRAY;
		}
		if (settings.contains(NS_CMD_S)) {
			nsCmd = settings.getSetting(NS_CMD_S);
		}
		else {
			nsCmd = DEF_NS_CMD;
		}

		init();
	}

	public void initialLocation(DTNHost host, Coord location) {
		int index = host.getAddress();
		write(nodeArray + "("+ index + ") set X_ " + fix(location.getX()));
		write(nodeArray + "("+ index + ") set Y_ " + fix(location.getY()));
		write(nodeArray + "("+ index + ") set Z_ 0");
	}

	public void newDestination(DTNHost host, Coord dst, double speed) {
		int index = host.getAddress();
		double time = getSimTime();

		write(nsCmd + " at " + time + " \"\\" + nodeArray +	"(" + index + ")" +
				" setdest " + fix(dst.getX()) + " " + fix(dst.getY()) +
				" " + speed + "\"");
	}

	/**
	 * Fixes and formats coordinate values suitable for Ns2 module.
	 * I.e. converts zero-values to {@value EPSILON} and formats values
	 * with {@link #COORD_FORMAT}.
	 * @param val The value to fix
	 * @return The fixed value
	 */
	private String fix(double val) {
		val = val == 0 ? EPSILON : val;
		return String.format(COORD_FORMAT, val);
	}
}
