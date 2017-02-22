package gui.playfield;

import core.Coord;
import input.VHMEvent;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Graphics class for VHMEvents
 *
 * Created by Marius Meyer on 17.02.17.
 */
public class VHMEventGraphic extends PlayFieldGraphic {

    private static Color eventRangeColor = Color.lightGray;
    private static Color safeRangeColor = Color.orange;
    private static Color maxRangeColor = Color.yellow;
    private static Color eventLocationColor = Color.red;
    private static Color eventNameColor = Color.black;
    private static Color eventColor = Color.CYAN;

    private static boolean drawAllRanges = true;
    private static boolean drawEventName = true;

    private VHMEvent event;

    public VHMEventGraphic(VHMEvent e){
        this.event = e;
    }

    @Override
    public void draw(Graphics2D g2) {
        drawEvent(g2);
    }

    private void drawEventRange(Graphics2D g2,Coord location, double range, Color c){
        Ellipse2D.Double eventRange = new Ellipse2D.Double(scale(location.getX()-range),
                scale(location.getY()-range), scale(range * 2),
                scale(range * 2));

        g2.setColor(c);
        g2.draw(eventRange);
    }

    private void drawEvent(Graphics2D g2){

        if (drawAllRanges){
            drawEventRange(g2,event.getLocation(),event.getMaxRange(),maxRangeColor);
            drawEventRange(g2,event.getLocation(),event.getSafeRange(),safeRangeColor);
        }

        drawEventRange(g2,event.getLocation(),event.getEventRange(),eventRangeColor);


        if (drawEventName) {
            g2.setColor(eventNameColor);
            // Draw event's identifier next to it
            g2.drawString(event.getIdentifier(), scale(event.getLocation().getX()),
                    scale(event.getLocation().getY()));
        }

		/* draw node rectangle */
        g2.setColor(eventColor);
        g2.drawRect(scale(event.getLocation().getX()-1),scale(event.getLocation().getY()-1),
                scale(2),scale(2));

    }

    @Override
    public boolean equals(Object o){
        if (o instanceof VHMEventGraphic) {
            return ((VHMEventGraphic) o).event.getIdentifier().equals(this.event.getIdentifier());
        }else if (o instanceof VHMEvent){
            return ((VHMEvent)o).getIdentifier().equals(this.event.getIdentifier());
        } else return false;
    }
}
