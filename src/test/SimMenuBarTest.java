package test;

import gui.SimMenuBar;
import gui.playfield.VhmEventGraphic;
import junit.framework.TestCase;
import org.junit.Test;

import java.awt.event.ActionEvent;

/**
 * Add tests vor the VHM related check boxes in the GUI
 *
 * Created by Marius Meyer on 05.04.17.
 */
public class SimMenuBarTest {

    private SimMenuBar menu = new SimMenuBar(PlayFieldTest.createTestPlayField(),null);

    /**
     * Dummy action event to call actionPerformed
     */
    private ActionEvent event = new ActionEvent(menu.showEventAllRanges,0,"Click");

    @Test
    public void testCheckBoxAllRangesIsChecked(){
        menu.showEventAllRanges.setState(true);
        menu.actionPerformed(event);
        TestCase.assertTrue(VhmEventGraphic.getDrawAllRanges());
    }

    @Test
    public void testCheckBoxAllRangesIsNotChecked(){
        menu.showEventAllRanges.setState(false);
        menu.actionPerformed(event);
        TestCase.assertFalse(VhmEventGraphic.getDrawAllRanges());
    }

    @Test
    public void testCheckBoxEventNameIsChecked(){
        menu.showEventName.setState(true);
        menu.actionPerformed(event);
        TestCase.assertTrue(VhmEventGraphic.getDrawEventName());
    }

    @Test
    public void testCheckBoxEventNameIsNotChecked(){
        menu.showEventName.setState(false);
        menu.actionPerformed(event);
        TestCase.assertFalse(VhmEventGraphic.getDrawEventName());
    }
}
