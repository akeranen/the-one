#!/usr/bin/env python3
"""
Publication-quality charts for Scopus paper.
Horizontal bar style, narrow bars, no text overlap, vector PDF output.
7 protocols: PoDC, Epidemic, Prophet, SprayAndWait, MaxProp, FirstContact, DirectDelivery.
"""

import csv
import glob
import os
import sys
from collections import defaultdict

try:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    import matplotlib.ticker as mticker
    import numpy as np
except ImportError:
    print("ERROR: matplotlib and numpy required.  pip install matplotlib numpy")
    sys.exit(1)

# ── Global style ─────────────────────────────────────────────────────

plt.rcParams.update({
    "font.family": "sans-serif",
    "font.sans-serif": ["Arial", "Helvetica", "DejaVu Sans"],
    "font.size": 9,
    "axes.labelsize": 10,
    "axes.titlesize": 11,
    "axes.titleweight": "bold",
    "legend.fontsize": 8,
    "legend.framealpha": 0.9,
    "xtick.labelsize": 8,
    "ytick.labelsize": 8,
    "figure.dpi": 300,
    "savefig.bbox": "tight",
    "savefig.pad_inches": 0.08,
    "axes.spines.top": False,
    "axes.spines.right": False,
    "axes.grid": True,
    "grid.alpha": 0.15,
    "grid.linewidth": 0.5,
})

CSV_PATH = os.path.join("reports", "full_results.csv")
OUT_DIR  = "reports"

PALETTE = {
    "PoDC":           "#1B9E77",
    "Epidemic":       "#D95F02",
    "Prophet":        "#7570B3",
    "SprayAndWait":   "#E7298A",
    "MaxProp":        "#66A61E",
    "FirstContact":   "#E6AB02",
    "DirectDelivery": "#A6761D",
}
PROTOCOL_ORDER = [
    "PoDC", "Epidemic", "Prophet", "SprayAndWait",
    "MaxProp", "FirstContact", "DirectDelivery",
]
SHORT_NAMES = {
    "PoDC": "PoDC",
    "Epidemic": "Epidemic",
    "Prophet": "Prophet",
    "SprayAndWait": "S&W",
    "MaxProp": "MaxProp",
    "FirstContact": "FirstCont.",
    "DirectDelivery": "DirectDel.",
}
NODE_COUNTS = [50, 100, 150]


def save_fig(fig, name):
    for ext in ("pdf", "png"):
        fig.savefig(os.path.join(OUT_DIR, f"{name}.{ext}"), dpi=300)
    plt.close(fig)
    print(f"  -> {name}.pdf/.png")


# ── Data loading ─────────────────────────────────────────────────────

