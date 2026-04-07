#!/usr/bin/env python3
"""
Generate publication-quality charts for Scopus paper from simulation data.

Outputs (PDF + PNG for each):
  1.  chart_delivery_ratio      — grouped bar (3 protocols × 3 node counts)
  2.  chart_overhead_ratio      — grouped bar
  3.  chart_latency             — grouped bar
  4.  chart_hopcount            — grouped bar
  5.  chart_sybil               — PoDC clean N=126 vs Sybil N=126
  6.  chart_work_evolution      — fraction of forwards by experienced nodes
  7.  chart_cdf_latency         — CDF of per-message latency (N=100)
  8.  chart_boxplot_latency     — boxplot of delivery time (N=100)
  9.  chart_gini_evolution      — Gini coefficient over time
  10. chart_sensitivity_drop    — delivery ratio vs dropThreshold
  11. chart_proof_size          — proof chain size histogram
  12. chart_work_scatter        — work vs delivery contributions scatter
  13. chart_score_quintile      — score quintiles vs delivery success rate
  + summary_table.txt
"""

import csv
import glob
import os
import re
import sys
from collections import defaultdict

try:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    import numpy as np
except ImportError:
    print("ERROR: matplotlib and numpy required.  pip install matplotlib numpy")
    sys.exit(1)

# ── Global style ─────────────────────────────────────────────────────

plt.rcParams.update({
    "font.family": "sans-serif",
    "font.sans-serif": ["Arial", "DejaVu Sans"],
    "font.size": 10,
    "axes.labelsize": 11,
    "axes.titlesize": 12,
    "legend.fontsize": 9,
    "xtick.labelsize": 9,
    "ytick.labelsize": 9,
    "figure.dpi": 200,
    "savefig.bbox": "tight",
    "savefig.pad_inches": 0.1,
})

CSV_PATH = os.path.join("reports", "full_results.csv")
OUT_DIR  = "reports"

PROTOCOL_COLORS = {"PoDC": "#2196F3", "Epidemic": "#FF9800", "Prophet": "#4CAF50"}
PROTOCOL_ORDER  = ["PoDC", "Epidemic", "Prophet"]
NODE_COUNTS     = [50, 100, 150]


def save_fig(fig, name):
    for ext in ("pdf", "png"):
        path = os.path.join(OUT_DIR, f"{name}.{ext}")
        fig.savefig(path, dpi=200)
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
# 1-4. Grouped bar charts (protocols × node counts)
# ══════════════════════════════════════════════════════════════════════

def plot_grouped_bars(data, metric_key, ylabel, title, filename):
    fig, ax = plt.subplots(figsize=(7, 4.5))
    x = np.arange(len(NODE_COUNTS))
    width = 0.22
    offsets = np.arange(len(PROTOCOL_ORDER)) - 1

    for i, proto in enumerate(PROTOCOL_ORDER):
        means, errs = [], []
        for nc in NODE_COUNTS:
            vals = [r[metric_key] for r in data
                    if r.get("protocol") == proto and r.get("nodes") == nc]
            m, e = ci95(vals)
            means.append(m)
            errs.append(e)
        bars = ax.bar(x + offsets[i] * width, means, width,
                       yerr=errs, capsize=3, label=proto,
                       color=PROTOCOL_COLORS.get(proto, "#999"),
                       edgecolor="white", linewidth=0.5)
        for bar, m_val in zip(bars, means):
            if m_val > 0:
                ax.text(bar.get_x() + bar.get_width() / 2,
                        bar.get_height(),
                        f"{m_val:.2f}", ha="center", va="bottom", fontsize=7)

    ax.set_xticks(x)
    ax.set_xticklabels([str(n) for n in NODE_COUNTS])
    ax.set_xlabel("Number of nodes")
    ax.set_ylabel(ylabel)
    ax.set_title(title)
    ax.legend(loc="upper left")
    ax.grid(axis="y", alpha=0.3, linewidth=0.5)
    fig.tight_layout()
    save_fig(fig, filename)


# ══════════════════════════════════════════════════════════════════════
# 5. Sybil comparison (PoDC clean N=126 vs Sybil N=126)
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

    fig, ax = plt.subplots(figsize=(5, 4))
    colors = ["#2196F3", "#E91E63"]
    bars = ax.bar(labels, means, yerr=errs, capsize=5,
                   color=colors, edgecolor="white", width=0.45)
    for bar, m_val in zip(bars, means):
        ax.text(bar.get_x() + bar.get_width() / 2, bar.get_height(),
                f"{m_val:.4f}", ha="center", va="bottom", fontsize=9)
    ax.set_ylabel("Delivery ratio")
    ax.set_title("Sybil attack impact on delivery ratio")
    ax.grid(axis="y", alpha=0.3)
    fig.tight_layout()
    save_fig(fig, "chart_sybil")


