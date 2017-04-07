package test;

import core.Coord;
import gui.playfield.VhmEventGraphic;
import input.VhmEvent;
import input.VhmEventEndEvent;
import input.VhmEventStartEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Contains tests for the {@link gui.playfield.VhmEventGraphic} class.
 *
 * Created by Britta Heymann on 02.04.2017.
 */
public class VhmEventGraphicTest {
    /** The number of directions available in one dimension. */
    private static final int NUM_DIRECTIONS = 2;

    private Graphics2D mockedGraphics = Mockito.mock(Graphics2D.class);

    /** Arbitrary {@link VhmEvent} and its {@link VhmEventGraphic}. */
    private VhmEvent event = new VhmEvent("testEvent", VhmEventTest.createJsonForCompletelySpecifiedEvent());
    private VhmEventGraphic vhmEventGraphic = new VhmEventGraphic(event);

    @Before
    public void setGraphicScaleTo1() {
        VhmEventGraphic.setScale(1.0);
    }

    @Test
    public void testDrawDrawsRectangleAtEventLocation() {
        this.vhmEventGraphic.draw(this.mockedGraphics);

        verifyRectangleIsDrawnAtEventLocation(mockedGraphics,event,Mockito.atLeastOnce());
    }

    @Test
    public void testDrawWritesEventNameIfEnabled() {
        VhmEventGraphic.setDrawEventName(true);
        this.vhmEventGraphic.draw(this.mockedGraphics);

        Mockito.verify(this.mockedGraphics).drawString(
                this.event.getName(), (int)this.event.getLocation().getX(), (int)this.event.getLocation().getY());
    }

    @Test
    public void testDrawIgnoresEventNameIfDisabled() {
        VhmEventGraphic.setDrawEventName(false);

        Mockito.verify(this.mockedGraphics, Mockito.never()).drawString(
                this.event.getName(), (int)this.event.getLocation().getX(), (int)this.event.getLocation().getY());
    }

    @Test
    public void testDrawDrawsAllRangesIfRangesEnabled() {
        VhmEventGraphic.setDrawAllRanges(true);
        this.vhmEventGraphic.draw(this.mockedGraphics);

        this.checkRangeWasDrawn(this.event.getEventRange(), Mockito.times(1));
        this.checkRangeWasDrawn(this.event.getSafeRange(), Mockito.times(1));
        this.checkRangeWasDrawn(this.event.getMaxRange(), Mockito.times(1));
    }

    @Test
    public void testDrawOnlyDrawsEventRangeIfRangesDisabled() {
        VhmEventGraphic.setDrawAllRanges(false);
        this.vhmEventGraphic.draw(this.mockedGraphics);

        this.checkRangeWasDrawn(this.event.getEventRange(), Mockito.times(1));
        this.checkRangeWasDrawn(this.event.getSafeRange(), Mockito.never());
        this.checkRangeWasDrawn(this.event.getMaxRange(), Mockito.never());
    }

    /**
     * Checks if a circle with the provided radius has been drawn the expected number of times.
     * @param radius The radius to check for.
     * @param mode Expectation on the number of method calls.
     */
    private void checkRangeWasDrawn(double radius, VerificationMode mode) {
        Mockito.verify(this.mockedGraphics, mode).draw(new Ellipse2D.Double(
                this.event.getLocation().getX() - radius,
                this.event.getLocation().getY() - radius,
                radius * VhmEventGraphic.EVENT_RANGE_SIZE_FACTOR,
                radius * VhmEventGraphic.EVENT_RANGE_SIZE_FACTOR));
    }

    @Test
    public void testEqualsNullReturnsFalse() {
        assertFalse("Equals method should have returned false for null.", this.vhmEventGraphic.equals(null));
    }

    @Test
    public void testEqualsNonVhmEventGraphicReturnsFalse() {
        Coord nonVhmEventGraphic = new Coord(0, 0);
        assertFalse(
                "Equals method should have returned false for non VHM event graphic.",
                this.vhmEventGraphic.equals(nonVhmEventGraphic));
    }

    @Test
    public void testEqualsSameEventIdReturnsTrue() {
        VhmEventGraphic startGraphic = new VhmEventGraphic(new VhmEventStartEvent(this.event));
        VhmEventGraphic endGraphic = new VhmEventGraphic(new VhmEventEndEvent(this.event));
        assertTrue(
                "Equals method should have returned true for two VHM event graphics showing a VHM event with same ID.",
                startGraphic.equals(endGraphic));
    }

    @Test
    public void testHashCode() {
        assertEquals(
                "VHM event graphic hash code should have equaled the event's ID.",
                this.event.getID(),
                this.vhmEventGraphic.hashCode());
    }

    public static void verifyRectangleIsDrawnAtEventLocation(Graphics2D mockedGraphics, VhmEvent event,
                                                             VerificationMode usedMode){
        Mockito.verify(mockedGraphics,usedMode).drawRect(
                (int)event.getLocation().getX() - VhmEventGraphic.EVENT_RECT_SIZE / NUM_DIRECTIONS,
                (int)event.getLocation().getY() - VhmEventGraphic.EVENT_RECT_SIZE / NUM_DIRECTIONS,
                VhmEventGraphic.EVENT_RECT_SIZE,
                VhmEventGraphic.EVENT_RECT_SIZE);
    }
}