def load_csv():
    rows = []
    with open(CSV_PATH, newline="", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        for r in reader:
            for k in ("nodes", "seed", "created", "delivered", "relayed"):
                if r.get(k) and r[k].strip():
                    r[k] = int(r[k].strip())
            for k in ("delivery_prob", "overhead_ratio", "latency_avg",
                       "hopcount_avg"):
                v = r.get(k, "").strip()
                r[k] = float(v) if v and v != "NaN" else None
            rows.append(r)
    return rows


def ci95(values):
    vals = [v for v in values if v is not None]
    if not vals:
        return 0.0, 0.0
    m = sum(vals) / len(vals)
    if len(vals) < 2:
        return m, 0.0
    s = (sum((x - m) ** 2 for x in vals) / (len(vals) - 1)) ** 0.5
    return m, 1.96 * s / (len(vals) ** 0.5)


def mean_std(values):
    vals = [v for v in values if v is not None]
    if not vals:
        return 0.0, 0.0
    m = sum(vals) / len(vals)
    if len(vals) < 2:
        return m, 0.0
    s = (sum((x - m) ** 2 for x in vals) / (len(vals) - 1)) ** 0.5
    return m, s


# ══════════════════════════════════════════════════════════════════════
# 1-4. Horizontal grouped bar charts (protocols x node counts)
# ══════════════════════════════════════════════════════════════════════

def plot_grouped_bars(data, metric_key, xlabel, title, filename):
    protos = [p for p in PROTOCOL_ORDER
              if any(r.get("protocol") == p for r in data)]
    n_proto = len(protos)
    if n_proto == 0:
        print(f"  (no data for {filename})")
        return

    bar_h = 0.11
    gap = 0.06
    group_h = n_proto * bar_h + gap

    fig_h = max(3.5, len(NODE_COUNTS) * group_h + 1.2)
    fig, ax = plt.subplots(figsize=(7, fig_h))

    y_positions = np.arange(len(NODE_COUNTS))

    for i, proto in enumerate(protos):
        means, errs = [], []
        for nc in NODE_COUNTS:
            vals = [r[metric_key] for r in data
                    if r.get("protocol") == proto and r.get("nodes") == nc]
            m, e = ci95(vals)
            means.append(m)
            errs.append(e)

        offset = (i - n_proto / 2 + 0.5) * bar_h
        bars = ax.barh(y_positions + offset, means, bar_h * 0.85,
                        label=SHORT_NAMES.get(proto, proto),
                        color=PALETTE.get(proto, "#999"),
                        edgecolor="white", linewidth=0.4)
        for bar, m_val in zip(bars, means):
            if m_val > 0:
                ax.text(m_val + max(means) * 0.015,
                        bar.get_y() + bar.get_height() / 2,
                        f"{m_val:.2f}", va="center", ha="left", fontsize=6.5)

    ax.set_yticks(y_positions)
    ax.set_yticklabels([f"N={n}" for n in NODE_COUNTS])
    ax.set_xlabel(xlabel)
    ax.set_title(title)
    ax.legend(loc="lower right", ncol=2, fontsize=7)
    ax.invert_yaxis()
    fig.tight_layout()
    save_fig(fig, filename)


# ══════════════════════════════════════════════════════════════════════
# 5. Sybil comparison
# ══════════════════════════════════════════════════════════════════════

def plot_sybil(rows):
    clean_vals = [r["delivery_prob"] for r in rows
                  if r["protocol"] == "PoDC" and r["scenario"] == "base"
                  and r.get("nodes") == 126]
    sybil_vals = [r["delivery_prob"] for r in rows
                  if r["protocol"] == "PoDC" and r["scenario"] == "sybil20"]
    if not sybil_vals:
        print("  (no sybil data)")
        return
    if not clean_vals:
        clean_vals = [r["delivery_prob"] for r in rows
                      if r["protocol"] == "PoDC" and r["scenario"] == "base"
                      and r.get("nodes") == 100]

    labels = ["PoDC clean\n(N=126)", "PoDC + 20% Sybil\n(N=126)"]
    data_sets = [clean_vals, sybil_vals]
    means, errs = [], []
    for d in data_sets:
        m, e = ci95(d)
        means.append(m)
        errs.append(e)

    fig, ax = plt.subplots(figsize=(5, 3.5))
    colors = ["#1B9E77", "#D95F02"]
    bars = ax.barh(labels, means,
                    color=colors, edgecolor="white", height=0.4)
    for bar, m_val in zip(bars, means):
        ax.text(m_val + 0.005, bar.get_y() + bar.get_height() / 2,
                f"{m_val:.4f}", va="center", ha="left", fontsize=9)
    ax.set_xlabel("Delivery ratio")
    ax.set_title("Sybil attack impact on delivery ratio")
    ax.invert_yaxis()
    fig.tight_layout()
    save_fig(fig, "chart_sybil")


# ══════════════════════════════════════════════════════════════════════
# 6. Work evolution
# ══════════════════════════════════════════════════════════════════════

def plot_work_evolution():
    ts_files = glob.glob(os.path.join(OUT_DIR, "PoDC_n*_work_timeseries.csv"))
    if not ts_files:
        print("  (no work_timeseries files found)")
        return

    all_data = defaultdict(list)
    for fpath in ts_files:
        with open(fpath, newline="") as f:
            reader = csv.DictReader(f)
            for row in reader:
                t = int(row["time"])
                total = int(row["total_forwards"])
                frac = float(row["fraction_experienced"])
                if total > 0:
                    all_data[t].append(frac)

    if not all_data:
        return

    times = sorted(all_data.keys())
    means, lo, hi = [], [], []
    for t in times:
        vals = all_data[t]
        m = sum(vals) / len(vals)
        means.append(m)
        if len(vals) >= 2:
            s = (sum((x - m) ** 2 for x in vals) / (len(vals) - 1)) ** 0.5
            ci = 1.96 * s / (len(vals) ** 0.5)
        else:
            ci = 0
        lo.append(m - ci)
        hi.append(m + ci)

    fig, ax = plt.subplots(figsize=(6.5, 3.5))
    ax.plot(times, means, color="#1B9E77", linewidth=1.8,
            label="Fraction of forwards by nodes with work > 0")
    ax.fill_between(times, lo, hi, alpha=0.18, color="#1B9E77")
    ax.axhline(y=0.5, color="#888", linestyle="--", linewidth=0.7,
               label="Random baseline (50%)")
    ax.set_xlabel("Simulation time (s)")
    ax.set_ylabel("Fraction of forwards")
    ax.set_title("Natural selection: experienced nodes dominate forwarding")
    ax.set_ylim(0, 1.05)
    ax.legend(loc="lower right", fontsize=7)
    fig.tight_layout()
    save_fig(fig, "chart_work_evolution")


# ══════════════════════════════════════════════════════════════════════
# 7. CDF of per-message latency (N=100, all protocols)
# ══════════════════════════════════════════════════════════════════════

def load_delay_reports(pattern):
    files = glob.glob(os.path.join(OUT_DIR, pattern))
    delays = []
    for f in files:
        with open(f, encoding="utf-8", errors="replace") as fp:
            for line in fp:
                line = line.strip()
                if line.startswith("#") or not line:
                    continue
                parts = line.split()
                if len(parts) >= 1:
                    try:
                        delays.append(float(parts[0]))
                    except ValueError:
                        pass
    return delays


def plot_cdf_latency():
    fig, ax = plt.subplots(figsize=(6.5, 4))
    has_data = False

    for proto in PROTOCOL_ORDER:
        pattern = f"{proto}_n100_s*_MessageDelayReport.txt"
        delays = load_delay_reports(pattern)
        if not delays:
            continue
        has_data = True
        delays.sort()
        n = len(delays)
        cdf = np.arange(1, n + 1) / n
        ax.plot(delays, cdf, linewidth=1.3,
                label=SHORT_NAMES.get(proto, proto),
                color=PALETTE.get(proto, "#999"))

    if not has_data:
        print("  (no MessageDelayReport files for N=100)")
        plt.close(fig)
        return

    ax.set_xlabel("Message delivery latency (s)")
    ax.set_ylabel("CDF")
    ax.set_title("Cumulative distribution of delivery latency (N=100)")
    ax.legend(loc="lower right", fontsize=7)
    ax.set_xlim(left=0)
    ax.set_ylim(0, 1.05)
    fig.tight_layout()
    save_fig(fig, "chart_cdf_latency")


# ══════════════════════════════════════════════════════════════════════
# 8. Boxplot of delivery latency (N=100)
# ══════════════════════════════════════════════════════════════════════

def plot_boxplot_latency():
    data_lists = []
    labels = []

    for proto in PROTOCOL_ORDER:
        pattern = f"{proto}_n100_s*_MessageDelayReport.txt"
        delays = load_delay_reports(pattern)
        if delays:
            data_lists.append(delays)
            labels.append(SHORT_NAMES.get(proto, proto))

    if not data_lists:
        print("  (no delay data for boxplot)")
        return

    fig, ax = plt.subplots(figsize=(7, 3.5))
    bp = ax.boxplot(data_lists, vert=False, patch_artist=True,
                    showmeans=True,
                    meanprops=dict(marker="D", markerfacecolor="white",
                                   markeredgecolor="black", markersize=4),
                    medianprops=dict(color="black", linewidth=1.2),
                    flierprops=dict(marker=".", markersize=2, alpha=0.3),
                    widths=0.5)

    proto_keys = [p for p in PROTOCOL_ORDER
                  if SHORT_NAMES.get(p, p) in labels]
    colors = [PALETTE.get(p, "#999") for p in proto_keys]
    for patch, c in zip(bp["boxes"], colors):
        patch.set_facecolor(c)
        patch.set_alpha(0.65)

    ax.set_yticklabels(labels)
    ax.set_xlabel("Delivery latency (s)")
    ax.set_title("Distribution of delivery latency (N=100)")
    fig.tight_layout()
    save_fig(fig, "chart_boxplot_latency")


# ══════════════════════════════════════════════════════════════════════
# 9. Gini coefficient evolution
# ══════════════════════════════════════════════════════════════════════

def plot_gini_evolution():
    gini_files = glob.glob(os.path.join(OUT_DIR, "PoDC_n*_gini_timeseries.csv"))
    if not gini_files:
        print("  (no gini_timeseries files found)")
        return

    all_data = defaultdict(list)
    for fpath in gini_files:
        with open(fpath, newline="") as f:
            reader = csv.DictReader(f)
            for row in reader:
                t = int(row["time"])
                g = float(row["gini"])
                if g > 0:
                    all_data[t].append(g)

    if not all_data:
        print("  (empty gini data)")
        return

    times = sorted(all_data.keys())
    means, lo, hi = [], [], []
    for t in times:
        vals = all_data[t]
        m = sum(vals) / len(vals)
        means.append(m)
        if len(vals) >= 2:
            s = (sum((x - m) ** 2 for x in vals) / (len(vals) - 1)) ** 0.5
            ci = 1.96 * s / (len(vals) ** 0.5)
        else:
            ci = 0
        lo.append(max(0, m - ci))
        hi.append(min(1, m + ci))

    fig, ax = plt.subplots(figsize=(6.5, 3.5))
    ax.plot(times, means, color="#7570B3", linewidth=1.8, label="Gini coefficient")
    ax.fill_between(times, lo, hi, alpha=0.18, color="#7570B3")
    ax.set_xlabel("Simulation time (s)")
    ax.set_ylabel("Gini coefficient of work")
    ax.set_title("Fairness of work distribution over time")
    ax.set_ylim(0, 1.0)
    ax.legend(loc="upper right", fontsize=7)
    fig.tight_layout()
    save_fig(fig, "chart_gini_evolution")


# ══════════════════════════════════════════════════════════════════════
# 10. dropThreshold sensitivity
# ══════════════════════════════════════════════════════════════════════

def plot_sensitivity(rows):
    thresholds = [0.3, 0.5, 0.7]
    scenario_map = {"drop0.3": 0.3, "base": 0.5, "drop0.7": 0.7}

    means_dr, errs_dr = [], []
    means_oh, errs_oh = [], []

    for thr in thresholds:
        scen_key = [k for k, v in scenario_map.items() if v == thr]
        vals_dr, vals_oh = [], []
        for sk in scen_key:
            if sk == "base":
                vals_dr += [r["delivery_prob"] for r in rows
                            if r["protocol"] == "PoDC" and r["scenario"] == "base"
                            and r.get("nodes") == 100]
                vals_oh += [r["overhead_ratio"] for r in rows
                            if r["protocol"] == "PoDC" and r["scenario"] == "base"
                            and r.get("nodes") == 100]
            else:
                vals_dr += [r["delivery_prob"] for r in rows
                            if r["protocol"] == "PoDC" and r["scenario"] == sk]
                vals_oh += [r["overhead_ratio"] for r in rows
                            if r["protocol"] == "PoDC" and r["scenario"] == sk]

        m_dr, e_dr = ci95(vals_dr)
        m_oh, e_oh = ci95(vals_oh)
        means_dr.append(m_dr)
        errs_dr.append(e_dr)
        means_oh.append(m_oh)
        errs_oh.append(e_oh)

    if not any(m > 0 for m in means_dr):
        print("  (no sensitivity data)")
        return

    fig, ax1 = plt.subplots(figsize=(5.5, 3.5))
    x = np.arange(len(thresholds))
    w = 0.28

    bars1 = ax1.bar(x - w / 2, means_dr, w,
                     color="#1B9E77", label="Delivery ratio", edgecolor="white")
    ax1.set_ylabel("Delivery ratio", color="#1B9E77")
    ax1.tick_params(axis="y", labelcolor="#1B9E77")

    ax2 = ax1.twinx()
    bars2 = ax2.bar(x + w / 2, means_oh, w,
                     color="#D95F02", label="Overhead ratio", edgecolor="white")
    ax2.set_ylabel("Overhead ratio", color="#D95F02")
    ax2.tick_params(axis="y", labelcolor="#D95F02")
    ax2.spines["right"].set_visible(True)

    ax1.set_xticks(x)
    ax1.set_xticklabels([str(t) for t in thresholds])
    ax1.set_xlabel("dropThreshold parameter")
    ax1.set_title("Sensitivity of PoDC to dropThreshold (N=100)")

    for bar, m_val in zip(bars1, means_dr):
        ax1.text(bar.get_x() + bar.get_width() / 2, bar.get_height(),
                 f"{m_val:.3f}", ha="center", va="bottom", fontsize=7)
    for bar, m_val in zip(bars2, means_oh):
        ax2.text(bar.get_x() + bar.get_width() / 2, bar.get_height(),
                 f"{m_val:.1f}", ha="center", va="bottom", fontsize=7)

    lines1, labels1 = ax1.get_legend_handles_labels()
    lines2, labels2 = ax2.get_legend_handles_labels()
    ax1.legend(lines1 + lines2, labels1 + labels2, loc="upper left", fontsize=7)

    fig.tight_layout()
    save_fig(fig, "chart_sensitivity_drop")


# ══════════════════════════════════════════════════════════════════════
# 11. Proof chain size histogram + scatter
# ══════════════════════════════════════════════════════════════════════

def plot_proof_size():
    pm_files = glob.glob(os.path.join(OUT_DIR, "PoDC_n*_per_message.csv"))
    if not pm_files:
        print("  (no per_message files for proof size)")
        return

    proof_bytes_all, hops_all = [], []
    for fpath in pm_files:
        with open(fpath, newline="") as f:
            reader = csv.DictReader(f)
            for row in reader:
                pb = int(row["proof_bytes"])
                h = int(row["hops"])
                if pb > 0:
                    proof_bytes_all.append(pb)
                    hops_all.append(h)

    if not proof_bytes_all:
        print("  (no proof data)")
        return

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(9, 3.5))

    ax1.hist(proof_bytes_all, bins=25, color="#1B9E77", edgecolor="white",
             alpha=0.8)
    ax1.set_xlabel("Proof chain size (bytes)")
    ax1.set_ylabel("Number of messages")
    ax1.set_title("Distribution of proof sizes")
    ax1.axvline(np.mean(proof_bytes_all), color="#D95F02", linestyle="--",
                linewidth=1.2, label=f"Mean = {np.mean(proof_bytes_all):.0f} B")
    ax1.legend(fontsize=7)

    ax2.scatter(hops_all, proof_bytes_all, alpha=0.25, s=8, color="#7570B3",
                edgecolors="none")
    ax2.set_xlabel("Number of hops")
    ax2.set_ylabel("Proof chain size (bytes)")
    ax2.set_title("Proof size vs. hop count")

    fig.tight_layout()
    save_fig(fig, "chart_proof_size")


