package test;

import core.Coord;
import core.SimError;
import input.VhmEvent;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Contains tests for the {@link input.VhmEvent} class.
 *
 * Created by Britta Heymann on 01.04.2017.
 */
public class VhmEventTest {
    private static final double DOUBLE_COMPARING_DELTA = 0.01;

    private static final String EVENT_NAME = "testEvent";
    private static final double START_TIME = 1;
    private static final double END_TIME = 20;
    private static final double X_COORDINATE = 4000;
    private static final double Y_COORDINATE = 2000;
    private static final double EVENT_RANGE = 200;
    private static final double SAFE_RANGE = 450;
    private static final double MAX_RANGE = 650;
    private static final int INTENSITY = 2;

    @Test
    public void testConstructorParsesAllFieldsFromJson() {
        JsonObject completelySpecifiedEvent = VhmEventTest.createMinimalVhmEventBuilder()
            .add(VhmEvent.START_TIME, START_TIME)
            .add(VhmEvent.END_TIME, END_TIME)
            .add(VhmEvent.SAFE_RANGE, SAFE_RANGE)
            .add(VhmEvent.MAX_RANGE, MAX_RANGE)
            .add(VhmEvent.EVENT_INTENSITY, INTENSITY)
            .build();
        VhmEvent event = new VhmEvent(EVENT_NAME, completelySpecifiedEvent);

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
        JsonObject emptyVhmEventJson = VhmEventTest.createMinimalVhmEventBuilder().build();
        VhmEvent event = new VhmEvent(EVENT_NAME, emptyVhmEventJson);

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
        assertEquals(
                "Intensity should have been default intensity.",
                VhmEvent.DEFAULT_INTENSITY,
                event.getIntensity());
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
    /*
testConstructorThrowsOnInvalidJson*/

    @Test(expected = SimError.class)
    public void testConstructorThrowsOnMissingEventName() {
        new VhmEvent(null, VhmEventTest.createMinimalVhmEventBuilder().build());
    }

    private static JsonObjectBuilder createMinimalVhmEventBuilder() {
        return Json.createObjectBuilder()
                .add(VhmEvent.EVENT_TYPE, VhmEvent.VhmEventType.DISASTER.toString())
                .add(VhmEvent.EVENT_LOCATION, VhmEventTest.createLocationBuilder())
                .add(VhmEvent.EVENT_RANGE, EVENT_RANGE);
    }

    private static JsonObjectBuilder createLocationBuilder() {
        return Json.createObjectBuilder()
            .add(VhmEvent.EVENT_LOCATION_X, X_COORDINATE)
            .add(VhmEvent.EVENT_LOCATION_Y, Y_COORDINATE);
    }
}
