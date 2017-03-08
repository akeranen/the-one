package input;

import core.Coord;
import core.SimError;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import java.io.IOException;

/**
 * This is a container that includes all parameters of a VhmEvent
 * <p>
 * Created by Marius Meyer on 15.02.17.
 */
public class VhmEvent extends ExternalEvent {

    private static final long serialVersionUID = 1;

    /**
     * Minimum possible event intensity
     */
    public static final int MIN_INTENSITY = 1;

    /**
     * Maximum possible event intensity
     */
    public static final int MAX_INTENSITY = 10;


    /**
     * Default event intensity that is used if no other value is given
     */
    public static final int DEFAULT_INTENSITY = MIN_INTENSITY;

    /**
     * Default event maximum range that is used if no other value is given
     */
    public static final double DEFAULT_MAX_RANGE = Double.MAX_VALUE;

    /**
     * Default event end time that is used if no other value is given
     */
    public static final double DEFAULT_END_TIME = Double.MAX_VALUE;

    /**
     * Default event start time that is used if no other value is given
     */
    public static final double DEFAULT_START_TIME = 0.0;

    /**
     * Default event safe range that is used if no other value is given
     */
    public static final double DEFAULT_SAFE_RANGE = 0.0;

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
     * Type of a VhmEvent. This event type can be requested by a node
     */
    public enum VhmEventType {
        /**
         * A disaster where nodes will try to help or flee
         */
        DISASTER,

        /**
         * A hospital where nodes will transport victims to
         */
        HOSPITAL
    }

    /**
     * Static variable to store the event ID of the next created VhmEvent.
     * Do not access this directly because of synchronization issues.
     * Use {@link VhmEvent#getNextEventID()} instead.
     */
    private static long nextEventID;

    /**
     * Name of an event
     */
    private String name;

    /**
     * Unique id of an event that is used to distinguish between events
     */
    private long id;

    /**
     * The type of an event
     */
    private VhmEventType type;

    /**
     * The time point an event will start
     */
    private double startTime;

    /**
     * The time point an event will end
     */
    private double endTime;

    /**
     * The location of an event
     */
    private transient Coord location;

    /**
     * The range around an event's location, where nodes will be directly affected
     * by the event. This may be the area of a flooding or the buildings of a hospital.
     */
    private double eventRange;

    /**
     * The range around an event's location, where nodes are safe
     */
    private double safeRange;

    /**
     * The range around an event's location, where nodes will react to an event
     */
    private double maxRange;

    /**
     * The intensity of an event. This is an integer between {@link VhmEvent#MIN_INTENSITY}
     * and {@link VhmEvent#MAX_INTENSITY}
     */
    private int intensity;

    /**
     * Creates a new VhmEvent using a JSON object
     *
     * @param name   The name of the event. This should be the key value of the JSON object
     * @param object The JSON object, that represents the event
     * @throws IOException If the JSON object could not be parsed to an VhmEvent
     */
    public VhmEvent(String name, JsonObject object) throws IOException {
        super(0);
        try {
            this.id = getNextEventID();
            //Parse mandatory parameters
            if (name != null) {
                this.name = name;
            } else {
                throw new SimError("Event must have an identifier!");
            }

            //parse event type
            this.type = VhmEventType.valueOf(object.getJsonString(EVENT_TYPE).getString());

            //parse event location
            JsonObject loc = (JsonObject) object.get(EVENT_LOCATION);
            double x = loc.getJsonNumber(EVENT_LOCATION_X).doubleValue();
            double y = loc.getJsonNumber(EVENT_LOCATION_Y).doubleValue();
            this.location = new Coord(x, y);

            //parse event range
            this.eventRange = object.getJsonNumber(EVENT_RANGE).doubleValue();

            safeRange = getDoubleOrDefault(object, SAFE_RANGE, DEFAULT_SAFE_RANGE);

            maxRange = getDoubleOrDefault(object, MAX_RANGE, DEFAULT_MAX_RANGE);

            startTime = getDoubleOrDefault(object, START_TIME, DEFAULT_START_TIME);

            endTime = getDoubleOrDefault(object, END_TIME, DEFAULT_END_TIME);

            //parse intensity or set it to min intensity
            if (object.containsKey(EVENT_INTENSITY)) {
                this.intensity = ((JsonNumber) object.get(EVENT_INTENSITY)).intValue();
                assert intensity >= MIN_INTENSITY &&
                        intensity <= MAX_INTENSITY : "Intensity must be integer between "
                        + MIN_INTENSITY + " and " + MAX_INTENSITY;
            } else {
                this.intensity = DEFAULT_INTENSITY;
            }
        } catch (Exception e) {
            throw new IOException("VhmEvent could not be parsed from JSON: " + e);
        }
    }

