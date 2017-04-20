package test;

import core.SimScenario;
import gui.DTNSimGUI;
import gui.playfield.PlayField;
import input.VhmEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.awt.Graphics2D;

/**
 * Contains tests for the VhmListener methods in the {@link PlayField} class
 *
 * Created by Marius Meyer on 01.04.17.
 */
public class PlayFieldTest {

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
    public void setUpPlayFieldAndCallVhmEventStartedMethod(){
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
        return new PlayField(SimScenario.getInstance().getWorld(),new DTNSimGUI());
    }

    @Test
    public void testPaintDrawsEventAfterEventStart(){
        field.paint(mockedGraphics);
        VhmEventGraphicTest.verifyRectangleIsDrawnAtEventLocation(mockedGraphics,event,Mockito.atLeastOnce());
    }

    @Test
    public void testPaintDoesntDrawEventAfterEventEnd(){
        field.vhmEventEnded(event);
        field.paint(mockedGraphics);

        VhmEventGraphicTest.verifyRectangleIsDrawnAtEventLocation(mockedGraphics,event,Mockito.never());
    }
}
