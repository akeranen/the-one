/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */

package core;

import input.DTN2Events;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import report.DTN2Reporter;
import fi.tkk.netlab.dtn.ecla.Bundle;
import fi.tkk.netlab.dtn.ecla.CLAParser;

/** 
 * Manages the external convergence layer connections to dtnd.
 * Parses the configuration file and sets up the CLAParsers
 * and EID->host mappings.
 * @author teemuk
 */
public class DTN2Manager {
	private static Map<DTNHost, CLAParser>	CLAs = null;
	/** Mapping from EID to DTNHost */
	private static Collection<EIDHost>		EID_to_host = null;	
	/** Set of all bundles in the simulator */
	private static Map<String, Bundle>		bundles = null;
	/** Reporter object that passes messages from ONE to dtnd */
	private static DTN2Reporter				reporter = null;
	/** Events object that passes messages from dtnd to ONE */
	private static DTN2Events				events = null;
	
	/**
	 * EID to DTNHost mapping elements.
	 */
	public static class EIDHost {
		public String	EID;
		public int		host_id;
		public DTNHost	host;
		public EIDHost(String eid, int host_id, DTNHost host) {
			this.EID = eid;
			this.host = host;
			this.host_id = host_id;
		}
	}
	
	/**
	 * Sets up the dtnd connections by parsing the configuration file
	 * defined in the <code>DTN2.configFile</code> setting.
	 * @param world		reference to the world that contains the nodes
	 */
	public static void setup(World world) {
		FileInputStream		f_in;
		InputStreamReader	isr;
		BufferedReader		in;
		File				f;
		String				s;
		String[]			attrs;
		int					nodeID, dtnd_port, console_port;
		String				nodeEID, dtnd_host;
			
		DTN2Manager.CLAs = new HashMap<DTNHost, CLAParser>();
		DTN2Manager.EID_to_host = new LinkedList<EIDHost>();
		DTN2Manager.bundles = new HashMap<String, Bundle>();
		
		// Check if DTN2Reporter and DTN2Events have been loaded.
		// If not, we do nothing here.
		if (DTN2Manager.reporter==null || DTN2Manager.events==null)
			return;
		
		// Get input stream from the settings file.
		Settings conf = new Settings("DTN2");
		String fname;
		try {
			fname = conf.getSetting("configFile");
		} catch (SettingsError se) {
			return;
		}
		f = new File(fname);
		if (!f.exists()) return;
		try {
			f_in = new FileInputStream(f);
			isr = new InputStreamReader(f_in);
			in = new BufferedReader(isr);
		} catch (Exception e) {
			Debug.p("Could not load requested DTN2 configuration file '"
					+fname+"'");
			return;
		}
		
		// Create a directory to hold copies of the bundles
		f = new File("bundles");
		if (!f.exists())
			f.mkdir();
		
		// Parse config file
		try {
				s = in.readLine();
			} catch (Exception e) {
				return;
			}
		while (s!=null) {
			attrs = s.split(" ");
			if (attrs.length==5 && !s.startsWith("#")) {
				nodeID = Integer.parseInt(attrs[0]);
				nodeEID = attrs[1];
				dtnd_host = attrs[2];
				dtnd_port = Integer.parseInt(attrs[3]);
				console_port = Integer.parseInt(attrs[4]);
				
				// Find the host
				DTNHost h = world.getNodeByAddress(nodeID);

				// Add to the EID -> Host mapping
				DTN2Manager.EIDHost e = new DTN2Manager.EIDHost(nodeEID, 
						nodeID, h);
				DTN2Manager.EID_to_host.add(e);
				
				// Configure and start the CLA
				CLAParser p;
				p = new CLAParser(dtnd_host, dtnd_port, "ONE");
				DTN2Events.ParserHandler ph = 
					DTN2Manager.events.getParserHandler(nodeID, dtnd_host,
							console_port); 
				p.setListener(ph);
				Thread t = new Thread(p);
				t.start();
				// Save reference to the CLA
				DTN2Manager.CLAs.put(h,p);
			}
			try {
				s = in.readLine();
			} catch (Exception e) {
				return;
			}
		}
	}
	
	
	/**
	 * Sets the <code>DTN2Reporter</code> object used to pass messages from ONE
	 * to dtnd.
	 * This should be used by the dynamically loaded DTN2Reporter object to
	 * allow other objects get reference to it.
	 * @param reporter	the reporter object to save reference to
	 */
	public static void setReporter(DTN2Reporter reporter) {
		DTN2Manager.reporter = reporter;
	}
	
	/**
	 * Returns reference to the <code>DTN2Reporter</code> object.
	 * @return reference to the active DTN2Reporter object
	 */ 
	public static DTN2Reporter getReporter() {
		return DTN2Manager.reporter;
	}
	
	/**
	 * Sets the DTN2Events object.
	 * @param events	the active events object to use
	 */
	public static void setEvents(DTN2Events events) {
		DTN2Manager.events = events;
	}
	
	/** 
	 * Returns the DTN2Events object.
	 * @return the currently active events object.
	 */
	public static DTN2Events getEvents() {
		return DTN2Manager.events;
	}
	
	/**
	 * Returns the ECL parser associated with the host.
	 * @param host	the host who's parser to return
	 * @return the host's parser.
	 */
	public static CLAParser getParser(DTNHost host) {
		return DTN2Manager.CLAs.get(host);
	}
	
	/** 
	 * Returns a <code>Collection</code> of <code>DTNHost</code>
	 * objects corresponding to the given EID.
	 * @param EID	EID of the host
	 * @return		the host corresponding to the EID
	 */
	public static Collection<EIDHost> getHosts(String EID) {
		Collection<EIDHost> c = new LinkedList<EIDHost>();
		for (EIDHost e : DTN2Manager.EID_to_host) {
			if (EID.matches(e.EID)) c.add(e);
		}
		return c;
	}
	
	/**
	 * Stores a reference to a bundle corresponding to the given message.
	 * @param	id		the id of the message
	 * @param	bundle	the bundle associated with the message
	 */
	public static void addBundle(String id, Bundle bundle) {
		DTN2Manager.bundles.put(id, bundle);
	}
	
	/** 
	 * Returns the bundle associated with the given message id.
	 * @param	id	the message id
	 * @return	the bundle associated with the message
	 */
	public static Bundle getBundle(String id) {
		return DTN2Manager.bundles.remove(id);
	}
}
