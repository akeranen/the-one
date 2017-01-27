/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

/**
 * Error in the simulation
 *
 */
public class SimError extends AssertionError {
	private Exception e;

	public SimError(String cause) {
		super(cause);
		e = null;
	}

	public SimError(String cause, Exception e) {
		super(cause);
		this.e = e;
	}

	public SimError(Exception e) {
		this(e.getMessage(),e);
	}

	public Exception getException() {
		return e;
	}

}
