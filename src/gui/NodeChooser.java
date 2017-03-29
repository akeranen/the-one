/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.Timer;

import gui.nodefilter.*;
import gui.playfield.NodeGraphic;
import core.DTNHost;
import core.Settings;

/**
 * Node chooser panel
 */
public class NodeChooser extends JPanel implements ActionListener {
	private DTNSimGUI gui;
	/** the maximum number of allNodes to show in the list per page */
	public static final int MAX_NODE_COUNT = 500;
	private Timer refreshTimer;
	/** how often auto refresh is performed */
	private static final int AUTO_REFRESH_DELAY = 100;

	/** Default message node filters -setting id ({@value}). Comma separate
	 * list of message IDs from which the default filter set is created. */
	public static final String NODE_MESSAGE_FILTERS_S = "nodeMessageFilters";

	private static final String HOST_KEY = "host";
	private List<DTNHost> allNodes;
	private List<DTNHost> shownNodes;

	private JComboBox groupChooser;
	private JPanel nodesPanel;
	private JPanel chooserPanel;
	private Vector<NodeFilter> filters;


	public NodeChooser(List<DTNHost> nodes,	DTNSimGUI gui) {
		Settings s = new Settings(MainWindow.GUI_NS);
		// create a replicate to not interfere with original's ordering
		this.allNodes = new ArrayList<DTNHost>(nodes);
		this.shownNodes = allNodes;
		this.gui = gui;
		this.filters = new Vector<NodeFilter>();

		if (s.contains(NODE_MESSAGE_FILTERS_S)) {
			String[] filterIds = s.getCsvSetting(NODE_MESSAGE_FILTERS_S);
			for (String id : filterIds) {
				this.filters.add(new NodeMessageFilter(id));
				this.refreshTimer = new Timer(AUTO_REFRESH_DELAY, this);
				this.refreshTimer.start();
			}
		}

		Collections.sort(this.allNodes);

		init();
	}

	/**
	 * Adds a new node filter to the node chooser
	 * @param f The filter to add
	 */
	public void addFilter(NodeFilter f) {
		this.filters.add(f);
		updateShownNodes();
		if (this.refreshTimer == null) {
			this.refreshTimer = new Timer(AUTO_REFRESH_DELAY, this);
			this.refreshTimer.start();
		}
	}

	/**
	 * Clears all node filters
	 */
	public void clearFilters() {
		this.filters = new Vector<NodeFilter>();
		this.shownNodes = allNodes;
		if (this.refreshTimer != null) {
			this.refreshTimer.stop();
		}
		this.refreshTimer = null;

		NodeGraphic.setHighlightedNodes(null);
		updateList();
	}

	private void updateList() {
		setNodes(0);
		if (this.groupChooser != null) {
			this.groupChooser.setSelectedIndex(0);
		}
	}


	private void updateShownNodes() {
		List<DTNHost> oldShownNodes = shownNodes;
		List<DTNHost>nodes = new Vector<DTNHost>();

		for (DTNHost node : allNodes) {
			for (NodeFilter f : this.filters) {
				if (f.filterNode(node)) {
					nodes.add(node);
					break;
				}
			}
		}

		if (nodes.size() == oldShownNodes.size() &&
			oldShownNodes.containsAll(nodes)) {
			return; /* nothing to update */
		} else {
			this.shownNodes = nodes;
			updateList();
			NodeGraphic.setHighlightedNodes(nodes);
		}

	}

	/**
	 * Initializes the node chooser panels
	 */
	private void init() {
		nodesPanel = new JPanel();
		chooserPanel = new JPanel();

		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;

		nodesPanel.setLayout(new BoxLayout(nodesPanel,BoxLayout.Y_AXIS));
		nodesPanel.setBorder(BorderFactory.createTitledBorder(getBorder(),
				"Nodes"));

		if (shownNodes.size() > MAX_NODE_COUNT) {
			String[] groupNames = new String[(shownNodes.size()-1)
			                                 / MAX_NODE_COUNT+1];
			int last = 0;
			for (int i=0, n=shownNodes.size();
				i <= (n-1) / MAX_NODE_COUNT; i++) {
				int next = MAX_NODE_COUNT * (i+1) - 1;
				if (next > n) {
					next = n-1;
				}
				groupNames[i] = (last + "..." + next);
				last = next + 1;
			}
			groupChooser = new JComboBox(groupNames);
			groupChooser.addActionListener(this);
			chooserPanel.add(groupChooser);
		}

		setNodes(0);
		c.gridy = 0;
		this.add(chooserPanel, c);
		c.gridy = 1;
		this.add(nodesPanel, c);
	}

	/**
	 * Sets the right set of allNodes to display
	 * @param offset Index of the first node to show
	 */
	private void setNodes(int offset) {
		nodesPanel.removeAll();

		for (int i=offset; i< shownNodes.size() &&
			i < offset + MAX_NODE_COUNT; i++) {
			DTNHost h = shownNodes.get(i);
			JButton jb = new JButton(h.toString());
			jb.putClientProperty(HOST_KEY, h);
			jb.addActionListener(this);
			nodesPanel.add(jb);
		}

		revalidate();
		repaint();
	}

	/**
	 * Action listener method for buttons and node set chooser
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			JButton source = (JButton)e.getSource();
			DTNHost host = (DTNHost)source.getClientProperty(HOST_KEY);
			gui.setFocus(host);
		}
		else if (e.getSource() == this.groupChooser) {
			setNodes(groupChooser.getSelectedIndex() * MAX_NODE_COUNT);
		}
		else if (e.getSource() == this.refreshTimer) {
			updateShownNodes();
		}
	}

}
