package input;

import core.Settings;

public class EmergencyMessageGenerator extends MessageEventGenerator {

    public static final String EMERGENCY_TIME_S = "emergencyTime";

    private int nextFromOffset;

    public EmergencyMessageGenerator(Settings s) {
        super(s);

        this.nextEventsTime = s.getDouble(EMERGENCY_TIME_S, 0);

        this.nextFromOffset = 0;
    }

    public ExternalEvent nextEvent() {
        int responseSize = 0;
        int msgSize;
        int interval;
        int from;
        int to;
        boolean burstDone = false;

        int firstHostRangeElement = this.hostRange[0];
        int lastExclusiveHostRangeElement = this.hostRange[1] - 1;

        from = firstHostRangeElement + nextFromOffset;
        to = drawToAddress(hostRange, from);

        msgSize = drawMessageSize();
        MessageCreateEvent mce = new MessageCreateEvent(from, to, getID(),
                msgSize, responseSize, this.nextEventsTime);

        if (from < lastExclusiveHostRangeElement) {
            this.nextFromOffset++;
        } else {
            burstDone = true;
        }

        if (burstDone) {
            interval = drawNextEventTimeDiff();
            this.nextEventsTime += interval;
            this.nextFromOffset = 0;
        }

        if (this.msgTime != null && this.nextEventsTime > this.msgTime[1]) {
            /* next event would be later than the end time */
            this.nextEventsTime = Double.MAX_VALUE;
        }

        return mce;
    }
}
