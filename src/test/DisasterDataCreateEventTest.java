package test;

import core.Coord;
import core.DTNHost;
import core.DisasterData;
import core.UpdateListener;
import core.World;
import input.DisasterDataCreateEvent;
import input.DisasterDataNotifier;
import input.EventQueue;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the {@link DisasterDataCreateEvent} class.
 *
 * Created by Britta Heymann on 05.04.2017.
 */
public class DisasterDataCreateEventTest {
    /** Number of {@link DTNHost}s in the scenario. */
    private static final int NUMBER_HOSTS = 3;

    /* Properties of the data used in tests. */
    private static final DisasterData.DataType TYPE = DisasterData.DataType.RESOURCE;
    private static final int CREATOR_ADDRESS = 0;
    private static final int SIZE = 350;
    private static final double TIME = 20.4;
    private static final Coord OFFSET = new Coord(2, 3);

    /** World used in tests. */
    private World world;
    private static final int WORLD_WIDTH = 100;
    private static final int WORLD_HEIGHT = 50;

    /** Object recording created disaster data. */
    private RecordingDisasterDataCreationListener recorder = new RecordingDisasterDataCreationListener();

    /** {@link DisasterDataCreateEvent} used in tests. */
    private DisasterDataCreateEvent event = new DisasterDataCreateEvent(CREATOR_ADDRESS, TYPE, SIZE, OFFSET, TIME);

    public DisasterDataCreateEventTest() {
        // Constructor logic is handled in @Before methods.
        // This constructor is added to fix S1258 (SonarQube issue).
    }

    @Before
    public void setUp() {
        /* Create hosts. */
        DTNHost.reset();
        TestUtils utils = new TestUtils(null, null, new TestSettings());
        List<DTNHost> hosts = new ArrayList<>(NUMBER_HOSTS);
        for (int i = 0; i < NUMBER_HOSTS; i++) {
            hosts.add(utils.createHost());
        }

        /* Create a world object. */
        this.world = new World(
                hosts,
                WORLD_WIDTH,
                WORLD_HEIGHT,
                1,
                new ArrayList<UpdateListener>(),
                false,
                new ArrayList<EventQueue>());

        /* Add a disaster data creation recorder */
        DisasterDataNotifier.addListener(this.recorder);
    }

    @Test
    public void testTimeIsSetCorrectly() {
        TestCase.assertEquals("Time is not as expected.", TIME, this.event.getTime());
    }

    @Test
    public void testProcessEventCreatesData() {
        this.event.processEvent(this.world);
        TestCase.assertEquals(
                "Different number of data creations than expected.",
                1,
                this.recorder.getNumDisasterDataCreatedCalls());
    }

    @Test
    public void testProcessEventCreatesDataWithCorrectProperties() {
        this.event.processEvent(this.world);

        DisasterData data = this.recorder.getLastData();
        DTNHost creator = this.recorder.getLastCreator();
        Coord creatorLocation = creator.getLocation();

        TestCase.assertEquals("Creator address is not as expected.", CREATOR_ADDRESS, creator.getAddress());
        TestCase.assertEquals("Type is not as expected.", TYPE, data.getType());
        TestCase.assertEquals("Size is not as expected.", SIZE, data.getSize());
        TestCase.assertEquals("Creation time is not as expected.", TIME, data.getCreation());
        TestCase.assertEquals(
                "Location is not as expected.",
                new Coord(creatorLocation.getX() + OFFSET.getX(), creatorLocation.getY() + OFFSET.getY()),
                data.getLocation());
    }

    @Test
    public void testToString() {
        java.util.Locale.setDefault(java.util.Locale.US);
        TestCase.assertEquals(
                "ToString returned unexpected string.",
                "DATA @20.40 RESOURCE size:350 offset:(2.0,3.0) CREATE by host 0",
                this.event.toString());
    }
}
