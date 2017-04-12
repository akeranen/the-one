/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import java.util.Random;

import core.Settings;
import core.SettingsError;

/**
 * Message creation -external events generator. Creates uniformly distributed
 * message creation patterns whose message size and inter-message intervals can
 * be configured.
 */
public abstract class AbstractMessageEventGenerator implements EventQueue {
    /** Message size range -setting id ({@value}). Can be either a single
     * value or a range (min, max) of uniformly distributed random values.
     * Defines the message size (bytes). */
    public static final String MESSAGE_SIZE_S = "size";
    /** Message creation interval range -setting id ({@value}). Can be either a
     * single value or a range (min, max) of uniformly distributed
     * random values. Defines the inter-message creation interval (seconds). */
    public static final String MESSAGE_INTERVAL_S = "interval";
    /** Sender/receiver address range -setting id ({@value}).
     * The lower bound is inclusive and upper bound exclusive. */
    public static final String HOST_RANGE_S = "hosts";

    /** Message ID prefix -setting id ({@value}). The value must be unique
     * for all message sources, so if you have more than one message generator,
     * use different prefix for all of them. The random number generator's
     * seed is derived from the prefix, so by changing the prefix, you'll get
     * also a new message sequence. */
    public static final String MESSAGE_ID_PREFIX_S = "prefix";
    /** Message creation time range -setting id ({@value}). Defines the time
     * range when messages are created. No messages are created before the first
     * and after the second value. By default, messages are created for the
     * whole simulation time. */
    public static final String MESSAGE_TIME_S = "time";
    /** Message priority range -setting id ({@value}}). Defines the range of
     * possible ranges */
    public static final String PRIORITY_S = "priorities";

    /**
     * The minimum number of hosts needed for communication.
     */
    protected static final int NUMBER_HOSTS_NEEDED_FOR_COMMUNICATION = 2;

    /** Time of the next event (simulated seconds) */
    protected double nextEventsTime;
    /** Range of host addresses that can be senders or receivers */
    protected int[] hostRange = {0, 0};
    /** Next identifier for a message */
    private int id;
    /** Prefix for the messages */
    protected String idPrefix;
    /** Size range of the messages (min, max) */
    private int[] sizeRange;
    /** Interval between messages (min, max) */
    private int[] msgInterval;
    /** Time range for message creation (min, max) */
    protected double[] msgTime;
    /** Range of possible priorities */
    protected int[] priorityRange = {0, 0};

    /** Random number generator for this Class */
    protected Random rng;

    /**
     * Constructor, initializes the interval between events,
     * and the size of messages generated, as well as number
     * of hosts in the network.
     * @param s Settings for this generator.
     * @param checkForSufficientHostRange Whether or not this constructor should throw an error if the host range given
     *                                    by settings contains less than 2 hosts.
     */
    public AbstractMessageEventGenerator(Settings s, boolean checkForSufficientHostRange){
        this.sizeRange = s.getCsvInts(MESSAGE_SIZE_S);
        this.msgInterval = s.getCsvInts(MESSAGE_INTERVAL_S);
        this.hostRange = s.getCsvInts(HOST_RANGE_S, Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE);
        this.idPrefix = s.getSetting(MESSAGE_ID_PREFIX_S);

        if (s.contains(MESSAGE_TIME_S)) {
            this.msgTime = s.getCsvDoubles(MESSAGE_TIME_S, Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE);
        } else {
            this.msgTime = null;
        }
        
        if(s.contains(PRIORITY_S)){
            this.priorityRange = s.getCsvInts(PRIORITY_S, Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE); 
        }

        /* Make sure simulation stays reproducible. */
        /* if prefix is unique, so will be the rng's sequence. */
        this.rng = new Random(idPrefix.hashCode());

        if (this.sizeRange.length == 1) {
            /* convert single value to range with 0 length */
            this.sizeRange = new int[] {this.sizeRange[0], this.sizeRange[0]};
        } else {
            s.assertValidRange(this.sizeRange, MESSAGE_SIZE_S);
        }
        if (this.msgInterval.length == 1) {
            this.msgInterval = new int[] {this.msgInterval[0],
                    this.msgInterval[0]};
        } else {
            s.assertValidRange(this.msgInterval, MESSAGE_INTERVAL_S);
        }
        s.assertValidRange(this.hostRange, HOST_RANGE_S);

        if (checkForSufficientHostRange
                && this.hostRange[1] - this.hostRange[0] < NUMBER_HOSTS_NEEDED_FOR_COMMUNICATION) {
            throw new SettingsError("Host range must contain at least two nodes.");
        }

        /* Calculate the first event's time:
        It should be shortly after the earliest message time, ... */
        double earliestMessageTime = 0;
        if (this.msgTime != null) {
            earliestMessageTime = this.msgTime[0];
        }
        /* ...but happens only after the usual interval between messages.
        That interval is not fixed; we randomly choose a interval duration between msgInterval[0] and msgInterval[1]
        to select it.*/
        int diffToShortestPossibleInterval = 0;
        if (msgInterval[0] != msgInterval[1]) {
            diffToShortestPossibleInterval = rng.nextInt(msgInterval[1] - msgInterval[0]);
        }
        this.nextEventsTime = earliestMessageTime + msgInterval[0] + diffToShortestPossibleInterval;
    }

    /**
     * Draws a random host address from the configured address range
     * @param hostRange The range of hosts
     * @return A random host address
     */
    protected int drawHostAddress(int[] hostRange) {
        if (hostRange[1] == hostRange[0]) {
            return hostRange[0];
        }
        return hostRange[0] + rng.nextInt(hostRange[1] - hostRange[0]);
    }
    
    /**
     * Draws a random priority from the configured address range
     * @return A random priority
     */
    protected int drawPriority(){
        if(priorityRange[1] == priorityRange[0]){
            return priorityRange[0];
        }
        return priorityRange[0]+ rng.nextInt(priorityRange[1] - priorityRange[0]);
    }

    /**
     * Generates a (random) message size
     * @return message size
     */
    protected int drawMessageSize() {
        int sizeDiff = 0;
        if (sizeRange[0] != sizeRange[1]) {
            sizeDiff = rng.nextInt(sizeRange[1] - sizeRange[0]);
        }
        return sizeRange[0] + sizeDiff;
    }

    /**
     * Generates a (random) time difference between two events
     * @return the time difference
     */
    protected int drawNextEventTimeDiff() {
        int timeDiff = 0;
        if (msgInterval[0] != msgInterval[1]) {
            timeDiff = rng.nextInt(msgInterval[1] - msgInterval[0]);
        }
        return msgInterval[0] + timeDiff;
    }

    /**
     * Increases point in time for next event by the provided time span.
     * If the next event time would then be later than the maximum time as specified by this.msgTime[1], it is set to
     * {@see Double.MAX_VALUE} instead.
     * @param noEventsInterval Time span in which no events should happen.
     */
    protected void advanceToNextEvent(double noEventsInterval) {
        this.nextEventsTime += noEventsInterval;
        if (this.msgTime != null && this.nextEventsTime > this.msgTime[1]) {
            this.nextEventsTime = Double.MAX_VALUE;
        }
    }

    /**
     * Returns next message creation event's time
     * @see input.EventQueue#nextEventsTime()
     */
    @Override
    public double nextEventsTime() {
        return this.nextEventsTime;
    }

    /**
     * Returns a next free message ID
     * @return next globally unique message ID
     */
    protected String getID(){
        this.id++;
        return idPrefix + this.id;
    }
}
