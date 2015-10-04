/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package gui;

import gui.nodefilter.NodeMessageFilter;
import gui.playfield.PlayField;
import gui.playfield.NodeGraphic;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import core.Settings;
import core.SettingsError;

/**
 * Menu bar of the simulator GUI
 *
 */
public class SimMenuBar extends JMenuBar implements ActionListener {
	/** title of the about window */
	public static final String ABOUT_TITLE = "about ONE";
	/** GPLv3 license text for about window */
	public static final String ABOUT_TEXT = 
	"Copyright (C) 2007-2011 Aalto University, Comnet\n\n"+
	"This program is free software: you can redistribute it and/or modify\n"+
    "it under the terms of the GNU General Public License as published by\n"+
    "the Free Software Foundation, either version 3 of the License, or\n"+
    "(at your option) any later version.\n\n"+
    "This program is distributed in the hope that it will be useful,\n"+
    "but WITHOUT ANY WARRANTY; without even the implied warranty of\n"+
    "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"+
    "GNU General Public License for more details.\n\n" +
    "You should have received a copy of the GNU General Public License\n"+
    "along with this program.  If not, see <http://www.gnu.org/licenses/>.\n\n"+
    "Map data copyright: Maanmittauslaitos, 2007";
	
	private JCheckBoxMenuItem enableBgImage;
	private JCheckBoxMenuItem showNodeName;
	private JCheckBoxMenuItem showNodeCoverage;
	private JCheckBoxMenuItem showNodeConnections;
	private JCheckBoxMenuItem showBuffer;

	private JCheckBoxMenuItem enableMapGraphic;
	private JCheckBoxMenuItem autoClearOverlay;
	private JCheckBoxMenuItem focusOnClick;
	
	private JMenuItem clearOverlay;
	private JMenuItem addNodeMessageFilter;
	private JMenuItem clearNodeFilters;
	
	private JMenuItem about;
	private PlayField field;
	private NodeChooser chooser;

	/** Show node name string -setting id ({@value})*/
	public static final String SHOW_NODE_NAMESTR_S = "showNodeNameStrings";
	/** Show node radio coverage -setting id ({@value})*/
	public static final String SHOW_RADIO_COVERAGES_S = "showNodeRadioCoverages";
	/** Show node connections -setting id ({@value})*/
	public static final String SHOW_CONNECTIONS_S = "showNodeConnections";
	/** Show nodes' messages -setting id ({@value})*/
	public static final String SHOW_BUFFER_S = "showMessageBuffer";
	/** Show node connections -setting id ({@value})*/
	public static final String FOCUS_ON_CLICK_S = "focusOnClick";
	/** The namespace where underlay image -related settings are found */
	public static final String UNDERLAY_NS = "GUI.UnderlayImage";
	
	public SimMenuBar(PlayField field, NodeChooser nodeChooser) {
		this.field = field;
		this.chooser = nodeChooser;
		init();
	}

	private void init() {
		JMenu pfMenu = new JMenu("Playfield options");
		JMenu pfToolsMenu = new JMenu("Tools");
		JMenu help = new JMenu("Help");
		JMenu nodeFilters = new JMenu("Add node filter");
		Settings settings = new Settings(UNDERLAY_NS);
		
		if (settings.contains("fileName")) {
			// create underlay image menu item only if filename is specified 
			enableBgImage = createCheckItem(pfMenu,"Show underlay image",
					false, null);
		}
		
		settings.setNameSpace(gui.MainWindow.GUI_NS);
		
		showNodeName = createCheckItem(pfMenu, "Show node name strings",
				true, SHOW_NODE_NAMESTR_S);		
		showNodeCoverage = createCheckItem(pfMenu, 
				"Show node radio coverages", true, SHOW_RADIO_COVERAGES_S);
		showNodeConnections = createCheckItem(pfMenu,
				"Show node connections", true, SHOW_CONNECTIONS_S);
		showBuffer = createCheckItem(pfMenu,
				"Show message buffer", true, SHOW_BUFFER_S);
		focusOnClick = createCheckItem(pfMenu,
				"Focus to closest node on mouse click", false,FOCUS_ON_CLICK_S);

		enableMapGraphic = createCheckItem(pfMenu,"Show map graphic",
				true, null);
		autoClearOverlay = createCheckItem(pfMenu, "Autoclear overlay",
				true, null);
		clearOverlay = createMenuItem(pfToolsMenu, "Clear overlays");
		
		
		pfToolsMenu.addSeparator();
		addNodeMessageFilter = createMenuItem(nodeFilters, "message filter");
		pfToolsMenu.add(nodeFilters);
		clearNodeFilters = createMenuItem(pfToolsMenu, "Clear node filters");

		updatePlayfieldSettings();
		
		about = createMenuItem(help,"about");
		this.add(pfMenu);
		this.add(pfToolsMenu);
		this.add(Box.createHorizontalGlue());
		this.add(help);
	}
	
