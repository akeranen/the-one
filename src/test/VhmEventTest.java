package test;

import core.Coord;
import core.SimError;
import input.VhmEvent;
import input.VhmEventEndEvent;
import input.VhmEventStartEvent;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Contains tests for the {@link input.VhmEvent} class.
 *
 * Created by Britta Heymann on 01.04.2017.
 */
public class VhmEventTest {
    /** Properties of VHM events used in tests, if specified explicitly (instead of using the default value). */
    private static final String EVENT_NAME = "testEvent";
    private static final double START_TIME = 1;
    private static final double END_TIME = 20;
    private static final double X_COORDINATE = 4000;
    private static final double Y_COORDINATE = 2000;
    private static final double EVENT_RANGE = 200;
    private static final double SAFE_RANGE = 450;
    private static final double MAX_RANGE = 650;
    private static final int INTENSITY = 10;

    /** Delta used when asserting double equality. */
    private static final double DOUBLE_COMPARING_DELTA = 0.01;

    @Test
    public void testConstructorParsesAllFieldsFromJson() {
        VhmEvent event = new VhmEvent(EVENT_NAME, VhmEventTest.createJsonForCompletelySpecifiedEvent());

        assertEquals("Event type was not as specified.", VhmEvent.VhmEventType.DISASTER, event.getType());
        assertEquals("Location was not as specified.", new Coord(X_COORDINATE, Y_COORDINATE), event.getLocation());
        assertEquals("Event range was not as specified.", EVENT_RANGE, event.getEventRange(), DOUBLE_COMPARING_DELTA);
        assertEquals("Safe range was not as specified.", SAFE_RANGE, event.getSafeRange(), DOUBLE_COMPARING_DELTA);
        assertEquals("Max range was not as specified.", MAX_RANGE, event.getMaxRange(), DOUBLE_COMPARING_DELTA);
        assertEquals("Start time was not as specified.", START_TIME, event.getStartTime(), DOUBLE_COMPARING_DELTA);
        assertEquals("End time was not as specified.", END_TIME, event.getEndTime(), DOUBLE_COMPARING_DELTA);
        assertEquals("Intensity was not as specified.", INTENSITY, event.getIntensity());
    }