# ══════════════════════════════════════════════════════════════════════
# 12. Work vs delivery contributions scatter
# ══════════════════════════════════════════════════════════════════════

def load_per_node_data():
    pn_files = glob.glob(os.path.join(OUT_DIR, "PoDC_n*_per_node.csv"))
    records = []
    for fpath in pn_files:
        with open(fpath, newline="") as f:
            reader = csv.DictReader(f)
            for row in reader:
                records.append({
                    "node_id": row["node_id"],
                    "work": float(row["work"]),
                    "score": float(row["score"]),
                    "connectivity": float(row["connectivity"]),
                    "forwards": int(row["forwards"]),
                    "delivery_contributions": int(row["delivery_contributions"]),
                })
    return records


def plot_work_scatter():
    nodes = load_per_node_data()
    if not nodes:
        print("  (no per-node data for scatter)")
        return

    works = [n["work"] for n in nodes]
    contribs = [n["delivery_contributions"] for n in nodes]

    fig, ax = plt.subplots(figsize=(5.5, 4))
    ax.scatter(works, contribs, alpha=0.25, s=10, color="#1B9E77",
               edgecolors="none")
    ax.set_xlabel("Accumulated work (reputation)")
    ax.set_ylabel("Delivery contributions")
    ax.set_title("Work vs. delivery contributions per node")

    if len(works) > 2 and max(works) > 0:
        z = np.polyfit(works, contribs, 1)
        p = np.poly1d(z)
        xs = np.linspace(0, max(works), 100)
        ax.plot(xs, p(xs), color="#D95F02", linestyle="--", linewidth=1.3,
                label=f"Linear fit (slope={z[0]:.1f})")
        corr = np.corrcoef(works, contribs)[0, 1]
        ax.legend(title=f"Pearson r = {corr:.3f}", fontsize=7)

    fig.tight_layout()
    save_fig(fig, "chart_work_scatter")


