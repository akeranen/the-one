/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package test;

import junit.framework.TestCase;
import core.Coord;

public class CoordTest extends TestCase {

	public void testHashCode() {
		Coord c1 = new Coord(1,1);
		Coord c2 = new Coord(1,1);
		Coord c3 = new Coord(2,3);
		Coord c4 = new Coord(3,2);
		Coord c5 = new Coord(-2,-3);
		Coord c6 = new Coord(-2,-3);
		
		assertTrue(c1.hashCode() == c2.hashCode());
		assertTrue(c1.hashCode() != c3.hashCode());
		assertTrue(c3.hashCode() != c4.hashCode());
		assertTrue(c5.hashCode() == c6.hashCode());
		assertTrue(c3.hashCode() != c5.hashCode());
		
		c5.translate(1, 1);
		assertTrue(c5.hashCode() != c6.hashCode());
		
	}

}
