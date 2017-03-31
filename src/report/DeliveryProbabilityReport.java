package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Report for generating statistics about the delivery probability of One-to-One messages.
 * @author Nils Weidmann
 *
 */
public final class DeliveryProbabilityReport  extends Report implements MessageListener {

	private Map<String, Double> creationTimes;
	private List<Double> latencies;
	private List<Integer> hopCounts;
	private List<Double> roundTripTimes;

	private int nrofCreated;
	private int nrofDelivered;
	
	/**
	 * Constructor, it just calls the init-Methods
	 */
	public DeliveryProbabilityReport() {
		init();
	}
	
	/**
	 * Sets basic settings
	 */
	protected void init() {
		super.init();
		this.creationTimes = new HashMap<String, Double>();
		this.latencies = new ArrayList<Double>();
		this.hopCounts = new ArrayList<Integer>();
		this.roundTripTimes = new ArrayList<Double>();

		this.nrofCreated = 0;
		this.nrofDelivered = 0;
	}
	
	/**
	 * Is called when a message is deleted from the buffer
	 * Method is not used, but must be implemented due to the class hierarchy
	 * @param m Message to be delivered
	 * @param where DTN host where the Message is located
	 * @param dropped
	 */
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if (isWarmupID(m.getId())) {
			return;
		}
	}
	
	/**
	 * Method is not used, but must be implemented due to the class hierarchy
	 * @param m Message to be delivered
	 * @param from sender of the message
	 * @param to receiver of the message
	 */
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		// Method is not used, but must be implemented due to the class hierarchy
	}
	
	/**
	 * @param m Message
	 * @param from sender of the message
	 * @param to receiver of the message
	 * @param finalTarget intended target of the message
	 */
	public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {
		if (isWarmupID(m.getId())) {
			return;
		}
        
		if (m.getType() == core.Message.MessageType.ONE_TO_ONE && finalTarget) {
			this.latencies.add(getSimTime() -
				this.creationTimes.get(m.getId()) );
			this.nrofDelivered++;
			this.hopCounts.add(m.getHops().size() - 1);

			if (m.isResponse()) {
				this.roundTripTimes.add(getSimTime() -	m.getRequest().getCreationTime());
			}
		}
	}
	
	/**
	 * This method is invoked when a new message is created
	 * @param m Message that was recently created
	 */
	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}

		this.creationTimes.put(m.getId(), getSimTime());

		switch (m.getType()) {
            case ONE_TO_ONE:
            	this.nrofCreated += 1;
                break;
            case BROADCAST:
                return; //only one-to-one messages are considered
            default:
                throw new UnsupportedOperationException("No implementation for message type " + m.getType() + ".");
        }
	}
	
	/**
	 * Method is not used, but must be implemented due to the class hierarchy
	 * @param m Message to be delivered
	 * @param from sender of the message
	 * @param to receiver of the message
	 */
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		// Method is not used, but must be implemented due to the class hierarchy
	}
	
	/**
	 * Writes the results of the report to a text file
	 */
	@Override
	public void done() {
		write("Message stats for scenario " + getScenarioName() +
				"\nsim_time: " + format(getSimTime()));
		double deliveryProb = 0;
		
		if (this.nrofCreated > 0) {
            deliveryProb = (1.0 * this.nrofDelivered) / this.nrofCreated;
		}
		
		String statsText = "created: " + this.nrofCreated +
				"\ndelivered: " + this.nrofDelivered +
				"\ndelivery_prob: " + format(deliveryProb)
				;
		
		write(statsText);
		super.done();
	}
}