# ══════════════════════════════════════════════════════════════════════
# 6. Work evolution (natural selection)
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

    fig, ax = plt.subplots(figsize=(7, 4))
    ax.plot(times, means, color="#2196F3", linewidth=2,
            label="Fraction of forwards by nodes with work > 0")
    ax.fill_between(times, lo, hi, alpha=0.2, color="#2196F3")
    ax.axhline(y=0.5, color="gray", linestyle="--", linewidth=0.8,
               label="Random baseline (50%)")
    ax.set_xlabel("Simulation time (s)")
    ax.set_ylabel("Fraction of forwards by\nnodes with work > 0")
    ax.set_title("Natural selection effect: experienced nodes dominate forwarding")
    ax.set_ylim(0, 1.05)
    ax.legend(loc="lower right")
    ax.grid(alpha=0.3)
    fig.tight_layout()
    save_fig(fig, "chart_work_evolution")


# ══════════════════════════════════════════════════════════════════════
# 7. CDF of per-message latency (N=100, all 3 protocols)
# ══════════════════════════════════════════════════════════════════════

def load_delay_reports(pattern):
    """Parse MessageDelayReport files: each line is 'delay cumProb'."""
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
    fig, ax = plt.subplots(figsize=(7, 4.5))
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
        ax.plot(delays, cdf, linewidth=1.5, label=proto,
                color=PROTOCOL_COLORS[proto])

    if not has_data:
        print("  (no MessageDelayReport files for N=100)")
        plt.close(fig)
        return

    ax.set_xlabel("Message delivery latency (s)")
    ax.set_ylabel("CDF")
    ax.set_title("Cumulative distribution of delivery latency (N=100)")
    ax.legend(loc="lower right")
    ax.grid(alpha=0.3)
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
            labels.append(proto)

    if not data_lists:
        print("  (no delay data for boxplot)")
        return

    fig, ax = plt.subplots(figsize=(5, 4.5))
    bp = ax.boxplot(data_lists, labels=labels, patch_artist=True,
                    showmeans=True, meanline=True,
                    medianprops=dict(color="black", linewidth=1.5),
                    meanprops=dict(color="red", linewidth=1, linestyle="--"))
    colors = [PROTOCOL_COLORS[l] for l in labels]
    for patch, c in zip(bp["boxes"], colors):
        patch.set_facecolor(c)
        patch.set_alpha(0.6)

    ax.set_ylabel("Delivery latency (s)")
    ax.set_title("Distribution of delivery latency (N=100)")
    ax.grid(axis="y", alpha=0.3)
    fig.tight_layout()
    save_fig(fig, "chart_boxplot_latency")


# ══════════════════════════════════════════════════════════════════════
# 9. Gini coefficient evolution over time
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

    fig, ax = plt.subplots(figsize=(7, 4))
    ax.plot(times, means, color="#9C27B0", linewidth=2, label="Gini coefficient")
    ax.fill_between(times, lo, hi, alpha=0.2, color="#9C27B0")
    ax.set_xlabel("Simulation time (s)")
    ax.set_ylabel("Gini coefficient of work")
    ax.set_title("Fairness of work distribution over simulation time")
    ax.set_ylim(0, 1.0)
    ax.legend(loc="upper right")
    ax.grid(alpha=0.3)
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
        vals_dr = []
        vals_oh = []
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

    fig, ax1 = plt.subplots(figsize=(6, 4.5))
    x = np.arange(len(thresholds))
    w = 0.3

    bars1 = ax1.bar(x - w/2, means_dr, w, yerr=errs_dr, capsize=4,
                     color="#2196F3", label="Delivery ratio", edgecolor="white")
    ax1.set_ylabel("Delivery ratio", color="#2196F3")
    ax1.tick_params(axis="y", labelcolor="#2196F3")

    ax2 = ax1.twinx()
    bars2 = ax2.bar(x + w/2, means_oh, w, yerr=errs_oh, capsize=4,
                     color="#FF9800", label="Overhead ratio", edgecolor="white")
    ax2.set_ylabel("Overhead ratio", color="#FF9800")
    ax2.tick_params(axis="y", labelcolor="#FF9800")

    ax1.set_xticks(x)
    ax1.set_xticklabels([str(t) for t in thresholds])
    ax1.set_xlabel("dropThreshold parameter")
    ax1.set_title("Sensitivity of PoDC to dropThreshold (N=100)")

    for bar, m_val in zip(bars1, means_dr):
        ax1.text(bar.get_x() + bar.get_width()/2, bar.get_height(),
                 f"{m_val:.3f}", ha="center", va="bottom", fontsize=8)
    for bar, m_val in zip(bars2, means_oh):
        ax2.text(bar.get_x() + bar.get_width()/2, bar.get_height(),
                 f"{m_val:.1f}", ha="center", va="bottom", fontsize=8)

    lines1, labels1 = ax1.get_legend_handles_labels()
    lines2, labels2 = ax2.get_legend_handles_labels()
    ax1.legend(lines1 + lines2, labels1 + labels2, loc="upper left")

    ax1.grid(axis="y", alpha=0.3)
    fig.tight_layout()
    save_fig(fig, "chart_sensitivity_drop")


