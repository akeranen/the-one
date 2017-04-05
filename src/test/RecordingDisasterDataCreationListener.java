package test;

import core.DTNHost;
import core.DisasterData;
import input.DisasterDataCreateEvent;
import input.DisasterDataCreationListener;

/**
 * A {@link input.DisasterDataCreationListener} that simply records calls to its method.
 *
 * Created by Britta Heymann on 05.04.2017.
 */
public class RecordingDisasterDataCreationListener implements DisasterDataCreationListener {
    private int numDisasterDataCreatedCalls;
    private DTNHost lastCreator;
    private DisasterData lastData;

    RecordingDisasterDataCreationListener() {
        this.numDisasterDataCreatedCalls = 0;
        this.lastCreator = null;
        this.lastData = null;
    }

    /**
     * Called when a {@link DisasterData} got created by a {@link DisasterDataCreateEvent}.
     *
     * @param by   {@link DTNHost} that created the data.
     * @param data The created {@link DisasterData}.
     */
    @Override
    public void disasterDataCreated(DTNHost by, DisasterData data) {
        numDisasterDataCreatedCalls++;
        this.lastCreator = by;
        this.lastData = data;
    }


    int getNumDisasterDataCreatedCalls() {
        return this.numDisasterDataCreatedCalls;
    }

    DTNHost getLastCreator() {
        return this.lastCreator;
    }

    DisasterData getLastData() {
        return this.lastData;
    }
}
