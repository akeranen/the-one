/*
 * Copyright (C) 2016 Michael
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the mobility level from an external file.
 */
public class ExternalMobility extends Mobility {

	/**
	 * Configuration id for the mobility filename.
	 */
    public static final String MOBILITY_FILE_S = "mobilityFile";
    /**
     * Stores the mobility values to use later.
     */
    private static Map<Integer, Double> values;

    /**
     * Constructor that initializes configurations.
     * @param s A reference to simulation settings.
     */
    public ExternalMobility(Settings s) {
        // Load centrality values from an external file.
        if (values == null) {
            values = new HashMap<Integer, Double>();
            String filename = s.getSetting(MOBILITY_FILE_S);
            
            FileReader file = null;
            BufferedReader reader = null;
            
            try {
            	file = new FileReader(filename);
            	reader = new BufferedReader(file);
            	
            	String line = reader.readLine();
            	while (line != null) {
            		
            		String[] temp = line.split(" ");
                    values.put(Integer.parseInt(temp[0]), Double.parseDouble(temp[1]));
            		
            		line = reader.readLine();
            	}
            }
            catch (FileNotFoundException exc) {
            	System.out.println(String.format("Couldn't find the file %s.", filename));
            	System.exit(1);
            }
            catch (IOException exc) {
            	System.out.println(String.format("Error while reading the file %s. Details: \n%s", filename, exc.getMessage()));
            	System.exit(1);
            }
            finally {
            	try {
            		reader.close();
            		file.close();
            	}
            	catch (IOException exc) {
            		// Nothing to do here.
            	}
            }

        }
    }
    
    /**
     * Copy constructor.
     * @param prot Prototype.
     */
    public ExternalMobility(ExternalMobility prot) {
        // Copy instance specific settings
    }

    @Override
    public double getMobilityLevel() {
        return values.get(this.host.getAddress());
    }

    @Override
    public void addLocation(Coord location) {
        // Not useful here

    }

    @Override
    public Mobility replicate() {
        return new ExternalMobility(this);
    }
}
