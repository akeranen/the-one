package input;

import core.Coord;

/**
 * This is a container that includes all parameters of a VHMEvent
 *
 * Created by Marius Meyer on 15.02.17.
 */
public class VHMEvent extends ExternalEvent {

    private static final int MIN_INTENSITY = 1;
    private static final int MAX_INTENSITY = 10;

    /**
     * Type of a VHMEvent. This event type can be requested by a node
     */
    public enum VHMEventType{
        DISASTER,
        HOSPITAL
    }

    //Parameters as defined in the specification of the VHM
    private VHMEventType type;
    private double startTime;
    private double endTime;
    private transient Coord location;
    private double eventRange;
    private double safeRange;
    private double maxRange;
    private int intensity;

    public VHMEvent(VHMEventType type, double startTime, double endTime, Coord location, double eventRange,
                    double safeRange, double maxRange,int intensity){
        super(startTime);
        assert type != null : "Type must be set!";
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.eventRange = eventRange;
        this.safeRange = safeRange;
        this.maxRange = maxRange;
        assert intensity >= MIN_INTENSITY && intensity <= MAX_INTENSITY : "Intensity must be integer between " + MIN_INTENSITY
                + " and " + MAX_INTENSITY;
        this.intensity = intensity;
    }

    public VHMEvent(VHMEvent event){
        this(event.type,event.startTime,event.endTime,event.location.clone(),event.eventRange,event.safeRange,
                event.maxRange,event.intensity);
    }

    public VHMEventType getType() {
        return type;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public Coord getLocation() {
        return location.clone();
    }

    public double getEventRange() {
        return eventRange;
    }

    public double getSafeRange() {
        return safeRange;
    }

    public double getMaxRange() {
        return maxRange;
    }

    public int getIntensity() {
        return intensity;
    }

}
