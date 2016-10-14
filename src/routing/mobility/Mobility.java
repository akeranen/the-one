/*
 * Copyright (C) 2016 Michael Dougras da Silva
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package routing.mobility;

import core.Coord;
import core.DTNHost;

/**
 * Interface for mobility level calculation.
 */
public abstract class Mobility {

	/**
	 * Keeps a reference to the node being observed.
	 */
    protected DTNHost host;
    
    /**
     * Return the mobility level of the node.
     */
    public abstract double getMobilityLevel();

    /**
     * Add a new location point in the list visited coordinates.
     * @param location
     */
    public abstract void addLocation(Coord location);

    /**
     * Return a semantically copy of the mobility instance.
     */
    public abstract Mobility replicate();
    
    /**
     * Set the host related to this instance of mobility.
     */
    public void setHost(DTNHost host) {
        this.host = host;
    }
}
