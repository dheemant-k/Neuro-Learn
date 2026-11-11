"""Charts for docs/evaluation.md from summary.csv and trajectory.csv (run from this folder).

    python make_charts.py
"""
import csv
import os

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
LABELS = {
    "fixed": "Fixed ramp (5 easy, 5 medium, then hard)",
    "adaptive": "Current engine (BKT + P(L) cut-offs)",
    "adaptive_0.20_0.60_levelaware": "Tuned engine (0.20/0.60, level-aware)",
    "elo_target50": "Difficulty-aware Elo (50% target)",
    "ramp_remediation": "Ramp with remediation",
    "random": "Random level",
}
ORDER = list(LABELS)
COLORS = {
    "fixed": "#2a6fdb",
    "adaptive": "#e0533d",
    "adaptive_0.20_0.60_levelaware": "#f2a03d",
    "elo_target50": "#2e9e6b",
    "ramp_remediation": "#8b5cf6",
    "random": "#9aa3ad",
}
SCENARIOS = {
    "baseline": "Baseline",
    "zpd_at_level": "Learns best at own level",
    "zpd_far_above": "Learns best well above level",
    "weak_start": "Weak starters",
    "strong_start": "Strong starters",
    "frustration_quit": "Frustration → quitting",
}


def read(name):
    with open(os.path.join(HERE, name), encoding="utf-8") as f:
        return list(csv.DictReader(f))


summary = read("summary.csv")
traj = read("trajectory.csv")
plt.rcParams.update({"font.size": 10, "axes.spines.top": False, "axes.spines.right": False})


def series(scenario, policy, col):
    rows = [r for r in traj if r["scenario"] == scenario and r["policy"] == policy]
    return [int(r["step"]) for r in rows], [float(r[col]) * 100 for r in rows]


# 1. Mastery over time — baseline and frustration
fig, axes = plt.subplots(1, 2, figsize=(12, 4.4), sharey=True)
for ax, sc in zip(axes, ["baseline", "frustration_quit"]):
    for p in ORDER:
        x, y = series(sc, p, "share_mastered")
        ax.plot(x, y, label=LABELS[p], color=COLORS[p], lw=2)
    ax.set_title(f"{SCENARIOS[sc]}: share of students truly mastered")
    ax.set_xlabel("Question number")
    ax.grid(alpha=0.25)
axes[0].set_ylabel("% of simulated students")
axes[1].legend(loc="upper left", fontsize=8, frameon=False)
fig.tight_layout()
fig.savefig(os.path.join(HERE, "mastery_over_time.png"), dpi=150)
plt.close(fig)

# 2. Mastered by question 30 across scenarios
fig, ax = plt.subplots(figsize=(12, 4.6))
scs = list(SCENARIOS)
width = 0.8 / len(ORDER)
for i, p in enumerate(ORDER):
    vals = [float(next(r for r in summary if r["scenario"] == s and r["policy"] == p)["mastered_by_end"]) * 100 for s in scs]
    ax.bar([j + i * width for j in range(len(scs))], vals, width, label=LABELS[p], color=COLORS[p])
ax.set_xticks([j + width * (len(ORDER) - 1) / 2 for j in range(len(scs))])
ax.set_xticklabels([SCENARIOS[s] for s in scs], fontsize=9)
ax.set_ylabel("% truly mastered by question 30")
ax.set_title("Mastery within 30 questions, by learning assumption")
ax.legend(fontsize=8, frameon=False, ncol=3, loc="upper left")
ax.grid(axis="y", alpha=0.25)
fig.tight_layout()
fig.savefig(os.path.join(HERE, "mastery_by_scenario.png"), dpi=150)
plt.close(fig)

# 3. Frustration scenario: quit rate vs mastery
fig, ax = plt.subplots(figsize=(6.5, 4.6))
for p in ORDER:
    r = next(r for r in summary if r["scenario"] == "frustration_quit" and r["policy"] == p)
    q, m = float(r["quit_rate"]) * 100, float(r["mastered_by_end"]) * 100
    ax.scatter(q, m, s=90, color=COLORS[p], label=LABELS[p], zorder=3)
ax.set_xlabel("% who quit early (lower is better)")
ax.set_ylabel("% truly mastered by question 30")
ax.set_title("Frustration scenario: keeping students vs. moving them on")
ax.grid(alpha=0.25)
ax.legend(fontsize=7.5, frameon=False, loc="lower left")
fig.tight_layout()
fig.savefig(os.path.join(HERE, "frustration_tradeoff.png"), dpi=150)
plt.close(fig)

# 4. When is "Mastered" claimed vs reached? (baseline)
fig, ax = plt.subplots(figsize=(6.5, 4.6))
base = {r["policy"]: r for r in summary if r["scenario"] == "baseline"}
names, claim, truth, prec = [], [], [], []
for p in ["adaptive", "adaptive_0.20_0.60_levelaware", "elo_target50"]:
    r = base[p]
    names.append(LABELS[p].split(" (")[0])
    claim.append(float(r["median_engine_claim_step"]))
    truth.append(float(r["median_questions_to_mastery"]))
    prec.append(float(r["engine_claim_precision"]) * 100)
y = range(len(names))
ax.barh([i + 0.2 for i in y], claim, 0.4, color="#e0533d", label="'Mastered' shown (median question)")
ax.barh([i - 0.2 for i in y], truth, 0.4, color="#2a6fdb", label="Truly mastered (median question)")
for i, pr in enumerate(prec):
    ax.text(max(claim[i], truth[i]) + 0.5, i, f"claim correct {pr:.0f}%", va="center", fontsize=8)
ax.set_yticks(list(y))
ax.set_yticklabels(names)
ax.set_xlabel("Question number")
ax.set_xlim(0, 36)
ax.set_title("Is the 'Mastered' badge earned? (baseline)")
ax.legend(fontsize=8, frameon=False, loc="upper center", bbox_to_anchor=(0.5, -0.16), ncol=2)
fig.tight_layout()
fig.savefig(os.path.join(HERE, "mastery_claim_accuracy.png"), dpi=150)
plt.close(fig)

print("charts written to", HERE)