	private JMenuItem createMenuItem(Container c, String txt) {
		JMenuItem i = new JMenuItem(txt);
		i.addActionListener(this);
		c.add(i);
		return i;
	}
	
	/**
	 * Creates a new check box menu item to the given container
	 * @param c The container
	 * @param txt Text for the menu item
	 * @param selected Is the check box selected (by default)
	 * @param setting Name of the setting where the check-box-selected 
	 * true/false value is read from (if not found, the "selected" parameter 
	 * value is used). If null, no setting is read and the default is 
	 * used as such.
	 * @return The created check box menu item
	 */
	private JCheckBoxMenuItem createCheckItem(Container c,String txt, 
			boolean selected, String setting) {
		Settings s = new Settings(gui.MainWindow.GUI_NS);
		
		JCheckBoxMenuItem i = new JCheckBoxMenuItem(txt);
		if (setting == null) {
			i.setSelected(selected);
		} else {
			i.setSelected(s.getBoolean(setting, selected));
		}
		
		i.addActionListener(this);
		c.add(i);
		
		return i;
	}

	
	private void updatePlayfieldSettings() {
		NodeGraphic.setDrawNodeName(showNodeName.isSelected());
		NodeGraphic.setDrawCoverage(showNodeCoverage.isSelected());
		NodeGraphic.setDrawConnections(showNodeConnections.isSelected());
		NodeGraphic.setDrawBuffer(showBuffer.isSelected());
		field.setShowMapGraphic(enableMapGraphic.isSelected());
		field.setAutoClearOverlay(autoClearOverlay.isSelected());
		field.setFocusOnClick(focusOnClick.isSelected());
	}
	
	private String getFilterString(String message) {
		return (String)JOptionPane.showInputDialog(
                this, message, "Filter input", JOptionPane.PLAIN_MESSAGE);
	}
	
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == enableBgImage) {
			toggleUnderlayImage();
		}
		else if (source == this.showNodeName || 
				source == this.showNodeCoverage ||
				source == this.showNodeConnections ||
				source == this.enableMapGraphic ||
				source == this.autoClearOverlay ||
				source == this.showBuffer ||
				source == this.focusOnClick) {
			updatePlayfieldSettings();
		}

		else if (source == this.clearOverlay) {
			field.clearOverlays();
		}
		else if (source == addNodeMessageFilter) {
			chooser.addFilter(
					new NodeMessageFilter(getFilterString("Message ID")));
		}
		else if (source == clearNodeFilters) {
			chooser.clearFilters();
		}
		else if (source == this.about) {
			JOptionPane.showMessageDialog(this, ABOUT_TEXT, ABOUT_TITLE, 
					JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	/**
	 * Toggles the showing of underlay image. Image is read from the file only
	 * when it is enabled to save some memory.
	 */
	private void toggleUnderlayImage() {
		if (enableBgImage.isSelected()) {
			String imgFile = null;
			int[] offsets;
			double scale, rotate;
			BufferedImage image;
			try {
				Settings settings = new Settings(UNDERLAY_NS);
				imgFile = settings.getSetting("fileName");
				offsets = settings.getCsvInts("offset", 2);
				scale = settings.getDouble("scale");
				rotate = settings.getDouble("rotate");
	            image = ImageIO.read(new File(imgFile));
	        } catch (IOException ex) {
	        	warn("Couldn't set underlay image " + imgFile + ". " + 
	        			ex.getMessage());
	        	enableBgImage.setSelected(false);
	        	return;
	        }
	        catch (SettingsError er) {
	        	warn("Problem with the underlay image settings: " + 
	        			er.getMessage());
	        	return;
	        }
			field.setUnderlayImage(image, offsets[0], offsets[1],
					scale, rotate);
		}
		else {
			// disable the image
			field.setUnderlayImage(null, 0, 0, 0, 0);
		}
	}
	
	private void warn(String txt) {
		JOptionPane.showMessageDialog(null, txt, "warning", 
				JOptionPane.WARNING_MESSAGE);
	}
	
}