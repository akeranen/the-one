package test;

import util.Tuple;

import org.junit.Assert;
import org.junit.Test;

public class TupleTest {
    
    /**
     * Some string object
     */
    private String text = "testString";
    
    /**
     * Another object containing the same string
     */
    private String sameText = "testString";
    
    /**
     * A different object with a different string
     */
    private String otherText = "anotherString";
    
    /**
     * Some integer object
     */
    private Integer number = 3;
    
    /**
     * Another object with the same value
     */
    private Integer sameNumber = 3;
    
    /**
     * A different object with a different value 
     */
    private Integer otherNumber = 2;
    
    /**
     * Tests whether an object is equal to another object that has the same string / integer value
     */
    @Test
    public void testObjectsEqualForEqualObjects() {
        Tuple<String,Integer> tuple1 = new Tuple<String,Integer>(text,number);
        Tuple<String,Integer> tuple2 = new Tuple<String,Integer>(sameText,number);
        
        Assert.assertTrue("Expected objects to be equal!", tuple1.equals(tuple2));
    }
    
    /**
     * Tests whether objects with different values are unequal
     */
    @Test
    public void testObjectsDoNotEqualForDifferentObjects() {
            
        Tuple<String,Integer> tuple1 = new Tuple<String,Integer>(text,number);
        Tuple<String,Integer> tuple2 = new Tuple<String,Integer>(otherText,number);
        Tuple<String,Integer> tuple3 = new Tuple<String,Integer>(text,otherNumber);
        
        Assert.assertFalse("Expected objects to be different!", tuple1.equals(tuple2));
        Assert.assertFalse("Expected objects to be different!", tuple1.equals(tuple3));
    }
    
    /**
     * Tests whether an object has the same hash code as another object that has the same string / integer value
     */
    @Test
    public void testHashCodeEqualsForEqualObjects() {
        Tuple<String,Integer> tuple1 = new Tuple<String,Integer>(text,number);
        Tuple<String,Integer> tuple2 = new Tuple<String,Integer>(sameText,number);
        
        Assert.assertTrue("Expected hash codes to be equal!",tuple1.hashCode() == tuple2.hashCode());
    }
    
    /**
     * Tests whether objects with different values have different hash codes
     */
    @Test
    public void testHashCodeDoesNotEqualForDifferentObjects() {
        
        Tuple<String,Integer> tuple1 = new Tuple<String,Integer>(text,number);
        Tuple<String,Integer> tuple2 = new Tuple<String,Integer>(otherText,number);
        Tuple<String,Integer> tuple3 = new Tuple<String,Integer>(text,otherNumber);
        
        Assert.assertFalse("Expected hash codes to be different!", tuple1.hashCode() == tuple2.hashCode());
        Assert.assertFalse("Expected hash codes to be different!", tuple1.hashCode() == tuple3.hashCode());
    }
    
    /**
     * Tests whether an object is equal to itself
     */
    @Test
    public void testObjectEqualsItself() {
        Tuple<String,Integer> tuple = new Tuple<String,Integer>(text,number);
        
        Assert.assertTrue("Expected object to equal itself!", tuple.equals(tuple));
    }
    
    /**
     * Tests whether an object is equal to null
     */
    @Test
    public void testObjectDoesNotEqualNull() {
        Tuple<String,Integer> tuple = new Tuple<String,Integer>(text,number);
        
        Assert.assertFalse("Expected object to not equal null", tuple.equals(null));
    }
    
    /**
     * Tests whether an object is equal to an object from another class, e.g. to an integer
     */
    @Test
    public void testObjectDoesNotEqualObjectFromOtherClass() {
        Tuple<String,Integer> tuple = new Tuple<String,Integer>(text,number);
        
        Assert.assertFalse("Expected object to not equal null", tuple.equals(new Integer(42)));
    }
}
