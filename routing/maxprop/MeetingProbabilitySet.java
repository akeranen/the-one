/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing.maxprop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.SimClock;

/**
 * Class for storing and manipulating the meeting probabilities for the MaxProp
 * router module.
 */
public class MeetingProbabilitySet {
	public static final int INFINITE_SET_SIZE = Integer.MAX_VALUE;
	/** meeting probabilities (probability that the next node one meets is X) */
	private Map<Integer, Double> probs;
	/** the time when this MPS was last updated */
	private double lastUpdateTime;
	/** the alpha parameter */
	private double alpha;
    private int maxSetSize;
	
	/**
	 * Constructor. Creates a probability set with empty node-probability
	 * mapping.
	 * @param maxSetSize Maximum size of the probability set; when the set is
	 *        full, smallest values are dropped when new are added
	 */
	public MeetingProbabilitySet(int maxSetSize, double alpha) {
		this.alpha = alpha;
		this.probs = new HashMap<Integer, Double>();
        if (maxSetSize == INFINITE_SET_SIZE || maxSetSize < 1) {
        	this.probs = new HashMap<Integer, Double>();
        	this.maxSetSize = INFINITE_SET_SIZE;
        } else {
        	this.probs = new HashMap<Integer, Double>(maxSetSize);
            this.maxSetSize = maxSetSize;
        }
		this.lastUpdateTime = 0;
	}
	
	/**
	 * Constructor. Creates a probability set with empty node-probability
	 * mapping and infinite set size
	 */
	public MeetingProbabilitySet() {
		this(INFINITE_SET_SIZE, 1);
	}
	
	/**
	 * Constructor. Creates a probability set with equal probability for
	 * all the given node indexes.
	 */
	public MeetingProbabilitySet(double alpha, 
				List<Integer> initiallyKnownNodes) {
		this(INFINITE_SET_SIZE, alpha);
		double prob = 1.0/initiallyKnownNodes.size();
		for (Integer i : initiallyKnownNodes) {
			this.probs.put(i, prob);
		}
	}
	
	/**
	 * Updates meeting probability for the given node index.
	 * <PRE> P(b) = P(b)_old + alpha
	 * Normalize{P}</PRE> 
	 * I.e., The probability of the given node index is increased by one and
	 * then all the probabilities are normalized so that their sum equals to 1.
	 * @param index The node index to update the probability for
	 */
	public void updateMeetingProbFor(Integer index) {
        Map.Entry<Integer, Double> smallestEntry = null;
        double smallestValue = Double.MAX_VALUE;

		this.lastUpdateTime = SimClock.getTime();
		
		if (probs.size() == 0) { // first entry
			probs.put(index, 1.0);
			return;
		}
		
		double newValue = getProbFor(index) + alpha;
		probs.put(index, newValue);

		/* now the sum of all entries is 1+alpha;
		 * normalize to one by dividing all the entries by 1+alpha */ 
		for (Map.Entry<Integer, Double> entry : probs.entrySet()) {
			entry.setValue(entry.getValue() / (1+alpha));
            if (entry.getValue() < smallestValue) {
                smallestEntry = entry;
                smallestValue = entry.getValue();
            }

		}

        if (probs.size() >= maxSetSize) {
            core.Debug.p("Probsize: " + probs.size() + " dropping " + 
                    probs.remove(smallestEntry.getKey()));
        }
	}
	
	public void updateMeetingProbFor(Integer index, double iet)	{
		probs.put(index, iet);
	}
	
	/**
	 * Returns the current delivery probability value for the given node index 
	 * @param index The index of the node to look the P for
	 * @return the current delivery probability value
	 */
	public double getProbFor(Integer index) {
		if (probs.containsKey(index)) {
			return probs.get(index);
		}
		else {
			/* the node with the given index has not been met */
			return 0.0;
		}
	}
	
	/**
	 * Returns a reference to the probability map of this probability set
	 * @return a reference to the probability map of this probability set
	 */
	public Map<Integer, Double> getAllProbs() {
		return this.probs;
	}
	
	/**
	 * Returns the time when this probability set was last updated
	 * @return the time when this probability set was last updated
	 */
	public double getLastUpdateTime() {
		return this.lastUpdateTime;
	}
	
	/**
	 * Enables changing the alpha parameter dynamically
	 */
	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}
	
	/**
	 * Returns a deep copy of the probability set
	 * @return a deep copy of the probability set
	 */
	public MeetingProbabilitySet replicate() {
		MeetingProbabilitySet replica = new MeetingProbabilitySet(
				this.maxSetSize, alpha);
		
		// do a deep copy
		for (Map.Entry<Integer, Double> e : probs.entrySet()) {
			replica.probs.put(e.getKey(), e.getValue().doubleValue());
		}
		
		replica.lastUpdateTime = this.lastUpdateTime;
		return replica;
	}
	
	/**
	 * Returns a String presentation of the probabilities
	 * @return a String presentation of the probabilities
	 */
    @Override
	public String toString() {
		return "probs: " +	this.probs.toString();
	}
}