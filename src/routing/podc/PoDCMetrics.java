package routing.podc;

import core.DTNHost;
import core.SimClock;
import core.SimScenario;
import routing.MessageRouter;
import routing.PoDCRouter;
import routing.SybilPoDCRouter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulation-wide statistics for PoDC runs. This class only observes;
 * routing decisions never read from it.
 */
public final class PoDCMetrics {

    private static final PoDCMetrics INSTANCE = new PoDCMetrics();
    public static PoDCMetrics get() { return INSTANCE; }
    private PoDCMetrics() { reset(); }

    private static final int BIN = 60;

    private final Map<String, PoDCRouter> byId = new HashMap<String, PoDCRouter>();
    private int created, delivered, decrypted;
    private long acksProcessed, invalidAcks;
    private final List<Double> latencies = new ArrayList<Double>();
    private final List<Integer> hops = new ArrayList<Integer>();
    private final List<Integer> proofBytes = new ArrayList<Integer>();
    private final List<double[]> series = new ArrayList<double[]>();
    private int[] binForwards = new int[2];
    private int lastBin = 0;
    private boolean flushed;

    public void reset() {
        byId.clear();
        created = delivered = decrypted = 0;
        acksProcessed = invalidAcks = 0;
        latencies.clear(); hops.clear(); proofBytes.clear();
        series.clear();
        binForwards = new int[2];
        lastBin = 0;
        flushed = false;
    }

    public void register(String id, PoDCRouter r) { byId.put(id, r); }

    public void recordCreated() { created++; }
    public void recordAckProcessed() { acksProcessed++; }
    public void recordInvalidAck() { invalidAcks++; }

    public void recordDelivered(double latency, int nHops, int bytes,
                                boolean ok, List<ProofEntry> chain) {
        delivered++;
        if (ok) decrypted++;
        latencies.add(latency);
        hops.add(nHops);
        proofBytes.add(bytes);
    }

    public void recordDataForward(double t, DTNHost to) {
        binForwards[0]++;
        if (to.getRouter() instanceof SybilPoDCRouter) binForwards[1]++;
    }

    private static boolean isAttacker(PoDCRouter r) {
        return r instanceof SybilPoDCRouter;
    }

    /**
     * Mean work of attackers and of honest nodes as seen by honest
     * observers, and mean score ratio attacker/observer.
     */
    private double[] snapshot() {
        double att = 0, hon = 0;
        int na = 0, nh = 0;
        List<PoDCRouter> all = new ArrayList<PoDCRouter>(byId.values());
        for (PoDCRouter obs : all) {
            if (isAttacker(obs)) continue;
            for (PoDCRouter tgt : all) {
                if (tgt == obs) continue;
                double w = obs.observedWork(tgt.getNodeId());
                if (isAttacker(tgt)) { att += w; na++; }
                else { hon += w; nh++; }
            }
        }
        return new double[]{na > 0 ? att / na : 0, nh > 0 ? hon / nh : 0};
    }

    /** Called from every router update; samples once per bin. */
    public void tick() {
        double t = SimClock.getTime();
        int bin = (int) (t / BIN);
        if (bin > lastBin) {
            double[] s = snapshot();
            double frac = binForwards[0] > 0
                    ? (double) binForwards[1] / binForwards[0] : 0;
            series.add(new double[]{lastBin * BIN, s[0], s[1],
                    binForwards[0], binForwards[1], frac});
            binForwards = new int[2];
            lastBin = bin;
        }
        SimScenario sc = SimScenario.getInstance();
        if (!flushed && sc != null && t + 0.001 >= sc.getEndTime()) {
            flushed = true;
            flush(sc.getName());
        }
    }

    /** Gini coefficient of honest nodes' work averaged over observers. */
    private double giniConsensusWork() {
        List<PoDCRouter> all = new ArrayList<PoDCRouter>(byId.values());
        List<Double> w = new ArrayList<Double>();
        for (PoDCRouter tgt : all) {
            if (isAttacker(tgt)) continue;
            double s = 0; int n = 0;
            for (PoDCRouter obs : all) {
                if (obs == tgt || isAttacker(obs)) continue;
                s += obs.observedWork(tgt.getNodeId()); n++;
            }
            w.add(n > 0 ? s / n : 0);
        }
        return gini(w);
    }

    private void flush(String name) {
        String dir = "reports/podc";
        new File(dir).mkdirs();
        try (PrintWriter pw = new PrintWriter(new FileWriter(dir + "/" + name + "_summary.txt"))) {
            pw.println("created: " + created);
            pw.println("delivered: " + delivered);
            pw.println("decrypted: " + decrypted);
            pw.println("acks_processed: " + acksProcessed);
            pw.println("invalid_acks: " + invalidAcks);
            pw.println("avg_latency: " + avg(latencies));
            pw.println("avg_proof_hops: " + avgI(hops));
            pw.println("avg_proof_bytes: " + avgI(proofBytes));
            pw.println("gini_work: " + giniConsensusWork());
        } catch (IOException e) {
            System.err.println("PoDCMetrics: " + e);
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(dir + "/" + name + "_series.csv"))) {
            pw.println("time,attacker_work,honest_work,forwards,forwards_to_attackers,frac_to_attackers");
            for (double[] r : series) {
                pw.printf(java.util.Locale.ROOT, "%.0f,%.6f,%.6f,%.0f,%.0f,%.6f%n",
                        r[0], r[1], r[2], r[3], r[4], r[5]);
            }
        } catch (IOException e) {
            System.err.println("PoDCMetrics: " + e);
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(dir + "/" + name + "_per_node.csv"))) {
            pw.println("host,attacker,consensus_work,forwards");
            List<PoDCRouter> all = new ArrayList<PoDCRouter>(byId.values());
            for (PoDCRouter tgt : all) {
                double s = 0; int n = 0;
                for (PoDCRouter obs : all) {
                    if (obs == tgt || isAttacker(obs)) continue;
                    s += obs.observedWork(tgt.getNodeId()); n++;
                }
                pw.printf(java.util.Locale.ROOT, "%s,%d,%.6f,%d%n", tgt.getHostName(),
                        isAttacker(tgt) ? 1 : 0, n > 0 ? s / n : 0,
                        tgt.getNodeForwards());
            }
        } catch (IOException e) {
            System.err.println("PoDCMetrics: " + e);
        }
    }

    private static double avg(List<Double> v) {
        double s = 0;
        for (double d : v) s += d;
        return v.isEmpty() ? 0 : s / v.size();
    }

    private static double avgI(List<Integer> v) {
        double s = 0;
        for (int d : v) s += d;
        return v.isEmpty() ? 0 : s / v.size();
    }

    static double gini(List<Double> values) {
        int n = values.size();
        if (n == 0) return 0;
        List<Double> sorted = new ArrayList<Double>(values);
        Collections.sort(sorted);
        double sum = 0, cum = 0;
        for (int i = 0; i < n; i++) {
            sum += sorted.get(i);
            cum += sorted.get(i) * (i + 1);
        }
        if (sum == 0) return 0;
        return (2.0 * cum) / (n * sum) - (n + 1.0) / n;
    }
}
