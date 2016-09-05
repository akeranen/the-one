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

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import java.util.List;
import routing.centrality.Centrality;

/**
 * BubbleRap routing implementation.
 */
public class BubbleRapRouter extends CommunityAndRankRouter {

    /**
     * Base namespace for configuration parameters.
     */
    public static final String BUBBLERAP_NS = "BubbleRapRouter";
    /**
     * Centrality algorithm -setting id ({@value}).
     */
    public static final String CENTRALITY_ALG_S = "centralityAlg";
    /**
     * The default class to use for centrality computation.
     */
    public static final String DEFAULT_CENTRALITY_ALG = "CWindowCentrality";
    
    /**
     * A reference to a centrality instance.
     */
    private Centrality centrality;
        
    /**
     * Constructor.
     * @param set A reference to simulation settings.
     */
    public BubbleRapRouter(Settings set) {
        super(set, BUBBLERAP_NS);
        
        Settings settings = new Settings(BUBBLERAP_NS);
        String centralityAlg = settings.getSetting(CENTRALITY_ALG_S, DEFAULT_CENTRALITY_ALG);
        this.centrality = (Centrality) settings.createIntializedObject("routing.centrality." + centralityAlg);
    }
    
    /**
     * Copy constructor.
     * @param prot Prototype.
     */
    public BubbleRapRouter(BubbleRapRouter prot) {
        super(prot);
        this.centrality = prot.centrality.replicate();
    }

    @Override
    public double getGlobalRank() {
        return this.centrality.getGlobalCentrality(this.getConHistory());
    }

    @Override
    public double getLocalRank() {
        return this.centrality.getLocalCentrality(this.getConHistory(), this.getCommunity());
    }

    @Override
    public BubbleRapRouter replicate() {
        return new BubbleRapRouter(this);
    }

    @Override
    public void init(DTNHost host, List<MessageListener> mListeners) {
        super.init(host, mListeners);
        this.centrality.setHost(host);
    }
    
    /**
     * Used by unit tests.
     * @param host 
     */
    public void setTestHost(DTNHost host) {
        this.centrality.setHost(host);
        this.comdetect.setHost(host);
    }
    
}














