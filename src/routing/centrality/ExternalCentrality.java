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

import core.DTNHost;
import java.util.Map;
import util.Tuple;
import core.Settings;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import routing.community.Duration;

/**
 * Loads the centrality values from an external file.
 */
public class ExternalCentrality extends Centrality {
    
	/**
	 * Configuration id for the centrality file.
	 */
    public static final String CENTRALITY_FILE_S = "centralityFile";

    /**
     * Stores the centrality values to use later.
     */
    private static Map<Integer, Tuple<Double, Double>> values;

    /**
     * Constructor.
     * @param s A reference to simulation settings.
     */
    public ExternalCentrality(Settings s) {
        // Load centrality values from an external file. 
        if (values == null) {
            values = new HashMap<Integer, Tuple<Double, Double>>();
            String filename = s.getSetting(CENTRALITY_FILE_S);
            
            FileReader file = null;
            BufferedReader reader = null;
            try {
            	file = new FileReader(filename);
            	reader = new BufferedReader(file);
            	String line = reader.readLine();
            	while (line != null) {
            		
            		String[] temp = line.split(" ");
                    values.put(Integer.parseInt(temp[0]), 
                    		new Tuple<Double, Double>(Double.parseDouble(temp[1]), 
                    				Double.parseDouble(temp[2])));
            		
            		line = reader.readLine();
            	}
            }
            catch (FileNotFoundException exc) {
            	System.out.println(String.format("The file %s was not found.", filename));
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
            		// Nothing to do here
            	}
            }
            
        }
    }

    public ExternalCentrality(ExternalCentrality prot) {
        // Copy instance level settings if needed
    }

    @Override
    public double getGlobalCentrality(Map<DTNHost, List<Duration>> connHistory) {
        return values.get(this.host.getAddress()).getValue();
    }

    @Override
    public double getLocalCentrality(Map<DTNHost, List<Duration>> connHistory, Set<DTNHost> community) {
        return values.get(this.host.getAddress()).getKey();
    }

    @Override
    public ExternalCentrality replicate() {
        return new ExternalCentrality(this);
    }
}
