package test;

import core.SimScenario;
import gui.DTNSimGUI;
import gui.playfield.PlayField;
import gui.playfield.VhmEventGraphic;
import input.VhmEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.awt.Graphics2D;

/**
 * Contains tests for the VhmListener methods in the PlayField class
 *
 * Created by Marius Meyer on 01.04.17.
 */
public class PlayFieldTest {

    /** The number of directions available in one dimension. */
    private static final int NUM_DIRECTIONS = 2;

    /**
     * graphics for drawing in the tests
     */
    private Graphics2D mockedGraphics = Mockito.mock(Graphics2D.class);

    private PlayField field;
    private VhmEvent event;

    public PlayFieldTest(){
        //set up is done in functions with @before annotation
    }


    @Before
    public void setUpAllScenario(){
        field = createTestPlayField();
        event = VhmEventTest.createVhmEventWithDefaultValues();
        field.vhmEventStarted(event);
    }

    /**
     * Creates a minimal PlayField instance that can be used in tests
     * @return a PlayField instance
     */
    static PlayField createTestPlayField(){
        SimScenario.reset();
        TestSettings.addSettingsToEnableSimScenario(new TestSettings());
        DTNSimGUI gui = new DTNSimGUI();
        return new PlayField(SimScenario.getInstance().getWorld(),gui);
    }


    @Test
    public void testVhmEventStartedAddsAndDrawsAnEvent(){
        field.paint(mockedGraphics);
        Mockito.verify(this.mockedGraphics).drawRect(
                (int)this.event.getLocation().getX() - VhmEventGraphic.EVENT_RECT_SIZE / NUM_DIRECTIONS,
                (int)this.event.getLocation().getY() - VhmEventGraphic.EVENT_RECT_SIZE / NUM_DIRECTIONS,
                VhmEventGraphic.EVENT_RECT_SIZE,
                VhmEventGraphic.EVENT_RECT_SIZE);
    }

    @Test
    public void testVhmEventEndedRemovesAndDoesntDrawAnEvent(){
        field.vhmEventEnded(event);
        field.paint(mockedGraphics);
        Mockito.verify(this.mockedGraphics,Mockito.never()).drawRect(
                (int)this.event.getLocation().getX() - VhmEventGraphic.EVENT_RECT_SIZE / NUM_DIRECTIONS,
                (int)this.event.getLocation().getY() - VhmEventGraphic.EVENT_RECT_SIZE / NUM_DIRECTIONS,
                VhmEventGraphic.EVENT_RECT_SIZE,
                VhmEventGraphic.EVENT_RECT_SIZE);
    }


}
