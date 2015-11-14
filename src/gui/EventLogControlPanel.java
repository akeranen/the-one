/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Control panel for event log
 *
 */
public class EventLogControlPanel extends JPanel implements ActionListener {
	private static final String TITLE_TEXT = "Event log controls";
	private static final String SHOW_TEXT = "show";
	private static final String PAUSE_TEXT = "pause";
	private static final int PADDING = 5;
	private Font smallFont = new Font("sans",Font.PLAIN,11);
	private Font headingFont = new Font("sans",Font.BOLD,11);
	private Vector<EventLogControl> logControls;

	private JCheckBox showAllCheck;
	private JCheckBox pauseAllCheck;

	private GridBagLayout layout;
	private GridBagConstraints c;

	/**
	 * Constructor. Creates a new control panel.
	 */
	public EventLogControlPanel() {
		layout = new GridBagLayout();
		c = new GridBagConstraints();
		logControls = new Vector<EventLogControl>();

		c.ipadx = PADDING;

		setLayout(layout);
		this.setBorder(BorderFactory.createTitledBorder(
				getBorder(), TITLE_TEXT));

		c.fill = GridBagConstraints.BOTH;
		addLabel(" ");
		addLabel(SHOW_TEXT + "");
		c.gridwidth = GridBagConstraints.REMAINDER; //end row
		addLabel(PAUSE_TEXT);

		// create "show/pause on all selections
		c.gridwidth = 1;
		addLabel("all");
		showAllCheck = addCheckBox(true,false);
		pauseAllCheck = addCheckBox(false,true);
		showAllCheck.addActionListener(this);
		pauseAllCheck.addActionListener(this);

		this.setMinimumSize(new Dimension(0,0));
	}

	/**
	 * Adds a new filter&pause control
	 * @param name Name of the control
	 * @param showOn Is "show" initially selected
	 * @param pauseOn Is "pause" initially selected
	 * @return Event log control object that can be queried for status
	 */
	public EventLogControl addControl(String name, boolean showOn,
			boolean pauseOn) {
		JCheckBox filterCheck;
		JCheckBox pauseCheck;
		EventLogControl control;

		c.gridwidth = 1; // one component/cell
		addLabel(name);
		filterCheck = addCheckBox(showOn, false);
		pauseCheck = addCheckBox(pauseOn, true);

		control = new EventLogControl(filterCheck, pauseCheck);
		this.logControls.add(control);
		return control;
	}

	/**
	 * Creates and adds a new checkbox to this panel
	 * @param selected Is the checkbox initially selected
	 * @param endOfRow Is the box last in the row in the layout
	 * @return The created checkbox
	 */
	private JCheckBox addCheckBox(boolean selected, boolean endOfRow) {
		JCheckBox box = new JCheckBox();
		box.setSelected(selected);

		if (endOfRow) {
			c.gridwidth = GridBagConstraints.REMAINDER; // use rest of the line
		}
		else {
			c.gridwidth = 1; // default
		}

		layout.setConstraints(box, c);
		add(box);

		return box;
	}

	/**
	 * Adds a new filter&pause control with initially "show" checked
	 * but "pause" unchecked
	 * @param name Name of the control
	 * @return Event log control object that can be queried for status
	 * @see #addControl(String name, boolean showOn, boolean pauseOn)
	 */
	public EventLogControl addControl(String name) {
		return addControl(name, true, false);
	}

	/**
	 * Adds a new heading in the control panel. Subsequent addControl
	 * controls will be under this heading
	 * @param name The heading text
	 */
	public void addHeading(String name) {
		c.gridwidth = GridBagConstraints.REMAINDER;
		addLabel(name).setFont(this.headingFont);
	}

	private JLabel addLabel(String txt) {
		JLabel label = new JLabel(txt);
		label.setFont(this.smallFont);
		layout.setConstraints(label, c);
		add(label);
		return label;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.showAllCheck) {
			for (EventLogControl elc : logControls) {
				elc.setShowEvent(this.showAllCheck.isSelected());
			}
		}
		else if (e.getSource() == this.pauseAllCheck) {
			for (EventLogControl elc : logControls) {
				elc.setPauseOnEvent(this.pauseAllCheck.isSelected());
			}
		}


	}
}