    /**
     * Copy constructor for a VhmEvent
     *
     * @param event The event that should be copied
     */
    public VhmEvent(VhmEvent event) {
        super(0);
        this.name = event.name;
        this.id = event.id;
        this.type = event.type;
        this.startTime = event.startTime;
        this.endTime = event.endTime;
        this.location = event.location.clone();
        this.eventRange = event.eventRange;
        this.safeRange = event.safeRange;
        this.maxRange = event.maxRange;
        this.intensity = event.intensity;
    }

    /**
     * Returns the next unique event id.
     * This method should only be used in the event's constructor.
     *
     * @return next event id
     */
    private static synchronized long getNextEventID() {
        return nextEventID++;
    }

    /**
     * Gets a JSON object and tries to return a double with the specified key.
     * If this key does not exist, it returns a predefined default value.
     *
     * @param jsonEventObject The JSON object to get the value from
     * @param key             the JSON key of the desired value
     * @param defaultValue    the default value, that is returned, if the key does not exist
     * @return the key value or the default value
     */
    private static double getDoubleOrDefault(JsonObject jsonEventObject, String key, double defaultValue) {
        if (jsonEventObject.containsKey(key)) {
            return jsonEventObject.getJsonNumber(key).doubleValue();
        } else {
            return defaultValue;
        }
    }

    /**
     * Returns the event's type
     *
     * @return the event's {@link VhmEvent#type}
     */
    public VhmEventType getType() {
        return type;
    }

    /**
     * Returns the event's start time
     *
     * @return the event's {@link VhmEvent#startTime}
     */
    public double getStartTime() {
        return startTime;
    }

    /**
     * Returns the event's end time
     *
     * @return the event's {@link VhmEvent#endTime}
     */
    public double getEndTime() {
        return endTime;
    }

    /**
     * Returns the event's location
     *
     * @return the event's {@link VhmEvent#location}
     */
    public Coord getLocation() {
        return location.clone();
    }

    /**
     * Return the event's event range
     *
     * @return the event's {@link VhmEvent#eventRange}
     */
    public double getEventRange() {
        return eventRange;
    }

    /**
     * Return the event's safe range
     *
     * @return the event's {@link VhmEvent#safeRange}
     */
    public double getSafeRange() {
        return safeRange;
    }

    /**
     * Return the event's maximum range
     *
     * @return the event's {@link VhmEvent#maxRange}
     */
    public double getMaxRange() {
        return maxRange;
    }

    /**
     * Returns the intensity of an event.
     *
     * @return the event's {@link VhmEvent#intensity}
     */
    public int getIntensity() {
        return intensity;
    }

    /**
     * Returns the {@link VhmEvent#name} of the event that was specified in the JSON file.
     *
     * @return the event name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the unique {@link VhmEvent#id} of the event
     *
     * @return the unique id
     */
    public long getID() {
        return this.id;
    }

    /**
     * Checks, if two VhmEvents are equal by comparing their id
     *
     * @param ob the VhmEvent to compare to
     * @return true, if the object is a VhmEvent and has the same id as the calling instance
     */
    @Override
    public boolean equals(Object ob) {
        return ob != null && this.getClass() == ob.getClass() && ((VhmEvent) ob).getID() == getID();
    }

    /**
     * Returns a hash code value vor the event. This is the event id converted to an integer.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return (int) this.id;
    }

}
