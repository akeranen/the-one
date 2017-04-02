package test;

import core.World;
import input.VhmEvent;

import java.util.ArrayList;

/**
 * Contains common code for testing {@link input.VhmEventStartEvent} and {@link input.VhmEventEndEvent}.
 *
 * Created by Britta Heymann on 02.04.2017.
 */
public class ProcessableVhmEventTest {
    /** Delta used when asserting double equality. */
    protected static final double DOUBLE_COMPARING_DELTA = 0.01;

    /** A world object needed to process the event. */
    protected World world = new World(
            new ArrayList<>(), 0, 0,1, new ArrayList<>(),false, new ArrayList<>());

    /** A simple {@link input.VhmEvent}. */
    protected VhmEvent event =  VhmEventTest.createVhmEventWithDefaultValues();

    protected ProcessableVhmEventTest() {
        // This class should not be instantiated.
    }
}
