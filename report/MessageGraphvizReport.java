/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import java.util.List;
import java.util.Vector;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Creates a graphviz compatible graph of messages that were passed.
 * Messages created during the warm up period are ignored.
 */
public class MessageGraphvizReport extends Report implements MessageListener {
	/** Name of the graphviz report ({@value})*/
	public static final String GRAPH_NAME = "msggraph";
	private Vector<Message> deliveredMessages;

	/**
	 * Constructor.
	 */
	public MessageGraphvizReport() {
		init();
	}

	protected void init() {
		super.init();
		this.deliveredMessages = new Vector<Message>();
	}

	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
		}
	}

	public void messageTransferred(Message m, DTNHost from,
			DTNHost to,	boolean firstDelivery) {
		if (firstDelivery && !isWarmupID(m.getId())) {
			newEvent();
			this.deliveredMessages.add(m);
		}
	}

	/* nothing to implement for these */
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {	}
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}

	@Override
	public void done() {
		write("/* scenario " + getScenarioName() + "\n" +
				deliveredMessages.size() + " messages delivered at " +
				"sim time " + getSimTime() + " */") ;
		write("digraph " + GRAPH_NAME + " {");
		setPrefix("\t"); // indent following lines by one tab

		for (Message m : deliveredMessages) {
			List<DTNHost> path = m.getHops();
			String pathString = path.remove(0).toString(); // start node

			for (DTNHost next : path) {
				pathString += "->" + next.toString();
			}

			write (pathString + ";");
		}

		setPrefix(""); // don't indent anymore
		write("}");

		super.done();
	}

}
