/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

/**
 * Settings related error
 *
 */
public class SettingsError extends SimError {

	public SettingsError(String cause) {
		super(cause);
	}
	
	public SettingsError(String cause, Exception e) {
		super(cause,e);
	}
	
	public SettingsError(Exception e) {
		super(e);
	}

}
