package com.neurolearn.evaluation;

import com.neurolearn.adaptive.BktParameters;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Offline evaluation (FUTURE_WORKFLOW Phase 5). Plain Java — no Spring, no database.
 *
 * <pre>
 *   simulate &lt;outDir&gt; [students=5000] [horizon=30] [seed=42]
 *       fixed vs adaptive vs random on simulated students, baseline + sensitivity scenarios
 *       → outDir/summary.csv, outDir/trajectory.csv
 *   fit &lt;attempts.csv&gt; &lt;out.csv&gt;
 *       fit BKT per topic to logged attempts (columns: student,topic,timestamp,correct)
 *   recovery &lt;out.csv&gt; [seed=7]
 *       sanity check: simulate BKT data from known parameters and fit it back
 * </pre>
 * Run: {@code java -cp target/classes com.neurolearn.evaluation.EvaluationRunner simulate ../docs/evaluation}
 */
public final class EvaluationRunner {

    /** Scenario = population + learning-model assumptions. Baseline first. */
    record Scenario(String name, double meanTheta0, LearningModel model) {
    }

    static List<Scenario> scenarios() {
        LearningModel base = LearningModel.standard();
        return List.of(
                new Scenario("baseline", -1.0, base),
                new Scenario("zpd_at_level", -1.0, base.withZpdOffset(0.0)),
                new Scenario("zpd_far_above", -1.0, base.withZpdOffset(1.0)),
                new Scenario("weak_start", -2.0, base),
                new Scenario("strong_start", 0.5, base),
                new Scenario("frustration_quit", -1.0, base.withQuitRate(0.04))
        );
    }

    /**
     * Best variant from {@code tune} (selected on a separate training population, seed 1 —
     * see docs/evaluation.md). Candidate only: the app still uses the current engine.
     */
    static final double TUNED_TO_LEVEL2 = 0.20;
    static final double TUNED_TO_LEVEL3 = 0.60;
    static final boolean TUNED_LEVEL_AWARE = true;
    /** Best Elo success target from {@code tune} (50/60/70% tried). */
    static final double TUNED_ELO_TARGET = 0.50;

