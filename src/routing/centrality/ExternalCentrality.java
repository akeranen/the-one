/*
 * Copyright 2016, Michael D. Silva (micdoug.silva@gmail.com)
 * Released under GPLv3. See LICENSE.txt for details.
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
 * The file is expected to have in each line the following structure:
 * <node_index(int)> <local_centrality(double)> <global_centrality(double)>
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
