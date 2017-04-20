package test;

import core.Coord;
import core.DTNHost;
import core.DisasterData;
import input.DisasterDataNotifier;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Contains tests for the {@link DisasterDataTest} class.
 *
 * Created by Britta Heymann on 06.04.2017.
 */
public class DisasterDataNotifierTest {
    private TestUtils utils = new TestUtils(null, null, new TestSettings());

    @Test
    public void testDataCreated() {
        /* Add listener. */
        RecordingDisasterDataCreationListener recorder = new RecordingDisasterDataCreationListener();
        DisasterDataNotifier.addListener(recorder);

        /* Call dataCreated. */
        DisasterData data = new DisasterData(DisasterData.DataType.RESOURCE, 0, 0, new Coord(0, 0));
        DTNHost host = this.utils.createHost();
        DisasterDataNotifier.dataCreated(host, data);

        /* Make sure it was forwarded to the listener. */
        TestCase.assertEquals(
                "Expected one call to disasterDataCreated.", 1, recorder.getNumDisasterDataCreatedCalls());
        TestCase.assertEquals("Creator should have been different.", host, recorder.getLastCreator());
        TestCase.assertEquals("Data should have been different.", data, recorder.getLastData());
    }
}
