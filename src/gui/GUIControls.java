/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package gui;

import gui.playfield.PlayField;

import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import core.Coord;
import core.SimClock;

/**
 * GUI's control panel
 *
 */
public class GUIControls extends JPanel implements ActionListener, ChangeListener {
	private static final String PATH_GRAPHICS = "buttonGraphics/";
	private static final String ICON_PAUSE = "Pause16.gif";
	private static final String ICON_PLAY = "Play16.gif";
	private static final String ICON_ZOOM = "Zoom24.gif";
	private static final String ICON_STEP = "StepForward16.gif";
	private static final String ICON_FFW = "FastForward16.gif";

	private static final String TEXT_PAUSE = "pause simulation";
	private static final String TEXT_PLAY = "play simulation";
	private static final String TEXT_PLAY_UNTIL = "play simulation until sim time...";
	private static final String TEXT_STEP = "step forward one interval";
	private static final String TEXT_FFW = "enable/disable fast forward";
	private static final String TEXT_UP_CHOOSER = "GUI update:";
	private static final String TEXT_SCREEN_SHOT = "screen shot";
	private static final String TEXT_SIMTIME = "Simulation time - "+
		"click to force update, right click to change format";
	private static final String TEXT_SEPS = "simulated seconds per second";

	// "simulated events per second" averaging time (milliseconds)
	private static final int EPS_AVG_TIME = 2000;
	private static final String SCREENSHOT_FILE_TYPE = "png";
	private static final String SCREENSHOT_FILE = "screenshot";

	private JTextField simTimeField;
	private JLabel sepsField;	// simulated events per second field
	private JButton playButton;
	private JButton playUntilButton;
	private boolean paused;
	private JButton stepButton;
	private boolean step;
	private JButton ffwButton;
	private boolean isFfw;
	private int oldSpeedIndex; // what speed was selected before FFW

	private JButton screenShotButton;
	private JComboBox guiUpdateChooser;

	/**
	 * GUI update speeds. Negative values -> how many 1/10 seconds to wait
	 * between updates. Positive values -> show every Nth update
	 */
	public static final String[] UP_SPEEDS = {"-10", "-1", "0.1", "1", "10",
		"100", "1000", "10000", "100000"};

	/** Smallest value for the zoom level */
	public static final double ZOOM_MIN = 0.001;
	/** Highest value for the zoom level */
	public static final double ZOOM_MAX = 10;

	/** index of initial update speed setting */
	public static final int INITIAL_SPEED_SELECTION = 3;
	/** index of FFW speed setting */
	public static final int FFW_SPEED_INDEX = 7;

	private double guiUpdateInterval;
	private javax.swing.JSpinner zoomSelector;

	private PlayField pf;
	private DTNSimGUI gui;

	private long lastUpdate;
	private double lastSimTime;
	private double playUntilTime;

	private boolean useHourDisplay = false;

	public GUIControls(DTNSimGUI gui, PlayField pf) {
		/* TODO: read values for paused, isFfw etc from a file */
		this.pf = pf;
		this.gui = gui;
		this.lastUpdate = System.currentTimeMillis();
		this.lastSimTime = 0;
		this.paused = true;
		this.isFfw = false;
		this.playUntilTime = Double.MAX_VALUE;
		initPanel();
	}

