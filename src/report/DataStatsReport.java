package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;

/**
 * Message statistics that separate data messages from PoDC control
 * messages (ids starting with {@code PoDC_ACK_}), so that every router is
 * evaluated on the same data traffic. Control relays are reported
 * separately and added to the total overhead.
 *
 * Optional setting {@code DataStatsReport.maliciousPrefix}: host name
 * prefix of malicious nodes; transfers of data messages to such nodes
 * are counted as "absorbed".
 */
public class DataStatsReport extends Report implements MessageListener {
	public static final String ACK_PREFIX = "PoDC_ACK_";

	private Map<String, Double> creationTimes;
	private Set<String> deliveredIds;
	private List<Double> latencies;
	private List<Integer> hopCounts;
	private int created, delivered;
	private long relayedData, relayedCtrl, bytesData, bytesCtrl;
	private long toMalicious, dataTransfersTotal;
	private String maliciousPrefix;

	public DataStatsReport() {
		init();
		Settings s = new Settings("DataStatsReport");
		maliciousPrefix = s.contains("maliciousPrefix")
				? s.getSetting("maliciousPrefix") : null;
	}

	@Override
	protected void init() {
		super.init();
		creationTimes = new HashMap<String, Double>();
		deliveredIds = new HashSet<String>();
		latencies = new ArrayList<Double>();
		hopCounts = new ArrayList<Integer>();
		created = delivered = 0;
		relayedData = relayedCtrl = bytesData = bytesCtrl = 0;
		toMalicious = dataTransfersTotal = 0;
	}

	private static boolean isCtrl(Message m) {
		return m.getId().startsWith(ACK_PREFIX);
	}

	private boolean isMalicious(DTNHost h) {
		return maliciousPrefix != null
				&& h.toString().startsWith(maliciousPrefix);
	}

	public void newMessage(Message m) {
		if (isCtrl(m)) return;
		if (isWarmup()) { addWarmupID(m.getId()); return; }
		creationTimes.put(m.getId(), getSimTime());
		created++;
	}

	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean firstDelivery) {
		if (isWarmupID(m.getId())) return;
		if (isCtrl(m)) {
			relayedCtrl++;
			bytesCtrl += m.getSize();
			return;
		}
		if (!creationTimes.containsKey(m.getId())) return;
		relayedData++;
		bytesData += m.getSize();
		dataTransfersTotal++;
		if (isMalicious(to)) toMalicious++;
		if (firstDelivery && deliveredIds.add(m.getId())) {
			delivered++;
			latencies.add(getSimTime() - creationTimes.get(m.getId()));
			hopCounts.add(m.getHops().size() - 1);
		}
	}

	public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}

	@Override
	public void done() {
		double dr = created > 0 ? (double) delivered / created : 0;
		double ohData = delivered > 0
				? (double) (relayedData - delivered) / delivered : Double.NaN;
		double ohTotal = delivered > 0
				? (double) (relayedData + relayedCtrl - delivered) / delivered
				: Double.NaN;
		double absorbed = dataTransfersTotal > 0
				? (double) toMalicious / dataTransfersTotal : 0;
		write("scenario: " + getScenarioName());
		write("created: " + created);
		write("delivered: " + delivered);
		write("delivery_ratio: " + format(dr));
		write("overhead_data: " + format(ohData));
		write("overhead_total: " + format(ohTotal));
		write("relayed_data: " + relayedData);
		write("relayed_ctrl: " + relayedCtrl);
		write("bytes_data: " + bytesData);
		write("bytes_ctrl: " + bytesCtrl);
		write("latency_avg: " + getAverage(latencies));
		write("latency_med: " + getMedian(latencies));
		write("hopcount_avg: " + getIntAverage(hopCounts));
		write("absorbed_by_malicious: " + format(absorbed));
		super.done();
	}
}
