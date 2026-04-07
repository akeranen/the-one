#!/usr/bin/env python3
"""
Generate publication-quality charts from full_results.csv.
Produces:
  - chart_delivery_ratio.png   (3 protocols x 3 node counts, 95% CI)
  - chart_overhead_ratio.png
  - chart_latency.png
  - chart_hopcount.png
  - chart_sybil.png            (PoDC base vs sybil20)
  - chart_work_evolution.png   (fraction of forwards by experienced nodes)
  - summary_table.txt
"""

import csv
import os
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

CSV_PATH = os.path.join("reports", "full_results.csv")
OUT_DIR  = "reports"

PROTOCOL_COLORS = {"PoDC": "#2196F3", "Epidemic": "#FF9800", "Prophet": "#4CAF50"}
PROTOCOL_ORDER  = ["PoDC", "Epidemic", "Prophet"]
NODE_COUNTS     = [50, 100, 150]


def load_csv():
    rows = []
    with open(CSV_PATH, newline="", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        for r in reader:
            for k in ("nodes", "seed", "created", "delivered", "relayed"):
                if r.get(k):
                    r[k] = int(r[k])
            for k in ("delivery_prob", "overhead_ratio", "latency_avg",
                       "hopcount_avg"):
                v = r.get(k, "")
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


# ── Grouped bar chart (protocols x node counts) ──────────────────────

def plot_grouped_bars(data, metric_key, ylabel, title, filename):
    fig, ax = plt.subplots(figsize=(8, 5))
    x = np.arange(len(NODE_COUNTS))
    width = 0.25
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
                       yerr=errs, capsize=4, label=proto,
                       color=PROTOCOL_COLORS.get(proto, "#999"),
                       edgecolor="white", linewidth=0.5)
        for bar, m in zip(bars, means):
            if m > 0:
                ax.text(bar.get_x() + bar.get_width() / 2,
                        bar.get_height(),
                        f"{m:.2f}", ha="center", va="bottom", fontsize=7)

    ax.set_xticks(x)
    ax.set_xticklabels([str(n) for n in NODE_COUNTS])
    ax.set_xlabel("Number of Nodes")
    ax.set_ylabel(ylabel)
    ax.set_title(title)
    ax.legend()
    ax.grid(axis="y", alpha=0.3)
    fig.tight_layout()
    path = os.path.join(OUT_DIR, filename)
    fig.savefig(path, dpi=200)
    plt.close(fig)
    print(f"  -> {path}")


# ── Sybil comparison chart ───────────────────────────────────────────

def plot_sybil(rows):
    base_vals = [r["delivery_prob"] for r in rows
                 if r["protocol"] == "PoDC" and r["scenario"] == "base"
                 and r.get("nodes") in (100,)]
    sybil_vals = [r["delivery_prob"] for r in rows
                  if r["protocol"] == "PoDC" and r["scenario"] == "sybil20"]
    if not sybil_vals:
        return

    labels = ["PoDC (clean, N=100)", "PoDC (20% Sybil, N=126)"]
    data = [base_vals, sybil_vals]
    means, errs = [], []
    for d in data:
        m, e = ci95(d)
        means.append(m)
        errs.append(e)

    fig, ax = plt.subplots(figsize=(6, 4))
    bars = ax.bar(labels, means, yerr=errs, capsize=5,
                   color=["#2196F3", "#E91E63"], edgecolor="white", width=0.5)
    for bar, m in zip(bars, means):
        ax.text(bar.get_x() + bar.get_width() / 2, bar.get_height(),
                f"{m:.3f}", ha="center", va="bottom", fontsize=9)
    ax.set_ylabel("Delivery Ratio")
    ax.set_title("Sybil Attack Impact on Delivery Ratio")
    ax.grid(axis="y", alpha=0.3)
    fig.tight_layout()
    path = os.path.join(OUT_DIR, "chart_sybil.png")
    fig.savefig(path, dpi=200)
    plt.close(fig)
    print(f"  -> {path}")


# ── Work evolution (natural selection) chart ─────────────────────────

