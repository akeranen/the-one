/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import core.Coord;

import static org.junit.Assert.*;
import org.junit.Test;

public class CoordTest{

    @Test
    public void testHashCode() {
        final Coord c1 = new Coord(1,1);
        final Coord c2 = new Coord(1,1);
        final Coord c3 = new Coord(2,3);
        final Coord c4 = new Coord(3,2);
        final Coord c5 = new Coord(-2,-3);
        final Coord c6 = new Coord(-2,-3);

        assertEquals("Hash function has to be consistent", c1.hashCode(), c1.hashCode());
        assertEquals("Hash for two coordinate with the same values should be equal", c1.hashCode(), c2.hashCode());
        assertNotEquals("Hash for different coordinate should be different.",c1.hashCode(), c3.hashCode());
        assertNotEquals("Hash should depend on coordinate ordering.", c3.hashCode(), c4.hashCode());
        assertEquals("Hash for two coordinate with the same values should be equal",c5.hashCode(), c6.hashCode());
        assertNotEquals("Hash value should be different for positive and negative values", c3.hashCode(),c5.hashCode());

        //Move point to see if the hash changes
        c5.translate(1, 1);
        assertNotEquals("Cached hash must be recomputed after translation", c5.hashCode(), c6.hashCode());

    }

}
