package test;

import input.WKTMapReader;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.List;

import junit.framework.*;
import movement.MapBasedMovement;
import movement.MovementModel;
import movement.Path;
import movement.map.MapNode;
import movement.map.SimMap;
import movement.PanicMovement;
import core.Coord;
import core.DTNHost;
import core.Settings;

public class PanicMovementTest extends TestCase {
	/* Topology:  n7--n5
	 *            |   |
	 *        n1--n2--n6--n3
	 *         |
	 *        n4
	 */
	//										   n1       n2       n6        n3
	private final String WKT = "LINESTRING (1.0 1.0, 2.0 1.0, 3.0 1.0, 4.0 1.0) \n" +
	//              n1        n4
	"LINESTRING (1.0 1.0, 1.0 2.0)\n"+
	//              n2       n7       n3       n6
	"LINESTRING (2.0 1.0, 2.0 0.0, 3.0 0.0, 3.0 1.0)\n";
	
	private MapNode n1;
	private Coord c1 = new Coord(1,1);
	private MapNode n2;
	private Coord c2 = new Coord(2,1);
	private MapNode n6;
	private Coord c6 = new Coord(3,1);
	private MapNode n4;
	private Coord c4 = new Coord(1,2);
	private MapNode n5;
	private Coord c5 = new Coord(3,0);
	private Coord event = new Coord (2,1);

	private PanicMovement pm;
	private SimMap map;
	private TestSettings s;

	public void setUp() throws Exception {
		super.setUp();
		s = new TestSettings();
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

		s = new TestSettings();
		s.putSetting(MovementModel.SPEED, (speed != null ? speed : "1,1"));
		s.putSetting(MovementModel.WAIT_TIME, (wTime != null ? wTime : "0,0"));

		if (okTypes != null) {
			s.putSetting(MapBasedMovement.MAP_SELECT_S, okTypes);
		}
		map = reader.getMap();
		// accepts types 1-3, hosts must get in distance 1 to the event
		pm = new PanicMovement(s, map, 3, event, 1, 1); 

		n1 = map.getNodeByCoord(c1);
		n2 = map.getNodeByCoord(c2);
		n4 = map.getNodeByCoord(c4);
		n5 = map.getNodeByCoord(c5);
		n6 = map.getNodeByCoord(c6);
	}
	
	public void testGetPath() {
		setupMapData(null,null,null);
		Coord c,c2;
		pm.getInitialLocation();
		Path path = pm.getPath();
		c = path.getNextWaypoint();
		assertNotNull(c);

		while (path.hasNext()) {
			c2 = path.getNextWaypoint();
			// adjacent nodes are always 1 meter apart (in the test topology)
			assertEquals(1.0, c.distance(c2));
			c = c2;
		}
	}
	
	public void testHostMoving() {
		final int NROF = 15;

		setupMapData(null, "1,1", null);
		DTNHost[] h = new DTNHost[NROF];
		assertNotNull(h);
		
		for (int i=0; i<NROF; i++) {
			h[i] = setupHost();
			Path p = pm.getPath(h[i]);
			List<Coord> waypoints = p.getCoords();
			Coord lastWaypoint = waypoints.get(waypoints.size()-1);
			assert(event.distance(lastWaypoint) >= 1);
		}
	}
	
	private DTNHost setupHost() {
		TestUtils utils = new TestUtils(null, null, s);
		DTNHost h1 = utils.createHost(pm, null);

		h1.move(0); // get a path for the node
		// move node directly to first waypoint
		h1.setLocation(h1.getPath().getCoords().get(0));
		return h1;
	}
}
