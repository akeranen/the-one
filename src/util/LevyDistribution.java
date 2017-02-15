package util;

import java.util.Random;

/**
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Mark N. Read, 2016.
 *
 * Represents a Levy distributed random variable. This is calculated using the method outlined in
 * Jacobs's "Stochastic Processes for Physicists". 2010, Cambridge University Press.
 *
 *
 * @author Mark N. Read
 *
 * Edited by Melanie Bruns to use Java.util.Random instead of needing the Mason simulation library.
 * Mason library can be found at http://cs.gmu.edu/~eclab/projects/mason/
 *
 * @author Melanie Bruns
 *
 */
public class LevyDistribution
{
	private static Random rnd = new Random();

	private LevyDistribution(){
		//Empty constructor so no one tries to make an object from this util class
	}
	private static double boundedUniform(double low, double high)
	{
		// returns a double in inverval (0,1). IE, neither zero nor one will be returned. 		
		double x = nextDouble();

		double range = high - low;
		x *= range;	// scale onto the required range of values
		x += low;	// translate x onto the values requested

		return x;
	}

	/**
	 * Samples a Levy distribution wherein the power law decay can be adjusted between 1/x and 1/x^3.
	 *
	 * This method is based on that found in section 9.2.2 of 
	 * Jacobs's "Stochastic Processes for Physicists". 2010, Cambridge University Press.  
	 *
	 * Note that this sampling method can return negative values. Values are symmetrical around zero.
	 *
	 * @param mu must lie between 1 and 3. Corresponds to 1/x and 1/x^3
	 * @return A levy-distributed double. May be negative.
	 */
	private static double sample(double mu)
	{
		double x = boundedUniform(-Math.PI/2.0, Math.PI/2.0);
		double y = -Math.log(nextDouble());
		double alpha = mu - 1.0;
		// there's a lot going on here, written over several lines to aid clarity.
		return 	(	Math.sin(alpha * x)
				/
				Math.pow( Math.cos(x) , 1.0 / alpha )
		)
				*
				Math.pow(
						Math.cos((1.0-alpha) * x) / y,
						(1.0 - alpha) / alpha)
				;
	}

	/**
	 * Same as above, but ensures all values are positive. Negative values are simply negated, as the Levy distribution
	 * represented is symmetrical around zero.
	 */
	public static double samplePositive(double mu, double scale)
	{
		double l = sample(mu) * scale;
		if (l < 0.0)
		{	return -1.0 * l;	}
		return l;
	}

	/** Default value case, scale=1 */
	public static double samplePositive(double mu)
	{	return samplePositive(mu, 1.0);		}


    /**
     * Custom random double generator to avoid using
     * another library just for that
     * @return A double value uniformly chosen from (0,1)
     */
	private static double nextDouble(){
		//Use Java.Util.Random to generate values between 0 and 1
		double nextDouble=rnd.nextDouble();

		//We have trouble with 0 and 1 so we check if we got one of those
		if (nextDouble<=0 && nextDouble >=1){
			return nextDouble;
		}
		else{
			//In the unlikely case we got precisely 0 or 1, we just try again until we find another value
			return nextDouble();
		}
	}

}
