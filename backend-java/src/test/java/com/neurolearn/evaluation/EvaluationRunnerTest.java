package com.neurolearn.evaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The CLI writes the CSVs docs/evaluation/make_charts.py reads. */
class EvaluationRunnerTest {

    @Test
    void simulateWritesSummaryAndTrajectoryForEveryScenarioAndPolicy(@TempDir Path dir) throws Exception {
        List<PolicySummary> summaries = EvaluationRunner.simulate(dir, 60, 12, 3);

        int scenarios = EvaluationRunner.scenarios().size();
        int policies = EvaluationRunner.policies().size();
        assertThat(summaries).hasSize(scenarios * policies);

        List<String> summary = Files.readAllLines(dir.resolve("summary.csv"), StandardCharsets.UTF_8);
        assertThat(summary.get(0)).isEqualTo(PolicySummary.csvHeader());
        assertThat(summary).hasSize(1 + scenarios * policies);

        List<String> traj = Files.readAllLines(dir.resolve("trajectory.csv"), StandardCharsets.UTF_8);
        assertThat(traj.get(0)).startsWith("scenario,policy,step,").endsWith(",share_active");
        assertThat(traj).hasSize(1 + scenarios * policies * 12);
    }

    @Test
    void fitReadsLoggedAttemptsAndFlagsTooLittleData(@TempDir Path dir) throws Exception {
        Path in = dir.resolve("attempts.csv");
        Files.writeString(in, """
                student,topic,timestamp,correct
                1,intro,2026-01-01 10:00:00+00,f
                1,intro,2026-01-01 10:01:00+00,t
                2,intro,2026-01-01 11:00:00+00,t
                """);
        Path out = dir.resolve("fit.csv");

        EvaluationRunner.fitLogged(in, out);

        List<String> lines = Files.readAllLines(out, StandardCharsets.UTF_8);
        assertThat(lines.get(0)).startsWith("topic,sequences,answers,");
        assertThat(lines.get(1)).startsWith("intro,2,3,").endsWith(",false");
    }

    @Test
    void recoveryWritesOneRowPerCase(@TempDir Path dir) throws Exception {
        assertThat(EvaluationRunner.recovery(dir.resolve("r.csv"), 7)).hasSize(3);
    }
}
