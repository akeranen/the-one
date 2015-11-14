/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import java.util.Collection;
import java.util.HashMap;

import core.ConnectionListener;
import core.DTNHost;

/**
 * Generates Graphviz compatible graph from connections.
 * Connections that happen during the warm up period are ignored.
 */
public class AdjacencyGraphvizReport extends Report implements ConnectionListener {
	/** Name of the graphviz report ({@value})*/
	public static final String GRAPH_NAME = "adjgraph";

	private String HOST_DELIM = "<->"; // used in toString()
	private HashMap<String, ConnectionInfo> cons;
	private Collection<DTNHost> allHosts;

	/**
	 * Constructor.
	 */
	public AdjacencyGraphvizReport() {
		this.allHosts = null;
		init();
	}

	protected void init() {
		super.init();
		this.cons = new HashMap<String, ConnectionInfo>();
	}


	public void hostsConnected(DTNHost host1, DTNHost host2) {
		if (isWarmup()) {
			return;
		}

		newEvent();
		ConnectionInfo ci = cons.get(host1+HOST_DELIM+host2);

		if (ci == null) {
			cons.put(host1+HOST_DELIM+host2, new ConnectionInfo(host1,host2));
		}
		else {
			ci.nrofConnections++;
		}
	}

	// 	Nothing to do here..
	public void hostsDisconnected(DTNHost host1, DTNHost host2) {}

	/**
	 * Sets all hosts that should be in the graph at least once
	 * @param hosts Collection of hosts
	 */
	public void setAllHosts(Collection<DTNHost> hosts) {
		this.allHosts = hosts;
	}

	public void done() {
		write("graph " + GRAPH_NAME + " {");
		setPrefix("\t"); // indent following lines by one tab

		for (ConnectionInfo ci : cons.values()) {
			int weight = ci.nrofConnections;
			write(ci.h1 + "--" + ci.h2 + " [weight=" + weight + "];");
		}

		// mention all hosts in the graph at least once
		if (this.allHosts != null) {
			for (DTNHost h : allHosts) {
				write(h+ ";");
			}
		}

		setPrefix(""); // don't indent anymore
		write("}");

		super.done();
	}

	/**
	 * Private class stores information of the connected hosts
	 * and nrof times they have connected.
	 */
	private class ConnectionInfo {
		private DTNHost h1;
		private DTNHost h2;
		private int nrofConnections;

		public ConnectionInfo(DTNHost h1, DTNHost h2) {
			this.h1 = h1;
			this.h2 = h2;
			this.nrofConnections = 1;
		}

		public boolean equals(Object o) {
			if (o == null) return false;
			return o.toString().equals(this.toString());
		}

		public int hashCode() {
			return toString().hashCode();
		}

		public String toString() {
			return h1+HOST_DELIM+h2;
		}

		public int compareTo(Object o) {
			return nrofConnections - ((ConnectionInfo)o).nrofConnections;
		}
	}

}
