/* CBR router
 * CBR.java
 *
 * @Author Sujata Pal
 *
 * Created on August 02, 2014
 * Paper name: "Contact-Based Routing in DTNs" published in
 * ACM IMCOM 2015
 */
 
package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import java.util.ArrayList;
import java.util.List;
import util.Tuple;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.*;
import core.*;
import core.ConnectionListener;


public class CBR extends ActiveRouter {
   
   
    protected Map<Integer, ArrayList<Integer>> nodesInf;
    protected ArrayList<Integer> nodeConnections;
   
   
    /**
     * Constructor. Creates a new message router based on the settings in
     * the given Settings object.
     * @param s The settings object
     */
    public CBR(Settings s) {
        super(s);
        init();
       
    }
   
    /**
     * Copy constructor.
     * @param r The router prototype where setting values are copied from
     */
    protected CBR(CBR r) {
        super(r);
        init();
       
    }
   
    private void init() {
         
        this.nodesInf = new TreeMap<Integer, ArrayList<Integer>>();
        this.nodeConnections = new ArrayList<Integer> ();
     }
   
     @Override
    public void changedConnection(Connection con) {
        if (con.isUp()) {


                DTNHost self = this.getHost();
                DTNHost otherNode = con.getOtherNode(self);


                if (! this.nodesInf.containsKey(otherNode.getAddress())) {
                    ArrayList<Integer> items = new ArrayList<Integer>();
                    items.add(1);
                   
                    this.nodesInf.put(otherNode.getAddress(), items);
                }

                if (this.nodesInf.containsKey(otherNode.getAddress())) {     
                    ArrayList<Integer> items = this.nodesInf.get(otherNode.getAddress());

                    items.set(0, items.get(0) + 1);
                                   
                    this.nodesInf.put(otherNode.getAddress(), items);
                }
        }
    }


   
    @Override
    protected int checkReceiving(Message m, DTNHost from) {
        int recvCheck = super.checkReceiving(m, from);
       
        if (recvCheck == RCV_OK) {
            /* don't accept a message that has already traversed this node */
            if (m.getHops().contains(getHost())) {
                recvCheck = DENIED_OLD;
            }
        }
       
        return recvCheck;
    }
           
    @Override
    public void update() {
        super.update();
        //if(SimClock.getIntTime() == 43200)
            //System.out.println("[" + this.getHost() + "]  list: " + this.nodesInf + "\n");
        if (isTransferring() || !canStartTransfer()) {
            return;
        }
       
        if (exchangeDeliverableMessages() != null) {
            return;
        }
       
        tryAllMessagesToAllConnections();
    }
   
   
    @Override
    protected Connection tryMessagesToConnections(List<Message> messages,
            List<Connection> connections) {

        for (Message m : messages) {
           
            DTNHost msgDst = m.getTo();
            DTNHost self = this.getHost();
            int maxEnc = 0;
            DTNHost maxEncHost;
            Connection conEnc = connections.get(0);
            int k=0;
            for (int i=0, n=connections.size(); i<n; i++) {
                Connection con = connections.get(i);
                DTNHost otherNode = con.getOtherNode(self);
               
                CBR otherRouter = (CBR) otherNode.getRouter();
                        

                //if other end of the connection contain (meet with the) destination node of the current msg
                if (otherRouter.nodesInf.containsKey(msgDst.getAddress())) {
                    ArrayList<Integer> items = otherRouter.nodesInf.get(msgDst.getAddress());                   
                   
                    int encounter = items.get(0);
                    if (encounter > maxEnc)
                    {
                        maxEnc = encounter;
                        maxEncHost = otherNode;
                        conEnc = con;
                    }

                }

                Random r = new Random();
                k = r.nextInt(n);
               
            }
            if(maxEnc == 0)
            {   
                conEnc = connections.get(k);
            }
            int retVal = startTransfer(m, conEnc);
            if (retVal == RCV_OK) {
                return conEnc;    // accepted a message, don't try others
            }
            else if (retVal > 0) {
                return null; // should try later -> don't bother trying others
            }
        }

       
        return null;
    }
   
   
       
    @Override
    public CBR replicate() {
        return new CBR(this);
    }

}
