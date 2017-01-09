/*
 * Copyright 2016, Michael D. Silva (micdoug.silva@gmail.com)
 * Released under GPLv3. See LICENSE.txt for details.
 */

package routing;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import java.util.List;
import routing.centrality.Centrality;
import routing.centrality.ReportCentrality;
import routing.community.ReportCommunity;

/**
 * BubbleRap routing implementation.
 */
public class BubbleRapRouter extends CommunityAndRankRouter implements ReportCentrality, ReportCommunity {

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

    // Report interface implementation

    public double getLocalCentrality()
    {
        return this.getLocalRank();
    }

    public double getGlobalCentrality()
    { 
        return this.getGlobalRank();
    }
}