    @Test
    public void testConstructorSetsDefaultsForMissingValues() {
        VhmEvent event = VhmEventTest.createVhmEventWithDefaultValues();

        assertEquals(
                "Start time should have been default start time.",
                VhmEvent.DEFAULT_START_TIME,
                event.getStartTime(),
                DOUBLE_COMPARING_DELTA);
        assertEquals(
                "End time should have been default end time.",
                VhmEvent.DEFAULT_END_TIME,
                event.getEndTime(),
                DOUBLE_COMPARING_DELTA);
        assertEquals(
                "Safe range should have been default safe range.",
                VhmEvent.DEFAULT_SAFE_RANGE,
                event.getSafeRange(),
                DOUBLE_COMPARING_DELTA);
        assertEquals(
                "Max range should have been default max range.",
                VhmEvent.DEFAULT_MAX_RANGE,
                event.getMaxRange(),
                DOUBLE_COMPARING_DELTA);
        assertEquals("Intensity should have been default intensity.", VhmEvent.DEFAULT_INTENSITY, event.getIntensity());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorThrowsForMissingEventType() {
        JsonObject eventWithoutType = Json.createObjectBuilder()
                .add(VhmEvent.EVENT_LOCATION, VhmEventTest.createLocationBuilder())
                .add(VhmEvent.EVENT_RANGE, EVENT_RANGE)
                .build();
        new VhmEvent(EVENT_NAME, eventWithoutType);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorThrowsForMissingLocation() {
        JsonObject eventWithoutLocation = Json.createObjectBuilder()
                .add(VhmEvent.EVENT_TYPE, VhmEvent.VhmEventType.DISASTER.toString())
                .add(VhmEvent.EVENT_RANGE, EVENT_RANGE)
                .build();
        new VhmEvent(EVENT_NAME, eventWithoutLocation);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorThrowsForMissingXCoordinate() {
        JsonObject eventWithoutXCoordinate = Json.createObjectBuilder()
                .add(VhmEvent.EVENT_TYPE, VhmEvent.VhmEventType.DISASTER.toString())
                .add(VhmEvent.EVENT_LOCATION, Json.createObjectBuilder().add(VhmEvent.EVENT_LOCATION_Y, Y_COORDINATE))
                .add(VhmEvent.EVENT_RANGE, EVENT_RANGE)
                .build();
        new VhmEvent(EVENT_NAME, eventWithoutXCoordinate);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorThrowsForMissingYCoordinate() {
        JsonObject eventWithoutYCoordinate = Json.createObjectBuilder()
                .add(VhmEvent.EVENT_TYPE, VhmEvent.VhmEventType.DISASTER.toString())
                .add(VhmEvent.EVENT_LOCATION, Json.createObjectBuilder().add(VhmEvent.EVENT_LOCATION_X, X_COORDINATE))
                .add(VhmEvent.EVENT_RANGE, EVENT_RANGE)
                .build();
        new VhmEvent(EVENT_NAME, eventWithoutYCoordinate);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorThrowsForMissingEventRange() {
        JsonObject eventWithoutEventRange = Json.createObjectBuilder()
                .add(VhmEvent.EVENT_TYPE, VhmEvent.VhmEventType.DISASTER.toString())
                .add(VhmEvent.EVENT_LOCATION, VhmEventTest.createLocationBuilder())
                .build();
        new VhmEvent(EVENT_NAME, eventWithoutEventRange);
    }

    @Test(expected = AssertionError.class)
    public void testConstructorThrowsOnTooLowIntensity() {
        JsonObject eventWithTooLowIntensity = VhmEventTest.createMinimalVhmEventBuilder()
                .add(VhmEvent.EVENT_INTENSITY, VhmEvent.MIN_INTENSITY - 1)
                .build();
        new VhmEvent(EVENT_NAME, eventWithTooLowIntensity);
    }

    @Test
    public void testConstructorWorksForMinIntensity() {
        JsonObject eventWithMinIntensity = VhmEventTest.createMinimalVhmEventBuilder()
                .add(VhmEvent.EVENT_INTENSITY, VhmEvent.MIN_INTENSITY)
                .build();
        VhmEvent event = new VhmEvent(EVENT_NAME, eventWithMinIntensity);
        assertNotNull("Expected VhmEvent.", event);
    }

    @Test(expected = AssertionError.class)
    public void testConstructorThrowsOnTooHighIntensity() {
        JsonObject eventWithTooHighIntensity = VhmEventTest.createMinimalVhmEventBuilder()
                .add(VhmEvent.EVENT_INTENSITY, VhmEvent.MAX_INTENSITY + 1)
                .build();
        new VhmEvent(EVENT_NAME, eventWithTooHighIntensity);
    }

    @Test
    public void testConstructorWorksForMaxIntensity() {
        JsonObject eventWithMaxIntensity = VhmEventTest.createMinimalVhmEventBuilder()
                .add(VhmEvent.EVENT_INTENSITY, VhmEvent.MAX_INTENSITY)
                .build();
        VhmEvent event = new VhmEvent(EVENT_NAME, eventWithMaxIntensity);
        assertNotNull("Expected VhmEvent.", event);
    }

    @Test(expected = SimError.class)
    public void testConstructorThrowsOnMissingEventName() {
        new VhmEvent(null, VhmEventTest.createMinimalVhmEventBuilder().build());
    }

    @Test
    public void testCopyConstructorCopiesAllFields() {
        JsonObject completelySpecifiedEvent = VhmEventTest.createJsonForCompletelySpecifiedEvent();
        VhmEvent event = new VhmEvent(EVENT_NAME, completelySpecifiedEvent);
        VhmEvent copy = new VhmEvent(event);

        assertEquals("Event type was not copied.", event.getType(), copy.getType());
        assertEquals("Location was not copied.", event.getLocation(), copy.getLocation());
        assertEquals(
                "Event range was not copied.", event.getEventRange(), copy.getEventRange(), DOUBLE_COMPARING_DELTA);
        assertEquals("Safe range was not copied.", event.getSafeRange(), copy.getSafeRange(), DOUBLE_COMPARING_DELTA);
        assertEquals("Max range was not copied.", event.getMaxRange(), copy.getMaxRange(), DOUBLE_COMPARING_DELTA);
        assertEquals("Start time was not copied.", event.getStartTime(), copy.getStartTime(), DOUBLE_COMPARING_DELTA);
        assertEquals("End time was not copied.", event.getEndTime(), copy.getEndTime(), DOUBLE_COMPARING_DELTA);
        assertEquals("Intensity was not copied.", event.getIntensity(), copy.getIntensity());
    }

    @Test
    public void testEventIdsAreConsecutive() {
        VhmEvent event1 = VhmEventTest.createVhmEventWithDefaultValues();
        VhmEvent event2 = VhmEventTest.createVhmEventWithDefaultValues();
        assertEquals("Event ids should be consecutive.", event1.getID() + 1, event2.getID());
    }

    @Test
    public void testEqualsNullReturnsFalse() {
        VhmEvent event = VhmEventTest.createVhmEventWithDefaultValues();
        assertFalse("Equals method should have returned false for null.", event.equals(null));
    }

    @Test
    public void testEqualsNonVhmEventReturnsFalse() {
        VhmEvent event = VhmEventTest.createVhmEventWithDefaultValues();
        Coord nonVhmEvent = new Coord(0, 0);
        assertFalse("Equals method should have returned false for non VHM event,", event.equals(nonVhmEvent));
    }

    @Test
    public void testEqualsSameIdEventReturnsTrue() {
        VhmEvent event = VhmEventTest.createVhmEventWithDefaultValues();
        VhmEvent eventStart = new VhmEventStartEvent(event);
        VhmEvent eventEnd = new VhmEventEndEvent(event);
        assertTrue("Equals methods should have returned true for same VHM event ID.", eventStart.equals(eventEnd));
    }

    @Test
    public void testHashCode() {
        VhmEvent event = VhmEventTest.createVhmEventWithDefaultValues();
        assertEquals("Hash code should equal the event id.", event.getID(), event.hashCode());
    }

    @Test
    public void testResetVhmIdCounterSetsCounterToZero(){
        VhmEvent event1 = createVhmEventWithDefaultValues();
        VhmEvent.resetVhmEventIdCounter();
        VhmEvent event2 = createVhmEventWithDefaultValues();
        assertEquals("The ID of the event should be 0",0,event2.getID());
    }

    /**
     * Creates a {@link JsonObject} that completely specifies a {@link VhmEvent} s. t. no default values will be used.
     * @return The created {@link JsonObject}.
     */
    static JsonObject createJsonForCompletelySpecifiedEvent() {
        return VhmEventTest.createMinimalVhmEventBuilder()
                .add(VhmEvent.START_TIME, START_TIME)
                .add(VhmEvent.END_TIME, END_TIME)
                .add(VhmEvent.SAFE_RANGE, SAFE_RANGE)
                .add(VhmEvent.MAX_RANGE, MAX_RANGE)
                .add(VhmEvent.EVENT_INTENSITY, INTENSITY)
                .build();
    }

    /**
     * Creates a {@link VhmEvent} that uses default values at all possible places.
     * @return The created {@link VhmEvent}.
     */
    static VhmEvent createVhmEventWithDefaultValues() {
        return new VhmEvent(EVENT_NAME, VhmEventTest.createMinimalVhmEventBuilder().build());
    }

    /**
     * Creates a {@link JsonObjectBuilder} that contains the minimal specifications needed for providing the built
     * object to {@link VhmEvent}'s constructor.
     *
     * @param type the type of the created {@link VhmEvent}
     * @return The created {@link JsonObjectBuilder}.
     */
    static JsonObjectBuilder createMinimalVhmEventBuilder(VhmEvent.VhmEventType type) {
        return Json.createObjectBuilder()
                .add(VhmEvent.EVENT_TYPE, type.toString())
                .add(VhmEvent.EVENT_LOCATION, VhmEventTest.createLocationBuilder())
                .add(VhmEvent.EVENT_RANGE, EVENT_RANGE);
    }

    /**
     * Creates a {@link JsonObjectBuilder} that contains the minimal specifications needed for providing the built
     * object to {@link VhmEvent}'s constructor.
     *
     * @return The created {@link JsonObjectBuilder}.
     */
    private static JsonObjectBuilder createMinimalVhmEventBuilder() {
        return createMinimalVhmEventBuilder(VhmEvent.VhmEventType.DISASTER);
    }
    /**
     * Creates a {@link JsonObjectBuilder} for building a location.
     * @return The created {@link JsonObjectBuilder}.
     */
    static JsonObjectBuilder createLocationBuilder() {
        return Json.createObjectBuilder()
                .add(VhmEvent.EVENT_LOCATION_X, X_COORDINATE)
                .add(VhmEvent.EVENT_LOCATION_Y, Y_COORDINATE);
    }
}
