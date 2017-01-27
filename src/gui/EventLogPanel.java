/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SimClock;

/**
 * Event log panel where log entries are displayed.
 */
public class EventLogPanel extends JPanel
	implements ConnectionListener, MessageListener, ActionListener {

	/** Event log panel settings namespace ({@value}) */
	public static final String EL_PANEL_NS = "GUI.EventLogPanel";

	/** Number of events -setting id ({@value}). Defines the number of
	 * events to show in the panel. */
	public static final String NROF_EVENTS_S = "nrofEvents";

	/** Regular expression filter -setting id ({@value}). Defines the regular
	 * expression against which the event texts are matched; only matching
	 * events are not shown */
	public static final String EVENTS_RE_S = "REfilter";

	private static final String PANEL_TITLE = "Event log";
	/** format of a single log entry */
	private static final String ENTRY_FORMAT = "% 9.1f: %s ";
	private static final int FONT_SIZE = 12;
	private static final String FONT_TYPE = "monospaced";
	private static final Color LOG_BUTTON_BG = Color.WHITE;
	private static final String HOST_DELIM = "<->";
	private static final Color HIGHLIGHT_BG_COLOR = Color.GREEN;

	// constants used for button property
	private static final String HOST_PROP = "host";
	private static final String MSG_PROP = "message";

	/** How often the log is updated (milliseconds) */
	public static final int LOG_UP_INTERVAL = 500;

	/** Regular expression to filter log entries (changed trough Settings) */
	private String regExp = null;
	public static final int DEFAULT_MAX_NROF_EVENTS = 30;
	/** how many events to show in log (changed trough Settings) */
	private int maxNrofEvents;

	private Font font;	// font used in log entries
	private DTNSimGUI gui;
	private Vector<JPanel> eventPanes;
	private GridLayout layout;

	private EventLogControlPanel controls;
	private EventLogControl conUpCheck;
	private EventLogControl conDownCheck;
	private EventLogControl msgCreateCheck;
	private EventLogControl msgTransferStartCheck;
	private EventLogControl msgRelayCheck;
	private EventLogControl msgRemoveCheck;
	private EventLogControl msgDeliveredCheck;
	private EventLogControl msgDropCheck;
	private EventLogControl msgAbortCheck;

	/**
	 * Creates a new log panel
	 * @param gui The where this log belongs to (for callbacks)
	 */
	public EventLogPanel(DTNSimGUI gui) {
		this.gui = gui;
		String title = PANEL_TITLE;
		Settings s = new Settings(EL_PANEL_NS);

		this.maxNrofEvents = s.getInt(NROF_EVENTS_S,
				DEFAULT_MAX_NROF_EVENTS);
		this.regExp = s.getSetting(EVENTS_RE_S, null);

		layout = new GridLayout(maxNrofEvents,1);

		this.setLayout(layout);
		if (this.regExp != null) {
			title += " - RE-filter: " + regExp;
		}
		this.setBorder(BorderFactory.createTitledBorder(
				getBorder(), title));

		this.eventPanes = new Vector<JPanel>(maxNrofEvents);
		this.font = new Font(FONT_TYPE,Font.PLAIN, FONT_SIZE);
		this.controls = createControls();

		// set log view to update every LOG_UP_INTERVAL milliseconds
		// also ensures that the update is done in Swing's EDT
		ActionListener taskPerformer = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
		          updateLogView();
		      }
		  };
		  Timer t = new Timer(LOG_UP_INTERVAL, taskPerformer);
		  t.start();
	}

	/**
	 * Creates a control panel for the log
	 * @return The created EventLogControls
	 */
	private EventLogControlPanel createControls() {
		EventLogControlPanel c = new EventLogControlPanel();
		c.addHeading("connections");
		conUpCheck = c.addControl("up");
		conDownCheck = c.addControl("down");
		c.addHeading("messages");
		msgCreateCheck = c.addControl("created");
		msgTransferStartCheck = c.addControl("started relay");
		msgRelayCheck = c.addControl("relayed");
		msgDeliveredCheck = c.addControl("delivered");
		msgRemoveCheck = c.addControl("removed");
		msgDropCheck = c.addControl("dropped");
		msgAbortCheck = c.addControl("aborted");
		return c;
	}

	/**
	 * Returns the control panel that this log uses
	 * @return The control panel
	 */
	public EventLogControlPanel getControls() {
		return this.controls;
	}

	/**
	 * Adds a new event to the event log panel
	 * @param description Textual description of the event
	 * @param host1 Host that caused the event or null if there was not any
	 * @param host2 Another host that was involved in the event (or null)
	 * @param message Message that was involved in the event (or null)
	 * @param highlight If true, the log entry is highlighted
	 */
	private void addEvent(String description, DTNHost host1,
			DTNHost host2, Message message, boolean highlight) {
		JPanel eventPane = new JPanel();
		eventPane.setLayout(new BoxLayout(eventPane,BoxLayout.LINE_AXIS));

		String text = String.format(ENTRY_FORMAT,
				SimClock.getTime(),description);
		JLabel label = new JLabel(text);
		label.setFont(font);
		eventPane.add(label);

		if (host1 != null) {
			addInfoButton(eventPane,host1,HOST_PROP);
		}
		if (host2 != null) {
			JLabel betweenLabel = new JLabel(HOST_DELIM);
			betweenLabel.setFont(font);
			eventPane.add(betweenLabel);
			addInfoButton(eventPane,host2,HOST_PROP);
		}
		if (message != null) {
			addInfoButton(eventPane, message, MSG_PROP);
		}

		if (highlight) {
			eventPane.setBackground(HIGHLIGHT_BG_COLOR);
		}

		eventPanes.add(eventPane);

		// if the log is full, remove oldest entries first
		if (this.eventPanes.size() > maxNrofEvents) {
			eventPanes.remove(0);
		}
	}

	/**
	 * Updates the log view
	 */
	private void updateLogView() {
		//TODO Optimization: Check if update is really necessary
		this.removeAll();
		for (int i=0; i< this.eventPanes.size(); i++) {
			this.add(eventPanes.get(i));
		}
		revalidate();
	}


	/**
	 * Adds a new button to a log entry panel and attaches a client
	 * property into it
	 * @param panel Panel where to add the button
	 * @param o Client property object to add
	 * @param clientProp Client property key to use for the object
	 */
	private void addInfoButton(JPanel panel, Object o, String clientProp) {
		JButton hButton;
		hButton = new JButton(o.toString());
		hButton.putClientProperty(clientProp, o);
		hButton.addActionListener(this);
		hButton.setFont(font);
		hButton.setMargin(new Insets(0,0,0,0));
		hButton.setBackground(LOG_BUTTON_BG);
		panel.add(hButton);
	}

	/**
	 * Processes a log event
	 * @param check EventLogControls used to check if this entry type should
	 * be shown and/or paused upon
	 * @param name Text description of the event
	 * @param host1 First host involved in the event (if any, can be null)
	 * @param host2 Second host involved in the event (if any, can be null)
	 * @param message The message involved in the event (if any, can be null)
	 */
	private void processEvent(EventLogControl check, final String name,
			final DTNHost host1, final DTNHost host2, final Message message) {
		String descString;	// String format description of the event

		if (!check.showEvent()) {
			return; // if event's "show" is not checked, won't pause either
		}

		descString = name + " " +
			(host1!=null ? host1 : "") +
			(host2!= null ? (HOST_DELIM + host2) : "") +
			(message!=null ? " " + message : "");

		if (regExp != null && !descString.matches(regExp)){
			return;	// description doesn't match defined regular expression
		}

		if (check.pauseOnEvent()) {
			gui.setPaused(true);
			if (host1 != null) {
				gui.setFocus(host1);
			}
		}

	addEvent(name, host1, host2, message, check.pauseOnEvent());
	}

	// Implementations of ConnectionListener and MessageListener interfaces
	public void hostsConnected(DTNHost host1, DTNHost host2) {
		processEvent(conUpCheck, "Connection UP", host1, host2, null);
	}

	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		processEvent(conDownCheck, "Connection DOWN", host1, host2, null);
	}

	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if (!dropped) {
			processEvent(msgRemoveCheck, "Message removed", where, null, m);
		}
		else {
			processEvent(msgDropCheck, "Message dropped", where, null, m);
		}
	}

	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean firstDelivery) {
		if (firstDelivery) {
			processEvent(msgDeliveredCheck, "Message delivered", from, to, m);
		}
		else if (to == m.getTo()) {
			processEvent(msgDeliveredCheck, "Message delivered again",
					from, to, m);
		}
		else {
			processEvent(msgRelayCheck, "Message relayed", from, to, m);
		}
	}

	public void newMessage(Message m) {
		processEvent(msgCreateCheck, "Message created", m.getFrom(), null, m);
	}

	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		processEvent(msgAbortCheck, "Message relay aborted", from, to, m);
	}

	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		processEvent(msgTransferStartCheck,"Message relay started", from,
				to,m);

	}

	// end of message interface implementations


	/**
	 * Action listener for log entry (host & message) buttons
	 */
	public void actionPerformed(ActionEvent e) {
		JButton source = (JButton)e.getSource();

		if (source.getClientProperty(HOST_PROP) != null) {
			// button was a host button -> focus it on GUI
			gui.setFocus((DTNHost)source.getClientProperty(HOST_PROP));
		}
		else if (source.getClientProperty(MSG_PROP) != null) {
			// was a message button -> show information about the message
			Message m = (Message)source.getClientProperty(MSG_PROP);
			gui.getInfoPanel().showInfo(m);
		}
	}

	public String toString() {
		return this.getClass().getSimpleName() + " with " +
			this.eventPanes.size() + " events";
	}

}
