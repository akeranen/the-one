/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package test;

import input.EventQueueHandler;

import java.util.Properties;

import movement.MovementModel;
import core.SettingsError;
import core.SimScenario;

/**
 * Class for replacing Settings class in tests.
 *
 */
public class TestSettings extends core.Settings {
	
	public TestSettings() {
		init(null);
		
		/* put some test setting values to the Settings. These can be later 
		 * overridden by calls to putSetting(String, String) */
		String sns = SimScenario.SCENARIO_NS + ".";
		String ens = EventQueueHandler.SETTINGS_NAMESPACE + ".";
		
		putSetting(sns + SimScenario.NROF_GROUPS_S,	"1");
		putSetting(sns + SimScenario.NAME_S, "TEST-Scenario");
		putSetting(sns + SimScenario.END_TIME_S, "100");
		putSetting(sns + SimScenario.UP_INT_S, "0.1");
		putSetting(sns + SimScenario.SIM_CON_S, "true");
		
		putSetting(ens + EventQueueHandler.NROF_SETTING, "0");
		
		putSetting(MovementModel.MOVEMENT_MODEL_NS + "." + 
				MovementModel.WORLD_SIZE, "1000,1000");
	}
	
	public TestSettings(String ns) {
		super(ns);
	}
	
	public static void init(String propFile) throws SettingsError {
		props = new Properties();
	}
	
	/**
	 * Put a new setting or override an existing setting
	 * @param key
	 * @param value
	 */
	public void putSetting(String key, String value) {
		String nameSpace = getNameSpace();
		if (nameSpace == null) {
			nameSpace = "";
		} else {
			nameSpace += ".";
		}
		
		if (props == null) {
			try {
				init(null);
			} catch (SettingsError e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		props.put(nameSpace + key, value);
	}	

}