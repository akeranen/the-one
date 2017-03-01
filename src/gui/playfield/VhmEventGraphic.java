package gui.playfield;

import input.VhmEvent;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Graphics class for VHMEvents
 *
 * Created by Marius Meyer on 17.02.17.
 */
public class VhmEventGraphic extends PlayFieldGraphic {

    /**
     * Color of the circle around the event representing {@link VhmEvent#eventRange}
     */
    private static Color eventRangeColor = Color.lightGray;

    /**
     * Color of the circle around the event representing {@link VhmEvent#safeRange}
     */
    private static Color safeRangeColor = Color.orange;

    /**
     * Color of the circle around the event representing {@link VhmEvent#maxRange}
     */
    private static Color maxRangeColor = Color.yellow;

    /**
     * Color of the event's location. {@link VhmEvent#location}
     */
    private static Color eventLocationColor = Color.red;

    /**
     * Color of the event's name {@link VhmEvent#name}
     */
    private static Color eventNameColor = Color.black;


    /**
     * Boolean that defines, if all ranges should be drawn.
     * If false, only the event range will be drawn.
     */
    private static boolean drawAllRanges = true;

    /**
     * If true, the event's name will be drawn.
     */
    private static boolean drawEventName = true;

    /**
     * The event this graphics class is representing
     */
    private VhmEvent event;

    /**
     * Creates a new event graphics
     * @param e the event this graphics object will represent
     */
    public VhmEventGraphic(VhmEvent e){
        this.event = e;
    }

    @Override
    public void draw(Graphics2D g2) {
        //only draw other ranges, when enabled
        if (drawAllRanges) {
            drawEventRange(g2, event.getMaxRange(), maxRangeColor);
            drawEventRange(g2, event.getSafeRange(), safeRangeColor);
        }

        drawEventRange(g2, event.getEventRange(), eventRangeColor);


        if (drawEventName) {
            g2.setColor(eventNameColor);
            // Draw event's name next to it
            g2.drawString(event.getName(), scale(event.getLocation().getX()),
                    scale(event.getLocation().getY()));
        }

		/* draw node rectangle */
        g2.setColor(eventLocationColor);
        g2.drawRect(scale(event.getLocation().getX() - 1), scale(event.getLocation().getY() - 1),
                scale(2), scale(2));
    }

    /**
     * Draws a range for an event.
     *
     * @param g2 The graphics to draw on
     * @param range The range around the event, that should be drawn
     * @param c The color this range should be drawn with
     */
    private void drawEventRange(Graphics2D g2, double range, Color c){
        Ellipse2D.Double eventRange = new Ellipse2D.Double(scale(event.getLocation().getX()-range),
                scale(event.getLocation().getY()-range), scale(range * 2),
                scale(range * 2));

        g2.setColor(c);
        g2.draw(eventRange);
    }

    /**
     * Sets if  all event ranges should be drawn
     * @param draw if true, all ranges are drawn
     */
    public static void setDrawAllRanges(boolean draw){
        drawAllRanges = draw;
    }

    /**
     * Sets if the event names should be drawn
     * @param draw if true, the event names are drawn
     */
    public static void setDrawEventName(boolean draw){
        drawEventName = draw;
    }

    /**
     * Checks, if a event graphics is equal to another one by comparing the {@link VhmEvent#id}
     * of the events they are representing.
     *
     * @param o the object to compare to
     * @return true, if the objects are representing a event with the same id
     */
    @Override
    public boolean equals(Object o){
        if (o instanceof VhmEventGraphic) {
            return ((VhmEventGraphic) o).event.equals(this.event);
        }else return false;
    }
}