    static List<DifficultyPolicy> policies() {
        return List.of(DifficultyPolicy.fixed(), DifficultyPolicy.adaptiveEngine(),
                DifficultyPolicy.adaptiveVariant(TUNED_TO_LEVEL2, TUNED_TO_LEVEL3, TUNED_LEVEL_AWARE),
                DifficultyPolicy.eloTarget(TUNED_ELO_TARGET),
                DifficultyPolicy.rampWithRemediation(),
                DifficultyPolicy.random());
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("usage: simulate <outDir> [students] [horizon] [seed] | tune <out.csv> [students] [seed] | fit <attempts.csv> <out.csv> | recovery <out.csv> [seed]");
            System.exit(2);
        }
        switch (args[0]) {
            case "simulate" -> simulate(Path.of(args[1]),
                    args.length > 2 ? Integer.parseInt(args[2]) : 5000,
                    args.length > 3 ? Integer.parseInt(args[3]) : 30,
                    args.length > 4 ? Long.parseLong(args[4]) : 42L);
            case "tune" -> tune(Path.of(args[1]),
                    args.length > 2 ? Integer.parseInt(args[2]) : 2000,
                    args.length > 3 ? Long.parseLong(args[3]) : 1L);
            case "fit" -> fitLogged(Path.of(args[1]), Path.of(args[2]));
            case "recovery" -> recovery(Path.of(args[1]), args.length > 2 ? Long.parseLong(args[2]) : 7L);
            default -> throw new IllegalArgumentException("unknown command " + args[0]);
        }
    }

    // ── simulate ──────────────────────────────────────────────────

    static List<PolicySummary> simulate(Path outDir, int students, int horizon, long seed) throws IOException {
        Files.createDirectories(outDir);
        List<PolicySummary> summaries = new ArrayList<>();
        try (PrintWriter traj = writer(outDir.resolve("trajectory.csv"))) {
            traj.println("scenario,policy,step,mean_theta,share_mastered,mean_level,mean_p_correct,share_productive,mean_believed_mastery,share_active");
            for (Scenario sc : scenarios()) {
                List<Simulator.Student> pop = Simulator.population(students, sc.meanTheta0(), seed);
                Simulator sim = new Simulator(sc.model());
                for (DifficultyPolicy policy : policies()) {
                    List<Simulator.Run> runs = new ArrayList<>(students);
                    for (Simulator.Student s : pop) runs.add(sim.run(s, policy, horizon));
                    summaries.add(PolicySummary.of(sc.name(), policy.name(), runs));
                    writeTrajectory(traj, sc.name(), policy.name(), runs, horizon);
                }
            }
        }
        try (PrintWriter out = writer(outDir.resolve("summary.csv"))) {
            out.println(PolicySummary.csvHeader());
            summaries.forEach(s -> out.println(s.toCsv()));
        }
        summaries.forEach(s -> System.out.printf(Locale.ROOT,
                "%-16s %-30s mastered@20 %5.1f%%  @%d %5.1f%%  too-hard %5.1f%%  quit %5.1f%%  claim-precision %5.1f%%%n",
                s.scenario(), s.policy(), 100 * s.masteredBy20(), s.horizon(), 100 * s.masteredByEnd(),
                100 * s.shareTooHard(), 100 * s.quitRate(),
                Double.isNaN(s.engineClaimPrecision()) ? Double.NaN : 100 * s.engineClaimPrecision()));
        return summaries;
    }

    private static void writeTrajectory(PrintWriter out, String scenario, String policy, List<Simulator.Run> runs, int horizon) {
        for (int i = 0; i < horizon; i++) {
            double theta = 0, mastered = 0, level = 0, p = 0, productive = 0, belief = 0;
            int beliefN = 0;
            int activeN = 0;
            for (Simulator.Run r : runs) {
                Simulator.Step s = r.steps().get(i);
                theta += s.thetaAfter();
                mastered += s.trulyMastered() ? 1 : 0;
                if (!s.active()) continue;
                activeN++;
                level += s.level();
                p += s.pCorrect();
                productive += (s.pCorrect() >= PolicySummary.PRODUCTIVE_LOW && s.pCorrect() <= PolicySummary.PRODUCTIVE_HIGH) ? 1 : 0;
                if (!Double.isNaN(s.believedMastery())) {
                    belief += s.believedMastery();
                    beliefN++;
                }
            }
            int n = runs.size();
            int a = Math.max(1, activeN);
            out.println(String.join(",", scenario, policy, String.valueOf(i + 1),
                    PolicySummary.f(theta / n), PolicySummary.f(mastered / n), PolicySummary.f(level / a),
                    PolicySummary.f(p / a), PolicySummary.f(productive / a),
                    beliefN == 0 ? "" : PolicySummary.f(belief / beliefN),
                    PolicySummary.f(activeN / (double) n)));
        }
    }

    // ── tune (training population, baseline scenario only) ────────

    /**
     * Grid over the engine's P(L) cut-offs and level-aware evidence on a TRAINING population
     * (different seed from the reported results). Ranked by mastery within the horizon, then
     * by how often the "mastered" claim is true. The chosen values are hard-coded above as
     * TUNED_* so the reported run is reproducible.
     */
    static List<PolicySummary> tune(Path out, int students, long seed) throws IOException {
        Scenario base = scenarios().get(0);
        List<Simulator.Student> pop = Simulator.population(students, base.meanTheta0(), seed);
        Simulator sim = new Simulator(base.model());
        List<DifficultyPolicy> grid = new ArrayList<>();
        grid.add(DifficultyPolicy.adaptiveEngine());
        grid.add(DifficultyPolicy.fixed());
        grid.add(DifficultyPolicy.rampWithRemediation());
        for (boolean aware : new boolean[]{false, true})
            for (double lo : new double[]{0.20, 0.30, 0.40})
                for (double hi : new double[]{0.50, 0.60, 0.75})
                    grid.add(DifficultyPolicy.adaptiveVariant(lo, hi, aware));
        for (double target : new double[]{0.50, 0.60, 0.70})
            grid.add(DifficultyPolicy.eloTarget(target));
        List<PolicySummary> results = new ArrayList<>();
        for (DifficultyPolicy p : grid) {
            List<Simulator.Run> runs = new ArrayList<>(students);
            for (Simulator.Student s : pop) runs.add(sim.run(s, p, 30));
            results.add(PolicySummary.of("tuning", p.name(), runs));
        }
        results.sort(Comparator.comparingDouble((PolicySummary s) -> -s.masteredByEnd())
                .thenComparingDouble(s -> Double.isNaN(s.engineClaimPrecision()) ? 0 : -s.engineClaimPrecision()));
        try (PrintWriter w = writer(out)) {
            w.println(PolicySummary.csvHeader());
            results.forEach(s -> w.println(s.toCsv()));
        }
        results.forEach(s -> System.out.printf(Locale.ROOT, "%-32s mastered@30 %5.1f%%  @20 %5.1f%%  claim-precision %5.1f%%  claim-step %4.1f%n",
                s.policy(), 100 * s.masteredByEnd(), 100 * s.masteredBy20(),
                Double.isNaN(s.engineClaimPrecision()) ? Double.NaN : 100 * s.engineClaimPrecision(), s.medianEngineClaimStep()));
        return results;
    }

    // ── fit on logged data ────────────────────────────────────────

    static void fitLogged(Path attemptsCsv, Path out) throws IOException {
        // student,topic,timestamp,correct — already ordered by student, topic, timestamp
        Map<String, Map<String, List<Boolean>>> byTopic = new TreeMap<>();
        List<String> lines = Files.readAllLines(attemptsCsv, StandardCharsets.UTF_8);
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) continue;
            String[] p = line.split(",");
            byTopic.computeIfAbsent(p[1], k -> new LinkedHashMap<>())
                    .computeIfAbsent(p[0], k -> new ArrayList<>())
                    .add(p[3].trim().startsWith("t"));
        }
        BktParameters defaults = BktParameters.defaultParameters();
        try (PrintWriter w = writer(out)) {
            w.println("topic,sequences,answers,p_init,p_transit,p_guess,p_slip,logloss_fitted,logloss_default,enough_data");
            for (var e : byTopic.entrySet()) {
                List<List<Boolean>> seqs = new ArrayList<>(e.getValue().values());
                BktFitter.Fit fit = BktFitter.fit(seqs);
                BktFitter.Fit def = BktFitter.evaluate(seqs, defaults);
                boolean enough = fit.sequences() >= 30 && fit.observations() >= 300;
                w.println(String.join(",", e.getKey(), String.valueOf(fit.sequences()), String.valueOf(fit.observations()),
                        PolicySummary.f(fit.pInit()), PolicySummary.f(fit.pTransit()), PolicySummary.f(fit.pGuess()),
                        PolicySummary.f(fit.pSlip()), PolicySummary.f(fit.logLossPerAnswer()), PolicySummary.f(def.logLossPerAnswer()),
                        String.valueOf(enough)));
                System.out.printf(Locale.ROOT, "%-20s %3d students %4d answers  fitted L0=%.2f T=%.2f G=%.2f S=%.2f  logloss %.3f (default %.3f)%s%n",
                        e.getKey(), fit.sequences(), fit.observations(), fit.pInit(), fit.pTransit(), fit.pGuess(), fit.pSlip(),
                        fit.logLossPerAnswer(), def.logLossPerAnswer(), enough ? "" : "  [too little data]");
            }
        }
    }

    // ── recovery check ────────────────────────────────────────────

    /** Draws sequences from known BKT parameters and checks the fitter gets them back. */
    static List<String[]> recovery(Path out, long seed) throws IOException {
        double[][] truths = {{0.20, 0.15, 0.20, 0.08}, {0.10, 0.08, 0.25, 0.10}, {0.40, 0.25, 0.15, 0.05}};
        List<String[]> rows = new ArrayList<>();
        Random rng = new Random(seed);
        try (PrintWriter w = writer(out)) {
            w.println("case,true_p_init,true_p_transit,true_p_guess,true_p_slip,fit_p_init,fit_p_transit,fit_p_guess,fit_p_slip,sequences,length");
            for (int c = 0; c < truths.length; c++) {
                double[] t = truths[c];
                List<List<Boolean>> seqs = sampleBkt(rng, t, 500, 12);
                BktFitter.Fit f = BktFitter.fit(seqs);
                String[] row = {String.valueOf(c + 1), PolicySummary.f(t[0]), PolicySummary.f(t[1]), PolicySummary.f(t[2]), PolicySummary.f(t[3]),
                        PolicySummary.f(f.pInit()), PolicySummary.f(f.pTransit()), PolicySummary.f(f.pGuess()), PolicySummary.f(f.pSlip()), "500", "12"};
                rows.add(row);
                w.println(String.join(",", row));
                System.out.printf(Locale.ROOT, "case %d true L0=%.2f T=%.2f G=%.2f S=%.2f  →  fit L0=%.2f T=%.2f G=%.2f S=%.2f%n",
                        c + 1, t[0], t[1], t[2], t[3], f.pInit(), f.pTransit(), f.pGuess(), f.pSlip());
            }
        }
        return rows;
    }

    static List<List<Boolean>> sampleBkt(Random rng, double[] p, int students, int length) {
        List<List<Boolean>> seqs = new ArrayList<>(students);
        for (int i = 0; i < students; i++) {
            boolean known = rng.nextDouble() < p[0];
            List<Boolean> seq = new ArrayList<>(length);
            for (int j = 0; j < length; j++) {
                boolean correct = known ? rng.nextDouble() >= p[3] : rng.nextDouble() < p[2];
                seq.add(correct);
                if (!known && rng.nextDouble() < p[1]) known = true;
            }
            seqs.add(seq);
        }
        return seqs;
    }

    private static PrintWriter writer(Path p) throws IOException {
        Files.createDirectories(p.toAbsolutePath().getParent());
        return new PrintWriter(Files.newBufferedWriter(p, StandardCharsets.UTF_8));
    }
}
