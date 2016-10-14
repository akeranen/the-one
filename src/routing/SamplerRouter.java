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
package routing;

import core.Coord;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.MovementListener;
import core.Settings;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import routing.centrality.Centrality;
import routing.centrality.ReportCentrality;
import routing.community.ReportCommunity;
import routing.mobility.Mobility;
import routing.mobility.ReportMobility;
import util.Range;

/**
 * Implementation of the SAMPLER protocol.
 */
public class SamplerRouter extends CommunityAndRankRouter implements ReportCentrality, ReportCommunity, ReportMobility, MovementListener {

    /**
     * Base namespace for configuration parameters.
     */
    public static final String SAMPLER_NS = "SamplerRouter";
    /**
     * Centrality algorithm -setting id ({@value}).
     */
    public static final String CENTRALITY_ALG_S = "centralityAlg";
    /**
     * The default class to use for centrality computation.
     */
    public static final String DEFAULT_CENTRALITY_ALG = "CWindowCentrality";
    /**
     * Mobility algorithm -setting id ({@value}).
     */
    public static final String MOBILITY_ALG_S = "mobilityAlg";
    /**
     * Default mobility algorithm class.
     */
    public static final String DEFAULT_MOBILITY_ALG = "RadiusOfGyrationMobility";
    /**
     * Relay points -setting id ({@value}).
     */
    public static final String RELAY_POINTS_S = "relayPoints";

    private static Range[] relayPoints;

    /**
     * A reference to a centrality instance.
     */
    private Centrality centrality;

    /**
     * A reference to a mobility instance.
     */
    private Mobility mobility;

    /**
     * Indicates if this router is attached to a relay point.
     */
    private boolean isRelayPoint;

    public boolean isRelayPoint() {
        return isRelayPoint;
    }

    /**
     * Constructor.
     * @param set A reference to simulator settings.
     */
    public SamplerRouter(Settings set) {
        super(set, SAMPLER_NS);

        Settings settings = new Settings(SAMPLER_NS);

        // Creating the centrality instance
        String centralityAlg = settings.getSetting(CENTRALITY_ALG_S, DEFAULT_CENTRALITY_ALG);
        this.centrality = (Centrality) settings.createIntializedObject("routing.centrality." + centralityAlg);

        // Creating the mobility instance.
        String mobilityAlg = settings.getSetting(MOBILITY_ALG_S, DEFAULT_MOBILITY_ALG);
        this.mobility = (Mobility) settings.createIntializedObject("routing.mobility." + mobilityAlg);

        // Get the relay points id range
        relayPoints = settings.getCsvRanges(RELAY_POINTS_S);
    }

    /**
     * Copy constructor.
     * @param prot Prototype.
     */
    public SamplerRouter(SamplerRouter prot) {
        super(prot);
        this.centrality = prot.centrality.replicate();
        this.mobility = prot.mobility.replicate();
    }

    @Override
    public void init(DTNHost host, List<MessageListener> mListeners) {
        super.init(host, mListeners);
        for (Range range : relayPoints) {
            if (range.isInRange(host.getAddress())) {
                this.isRelayPoint = true;
                //System.out.println(host.getAddress());
                break;
            }
        }
        this.centrality.setHost(host);
        this.mobility.setHost(host);
        host.addMovementListener(this);
    }

    @Override
    public double getGlobalRank() {
        if (this.isRelayPoint) {
            return Double.POSITIVE_INFINITY;
        } else {
            return this.mobility.getMobilityLevel();
        }
    }

    @Override
    public double getLocalRank() {
        if (this.isRelayPoint) {
            return 0D;
        } else {
            return this.centrality.getLocalCentrality(this.getConHistory(), this.getCommunity());
        }
    }

    @Override
    public Set<DTNHost> getCommunity() {
        if (this.isRelayPoint) {
            return new HashSet<DTNHost>();
        } else {
            return super.getCommunity(); //To change body of generated methods, choose Tools | Templates.
        }
    }

    @Override
    public SamplerRouter replicate() {
        return new SamplerRouter(this);
    }
    
    /**
     * Used by unit tests.
     * @param host 
     */
    public void setTestHost(DTNHost host) {
        this.centrality.setHost(host);
        this.comdetect.setHost(host);
        this.mobility.setHost(host);
    }
    
    /**
     * Used by unit tests.
     */
    public boolean testRelayPoint() {
        return this.isRelayPoint;
    }

    @Override
    public double getLocalCentrality() {
        return this.centrality.getLocalCentrality(this.getConHistory(), this.getCommunity());
    }

    @Override
    public double getGlobalCentrality() {
        return this.centrality.getGlobalCentrality(this.getConHistory());
    }

    @Override
    public double getMobilityLevel() {
        return this.mobility.getMobilityLevel();
    }

	@Override
	public void newDestination(DTNHost host, Coord destination, double speed) {
		if (host == this.getHost()) {
			this.mobility.addLocation(destination);
			//System.out.println("New location");
		}		
	}

	@Override
	public void initialLocation(DTNHost host, Coord location) {
		if (host == this.getHost()) {
			this.mobility.addLocation(location);
		}
	}
    
}
