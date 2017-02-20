package test;

import core.Coord;
import core.DTNHost;
import core.Settings;
import movement.TransportingMovement;
import movement.Path;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class TransportingMovementTest {
    
	private TransportingMovement transport = new TransportingMovement(new TestSettings());
    private static final int MAX_COORD = 1000;
    private static final int TEST_RUNS = 2000;
    private TestSettings setting;
	
	@Before
    public void setUp(){
        Settings.init(null);
        java.util.Locale.setDefault(java.util.Locale.US);
        TestSettings testSettings = new TestSettings();
        transport = new TransportingMovement(testSettings);
        transport.getInitialLocation();
        setting = new TestSettings();
    }
	
   @Test
    public void testGetPath(){
        Path path = transport.getPath();
        assertNotNull(path);
        List<Coord> coords = path.getCoords();
        //Get the first point on the path
        Coord nextWaypoint = coords.get(1);
        assertNotNull(nextWaypoint);
        //Bounds in test Settings are 1000x1000
        assert( nextWaypoint.getX()>=0);
        assert( nextWaypoint.getY()>=0);
        assert( nextWaypoint.getX()<=MAX_COORD);
        assert( nextWaypoint.getY()<=MAX_COORD);
    }
	
	@Test
    public void testStaysWithinDefaultBounds(){
        for (int i=1; i<TEST_RUNS; i++){
            Path path = transport.getPath();
            List<Coord> coords = path.getCoords();
            Coord nextWaypoint = coords.get(1);
            assertNotNull(nextWaypoint);
            //Bounds in test Settings are 1000x1000
            assert( nextWaypoint.getX()>=0);
            assert( nextWaypoint.getY()>=0);
            assert( nextWaypoint.getX()<=MAX_COORD);
            assert( nextWaypoint.getY()<=MAX_COORD);
        }
    }
	
	@Test
	public void testHostMoving() {
		DTNHost host = setupHost();
		Path path;
		while(host.getLocation() != transport.getTransportDestination().getLocation()){
			path = transport.getPath();
			host.setLocation(path.getCoords().get(path.getCoords().size() -1));
		}
		assert(host.getLocation().equals(transport.getTransportDestination().getLocation()));
	}
	
	private DTNHost setupHost() {
		TestUtils utils = new TestUtils(null, null, setting);
		DTNHost host = utils.createHost(transport, null);

		host.move(0); // get a path for the node
		// move node directly to first waypoint
		host.setLocation(host.getPath().getCoords().get(0));
		return host;
	}
}