# ══════════════════════════════════════════════════════════════════════
# 13. Score quintile vs delivery success rate
# ══════════════════════════════════════════════════════════════════════

def plot_score_quintile():
    nodes = load_per_node_data()
    if not nodes:
        print("  (no per-node data for quintile)")
        return

    nodes_with_fwd = [n for n in nodes if n["forwards"] > 0]
    if len(nodes_with_fwd) < 5:
        print("  (too few forwarding nodes for quintile)")
        return

    nodes_with_fwd.sort(key=lambda n: n["score"])
    n = len(nodes_with_fwd)
    q_size = n // 5
    quintiles = []
    for qi in range(5):
        start = qi * q_size
        end = start + q_size if qi < 4 else n
        group = nodes_with_fwd[start:end]
        total_fwd = sum(nd["forwards"] for nd in group)
        total_contrib = sum(nd["delivery_contributions"] for nd in group)
        avg_score = sum(nd["score"] for nd in group) / len(group)
        rate = total_contrib / total_fwd if total_fwd > 0 else 0
        quintiles.append({
            "label": f"Q{qi + 1}\n({avg_score:.3f})",
            "rate": rate,
        })

    fig, ax = plt.subplots(figsize=(6, 3.5))
    labels_q = [q["label"] for q in quintiles]
    rates = [q["rate"] for q in quintiles]

    gradient = [plt.cm.viridis(x) for x in np.linspace(0.2, 0.85, 5)]
    bars = ax.barh(labels_q, rates, color=gradient, edgecolor="white", height=0.5)
    for bar, rate in zip(bars, rates):
        ax.text(rate + 0.005, bar.get_y() + bar.get_height() / 2,
                f"{rate:.3f}", va="center", ha="left", fontsize=8)

    ax.set_xlabel("Delivery contribution rate (contributions / forwards)")
    ax.set_title("Score quintile vs. delivery effectiveness")
    ax.invert_yaxis()
    fig.tight_layout()
    save_fig(fig, "chart_score_quintile")


