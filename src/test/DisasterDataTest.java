package test;

import core.DisasterData;
import core.Coord;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.Locale;

/**
 * Contains tests for the {@link DisasterData} class.
 *
 * Created by Britta Heymann on 05.04.2017.
 */
public class DisasterDataTest {
    private static final double DOUBLE_COMPARING_DELTA = 0.001;

    /* Properties of the data used in tests. */
    private static final DisasterData.DataType TYPE = DisasterData.DataType.RESOURCE;
    private static final int SIZE = 350;
    private static final double CREATION = 20.4;
    private static final Coord LOCATION = new Coord(2, 3);

    /* Data used in tests.*/
    private DisasterData data;

    public DisasterDataTest() {
        java.util.Locale.setDefault(java.util.Locale.US);
        this.data = new DisasterData(TYPE, SIZE, CREATION, LOCATION);
        java.util.Locale.setDefault(Locale.US);
    }

    @Test
    public void testGetType() {
        TestCase.assertEquals("Type should have been different.", TYPE, this.data.getType());
    }

    @Test
    public void testGetSize() {
        TestCase.assertEquals("Size should have been different.", SIZE, this.data.getSize());
    }

    @Test
    public void testGetCreation() {
        TestCase.assertEquals(
                "Creation time should have been different.", CREATION, this.data.getCreation(), DOUBLE_COMPARING_DELTA);
    }

    @Test
    public void testGetLocation() {
        TestCase.assertEquals("Location should have been different.", LOCATION, this.data.getLocation());
    }

    @Test
    public void testToString() {
        TestCase.assertEquals(
                "String representation was not as expected.", "RESOURCE@20.40@(2.00,3.00)", this.data.toString());
    }
}
