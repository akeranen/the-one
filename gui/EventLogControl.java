/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package gui;

import javax.swing.JCheckBox;

/**
 * Class capsulates the references to the controls one can add to
 * the EventLogControlPanel
 *
 */
public class EventLogControl {
	private JCheckBox show;
	private JCheckBox pause;
	
	/**
	 * Constructor.
	 * @param show The checkbox that controls showing this type of event
	 * @param pause The checkbox that controls pausing on this type of event
	 */
	public EventLogControl(JCheckBox show, JCheckBox pause) {
		this.show = show;
		this.pause = pause;
	}
	
	/** 
	 * Returns true if this event type should be shown
	 * @return true if this event type should be shown
	 */
	public boolean showEvent() {
		return this.show.isSelected();
	}
	
	/** 
	 * Returns true if this event type should cause pause
	 * @return true if this event type should cause pause
	 */

	public boolean pauseOnEvent() {
		return this.pause.isSelected();
	}
	
	/**
	 * Sets ought this event type should be shown (return true for 
	 * {@link #showEvent()} )
	 * @param show If true, events are set to be shown
	 */
	public void setShowEvent(boolean show) {
		this.show.setSelected(show);
	}

	/**
	 * Sets ought this event type cause pause (return true for 
	 * {@link #pauseOnEvent()} )
	 * @param pause If true, events cause pause
	 */
	public void setPauseOnEvent(boolean pause) {
		this.pause.setSelected(pause);
	}


}
