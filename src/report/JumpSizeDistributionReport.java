package report;

import core.Coord;
import core.DTNHost;
import core.Settings;
import core.SimScenario;

import java.util.List;

/**
 * <p>Calculates the jump size probability density function. The measure is
 * widely used for characterising human mobility, e.g., in Marta C. Gonzaléz,
 * César A. Hidalgo & Albert-László Barabási, "Understanding individual human
 * mobility patterns", Nature. 5. June 2008.</p>
 *
 *
 * <p>The simulation is sampled at fixed intervals (defined by the
 * '{@link SamplingReport#SAMPLE_INTERVAL_SETTING}' setting), and the
 * distance from the previous sample location is calculated for every active
 * node. I.e., the result is the distribution of displacements at the sampling
 * interval</p>
 *
 * <p>Note that picking too short sample interval will skew the distribution
 * since it limits the maximum observable displacement. For human mobility,
 * the sample interval should be in the order of a few hours to get prevent
 * skewing.</p>
 *
 * <p>Output line format is: "jump_length probability sample_count"</p>
 *
 * @author teemuk
 */
public final class JumpSizeDistributionReport
extends SamplingReport {

	//========================================================================//
	// Constants
	//========================================================================//
	/** Setting to control the number of buckets (bins) used when calculating
	 * the probability density function ({@value}). Default is the square
	 * root of the sample count.*/
	public static final String BUCKET_COUNT_SETTING = "bucketCount";

	/** Set to {@code true} to output the raw jump sizes instead of the
	 * distributions ({@value}).*/
	public static final String RAW_OUTPUT_SETTING = "outputRawData";
	//========================================================================//

	//========================================================================//
	// Instance vars
	//========================================================================//
	private final Coord[] previousLocations;
	private final double[][] samples;
	private final int[] sampleCounts;

	private final int bucketCount;

	private final boolean outputRawData;
	//========================================================================//


	//========================================================================//
	// Report implementation
	//========================================================================//
	public JumpSizeDistributionReport() {
		super();

		final SimScenario simScenario = SimScenario.getInstance();
		final int nodeCount = simScenario.getHosts().size();
		final double simDuration = simScenario.getEndTime();
		final int numSamples = (int) Math.ceil(simDuration / super.interval);

		this.previousLocations = new Coord[nodeCount];
		this.samples = new double[nodeCount][numSamples];
		this.sampleCounts = new int[nodeCount];

		int i = 0;
		for (final DTNHost host : simScenario.getHosts()) {
			if (host.isMovementActive()) {
				this.previousLocations[i] = host.getLocation().clone();
			}
			i++;
		}

		// Read settings
		final Settings settings = super.getSettings();

		// Use sqrt(nr of datapoints) as the default bucket count
		final int defaultBucketCount
				= (int) Math.ceil(Math.sqrt(nodeCount * numSamples));
		this.bucketCount
				= settings.getInt(BUCKET_COUNT_SETTING, defaultBucketCount);

		this.outputRawData = settings.getBoolean(RAW_OUTPUT_SETTING, false);
	}

	@Override
	public final void done() {
		if (this.outputRawData) {
			for (int host = 0; host < this.samples.length; host++) {
				final int sampleCount = this.sampleCounts[host];
				for (int sample = 0; sample < sampleCount; sample++) {
					final double jumpLength = this.samples[host][sample];
					super.write("" + jumpLength);
				}
			}
			super.done();
			return;
		}

		final double maxJumpLength = maxJump(this.samples, this.sampleCounts);
		// Extend the range 1% beyond the maximum value so that the max value
		// falls into the last bucket.
		final double bucketWidth = (1.01 * maxJumpLength) / this.bucketCount;
		final int[] frequencies = new int[this.bucketCount];

		int totalSamples = 0;
		for (int host = 0; host < this.samples.length; host++) {
			final int sampleCount = this.sampleCounts[host];
			totalSamples += sampleCount;
			for (int sample = 0; sample < sampleCount; sample++) {
				final double jumpLength = this.samples[host][sample];
				final int bucketIndex = (int) (jumpLength / bucketWidth);
				frequencies[bucketIndex] += 1;
			}
		}

		for (int i = 0; i < frequencies.length; i++) {
			final double jumpLength = (i + 0.5) * bucketWidth;
			final double density
					= 1.0 * frequencies[i] / (totalSamples * bucketWidth);
			final String densityLine = "" + jumpLength + " " + density + " "
					+ frequencies[i];
			super.write(densityLine);
		}

		super.done();
	}
	//========================================================================//


	//========================================================================//
	// SamplingReport implementation
	//========================================================================//
	@Override
	protected void sample(final List <DTNHost> hosts) {
		for (int i = 0; i < hosts.size(); i++ ) {
			final DTNHost host = hosts.get(i);
			if (host.isMovementActive()) {
				// Update the sampled locations
				final Coord currentLocation = host.getLocation();
				final Coord previousLocation = this.previousLocations[i];
				this.previousLocations[i] = currentLocation.clone();

				// If we have a new jump sample.
				if (previousLocation != null) {
					final double jumpSize
							= currentLocation.distance(previousLocation);

					final int curSample = this.sampleCounts[i];
					this.samples[i][curSample] = jumpSize;
					this.sampleCounts[i]++;
				}
			} else {
				// Clear previous location for inactive nodes.
				this.previousLocations[i] = null;
			}
		}
	}
	//========================================================================//


	//========================================================================//
	// Private
	//========================================================================//
	private static double maxJump(
			final double[][] samples,
			final int[] sampleCounts) {
		double max = 0;
		for (int host = 0; host < samples.length; host++) {
			for (int sample = 0; sample < sampleCounts[host]; sample++) {
				final double candidate = samples[host][sample];
				if (candidate > max) {
					max = candidate;
				}
			}
		}
		return max;
	}
	//========================================================================//
}
