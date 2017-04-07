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
 * The results are calculated as summary for the whole simulation run.
 * Format is like
 * Message stats for scenario deliveryProbabilityReport sim_time: 10073.4000
 * created: 339 delivered: 80 delivery_prob: 0.2360
 * 
 * where the total simulation time is part of the header line. Created gives the number of created messages,
 * delivered are the number of messages reaching its target. The quotient is the delivery probability. 
 *
 *
 * @author Nils Weidmann
 *
 */
public final class DeliveryProbabilityReport  extends Report implements MessageListener {

    private int nrofCreated;
    private int nrofDelivered;
    
    /**
     * Constructor, it just calls the init-Methods
     */
    public DeliveryProbabilityReport() {
        init();
    }
    
    public int getNrofCreated() {
        return nrofCreated;
    }
    
    public int getNrofDelivered() {
        return nrofDelivered;
    }
    /**
     * Sets basic settings
     */
    @Override
    protected void init() {
        super.init();
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
     * @param finalTarget if the target is thee intended target of the message and if it receives it for the first time
     */
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {
        if (isWarmupID(m.getId())) {
            return;
        }
        
        if (m.getType() == core.Message.MessageType.ONE_TO_ONE && finalTarget) {
            this.nrofDelivered++;
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

        if (m.getType().equals(Message.MessageType.ONE_TO_ONE)) {
            this.nrofCreated += 1;
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