# ══════════════════════════════════════════════════════════════════════
# Summary table
# ══════════════════════════════════════════════════════════════════════

def build_summary_table(rows):
    lines = []
    lines.append("=" * 115)
    lines.append(f"{'Protocol':<15} {'Scenario':<12} {'Nodes':<7} "
                 f"{'Del.Ratio':>14} {'Overhead':>14} "
                 f"{'Latency(s)':>16} {'Hops':>12}")
    lines.append("-" * 115)

    grouped = defaultdict(list)
    for r in rows:
        key = (r["protocol"], r["scenario"], r.get("nodes", "?"))
        grouped[key].append(r)

    for key in sorted(grouped.keys()):
        proto, scen, nodes = key
        rs = grouped[key]
        dr_m, dr_s = mean_std([r["delivery_prob"] for r in rs])
        ov_m, ov_s = mean_std([r["overhead_ratio"] for r in rs])
        la_m, la_s = mean_std([r["latency_avg"] for r in rs])
        hc_m, hc_s = mean_std([r["hopcount_avg"] for r in rs])
        lines.append(
            f"{proto:<15} {scen:<12} {str(nodes):<7} "
            f"{dr_m:>6.4f} +/- {dr_s:.4f} {ov_m:>7.2f} +/- {ov_s:>5.2f} "
            f"{la_m:>8.1f} +/- {la_s:>5.1f} {hc_m:>5.2f} +/- {hc_s:.2f}")
    lines.append("=" * 115)
    return "\n".join(lines)


