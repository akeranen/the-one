/*
 * Copyright 2016 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import core.Connection;
import core.ConnectionListener;
import core.DTNHost;
import core.Settings;
import core.SettingsError;
import core.SimError;

/**
 * Link connectivity report connector that sends simulated connection events
 * to the specified remote server. The server needs to listen for TCP 
 * connections on the defined address and port. Report connector sends a 
 * line based stream of time stamped ONE connectivity events, with 
 * connection speed as last/extra value, formatted with {@link #format(double)}. 
 */
public class ConnectivityReportConnector extends Report
	implements ConnectionListener {
	
	protected Socket socket;
	protected PrintWriter socketWriter;
	protected boolean writeToFile;
	
	/** Remote server address -setting id ({@value}).
	 * Defines the IP address or hostname of the server where results are sent. 
	 **/
	public static final String SERVER_ADDRESS_S = "address";
	
	/** Server port -setting id ({@value}).
	 * Defines the TCP port number of the server where results are sent. 
	 **/
	public static final String SERVER_PORT_S = "port";
	
	/** Write output also to file -setting id ({@value}).
	 * If set to false, results are not written to local report file
	 * (default = true).
	 */
	public static final String FILE_OUTPUT_S = "fileoutput";
	
	/**
	 * Constructor.
	 */
	public ConnectivityReportConnector() {
		init();
		Settings s = getSettings();
		String serverAddress = s.getSetting(SERVER_ADDRESS_S);
		int serverPort = s.getInt(SERVER_PORT_S);
		writeToFile = s.getBoolean(FILE_OUTPUT_S, true);
		
		try {
			socket = new Socket(serverAddress, serverPort);
			socketWriter = new PrintWriter(socket.getOutputStream(), true);
		} catch (UnknownHostException e) {
			throw new SettingsError("Unknown host" + serverAddress);
		} catch (IOException e) {
			throw new SimError("Can't connect to " + serverAddress + " port " +
					serverPort + ". Error: " + e);
		}
	}

	protected void write(String str) {
		/* TODO: detect if connection is lost */
		socketWriter.println(str);
		
		if (writeToFile) {
			super.write(str);
		}
	}
	
	public void hostsConnected(DTNHost h1, DTNHost h2) {
		Connection con = null;
		
		/* look for the corresponding connection object 
		 * TODO: extend interface to include the connection object? */
		for (Connection c : h1.getConnections()) {
			if (c.getOtherNode(h1).equals(h2)) {
				con = c;
				break;
			}
		}
		
		write(createTimeStamp() + " CONN " + connectionString(h1, h2) + 
				" up " + format(con.getSpeed()));
	}

	public void hostsDisconnected(DTNHost h1, DTNHost h2) {
		String conString = connectionString(h1, h2);
		write(createTimeStamp() + " CONN " + conString + " down");
	}

	/**
	 * Creates and returns a "@" prefixed time stamp of the current simulation
	 * time
	 * @return time stamp of the current simulation time
	 */
	private String createTimeStamp() {
		return String.format("%.2f", getSimTime());
	}

	/**
	 * Creates and returns a String presentation of the connection where the
	 * node with the lower network address is first
	 * @param h1 The other node of the connection
	 * @param h2 The other node of the connection
	 * @return String presentation of the connection
	 */
	private String connectionString(DTNHost h1, DTNHost h2) {
		if (h1.getAddress() < h2.getAddress()) {
		    return h1.getAddress() + " " + h2.getAddress();
		}
		else {
		    return h2.getAddress() + " " + h1.getAddress();
		}
	}

}
