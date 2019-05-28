/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import java.io.File;
import java.io.PrintWriter;

import junit.framework.TestCase;
import movement.ExternalMovement;
import movement.MovementModel;
import core.Coord;
import core.DTNHost;
import core.SimClock;

public class ExternalMovementTest extends TestCase {
	/* two nodes moving */
	private static final String[] INPUT = {
		"0 130 0 350 0 250 0 0",
		"10 1 10 0",
		"10 2 20 0",
		"20 1 20 10",
		"20 2 300.5 10",
		"30 1 30 10",
		"30 2 40 100",
		"40 1 30 30",
		"40 2 118 150.5",
		"50 1 25 30",
		"50 2 250.8 100",
		"60 1 15.5 20",
		"60 2 280 120",
		"70 1 0 15",
		"70 2 200 150",
		"80 1 0 0",
		"80 2 230 80.8",
		"90 1 12 8",
		"90 2 200 120",
		"100 1 15 15",
		"100 2 150 150.5",
		"110 1 40 60",
		"110 2 200 180",
		"120 1 20 80",
		"120 2 150 250",
		"130 1 30 50",
		"130 2 180 200"
	};

	/* two stationary nodes */
	private static final String[] STATIONARY_INPUT = {
		"0 0 0 0 0 0 0 0",
		"0 1 10 10",
		"0 2 20 20"
	};

	private static final Coord[][] INPUT_COORDS = {
		{ new Coord(10,0),  new Coord(20,10), new Coord(30,10),
		  new Coord(30,30), new Coord(25,30), new Coord(15.5,20),
		  new Coord(0,15), new Coord(0,0), new Coord(12,8),
		  new Coord(15,15), new Coord(40,60), new Coord(20,80),
		  new Coord(30, 50)}, // h1
		{ new Coord(20,0), new Coord(300.5,10), new Coord(40,100),
		  new Coord(118,150.5), new Coord(250.8,100), new Coord(280,120),  
		  new Coord(200,150), new Coord(230,80.8), new Coord(200,120),
		  new Coord(150,150.5), new Coord(200, 180), new Coord(150, 250),
		  new Coord(180, 200)}  // h2
	};
	private static final Coord[] STATIONARY_INPUT_COORDS = {
		new Coord(10,10) , new Coord(20,20)
	};

	private static final double CLOCK_STEP = 10;
	private Coord c0 = new Coord(0,0);
	private DTNHost h1;
	private DTNHost h2;
	private DTNHost h3;
	private SimClock clock;

	protected void setUpUsing(String[] input) throws Exception {
		super.setUp();
		ExternalMovement.reset();
		TestSettings ts = new TestSettings();
		ts.putSetting(MovementModel.MOVEMENT_MODEL_NS + "." +
						MovementModel.WORLD_SIZE, "1000,1000");
		File outFile = File.createTempFile("eMovementTest", ".tmp");
		outFile.deleteOnExit();
		PrintWriter pw = new PrintWriter(outFile);

		for (String s : input) {
			pw.println(s);
		}

		pw.close();

		ts.putSetting(ExternalMovement.EXTERNAL_MOVEMENT_NS + "." +
				ExternalMovement.MOVEMENT_FILE_S, outFile.getAbsolutePath());

		MovementModel emProto = (MovementModel)
			ts.createIntializedObject("movement.ExternalMovement");

		TestUtils utils = new TestUtils(null, null, ts);
		h1 = utils.createHost(emProto, "h1");
		h2 = utils.createHost(emProto, "h2");
		h3 = utils.createHost(emProto, "h3");
		clock = SimClock.getInstance();
		clock.setTime(0);
	}

	public void testMovement() throws Exception {
		setUpUsing(INPUT);

		// h3 should not get any fancy coordinates
		assertEquals(c0, h3.getLocation());
		assertFalse(h3.isMovementActive());

		// test that h1 and h2 move according to input data
		for (int i=0; i<INPUT_COORDS[0].length; i++) {
			assertEquals((i+1) + ". coord of h1",
					INPUT_COORDS[0][i], h1.getLocation());
			assertEquals((i+1) + ". coord of h2",
					INPUT_COORDS[1][i], h2.getLocation());

			clock.advance(CLOCK_STEP);
			moveAllHosts(CLOCK_STEP);
		}
	}

	public void testStationary() throws Exception {
		setUpUsing(STATIONARY_INPUT);

		for (int i=0; i<3; i++) {
			// hosts h1 & h2 should stay in the same place all the time
			assertEquals((i+1) + ". coord of h1",
					STATIONARY_INPUT_COORDS[0], h1.getLocation());
			assertEquals((i+1) + ". coord of h2",
					STATIONARY_INPUT_COORDS[1], h2.getLocation());
			moveAllHosts(CLOCK_STEP);
			// h3 should not get any fancy coordinates
			assertEquals(c0, h3.getLocation());
			assertFalse(h3.isMovementActive());
		}

	}

	public void moveAllHosts(double time) {
		h1.move(time);
		h2.move(time);
		h3.move(time);
	}
}
