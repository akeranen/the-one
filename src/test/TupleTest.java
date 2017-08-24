package test;

import util.Tuple;

import org.junit.Assert;
import org.junit.Test;

public class TupleTest {
	
	private String text = "testString";
	private String sameText = "testString";
	private String otherText = "anotherString";
	private Integer number = 3;
	
	@Test
	public void testObjectsEqualForEqualObjects() {
		Tuple<String,Integer> tuple1 = new Tuple<String,Integer>(text,number);
		Tuple<String,Integer> tuple2 = new Tuple<String,Integer>(sameText,number);
		
		Assert.assertTrue("Expected objects to be equal!", tuple1.equals(tuple2));
	}
	
	@Test
	public void testObjectsDoNotEqualForDifferentObjects() {
			
		Tuple<String,Integer> tuple1 = new Tuple<String,Integer>(text,number);
		Tuple<String,Integer> tuple2 = new Tuple<String,Integer>(otherText,number);
		
		Assert.assertFalse("Expected objects to be different!", tuple1.equals(tuple2));
	}
	
	@Test
	public void testHashCodeEqualsForEqualObjects() {
		Tuple<String,Integer> tuple1 = new Tuple<String,Integer>(text,number);
		Tuple<String,Integer> tuple2 = new Tuple<String,Integer>(sameText,number);
		
		Assert.assertTrue("Expected hash codes to be equal!",tuple1.hashCode() == tuple2.hashCode());
	}
	
	@Test
	public void testHashCodeDoesNotEqualForDifferentObjects() {
		
		Tuple<String,Integer> tuple1 = new Tuple<String,Integer>(text,number);
		Tuple<String,Integer> tuple2 = new Tuple<String,Integer>(otherText,number);
		
		Assert.assertFalse("Expected hash codes to be different!", tuple1.hashCode() == tuple2.hashCode());
	}
}
