package test;

import core.Coord;
import core.DTNHost;
import core.Settings;
import input.WKTMapReader;
import junit.framework.TestCase;
import movement.TransportingMovement;
import movement.map.SimMap;
import movement.MapBasedMovement;
import movement.MovementModel;
import movement.Path;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * 
 * Test class for the TransportingMovement
 * Tests the choice of the next destination and the movement to the next destination, 
 * including a check about staying in bounds.
 * 
 * @author Marcus Nachtigall
 *
 */
public class TransportingMovementTest extends TestCase {
    
    //	   n1       n2       n6        n3
    private final String WKT = "LINESTRING (1.0 1.0, 2.0 1.0, 3.0 1.0, 4.0 1.0) \n" +
    //              n1        n4
    "LINESTRING (1.0 1.0, 1.0 2.0)\n"+
    //              n2       n7       n3       n6
    "LINESTRING (2.0 1.0, 2.0 0.0, 3.0 0.0, 3.0 1.0)\n";
	
    private TestSettings settings;
	private TransportingMovement transport;
    private static final int MAX_COORD = 1000;
    private SimMap map;
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		settings = new TestSettings();
		setupMapData(null, "1,1", null);
		transport = new TransportingMovement(settings, map, 3);
		transport.setHost(setupHost());
		transport.getInitialLocation();
	}
	
	private void setupMapData(String okTypes, String speed, String wTime) {
		Settings.init(null);
		StringReader input = new StringReader(WKT);
		WKTMapReader reader = new WKTMapReader(true);
		try {
			reader.addPaths(input, 0);
		} catch (IOException e) {
			fail(e.toString());
		}
		settings = new TestSettings();
		settings.putSetting(MovementModel.SPEED, (speed != null ? speed : "1,1"));
		settings.putSetting(MovementModel.WAIT_TIME, (wTime != null ? wTime : "0,0"));
		if (okTypes != null) {
			settings.putSetting(MapBasedMovement.MAP_SELECT_S, okTypes);
		}
		map = reader.getMap();
		transport = new TransportingMovement(settings, map, 3);
	}
	
	@Test
	public void testSelectDestination(){
		assert(transport.selectDestination().getLocation().equals(transport.getEventLocation().getLocation()));
		transport.getHost().setLocation(transport.getEventLocation().getLocation());
		assert(transport.selectDestination().getLocation().equals(transport.getTransportDestination().getLocation()));
	}
	
	@Test
    public void testStaysWithinDefaultBounds(){
        Path path = transport.getPath();
        List<Coord> coords = path.getCoords();
        Coord nextWaypoint = coords.get(1);
        assertNotNull(nextWaypoint);
        assert( nextWaypoint.getX()>=0);
        assert( nextWaypoint.getY()>=0);
        assert( nextWaypoint.getX()<=MAX_COORD);
        assert( nextWaypoint.getY()<=MAX_COORD);
    }
	
	@Test
	public void testHostArrivesAtTransportDestination() {
		transport.getHost().setLocation(transport.getInitialLocation());
		Path path = transport.getPath();
		transport.getHost().setLocation(path.getCoords().get(path.getCoords().size() -1));
		assert(transport.getHost().getLocation().equals(transport.getEventLocation().getLocation()));
		path = transport.getPath();
		transport.getHost().setLocation(path.getCoords().get(path.getCoords().size() -1));
		assert(transport.getHost().getLocation().equals(transport.getTransportDestination().getLocation()));
	}
	
	private DTNHost setupHost() {
		TestUtils utils = new TestUtils(null, null, settings);
		DTNHost host = utils.createHost(transport, null);
        host.setLocation(transport.getEventLocation().getLocation());
		return host;
	}
}