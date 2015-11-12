/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

package input;

import fi.tkk.netlab.dtn.DTNConsoleConnection;
import fi.tkk.netlab.dtn.ecla.Bundle;
import fi.tkk.netlab.dtn.ecla.CLAInterface;
import fi.tkk.netlab.dtn.ecla.CLAParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import core.DTN2Manager;
import core.Debug;
import core.Settings;
import core.SimClock;

import static core.Constants.DEBUG;

/**
 * Delivers bundles from dtnd to ONE. Must be configured as an
 * external events generator in the configuration file.
 * @author teemuk
 */
public class DTN2Events implements EventQueue {

	private Queue<ExternalEvent>	events;

	/**
	  For keeping track of bundles we've seen in the past
	  Due to the routing implementation in dtnd it's likely
	  that dtnd will immediately return a bundle that is
	  forwarded to it from the ONE. */
	private Map<String, Bundle>			bundle_list;

	/**
	 * Creates a new events object.
	 * @param s Settings
	 */
	public DTN2Events(Settings s) {
		this.events = new LinkedList<ExternalEvent>();
		this.bundle_list = new HashMap<String, Bundle>();
		DTN2Manager.setEvents(this);
	}


	//************************************************************************//
	//                                    ParserHandler                                    //
	//************************************************************************//

	/**
	 * Inner class that implements the CLA interface for receiving bundles from
	 * dtnd.
	 * @author teemuk
	 */
	public class ParserHandler implements CLAInterface {
		private int						host_id;
		private DTN2Events				events;
		private String					c_host;
		private int						c_port;
		private DTNConsoleConnection	console;

		/**
		 * Creates a new parser handler.
		 * @param	hostID		ID of the host that this parser corresponds to
		 * @param	eventsHandler	Reference to the events handler
		 * @param	consoleHost		Hostname of the dtnd
		 * @param	consolePort		Console port of the dtnd
		 */
		public ParserHandler(int hostID, DTN2Events eventsHandler,
				String consoleHost, int consolePort) {
			this.host_id = hostID;
			this.events = eventsHandler;
			this.c_host = consoleHost;
			this.c_port = consolePort;
		}

		//********************************************************************//
		//                     CLAInterface Implementation                    //
		//********************************************************************//
		public BundleTransferReceipt incomingBundle(String location,
				CLAParser.BundleAttributes attributes) {
			FileInputStream		f_in;

			CLAInterface.BundleTransferReceipt r =
				new CLAInterface.BundleTransferReceipt();

			// Open the bundle file
			try {
				f_in = new FileInputStream(new File(location));
			} catch (FileNotFoundException ex) {
				if (DEBUG) Debug.p("CLAInterfaceImpl: Couldn't open file " +
						location + " (file not found)");
				return r;
			}

			// Make a copy of the bundle
			String filepath = "bundles/"+Math.round(Math.random()*1000000000)+
				".bundle";
			File new_f = new File(filepath);
			try {
				while (!new_f.createNewFile()) {
					filepath = "bundles/"+Math.round(Math.random()*1000000000)+
						".bundle";
					new_f = new File(filepath);
				}
				FileOutputStream f_out = new FileOutputStream(new_f);
				int i;
				while ( (i=f_in.read()) != -1 ) {
					f_out.write(i);
				}
				f_in.close();
				f_out.close();
			} catch (Exception e) {
				// TBD
			}

			// Parse the bundle
			Bundle bundle = new Bundle(new_f);

			// Check that we haven't forwarded this bundle before
			if (isReg(bundle)) {
				r.reply = false;
				r.bytes_sent = 0;
				return r;
			} else {
				regMsg(bundle);
			}

			// Lookup the receiving host
			Collection<DTN2Manager.EIDHost> c =
				DTN2Manager.getHosts(bundle.destination_EID);
			if (c==null || c.isEmpty()) {
				if (DEBUG) Debug.p( "Couldn't find destination matching '" +
						bundle.destination_EID + "'");
				r.reply = false;
				r.bytes_sent = 0;
				return r;
			}

			// Create a message for each matched recipient
			// XXX: Ideally we'd only have one message,
			//      but ONE requires each message to have exactly one recipient
			for (DTN2Manager.EIDHost e : c) {
				// Create a new message in the queue
				this.events.enqueMsg(this.host_id, e.host_id, bundle);
			}

			// Pretend we've transmitted the whole bundle
			r.reply = true;
			r.bytes_sent = bundle.file.length();

			return r;
		}

		public void connected() {
			/* The ECLA has been connected, we can now set it up through
			   the console */
			this.console = new DTNConsoleConnection(this.c_host, this.c_port);
			Thread t = new Thread(this.console);
			t.start();
			this.console.queue("link add one dtn:one ALWAYSON extcl " +
					"protocol=ONE\n");
			this.console.queue("route add \"dtn://*\" one\n");
		}

		public boolean error(String reason, Exception exception,
				boolean fatal) {
			return false;
		}

		public boolean parseError(String reason) {
			return false;
		}
		//********************************************************************//
	}
	//************************************************************************//



	//************************************************************************//
	//                              EventQueue Implementation                              //
	//************************************************************************//
	public ExternalEvent nextEvent() {
		if (!this.events.isEmpty()) {
			return this.events.remove();
		} else
			return new ExternalEvent(Double.MAX_VALUE);
	}

	public double nextEventsTime() {
		if (!this.events.isEmpty())
			return SimClock.getTime();
		else
			return Double.MAX_VALUE;
	}
	//************************************************************************//


	//************************************************************************//
	//                                   Public Methods                                    //
	//************************************************************************//

	/**
	 * Creates a parser handler for the given host.
	 * @param	hostID			ID of the host that this parser corresponds to
	 * @param	consoleHost		Hostname of the dtnd
	 * @param	consolePort		Console port of the dtnd
	 */
	public DTN2Events.ParserHandler getParserHandler(int hostID,
			String consoleHost, int consolePort) {
		return new ParserHandler(hostID, this, consoleHost, consolePort);
	}
	//************************************************************************//


	//************************************************************************//
	//                                   Private Methods                                   //
	//************************************************************************//
	private void enqueMsg(int from, int to, Bundle bundle) {
		String id;
		id = "bundle."+from+"-"+to+"-"+bundle.creation_timestamp_time+
			"-"+bundle.creation_timestamp_seq_no;
		MessageCreateEvent e = new MessageCreateEvent(from, to, id,
				(int)(bundle.file.length()), 0, SimClock.getTime());
		synchronized (this.events) {
			this.events.add(e);
		}
		DTN2Manager.addBundle(id,bundle);
	}

	// Keep track of the bundles we've received
	private void regMsg(Bundle bundle) {
		String key = bundle.source_EID+":"+bundle.destination_EID+":"+
			bundle.creation_timestamp_time+":"+bundle.creation_timestamp_seq_no;
		if (!this.bundle_list.containsKey(key))
			this.bundle_list.put(key,null);
	}

	// Check if the bundle has been received before
	private boolean isReg(Bundle bundle) {
		String key = bundle.source_EID+":"+bundle.destination_EID+":"+
			bundle.creation_timestamp_time+":"+bundle.creation_timestamp_seq_no;
		return this.bundle_list.containsKey(key);
	}
	//************************************************************************//

}
