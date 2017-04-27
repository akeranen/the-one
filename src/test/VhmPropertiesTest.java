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

    private static final String HELP_TIME_DIFFERS = "Help time differs from specified one";
    private static final String WAIT_TIME_DIFFERS = "Hospital wait time differs from specified one";
    private static final String INJURY_PROB_DIFFERS = "Injury probability differs from specified one";
    private static final String WAIT_PROB_DIFFERS = "Wait probability differs from specified one";
    private static final String INTESITY_WEIGHT_DIFFERS = "Intensity weight differs from specified one";

    private VhmProperties properties = new VhmProperties(new TestSettings());
    private VhmProperties noDefaultProp = new VhmProperties(VhmTestHelper.createSettingsWithoutDefaultValues());

    @Test
    public void testCopyConstructorCopiesAllProperties(){
        VhmProperties copiedProp = new VhmProperties(noDefaultProp);
        checkPropertiesUseGivenSettings(copiedProp);
    }

    @Test
    public void testPropertiesUseDefaultValuesWhenNoOthersAreGiven(){
        checkPropertiesUseDefaultValues(new VhmProperties(new TestSettings()));
    }

    @Test
    public void testPropertiesUseGivenSettings(){
        checkPropertiesUseGivenSettings(noDefaultProp);
    }

    static void checkPropertiesUseDefaultValues(VhmProperties properties){
        TestCase.assertEquals(HELP_TIME_DIFFERS,
                movement.VhmProperties.DEFAULT_HELP_TIME,properties.getHelpTime(),VhmTestHelper.DELTA);
        TestCase.assertEquals(WAIT_TIME_DIFFERS,
                movement.VhmProperties.DEFAULT_HOSPITAL_WAIT_TIME,
                properties.getHospitalWaitTime(),VhmTestHelper.DELTA);
        TestCase.assertEquals(WAIT_PROB_DIFFERS,
                movement.VhmProperties.DEFAULT_HOSPITAL_WAIT_PROBABILITY,
                properties.getWaitProbability(),VhmTestHelper.DELTA);
        TestCase.assertEquals(INJURY_PROB_DIFFERS,
                movement.VhmProperties.DEFAULT_INJURY_PROBABILITY,
                properties.getInjuryProbability(),VhmTestHelper.DELTA);
        TestCase.assertEquals(INTESITY_WEIGHT_DIFFERS,
                movement.VhmProperties.DEFAULT_INTENSITY_WEIGHT,
                properties.getIntensityWeight(),VhmTestHelper.DELTA);
        TestCase.assertFalse("Node shouldn't be local helper by default",properties.isLocalHelper());
    }

    static void checkPropertiesUseGivenSettings(VhmProperties properties){
        TestCase.assertEquals(HELP_TIME_DIFFERS,
                VhmTestHelper.HELP_TIME,properties.getHelpTime(),VhmTestHelper.DELTA);
        TestCase.assertEquals(WAIT_TIME_DIFFERS,
                VhmTestHelper.HOSPITAL_WAIT_TIME,properties.getHospitalWaitTime(),VhmTestHelper.DELTA);
        TestCase.assertEquals(WAIT_PROB_DIFFERS,
                VhmTestHelper.WAIT_PROBABILITY,properties.getWaitProbability(),VhmTestHelper.DELTA);
        TestCase.assertEquals(INJURY_PROB_DIFFERS,
                VhmTestHelper.INJURY_PROBABILITY,properties.getInjuryProbability(),VhmTestHelper.DELTA);
        TestCase.assertEquals(INTESITY_WEIGHT_DIFFERS,
                VhmTestHelper.INTENSITY_WEIGHT,properties.getIntensityWeight(),VhmTestHelper.DELTA);
        TestCase.assertTrue("Node should be local helper",properties.isLocalHelper());
    }

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
