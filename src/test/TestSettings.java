/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import core.SettingsError;
import core.SimScenario;
import input.EventQueueHandler;
import movement.MovementModel;
import movement.StationaryMovement;
import routing.EpidemicRouter;

import java.util.Properties;

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

	/**
	 *Extends existing settings to support {@link SimScenario} for test scenarios by
	 * adding all properties needed for groups
	 * @param settings The settings that should be extended
	 */
	static void addSettingsToEnableSimScenario(TestSettings settings) {
	    settings.setNameSpace(SimScenario.GROUP_NS);
	    settings.putSetting(SimScenario.GROUP_ID_S, "group");
	    settings.putSetting(SimScenario.NROF_HOSTS_S, "3");
	    settings.putSetting(SimScenario.NROF_INTERF_S, "0");
	    settings.putSetting(SimScenario.MOVEMENT_MODEL_S, StationaryMovement.class.getSimpleName());
	    settings.putSetting(StationaryMovement.LOCATION_S, "0, 0");
	    settings.putSetting(SimScenario.ROUTER_S, EpidemicRouter.class.getSimpleName());
	    settings.restoreNameSpace();
	}


}
