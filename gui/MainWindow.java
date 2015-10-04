/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package gui;

import gui.playfield.PlayField;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import core.Settings;
import core.World;

/**
 * Main window for the program. Takes care of layouting the main components
 * in the window.
 */
public class MainWindow extends JFrame {
	/** The namespace for general GUI settings */
	public static final String GUI_NS = "GUI";
	
	/** Main window settings namespace ({@value}) */
	public static final String GUI_WIN_NS = GUI_NS + ".window";
	
	/** Window width -setting id ({@value}). Defines the width of the GUI 
	 * window. Default {@link #WIN_DEFAULT_WIDTH} */
	public static final String WIN_WIDTH_S = "width";
	/** Window height -setting id ({@value}). Defines the height of the GUI 
	 * window. Default {@link #WIN_DEFAULT_HEIGHT} */
	public static final String WIN_HEIGHT_S = "height";

	/** Default width for the GUI window */
	public static final int WIN_DEFAULT_WIDTH = 900;
	/** Default height for the GUI window */
	public static final int WIN_DEFAULT_HEIGHT = 700;
	
	public static final String WINDOW_TITLE = "ONE";
	/** log panel's initial weight in the split panel */
	private static final double SPLIT_PANE_LOG_WEIGHT = 0.2;
	
	private JScrollPane playFieldScroll;
	
    public MainWindow(String scenName, World world, PlayField field,
    		GUIControls guiControls, InfoPanel infoPanel,
    		EventLogPanel elp, DTNSimGUI gui) {    	
    	super(WINDOW_TITLE + " - " + scenName);
    	JFrame.setDefaultLookAndFeelDecorated(true);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        JPanel leftPane = new JPanel();
        leftPane.setLayout(new BoxLayout(leftPane,BoxLayout.Y_AXIS));
    	JScrollPane hostListScroll;
        JSplitPane fieldLogSplit;
        JSplitPane logControlSplit;
        JSplitPane mainSplit;
        Settings s = new Settings(GUI_WIN_NS);
        NodeChooser chooser = new NodeChooser(world.getHosts(),gui);
        
    	setLayout(new BorderLayout());
        setJMenuBar(new SimMenuBar(field, chooser));
        
        playFieldScroll = new JScrollPane(field);
        playFieldScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 
        		Integer.MAX_VALUE));
        
        hostListScroll = new JScrollPane(chooser);
        hostListScroll.setHorizontalScrollBarPolicy(
        		JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        logControlSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
        		new JScrollPane(elp.getControls()),new JScrollPane(elp));
        logControlSplit.setResizeWeight(0.1);
        logControlSplit.setOneTouchExpandable(true);
        
        fieldLogSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        		leftPane, logControlSplit);
        fieldLogSplit.setResizeWeight(1-SPLIT_PANE_LOG_WEIGHT);
        fieldLogSplit.setOneTouchExpandable(true);
        
        setPreferredSize(new Dimension(
        		s.getInt(WIN_WIDTH_S, WIN_DEFAULT_WIDTH), 
        		s.getInt(WIN_HEIGHT_S, WIN_DEFAULT_HEIGHT)));

        leftPane.add(guiControls);
        leftPane.add(playFieldScroll);
        leftPane.add(infoPanel);
        
        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
        		fieldLogSplit, hostListScroll);
        mainSplit.setOneTouchExpandable(true);
        mainSplit.setResizeWeight(0.8);    
        this.getContentPane().add(mainSplit);
        
        pack();
    }

    /**
     * Returns a reference of the play field scroll panel
     * @return a reference of the play field scroll panel
     */
    public JScrollPane getPlayFieldScroll() {
    	return this.playFieldScroll;
    }
    
}
