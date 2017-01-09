/*
 * Copyright 2016, Michael D. Silva (micdoug.silva@gmail.com)
 * Released under GPLv3. See LICENSE.txt for details.
 */
 
package routing.community;

/**
 * A helper class for the community package that stores a start and end value
 * for some abstract duration. Generally, in this package, the duration being
 * stored is a time duration.
 */
public class Duration
{
	/** The start value */
	public double start;
	
	/** The end value */
	public double end;
	
	/**
	 * Standard constructor that assigns s to start and e to end.
	 * 
	 * @param s Initial start value
	 * @param e Initial end value
	 */
	public Duration(double s, double e) {start = s; end = e;}
}
