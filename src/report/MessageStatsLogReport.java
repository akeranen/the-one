/*
 * Copyright (C) 2016 micha
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.UpdateListener;
import java.util.ArrayList;
import java.util.List;
import routing.MessageRouter;
import routing.SamplerRouter;

/**
 *
 * @author micha
 */
public class MessageStatsLogReport extends Report implements MessageListener, UpdateListener {

    private int relayed = 0;
    private int delivered = 0;
    private int dropped = 0;

    public MessageStatsLogReport() {
        init();
        write("Time(h) Relayed Delivered Dropped");
    }

    @Override
    public void newMessage(Message m) {
        // Nothing to do here

    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        // Nothing to do here

    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (dropped) {
            this.dropped++;
        }

    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        // Nothing to do here

    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {

        this.relayed++;
        if (firstDelivery) {
            this.delivered++;
        }
//            if (from.getRouter() instanceof SamplerRouter) {
//                SamplerRouter router = (SamplerRouter) from.getRouter();
//                if (router.isRelayPoint()) {
//                    System.out.println("Relay point " + from.getAddress() + " transmitiu " + m.getId() + " para " + to.getAddress());
//                }
//            }
//            if (to.getRouter() instanceof SamplerRouter) {
//                SamplerRouter router = (SamplerRouter) to.getRouter();
//                if (router.isRelayPoint()) {
//                    System.out.println("Relay point " + to.getAddress() + " recebeu mensagem " + m.getId() + " e tem o buffer com " + to.getNrofMessages());
//                }
//            }
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        int curTime = SimClock.getIntTime();
        int temp = (int) Math.ceil(SimClock.getTime());
        if (curTime == temp && ((int) curTime) % 3600 == 0) {
            int hour = SimClock.getIntTime() / 3600;
            write("" + hour + " " + relayed + " " + delivered + " " + dropped);
        }
    }

}