	/**
	 * Creates panel's components and initializes them
	 */
	private void initPanel() {
		this.setLayout(new FlowLayout());
		this.simTimeField = new JTextField("0.0");
		this.simTimeField.setColumns(6);
		this.simTimeField.setEditable(false);
		this.simTimeField.setToolTipText(TEXT_SIMTIME);
		this.simTimeField.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					useHourDisplay = !useHourDisplay;
				}
				setSimTime(SimClock.getTime());
			}
		});

		this.sepsField = new JLabel("0.00");
		this.sepsField.setToolTipText(TEXT_SEPS);

		this.screenShotButton = new JButton(TEXT_SCREEN_SHOT);
		this.guiUpdateChooser = new JComboBox(UP_SPEEDS);

		this.zoomSelector = new JSpinner(new SpinnerNumberModel(1.0, ZOOM_MIN,
				ZOOM_MAX, 0.001));

		this.add(simTimeField);
		this.add(sepsField);

		playButton = addButton(paused ? ICON_PLAY : ICON_PAUSE,
				paused ? TEXT_PLAY : TEXT_PAUSE);
		stepButton = addButton(ICON_STEP, TEXT_STEP);
		ffwButton = addButton(ICON_FFW, TEXT_FFW);
		playUntilButton = addButton(ICON_PLAY, TEXT_PLAY_UNTIL);
		playUntilButton.setText("...");

		this.add(new JLabel(TEXT_UP_CHOOSER));
		this.add(this.guiUpdateChooser);
		this.guiUpdateChooser.setSelectedIndex(INITIAL_SPEED_SELECTION);
		this.updateUpdateInterval();

		this.add(new JLabel(createImageIcon(ICON_ZOOM)));
		this.updateZoomScale(false);

		this.add(this.zoomSelector);
		this.add(this.screenShotButton);

		guiUpdateChooser.addActionListener(this);
		zoomSelector.addChangeListener(this);
		this.screenShotButton.addActionListener(this);
	}


	private ImageIcon createImageIcon(String path) {
		java.net.URL imgURL = getClass().getResource(PATH_GRAPHICS+path);
		
		return new ImageIcon(imgURL);
	}


	private JButton addButton(String iconPath, String tooltip) {
		JButton button = new JButton(createImageIcon(iconPath));
		button.setToolTipText(tooltip);
		button.addActionListener(this);
		this.add(button);
		return button;
	}

	/**
	 * Sets the simulation time that control panel shows
	 * @param time The time to show
	 */
	public void setSimTime(double time) {
		long timeSinceUpdate = System.currentTimeMillis() - this.lastUpdate;

		if (timeSinceUpdate > EPS_AVG_TIME) {
			double val = ((time - this.lastSimTime) * 1000)/timeSinceUpdate;
			String sepsValue = String.format("%.2f 1/s", val);

			this.sepsField.setText(sepsValue);
			this.lastSimTime = time;
			this.lastUpdate = System.currentTimeMillis();
		}

		if (this.useHourDisplay) {
			int hours = (int)(time / 3600);
			int mins = (int)((time - hours * 3600) / 60);
			double secs = time % 60;
			this.simTimeField.setText(String.format("%02d:%02d:%02.1f",
					hours, mins, secs));
		} else {
			this.simTimeField.setText(String.format("%.1f", time));
		}
	}

	/**
	 * Sets simulation to pause or play.
	 * @param paused If true, simulation is put to pause
	 */
	public void setPaused(boolean paused) {
		if (!paused) {
			this.playButton.setIcon(createImageIcon(ICON_PAUSE));
			this.playButton.setToolTipText(TEXT_PAUSE);
			this.paused = false;
			if (SimClock.getTime() >= this.playUntilTime) {
				// playUntilTime passed -> disable it
				this.playUntilTime = Double.MAX_VALUE;
			}
		}
		else {
			this.playButton.setIcon(createImageIcon(ICON_PLAY));
			this.playButton.setToolTipText(TEXT_PLAY);
			this.paused = true;
			this.setSimTime(SimClock.getTime());
			this.pf.updateField();
		}
	}

	private void switchFfw() {
		if (isFfw) {
			this.isFfw = false; // set to normal play
			this.ffwButton.setIcon(createImageIcon(ICON_FFW));
			this.guiUpdateChooser.setSelectedIndex(oldSpeedIndex);
			this.ffwButton.setSelected(false);
		}
		else {
			this.oldSpeedIndex = this.guiUpdateChooser.getSelectedIndex();
			this.guiUpdateChooser.setSelectedIndex(FFW_SPEED_INDEX);
			this.isFfw = true; // set to FFW
			this.ffwButton.setIcon(createImageIcon(ICON_PLAY));
		}
	}

	/**
	 * Has user requested the simulation to be paused
	 * @return True if pause is requested
	 */
	public boolean isPaused() {
		if (step) { // if we want to step, return false once and reset stepping
			step = false;
			return false;
		}
		if (SimClock.getTime() >= this.playUntilTime) {
			this.setPaused(true);
		}
		return this.paused;
	}

	/**
	 * Is fast forward turned on
	 * @return True if FFW is on, false if not
	 */
	public boolean isFfw() {
		return this.isFfw;
	}

	/**
	 * Returns the selected update interval of GUI
	 * @return The update interval (seconds)
	 */
	public double getUpdateInterval() {
		return this.guiUpdateInterval;
	}

	/**
	 * Changes the zoom level
	 * @param delta How much to change the current level (can be negative or
	 * positive)
	 */
	public void changeZoom(int delta) {
		SpinnerNumberModel model =
			(SpinnerNumberModel)this.zoomSelector.getModel();
		double curZoom = model.getNumber().doubleValue();
		Number newValue = new Double(curZoom + model.getStepSize().
				doubleValue() * delta * curZoom * 100);

		if (newValue.doubleValue() < ZOOM_MIN) {
			newValue = ZOOM_MIN;
		} else if (newValue.doubleValue() > ZOOM_MAX) {
			newValue = ZOOM_MAX;
		}

		model.setValue(newValue);
		this.updateZoomScale(true);
	}


	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.playButton) {
			setPaused(!this.paused); // switch pause/play
		}
		else if (e.getSource() == this.stepButton) {
			setPaused(true);
			this.step = true;
		}
		else if (e.getSource() == this.ffwButton) {
			switchFfw();
		}
		else if (e.getSource() == this.playUntilButton) {
			setPlayUntil();
		}
		else if (e.getSource() == this.guiUpdateChooser) {
			updateUpdateInterval();
		}
		else if (e.getSource() == this.screenShotButton) {
			takeScreenShot();
		}
	}


	public void stateChanged(ChangeEvent e) {
		updateZoomScale(true);
	}


	private void setPlayUntil() {
		setPaused(true);
		String value = JOptionPane.showInputDialog(TEXT_PLAY_UNTIL);
		if (value == null) {
			return;
		}
		try {
			this.playUntilTime = Double.parseDouble(value);
			setPaused(false);
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(gui.getParentFrame(),
					"Invalid number '" + value+"'",
					"error",JOptionPane.ERROR_MESSAGE);
		}
	}


	private void updateUpdateInterval() {
		String selString = (String)this.guiUpdateChooser.getSelectedItem();
		this.guiUpdateInterval = Double.parseDouble(selString);
	}

	/**
	 * Updates zoom scale to the one selected by zoom chooser
	 * @param centerView If true, the center of the viewport should remain
	 * the same
	 */
	private void updateZoomScale(boolean centerView) {
		double scale = ((SpinnerNumberModel)zoomSelector.getModel()).
			getNumber().doubleValue();

		if (centerView) {
			Coord center = gui.getCenterViewCoord();
			this.pf.setScale(scale);
			gui.centerViewAt(center);
		}
		else {
			this.pf.setScale(scale);
		}
	}

	private void takeScreenShot() {
		try {
			JFileChooser fc = new JFileChooser();
			fc.setSelectedFile(new File(SCREENSHOT_FILE +
					"." + SCREENSHOT_FILE_TYPE));
			int retVal = fc.showSaveDialog(this);
			if (retVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				BufferedImage i = new BufferedImage(this.pf.getWidth(),
						this.pf.getHeight(), BufferedImage.TYPE_INT_RGB);
				Graphics2D g2 = i.createGraphics();

				this.pf.paint(g2);	// paint playfield to buffered image
				ImageIO.write(i, SCREENSHOT_FILE_TYPE, file);
			}
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(gui.getParentFrame(),
					"screenshot failed (problems with output file?)",
					"Exception", JOptionPane.ERROR_MESSAGE);
		}
	}

}
