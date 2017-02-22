package input;

import core.Coord;
import core.SimError;
import core.SimScenario;
import javax.json.*;

import java.io.IOException;

/**
 * This is a container that includes all parameters of a VHMEvent
 *
 * Created by Marius Meyer on 15.02.17.
 */
public class VHMEvent extends ExternalEvent{

    public static final int MIN_INTENSITY = 1;
    public static final int MAX_INTENSITY = 10;

    public static final int ZERO = 0;
    public static final double INVALID_DOUBLE = -1.0;

    public static final String EVENT_TYPE = "type";
    public static final String START_TIME = "start";
    public static final String END_TIME = "end";
    public static final String EVENT_LOCATION = "location";
    public static final String EVENT_LOCATION_X = "x";
    public static final String EVENT_LOCATION_Y = "y";
    public static final String EVENT_RANGE = "event_range";
    public static final String SAFE_RANGE = "safe_range";
    public static final String MAX_RANGE = "max_range";
    public static final String EVENT_INTENSITY = "intensity";


    /**
     * Type of a VHMEvent. This event type can be requested by a node
     */
    public enum VHMEventType{
        DISASTER,
        HOSPITAL
    }

    //Parameters as defined in the specification of the VHM
    private String identifier;
    private VHMEventType type;
    private double startTime;
    private double endTime;
    private transient Coord location;
    private double eventRange;
    private double safeRange;
    private double maxRange;
    private int intensity;

    public VHMEvent(String identifier,JsonObject object) throws IOException{
        super(0);
        try {
            //Parse mandatory parameters
            if (identifier != null) {
                this.identifier = identifier;
            } else throw new SimError("Event must have an identifier!");

            //parse event type
            this.type = VHMEventType.valueOf(((JsonString) object.get(EVENT_TYPE)).getString());

            //parse event location
            JsonObject loc = (JsonObject) object.get(EVENT_LOCATION);
            double x = ((JsonNumber)loc.get(EVENT_LOCATION_X)).doubleValue();
            double y = ((JsonNumber)loc.get(EVENT_LOCATION_Y)).doubleValue();
            this.location = new Coord(x,y);

            //parse event range
            this.eventRange = ((JsonNumber)object.get(EVENT_RANGE)).doubleValue();

            //parse safe range or use 0
            if (object.containsKey(SAFE_RANGE)){
                this.safeRange = ((JsonNumber)object.get(SAFE_RANGE)).doubleValue();
            }
            else{
                this.safeRange = ZERO;
            }

            //parse max range or take maximum distance as max range
            if (object.containsKey(MAX_RANGE)){
                this.maxRange = ((JsonNumber)object.get(MAX_RANGE)).doubleValue();
            }
            else{
                this.maxRange = INVALID_DOUBLE;

            }

            //parse start time or set default
            if (object.containsKey(START_TIME)){
                this.startTime = ((JsonNumber)object.get(START_TIME)).doubleValue();
            }
            else{
                this.startTime = ZERO;
            }

            //parse end time or set default
            if (object.containsKey(END_TIME)){
                this.endTime = ((JsonNumber)object.get(END_TIME)).doubleValue();
            }
            else{
                this.endTime = INVALID_DOUBLE;
            }

            //parse intensity or set it to min intensity
            if (object.containsKey(EVENT_INTENSITY)){
                this.intensity = ((JsonNumber)object.get(EVENT_INTENSITY)).intValue();
                assert intensity >= MIN_INTENSITY && intensity <= MAX_INTENSITY : "Intensity must be integer between " + MIN_INTENSITY
                        + " and " + MAX_INTENSITY;
            }
            else{
                this.intensity = MIN_INTENSITY;
            }
        }catch (Exception e){
            throw new IOException("VHMEvent could not be parsed from JSON: " + e.getMessage());
        }
    }

    public VHMEvent(VHMEvent event){
        super(0);
        this.type = event.type;
        this.startTime = event.startTime;
        this.endTime = event.endTime;
        this.location = event.location.clone();
        this.eventRange = event.eventRange;
        this.safeRange = event.safeRange;
        this.maxRange = event.maxRange;
        this.intensity = event.intensity;
    }

    public VHMEventType getType() {
        return type;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        if (endTime == INVALID_DOUBLE){
            return Double.MAX_VALUE;
            // SimScenario.getInstance().getEndTime();
        }else return endTime;
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
        if (maxRange == INVALID_DOUBLE){
            return Math.sqrt(SimScenario.getInstance().getWorldSizeY() *
                    SimScenario.getInstance().getWorldSizeY() +
                    SimScenario.getInstance().getWorldSizeX() *
                            SimScenario.getInstance().getWorldSizeX());
        }
        else return maxRange;
    }

    public int getIntensity() {
        return intensity;
    }

    public String getIdentifier(){
        return this.identifier;
    }

}
