package com.neurolearn.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * README §23 success path as a test:
 * register → start session → receive question → submit answer → performance recorded
 * → difficulty adapted → next question → complete → dashboard/profile updated.
 */
class LearningFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void studentCompletesAnAdaptiveSessionAndDashboardUpdates() throws Exception {
        String token = registerFresh("STUDENT");

        JsonNode before = getJson("/api/dashboard", token);
        assertThat(before.get("total_answered").asInt()).isZero();

        long sessionId = postJson("/api/sessions/start", token, Map.of("topic", "intro")).get("id").asLong();

        int answered = 0;
        int correct = 0;
        for (int i = 0; i < 3; i++) {
            JsonNode next = getJson("/api/sessions/" + sessionId + "/next-question?topic=intro&characterTheme=batman", token);
            assertThat(next.get("adaptiveAction").asText()).isIn("INCREASE", "MAINTAIN", "DECREASE");
            assertThat(next.get("adaptiveReason").asText()).isNotBlank();
            JsonNode q = next.get("question");
            assertThat(q.get("topic").asText()).isEqualTo("intro");

            boolean answerCorrect = i != 1; // correct, wrong, correct
            JsonNode attempt = postJson("/api/sessions/" + sessionId + "/attempt", token, Map.of(
                    "questionId", q.get("id").asLong(),
                    "correct", answerCorrect,
                    "responseTimeMs", 4000,
                    "attemptNumber", 1
            ));
            assertThat(attempt.get("correct").asBoolean()).isEqualTo(answerCorrect);
            answered++;
            if (answerCorrect) correct++;
        }

        JsonNode done = postJson("/api/sessions/" + sessionId + "/complete", token, Map.of());
        assertThat(done.get("totalQuestions").asInt()).isEqualTo(answered);
        assertThat(done.get("totalCorrect").asInt()).isEqualTo(correct);

        JsonNode after = getJson("/api/dashboard", token);
        assertThat(after.get("total_answered").asInt()).isEqualTo(answered);
        assertThat(after.get("total_correct").asInt()).isEqualTo(correct);
        assertThat(after.get("total_xp").asInt()).isPositive();
        assertThat(after.get("recent_history")).hasSize(answered);

        JsonNode progress = getJson("/api/progress", token);
        JsonNode intro = progress.get("sections").get(0);
        assertThat(intro.get("sectionId").asText()).isEqualTo("intro");
        assertThat(intro.get("attempts").asInt()).isEqualTo(answered);
    }

    @Test
    void sessionsAreOwnedByTheirStudent() throws Exception {
        String owner = registerFresh("STUDENT");
        String other = registerFresh("STUDENT");

        long sessionId = postJson("/api/sessions/start", owner, Map.of("topic", "intro")).get("id").asLong();
        long questionId = getJson("/api/sessions/" + sessionId + "/next-question?topic=intro", owner)
                .get("question").get("id").asLong();
        Map<String, Object> attempt = Map.of("questionId", questionId, "correct", true, "responseTimeMs", 1000, "attemptNumber", 1);

        mvc.perform(authed(get("/api/sessions/" + sessionId + "/next-question?topic=intro"), other))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This session belongs to another student"));
        assertThat(statusOf(jsonPost("/api/sessions/" + sessionId + "/attempt", attempt), other)).isEqualTo(403);
        assertThat(statusOf(jsonPost("/api/sessions/" + sessionId + "/complete", Map.of()), other)).isEqualTo(403);

        // The intruder's attempts must not have touched the owner's data.
        assertThat(getJson("/api/dashboard", owner).get("total_answered").asInt()).isZero();

        assertThat(statusOf(get("/api/sessions/99999999/next-question"), owner)).isEqualTo(404);
        assertThat(statusOf(get("/api/sessions/abc/next-question"), owner)).isEqualTo(400);
        assertThat(statusOf(jsonPost("/api/sessions/" + sessionId + "/attempt", Map.of("correct", true)), owner)).isEqualTo(400);
    }

    @Test
    void newStudentNeverSeesAnotherStudentsData() throws Exception {
        // Regression: Dashboard/Progress used to fall back to the first student in the DB
        // when the caller had no profile yet.
        String fresh = registerFresh("STUDENT");

        JsonNode dash = getJson("/api/dashboard", fresh);
        assertThat(dash.get("total_answered").asInt()).isZero();
        assertThat(dash.get("total_xp").asInt()).isZero();

        JsonNode progress = getJson("/api/progress", fresh);
        assertThat(progress.get("totalXp").asInt()).isZero();
        for (JsonNode s : progress.get("sections")) {
            assertThat(s.get("attempts").asInt()).as(s.get("sectionId").asText()).isZero();
        }
    }

    @Test
    void bossVictoryOnlyRewardsTheCaller() throws Exception {
        String student = login(STUDENT);
        int otherXpBefore = getJson("/api/dashboard", login(STUDENT_2)).get("total_xp").asInt();
        int xpBefore = getJson("/api/progress", student).get("totalXp").asInt();

        JsonNode after = postJson("/api/progress/boss-victory", student,
                Map.of("bossTopic", "final_boss", "chapterId", "linear_eq_2var"));

        assertThat(after.get("totalXp").asInt()).isEqualTo(xpBefore + 100);
        assertThat(getJson("/api/dashboard", login(STUDENT_2)).get("total_xp").asInt()).isEqualTo(otherXpBefore);
    }
}
