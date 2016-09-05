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
package routing.centrality;

/**
 * Used by the CentralityReport to get the global and local centrality from routers.
 */
public interface ReportCentrality {
    /**
     * Get the node's local centrality value.
     */
    public double getLocalCentrality();
    
    /**
     * Get the node's global centrality value.
     */
    public double getGlobalCentrality();
}