# ══════════════════════════════════════════════════════════════════════
# 11. Proof chain size histogram
# ══════════════════════════════════════════════════════════════════════

def plot_proof_size():
    pm_files = glob.glob(os.path.join(OUT_DIR, "PoDC_n*_per_message.csv"))
    if not pm_files:
        print("  (no per_message files for proof size)")
        return

    proof_bytes_all = []
    hops_all = []
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

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10, 4))

    ax1.hist(proof_bytes_all, bins=30, color="#2196F3", edgecolor="white",
             alpha=0.8)
    ax1.set_xlabel("Proof chain size (bytes)")
    ax1.set_ylabel("Number of delivered messages")
    ax1.set_title("Distribution of proof chain sizes")
    ax1.axvline(np.mean(proof_bytes_all), color="red", linestyle="--",
                label=f"Mean = {np.mean(proof_bytes_all):.0f} B")
    ax1.legend()
    ax1.grid(axis="y", alpha=0.3)

    ax2.scatter(hops_all, proof_bytes_all, alpha=0.3, s=10, color="#2196F3")
    ax2.set_xlabel("Number of hops")
    ax2.set_ylabel("Proof chain size (bytes)")
    ax2.set_title("Proof size vs. hop count")
    ax2.grid(alpha=0.3)

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

    fig, ax = plt.subplots(figsize=(6, 5))
    ax.scatter(works, contribs, alpha=0.3, s=12, color="#2196F3",
               edgecolors="none")
    ax.set_xlabel("Accumulated work (reputation)")
    ax.set_ylabel("Delivery contributions")
    ax.set_title("Correlation: work vs. participation in successful deliveries")
    ax.grid(alpha=0.3)

    if len(works) > 2 and max(works) > 0:
        z = np.polyfit(works, contribs, 1)
        p = np.poly1d(z)
        xs = np.linspace(0, max(works), 100)
        ax.plot(xs, p(xs), "r--", linewidth=1.5,
                label=f"Linear fit (slope={z[0]:.1f})")
        corr = np.corrcoef(works, contribs)[0, 1]
        ax.legend(title=f"Pearson r = {corr:.3f}")

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
            "label": f"Q{qi+1}\n({avg_score:.3f})",
            "rate": rate,
            "total_fwd": total_fwd,
            "total_contrib": total_contrib,
        })

    fig, ax = plt.subplots(figsize=(7, 4.5))
    labels_q = [q["label"] for q in quintiles]
    rates = [q["rate"] for q in quintiles]

    colors_q = plt.cm.Blues(np.linspace(0.3, 0.9, 5))
    bars = ax.bar(labels_q, rates, color=colors_q, edgecolor="white")
    for bar, rate in zip(bars, rates):
        ax.text(bar.get_x() + bar.get_width()/2, bar.get_height(),
                f"{rate:.3f}", ha="center", va="bottom", fontsize=8)

    ax.set_xlabel("Score quintile (mean score shown)")
    ax.set_ylabel("Delivery contribution rate\n(contributions / forwards)")
    ax.set_title("Score quintile vs. delivery effectiveness")
    ax.grid(axis="y", alpha=0.3)
    fig.tight_layout()
    save_fig(fig, "chart_score_quintile")


# ══════════════════════════════════════════════════════════════════════
# Summary table
# ══════════════════════════════════════════════════════════════════════

def build_summary_table(rows):
    lines = []
    lines.append("=" * 110)
    lines.append(f"{'Protocol':<12} {'Scenario':<12} {'Nodes':<7} "
                 f"{'Del.Ratio':>14} {'Overhead':>14} "
                 f"{'Latency(s)':>16} {'Hops':>12}")
    lines.append("-" * 110)

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
            f"{proto:<12} {scen:<12} {str(nodes):<7} "
            f"{dr_m:>6.4f} +/- {dr_s:.4f} {ov_m:>7.2f} +/- {ov_s:>5.2f} "
            f"{la_m:>8.1f} +/- {la_s:>5.1f} {hc_m:>5.2f} +/- {hc_s:.2f}")
    lines.append("=" * 110)
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

    print("\n-- Grouped bar charts --")
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