def plot_work_evolution():
    """
    Reads *_work_timeseries.csv files produced by PoDCMetrics and plots
    the fraction of data forwards performed by 'experienced' nodes
    (work > 0) over simulation time.
    """
    import glob
    ts_files = glob.glob(os.path.join(OUT_DIR, "*_work_timeseries.csv"))
    if not ts_files:
        print("  (no work_timeseries files found)")
        return

    all_series = defaultdict(lambda: defaultdict(list))

    for fpath in ts_files:
        fname = os.path.basename(fpath)
        with open(fpath, newline="") as f:
            reader = csv.DictReader(f)
            for row in reader:
                t = int(row["time"])
                frac = float(row["fraction_experienced"])
                total = int(row["total_forwards"])
                if total > 0:
                    all_series["all"][t].append(frac)

    if not all_series["all"]:
        print("  (empty timeseries data)")
        return

    times = sorted(all_series["all"].keys())
    means, lo, hi = [], [], []
    for t in times:
        vals = all_series["all"][t]
        m = sum(vals) / len(vals)
        means.append(m)
        if len(vals) >= 2:
            s = (sum((x - m) ** 2 for x in vals) / (len(vals) - 1)) ** 0.5
            ci = 1.96 * s / (len(vals) ** 0.5)
        else:
            ci = 0
        lo.append(m - ci)
        hi.append(m + ci)

    fig, ax = plt.subplots(figsize=(8, 4.5))
    ax.plot(times, means, color="#2196F3", linewidth=2,
            label="Fraction forwarded by experienced nodes")
    ax.fill_between(times, lo, hi, alpha=0.2, color="#2196F3")
    ax.axhline(y=0.5, color="gray", linestyle="--", linewidth=0.8,
               label="Random baseline (50%)")
    ax.set_xlabel("Simulation Time (s)")
    ax.set_ylabel("Fraction of Forwards by Nodes with Work > 0")
    ax.set_title("Natural Selection Effect: Experienced Nodes Dominate Forwarding")
    ax.set_ylim(0, 1.05)
    ax.legend(loc="lower right")
    ax.grid(alpha=0.3)
    fig.tight_layout()
    path = os.path.join(OUT_DIR, "chart_work_evolution.png")
    fig.savefig(path, dpi=200)
    plt.close(fig)
    print(f"  -> {path}")


# ── Summary table ────────────────────────────────────────────────────

def build_summary_table(rows):
    lines = []
    lines.append("=" * 100)
    lines.append(f"{'Protocol':<12} {'Scenario':<12} {'Nodes':<7} "
                 f"{'Del.Ratio':>12} {'Overhead':>12} "
                 f"{'Latency(s)':>14} {'Hops':>10}")
    lines.append("-" * 100)

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
            f"{dr_m:>6.4f}+/-{dr_s:.4f} {ov_m:>6.2f}+/-{ov_s:.2f} "
            f"{la_m:>7.1f}+/-{la_s:.1f} {hc_m:>5.2f}+/-{hc_s:.2f}")
    lines.append("=" * 100)
    return "\n".join(lines)


# ── Main ─────────────────────────────────────────────────────────────

def main():
    if not os.path.isfile(CSV_PATH):
        print(f"ERROR: {CSV_PATH} not found. Run experiments first.")
        sys.exit(1)

    rows = load_csv()
    base = [r for r in rows if r.get("scenario") == "base"]
    print(f"Loaded {len(rows)} rows ({len(base)} base)")

    print("\nGenerating charts...")

    plot_grouped_bars(base, "delivery_prob", "Delivery Ratio",
                      "Delivery Ratio by Protocol and Network Size",
                      "chart_delivery_ratio.png")
    plot_grouped_bars(base, "overhead_ratio", "Overhead Ratio",
                      "Overhead Ratio by Protocol and Network Size",
                      "chart_overhead_ratio.png")
    plot_grouped_bars(base, "latency_avg", "Average Latency (s)",
                      "Average Latency by Protocol and Network Size",
                      "chart_latency.png")
    plot_grouped_bars(base, "hopcount_avg", "Average Hop Count",
                      "Average Hop Count by Protocol and Network Size",
                      "chart_hopcount.png")

    plot_sybil(rows)
    plot_work_evolution()

    print("\nSummary table:")
    table = build_summary_table(rows)
    print(table)
    with open(os.path.join(OUT_DIR, "summary_table.txt"), "w",
              encoding="utf-8") as f:
        f.write(table)
    print(f"\nSaved to {os.path.join(OUT_DIR, 'summary_table.txt')}")


if __name__ == "__main__":
    main()
