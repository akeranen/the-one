package report;

import core.Coord;
import core.DTNHost;
import core.SimScenario;

import java.util.List;

/**
 * Calculates the radius of gyration for all the nodes in the simulation.
 *
 * @author teemuk
 */
public final class RadiusOfGyrationReport
extends SamplingReport {

	//========================================================================//
	// Instance vars
	//========================================================================//
	private final double[][][] samples;
	private final int[] sampleCounts;
	private final String[] nodeNames;
	//========================================================================//


	//========================================================================//
	// Report implementation
	//========================================================================//
	public RadiusOfGyrationReport() {
		super();

		final SimScenario simScenario = SimScenario.getInstance();
		final int nodeCount = simScenario.getHosts().size();
		final double simDuration = simScenario.getEndTime();
		final int numSamples = (int) Math.ceil(simDuration / super.interval);

		this.samples = new double[nodeCount][numSamples][2];
		this.sampleCounts = new int[nodeCount];
		this.nodeNames = new String[nodeCount];

		int i = 0;
		for (final DTNHost host : simScenario.getHosts()) {
			this.nodeNames[i] = host.toString();
			i++;
		}
	}

	@Override
	public final void done() {
		final int nodeCount = this.samples.length;

		for (int i = 0; i < nodeCount; i++) {
			final double[][] sample = this.samples[i];
			final int sampleCount = this.sampleCounts[i];
			final double[] geomCenter = geometricCenter(sample, sampleCount);

			double d_squared = 0;
			for (int j = 0; j < sampleCount; j++) {
				final double[] p = sample[j];
				final double d = distance(geomCenter, p);
				d_squared += d * d;
			}

			final double rg = Math.sqrt(1.0 / sampleCount * d_squared);
			super.write(this.nodeNames[i] + " "
					+ geomCenter[0] + " " + geomCenter[1] + " "
					+ rg);
		}

		super.done();
	}
	//========================================================================//


	//========================================================================//
	// SamplingReport implementation
	//========================================================================//
	@Override
	protected void sample(List<DTNHost> hosts) {
		for ( final DTNHost host : hosts ) {
			if ( host.isMovementActive() ) {
				final int nodeId = host.getAddress();
				final Coord location = host.getLocation();
				final double x = location.getX();
				final double y = location.getY();
				final int curSample = this.sampleCounts[nodeId];
				final double[] sample = this.samples[nodeId][curSample];
				sample[0] = x;
				sample[1] = y;
				this.sampleCounts[nodeId]++;
			}
		}
	}
	//========================================================================//


	//========================================================================//
	// Private
	//========================================================================//
	private static double[] geometricCenter(
			final double[][] samples,
			final int sampleCount) {
		double sumX = 0;
		double sumY = 0;
		for (int i = 0; i < sampleCount; i++) {
			sumX += samples[i][0];
			sumY += samples[i][1];
		}
		return new double[] {sumX / sampleCount, sumY / sampleCount};
	}

	private static double distance(
			final double[] p1,
			final double[] p2 ) {
		final double dx = p2[0] - p1[0];
		final double dy = p2[1] - p1[1];
		return Math.sqrt(dx * dx + dy * dy);
	}
	//========================================================================//
}
