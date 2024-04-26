/*
Author : Satya Vasanth Reddy
*/
package routing;

import java.lang.Math;
import core.Settings;
import java.lang.*;
import java.util.*;
import core.Application;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SettingsError;
import core.SimClock;
import core.SimError;
import java.util.AbstractMap.SimpleEntry;

public class GreedyGeoRouter extends ActiveRouter {
	public static final int PROXIMITY_THRESHOLD = 15;
	//This is the collection of DTNHosts in each of the six sectors around the router
	private Map<Integer , Collection<AbstractMap.SimpleEntry<DTNHost, Connection>> > sectorMap;
	
	public GreedyGeoRouter(Settings s) {
		super(s);
	}
	
	protected GreedyGeoRouter(GreedyGeoRouter r) {
		super(r);
		this.sectorMap = new HashMap<Integer , Collection<AbstractMap.SimpleEntry<DTNHost, Connection>> >();
	}
			
	@Override
	public void update() {

		super.update();
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}
		//Update the Sector Map
		updateSectorMap();
		List<Message> messages = 
			new ArrayList<Message>(this.getMessageCollection());
		if(messages.size()>0)
		{
			startProtocol(messages);
		}
	}
	
	
	@Override
	public GreedyGeoRouter replicate() {
		return new GreedyGeoRouter(this);
	}
	/**
	*Returns the Eucledian distance between two hosts
	*@param Host1
	*@param Host2
	*/
	public double getDistance(DTNHost h1,DTNHost h2){
		double dx = h1.getLocation().getX() - h2.getLocation().getX();
		double dy = h1.getLocation().getY() - h2.getLocation().getY();
		return Math.sqrt(dx*dx + dy*dy);
	}
	/**
	*Returns the sector number of Host2 in clock-wise direction relative to the Host
	*@param Host2
	*/
	public int getSector(DTNHost h2){
		DTNHost h1 = getHost();
		return (int)(h1.getAngleofHost(h2)/60);
	}

	public void updateSectorMap(){
		sectorMap = new HashMap<Integer , Collection<AbstractMap.SimpleEntry<DTNHost, Connection>> >();
		DTNHost to;
		int sector;
		DTNHost h = getHost();

		for (Connection con : getConnections()) {
		
			to = con.getOtherNode(getHost());
			sector = getSector(to);
			if (!sectorMap.containsKey(sector)){
				sectorMap.put(sector, new HashSet<AbstractMap.SimpleEntry<DTNHost, Connection>>());
			}
			sectorMap.get(sector).add(new AbstractMap.SimpleEntry<DTNHost, Connection>(to, con));
		}
		
	}
	/**
	*Returns the AbstractMap.SimpleEntry of Host and Connection information closer to the destination in a given sector
	*@param Destination Host
	*@param sector number of the destination relative to current host
	*@param The distance between current host and destination
	*/
	public AbstractMap.SimpleEntry<DTNHost, Connection> getACloserHostFromSector(DTNHost destination, int sector, int maxD){
		//System.out.println("getACloserHostFromSector: sector "+sector+", destination "+destination.getAddress());
		Collection<AbstractMap.SimpleEntry<DTNHost, Connection>> sectorList = sectorMap.get(sector);
		DTNHost h ;
		Connection con ;
		if(sectorList==null){
			return null;
		}
		for(AbstractMap.SimpleEntry<DTNHost, Connection> entry : sectorList){
			h = entry.getKey();
			con = entry.getValue();
			if(getDistance(h, destination) <= maxD){
				return entry;
			}
		}
		return null;	
	}
	/**
	*Returns the AbstractMap.SimpleEntry of Host and Connection information closer to the destination.
	*@param Destination Host
	*/

	public AbstractMap.SimpleEntry<DTNHost, Connection> getACloserHost(DTNHost destination){
		int sector = getSector(destination);
		int sl,sr;
		int maxD = (int)getDistance(getHost(), destination);
		AbstractMap.SimpleEntry<DTNHost, Connection> retVal;
		//System.out.println("getACloserHost "+maxD);
		for(int i=0;i<3;i++){
			sl = (sector + i)%6;
			sr = (sector +6 - i)%6;
			retVal = getACloserHostFromSector(destination, sl, maxD);
			if(retVal != null){
				return retVal;
			}
			if(sl != sr){
				retVal = getACloserHostFromSector(destination, sr, maxD);
				if(retVal != null){
					return retVal;
				}
			}
		}
		return null;
	}
	/**
	*Returns if the destination has a connection with the current host
	*@param Destination Host
	*/

	public Connection isDestinationConnected(DTNHost d){
		DTNHost n;
		//System.out.println("isDestinationConnected "+d.getAddress());
		for (Connection con : getConnections()) {
			n = con.getOtherNode(getHost());
			if (d.compareTo(n) == 0){
				//System.out.println("DTNHost is connected");
				return con;
			}
		}
		return null;
	}
	/**
	*Returns if the destination is closer to the current host
	*@param Destination Host
	*/
	public boolean isDestinationClose(DTNHost d){
		if (getDistance(this.getHost(), d) <= PROXIMITY_THRESHOLD){
			//System.out.println("Yes");
		//System.out.println("isDestinationClose ?"+this.getHost().getAddress()+" "+d.getAddress());

			return true;
		}
		//System.out.println("No");
		return false;
	}
	/**
	*Broadcasts the message to all its connections
	*@param Message to be broadcasted
	*@param Message mode
	*/
	public void localBroadCastMessage(Message m, int mode){
		m.setMsgType(mode);
		DTNHost to;
		//todo
		for (Connection con : getConnections()) {
			to = con.getOtherNode(getHost());
			//System.out.println("localBroadcast Message "+m.getId()+" "+m.getTo().getAddress()+" "+m.getFrom().getAddress()+" "+this.getHost().getAddress()+" "+to.getAddress());
		
			startTransfer(m,con);
		}
		return;
	}
	
	/**
	*Sends the message to the destination host depending on the type of the message and the mode
	*@param Message ID
	*@param Destination Host
	*/

	@Override
	public void sendMessage(String id, DTNHost to) {
		Message m = getMessage(id);
		Connection con;
		if((con=isDestinationConnected(to))!=null){
			startTransfer(m, con);
			return;
		}
		int h;
		if(m.getMsgType() == 1){
			h = m.getLocalHops();
			if (h==1){
				return;
			}
			m.setLocalHops(h-1);
			localBroadCastMessage(m, 1);
			return;
		}
		if(isDestinationClose(to)){
			m.setLocalHops(2);
			localBroadCastMessage(m, 1);
			return;
		}
		AbstractMap.SimpleEntry<DTNHost, Connection> tup = getACloserHost(to);
		if(tup == null){

			return;
		}
		startTransfer(m, tup.getValue());
	}

	public void startProtocol(List<Message> messages){
		for(Message m : messages){
			sendMessage(m.getId(),m.getTo());
		}
	}
}	
