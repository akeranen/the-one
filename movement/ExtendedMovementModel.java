/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;

/**
 * Classes derived from this can make use of other movement models that
 * implement the SwitchableMovement interface.
 *
 * @author Frans Ekman
 */
public abstract class ExtendedMovementModel extends MovementModel {

	private SwitchableMovement currentMovementModel;
	private boolean getPathCalledOnce;

	/**
	 * Creates a new ExtendedMovementModel
	 */
	public ExtendedMovementModel() {
		super();
	}

	/**
	 * Creates a new ExtendedMovementModel
	 * @param settings
	 */
	public ExtendedMovementModel(Settings settings) {
		super(settings);
	}

	/**
	 * Creates a new ExtendedMovementModel from a prototype
	 * @param mm
	 */
	public ExtendedMovementModel(ExtendedMovementModel mm) {
		super(mm);
	}

	/**
	 * Sets the current movement model to be used the next time getPath() is
	 * called
	 * @param mm Next movement model
	 */
	public void setCurrentMovementModel(SwitchableMovement mm) {
		Coord lastLocation = null;
		if (currentMovementModel != null) {
			lastLocation = currentMovementModel.getLastLocation();
		}
		currentMovementModel = mm;
		if (lastLocation != null) {
			currentMovementModel.setLocation(lastLocation);
		}
	}

	/**
	 * @return The movement model currently in use
	 */
	public SwitchableMovement getCurrentMovementModel() {
		return currentMovementModel;
	}

	@Override
	public Path getPath() {
		if (getPathCalledOnce) {
			if (currentMovementModel.isReady()) {
				newOrders();
			}
		}
		getPathCalledOnce = true;
		return ((MovementModel)currentMovementModel).getPath();
	}

	@Override
	protected double generateWaitTime() {
		return ((MovementModel)currentMovementModel).generateWaitTime();
	}

	/**
	 * Method is called between each getPath() request when the current MM is
	 * ready (isReady() method returns true). Subclasses should implement all
	 * changes of state that need to be made here, for example switching
	 * mobility model, etc.
	 * @return true if success
	 */
	public abstract boolean newOrders();

}
