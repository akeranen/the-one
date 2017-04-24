package test;

import junit.framework.TestCase;
import movement.VhmProperties;
import org.junit.Test;

/**
 * Contains tests for the {@link movement.VhmProperties}
 *
 * Created by Marius Meyer on 21.04.17.
 */
public class VhmPropertiesTest {

    private VhmProperties properties = new VhmProperties(new TestSettings());

    @Test
    public void testGetAndSetLocalHelper(){
        properties.setLocalHelper(true);
        TestCase.assertTrue("local helper should be set",properties.isLocalHelper());
        properties.setLocalHelper(false);
        TestCase.assertFalse("local helper shouldn't be set",properties.isLocalHelper());
    }

    @Test
    public void testGetAndSetInjuryProbability(){
        properties.setInjuryProbability(VhmTestHelper.INJURY_PROBABILITY);
        TestCase.assertEquals("Injury probability should be set to different value",
                VhmTestHelper.INJURY_PROBABILITY,properties.getInjuryProbability(), VhmTestHelper.DELTA);
    }

    @Test
    public void testGetAndSetWaitProbability(){
        properties.setWaitProbability(VhmTestHelper.WAIT_PROBABILITY);
        TestCase.assertEquals("Wait probability should be set to different value",
                VhmTestHelper.WAIT_PROBABILITY, properties.getWaitProbability(), VhmTestHelper.DELTA);
    }

    @Test
    public void testGetAndSetIntensityWeight(){
        properties.setIntensityWeight(VhmTestHelper.INTENSITY_WEIGHT);
        TestCase.assertEquals("Wrong intensity weight was set",
                VhmTestHelper.INTENSITY_WEIGHT,properties.getIntensityWeight());
    }
}
