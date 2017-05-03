package test;

import gui.SimMenuBar;
import gui.playfield.VhmEventGraphic;
import junit.framework.TestCase;
import org.junit.Test;

import java.awt.event.ActionEvent;

/**
 * Add tests vor the {@link movement.VoluntaryHelperMovement} related check boxes in the GUI
 *
 * Created by Marius Meyer on 05.04.17.
 */
public class SimMenuBarTest {

    private SimMenuBar menu = new SimMenuBar(PlayFieldTest.createTestPlayField(),null);

    /**
     * Dummy action event to call actionPerformed
     */
    private ActionEvent event = new ActionEvent(menu.getShowEventAllRanges(),0,"Click");

    @Test
    public void testCheckBoxAllRangesIsChecked(){
        menu.getShowEventAllRanges().setState(true);
        menu.actionPerformed(event);
        TestCase.assertTrue("draw all ranges should be set to true",VhmEventGraphic.getDrawAllRanges());
    }

    @Test
    public void testCheckBoxAllRangesIsNotChecked(){
        menu.getShowEventAllRanges().setState(false);
        menu.actionPerformed(event);
        TestCase.assertFalse("draw all ranges should be set to false",VhmEventGraphic.getDrawAllRanges());
    }

    @Test
    public void testCheckBoxEventNameIsChecked(){
        menu.getShowEventName().setState(true);
        menu.actionPerformed(event);
        TestCase.assertTrue("draw event name should be set to true",VhmEventGraphic.getDrawEventName());
    }

    @Test
    public void testCheckBoxEventNameIsNotChecked(){
        menu.getShowEventName().setState(false);
        menu.actionPerformed(event);
        TestCase.assertFalse("draw event name should be set to false",VhmEventGraphic.getDrawEventName());
    }
}
