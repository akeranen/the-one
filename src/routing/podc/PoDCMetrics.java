package routing.podc;

import core.DTNHost;
import core.SimClock;
import core.SimScenario;
import routing.MessageRouter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects per-simulation PoDC metrics and writes a single CSV line when
 * {@link #flush(String)} is called (typically at the end of the run).
 *
 * <p>The CSV header is written automatically if the file does not yet exist.
 * Subsequent runs append rows, making it easy to aggregate batch results.</p>
 *
 * <h3>Collected metrics</h3>
 * <ul>
 *   <li>delivery_ratio, overhead_ratio, avg_latency, avg_hops —
 *       standard DTN performance indicators</li>
 *   <li>gini_work — Gini coefficient over node work values (fairness)</li>
 *   <li>avg_proof_bytes — mean proof chain size for delivered messages</li>
 *   <li>total_forwarded, total_rejected, total_acks, total_invalid_acks —
 *       PoDC-specific counters</li>
 * </ul>
 */
public final class PoDCMetrics {

    /* ---- singleton ---- */
    private static final PoDCMetrics INSTANCE = new PoDCMetrics();
    public static PoDCMetrics get() { return INSTANCE; }
    private PoDCMetrics() { reset(); }

    /* ---- raw accumulators ---- */
    private int  created;
    private int  delivered;
    private long relayed;
    private long rejected;
    private long acksProcessed;
    private long invalidAcks;

    private final List<Double>  latencies  = new ArrayList<Double>();
    private final List<Integer> hopCounts  = new ArrayList<Integer>();
    private final List<Integer> proofBytes = new ArrayList<Integer>();

    /* ---- recording API (called from PoDCRouter) ---- */

    public void recordCreated()                 { created++; }
    public void recordDelivered(double latency,
                                int hops, int proofSize) {
        delivered++;
        latencies.add(latency);
        hopCounts.add(hops);
        proofBytes.add(proofSize);
    }
    public void recordForwarded()               { relayed++; }
    public void recordRejected()                { rejected++; }
    public void recordAckProcessed()            { acksProcessed++; }
    public void recordInvalidAck()              { invalidAcks++; }

    /* ---- computed metrics ---- */

    public double deliveryRatio() {
        return created == 0 ? 0 : (double) delivered / created;
    }

    public double overheadRatio() {
        return delivered == 0 ? Double.NaN
                : (double)(relayed - delivered) / delivered;
    }

    public double avgLatency() {
        return avg(latencies);
    }

    public double avgHops() {
        double s = 0;
        for (int h : hopCounts) s += h;
        return hopCounts.isEmpty() ? 0 : s / hopCounts.size();
    }

    public double avgProofBytes() {
        double s = 0;
        for (int b : proofBytes) s += b;
        return proofBytes.isEmpty() ? 0 : s / proofBytes.size();
    }

    /**
     * Gini coefficient over the {@code work} values of all PoDC routers
     * currently in the scenario.  0 = perfectly equal, 1 = maximally unequal.
     */
    public double giniWork() {
        SimScenario sc = SimScenario.getInstance();
        if (sc == null) return 0;
        List<Double> works = new ArrayList<Double>();
        for (DTNHost h : sc.getHosts()) {
            MessageRouter r = h.getRouter();
            if (r instanceof routing.PoDCRouter) {
                works.add(((routing.PoDCRouter) r).getWork());
            }
        }
        return gini(works);
    }

    /* ---- CSV output ---- */

    private static final String HEADER =
            "scenario,sim_time,nodes,created,delivered,delivery_ratio,"
          + "overhead_ratio,avg_latency,avg_hops,gini_work,"
          + "avg_proof_bytes,total_forwarded,total_rejected,"
          + "total_acks,total_invalid_acks";

    /**
     * Appends one CSV row to {@code reports/podc_results.csv}.
     * @param scenarioName label for this run (e.g. from Settings)
     */
    public void flush(String scenarioName) {
        String dir = "reports";
        new File(dir).mkdirs();
        String path = dir + "/podc_results.csv";
        boolean needsHeader = !new File(path).exists();
        try (PrintWriter pw = new PrintWriter(
                new FileWriter(path, true))) {
            if (needsHeader) pw.println(HEADER);
            int nodes = 0;
            SimScenario sc = SimScenario.getInstance();
            if (sc != null) nodes = sc.getHosts().size();
            pw.printf("%s,%.1f,%d,%d,%d,%.6f,%.4f,%.2f,%.2f,%.6f,%.1f,%d,%d,%d,%d%n",
                    scenarioName,
                    SimClock.getTime(),
                    nodes,
                    created,
                    delivered,
                    deliveryRatio(),
                    overheadRatio(),
                    avgLatency(),
                    avgHops(),
                    giniWork(),
                    avgProofBytes(),
                    relayed,
                    rejected,
                    acksProcessed,
                    invalidAcks);
        } catch (IOException e) {
            System.err.println("PoDCMetrics: cannot write CSV — " + e);
        }
    }

    /** Print a human-readable summary to stdout. */
    public void printSummary() {
        System.out.println("--- PoDC metrics summary ---");
        System.out.printf("  created          : %d%n", created);
        System.out.printf("  delivered        : %d%n", delivered);
        System.out.printf("  delivery_ratio   : %.4f%n", deliveryRatio());
        System.out.printf("  overhead_ratio   : %.4f%n", overheadRatio());
        System.out.printf("  avg_latency (s)  : %.2f%n", avgLatency());
        System.out.printf("  avg_hops         : %.2f%n", avgHops());
        System.out.printf("  gini_work        : %.6f%n", giniWork());
        System.out.printf("  avg_proof_bytes  : %.1f%n", avgProofBytes());
        System.out.printf("  total_forwarded  : %d%n", relayed);
        System.out.printf("  total_rejected   : %d%n", rejected);
        System.out.printf("  total_acks       : %d%n", acksProcessed);
        System.out.printf("  total_invalid    : %d%n", invalidAcks);
    }

    /** Clears all counters (called between batch runs). */
    public void reset() {
        created = delivered = 0;
        relayed = rejected = acksProcessed = invalidAcks = 0;
        latencies.clear();
        hopCounts.clear();
        proofBytes.clear();
    }

    /* ---- helpers ---- */

    private static double avg(List<Double> v) {
        if (v.isEmpty()) return 0;
        double s = 0;
        for (double d : v) s += d;
        return s / v.size();
    }

    static double gini(List<Double> values) {
        int n = values.size();
        if (n == 0) return 0;
        List<Double> sorted = new ArrayList<Double>(values);
        Collections.sort(sorted);
        double sum = 0, cumSum = 0;
        for (int i = 0; i < n; i++) {
            sum    += sorted.get(i);
            cumSum += sorted.get(i) * (i + 1);
        }
        if (sum == 0) return 0;
        return (2.0 * cumSum) / (n * sum) - (n + 1.0) / n;
    }
}