# ══════════════════════════════════════════════════════════════════════
# Main
# ══════════════════════════════════════════════════════════════════════

def main():
    if not os.path.isfile(CSV_PATH):
        print(f"ERROR: {CSV_PATH} not found. Run experiments first.")
        sys.exit(1)

    rows = load_csv()
    base = [r for r in rows if r.get("scenario") == "base"]
    print(f"Loaded {len(rows)} rows ({len(base)} base)")

    print("\n-- Grouped bar charts (horizontal) --")
    plot_grouped_bars(base, "delivery_prob", "Delivery ratio",
                      "Delivery ratio by protocol and network size",
                      "chart_delivery_ratio")
    plot_grouped_bars(base, "overhead_ratio", "Overhead ratio",
                      "Overhead ratio by protocol and network size",
                      "chart_overhead_ratio")
    plot_grouped_bars(base, "latency_avg", "Average latency (s)",
                      "Average latency by protocol and network size",
                      "chart_latency")
    plot_grouped_bars(base, "hopcount_avg", "Average hop count",
                      "Average hop count by protocol and network size",
                      "chart_hopcount")

    print("\n-- Sybil comparison --")
    plot_sybil(rows)

    print("\n-- Work evolution --")
    plot_work_evolution()

    print("\n-- CDF of latency --")
    plot_cdf_latency()

    print("\n-- Boxplot of latency --")
    plot_boxplot_latency()

    print("\n-- Gini evolution --")
    plot_gini_evolution()

    print("\n-- Sensitivity analysis --")
    plot_sensitivity(rows)

    print("\n-- Proof size --")
    plot_proof_size()

    print("\n-- Work vs contributions scatter --")
    plot_work_scatter()

    print("\n-- Score quintile --")
    plot_score_quintile()

    print("\n-- Summary table --")
    table = build_summary_table(rows)
    print(table)
    with open(os.path.join(OUT_DIR, "summary_table.txt"), "w",
              encoding="utf-8") as f:
        f.write(table)
    print(f"\nSaved to {os.path.join(OUT_DIR, 'summary_table.txt')}")
    print(f"\nTotal charts: 13 (PDF + PNG each)")


if __name__ == "__main__":
    main()
