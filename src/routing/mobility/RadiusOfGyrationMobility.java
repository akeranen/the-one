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
import core.Settings;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation mobility mobility algorithm using the metric
 * Radius of Gyration.
 */
public class RadiusOfGyrationMobility extends Mobility {

	/**
	 * Keeps the list of visited locations.
	 */
    private List<Double> locations;
    /**
     * Keeps the sum of visited points.
     */
    private double psum = 0d;
    /**
     * Keeps the computed center point.
     */
    private double pcenter = 0d;
    /**
     * Keeps the last locations list size in order to 
     * decide to recompute the mobility or not.
     */
    private int lastSize = 0;
    /**
     * Keeps the last computed value.
     */
    private double lastRadius = 0d;

    /**
     * Constructor.
     * @param s A reference to simulation settings.
     */
    public RadiusOfGyrationMobility(Settings s) {
        this.locations = new ArrayList<Double>();
    }

    /**
     * Copy constructor.
     * @param proto Prototype.
     */
    public RadiusOfGyrationMobility(RadiusOfGyrationMobility proto) {
        this.locations = new ArrayList<Double>();
    }

    @Override
    public double getMobilityLevel() {
        double radius = 0D;

        if (this.locations.size() == this.lastSize) {
            return this.lastRadius;
        } else {
            // Find the radius of gyration
            for (double p : this.locations) {
                radius += Math.pow((p - pcenter), 2);
            }
            radius /= this.locations.size();
            this.lastRadius = Math.sqrt(radius);
            return this.lastRadius;
        }
    }

    @Override
    public void addLocation(Coord location) {
        double val = Math.hypot(location.getX(), location.getY());
        this.locations.add(val);
        psum += val;
        pcenter = psum / this.locations.size();
    }

    @Override
    public Mobility replicate() {
        return new RadiusOfGyrationMobility(this);
    }
}
