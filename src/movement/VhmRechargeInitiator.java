package movement;

import core.DTNHost;
import core.UpdateListener;

import java.util.List;

/**
 * Responsible for initiating recharging of {@link DTNHost}s in the {@link VoluntaryHelperMovement}: This class
 * makes sure that at each update interval and for each host, we check whether its battery should be recharged.
 *
 * Created by Britta Heymann on 18.06.2017.
 */
public class VhmRechargeInitiator implements UpdateListener {
    /**
     * Method is called on every update cycle.
     *
     * @param hosts A list of all hosts in the world
     */
    @Override
    public void updated(List<DTNHost> hosts) {
        for (DTNHost host : hosts) {
            MovementModel movement = host.getMovement();
            if (movement instanceof VoluntaryHelperMovement) {
                ((VoluntaryHelperMovement)movement).possiblyRecharge();
            }
        }
    }
}
