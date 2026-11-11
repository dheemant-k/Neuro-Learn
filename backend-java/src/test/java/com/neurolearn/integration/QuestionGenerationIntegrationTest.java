package com.neurolearn.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 4b end to end with the offline fake LLM (app.ai.provider defaults to "fake"):
 * generated questions are PENDING, invisible to students until a teacher approves them.
 */
class QuestionGenerationIntegrationTest extends IntegrationTestBase {

    private JsonNode generate(String teacherToken, String topic, int count) throws Exception {
        return postJson("/api/teacher/questions/generate", teacherToken,
                Map.of("topic", topic, "difficulty", 1, "characterTheme", "batman", "count", count));
    }

    private Set<Long> servedIds(String studentToken, String topic) throws Exception {
        Set<Long> ids = new HashSet<>();
        for (JsonNode q : getJson("/api/questions?topic=" + topic, studentToken)) ids.add(q.get("id").asLong());
        return ids;
    }

    @Test
    void generatedQuestionsArePendingAndHiddenFromStudentsUntilApproved() throws Exception {
        String teacher = registerFresh("TEACHER");
        String student = registerFresh("STUDENT");

        JsonNode result = generate(teacher, "intro", 3);
        assertThat(result.get("model").asText()).isEqualTo("fake");
        assertThat(result.get("rejected")).isEmpty();
        JsonNode created = result.get("created");
        assertThat(created).hasSize(3);
        JsonNode first = created.get(0);
        assertThat(first.get("status").asText()).isEqualTo("PENDING");
        assertThat(first.get("source").asText()).isEqualTo("LLM");
        assertThat(first.get("correctAnswer").asText()).isEqualTo("A");
        assertThat(first.get("misconceptions").size()).isEqualTo(3);
        long approveId = first.get("id").asLong();
        long rejectId = created.get(1).get("id").asLong();

        // Not served, not answerable while pending.
        assertThat(servedIds(student, "intro")).doesNotContain(approveId, rejectId);
        long sessionId = postJson("/api/sessions/start", student, Map.of("topic", "intro")).get("id").asLong();
        assertThat(statusOf(jsonPost("/api/sessions/" + sessionId + "/attempt",
                Map.of("questionId", approveId, "selectedOption", "A")), student)).isEqualTo(404);

        assertThat(getJson("/api/teacher/questions", teacher)).hasSize(3);

        JsonNode approved = postJson("/api/teacher/questions/" + approveId + "/approve", teacher, Map.of());
        assertThat(approved.get("status").asText()).isEqualTo("APPROVED");
        JsonNode rejected = postJson("/api/teacher/questions/" + rejectId + "/reject", teacher, Map.of());
        assertThat(rejected.get("status").asText()).isEqualTo("REJECTED");

        Set<Long> served = servedIds(student, "intro");
        assertThat(served).contains(approveId).doesNotContain(rejectId);
        assertThat(getJson("/api/teacher/questions", teacher)).hasSize(1);
        assertThat(getJson("/api/teacher/questions?status=approved", teacher)).hasSize(1);

        // Approved question is answerable and graded on the server like any other.
        JsonNode attempt = postJson("/api/sessions/" + sessionId + "/attempt", student,
                Map.of("questionId", approveId, "selectedOption", "A", "responseTimeMs", 1000));
        assertThat(attempt.get("correct").asBoolean()).isTrue();

        // Reviewing twice is refused.
        mvc.perform(authed(post("/api/teacher/questions/" + approveId + "/approve"), teacher))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only pending questions can be reviewed (this one is APPROVED)"));
    }

    @Test
    void teacherCanEditAPendingQuestionAndEditsAreValidated() throws Exception {
        String teacher = registerFresh("TEACHER");
        long id = generate(teacher, "intro", 1).get("created").get(0).get("id").asLong();
        String path = "/api/teacher/questions/" + id;

        JsonNode edited = putJson(path, teacher, Map.of("questionText", "Edited: solve 2x + 2 = 10", "optionA", "x = 4"));
        assertThat(edited.get("questionText").asText()).isEqualTo("Edited: solve 2x + 2 = 10");

        // Making B correct needs a misconception for A (now a wrong option).
        assertThat(statusOf(putRequest(path, Map.of("correctAnswer", "B")), teacher)).isEqualTo(400);
        Map<String, Object> switchCorrect = new HashMap<>();
        switchCorrect.put("correctAnswer", "B");
        switchCorrect.put("misconceptions", Map.of("A", "Picked the value before checking it"));
        JsonNode switched = putJson(path, teacher, switchCorrect);
        assertThat(switched.get("correctAnswer").asText()).isEqualTo("B");
        assertThat(switched.get("misconceptions").has("B")).isFalse();
        assertThat(switched.get("misconceptions").get("A").asText()).isEqualTo("Picked the value before checking it");

        assertThat(statusOf(putRequest(path, Map.of("optionC", "x = 4")), teacher)).isEqualTo(400);  // duplicate of A
        assertThat(statusOf(putRequest(path, Map.of("difficulty", 7)), teacher)).isEqualTo(400);
    }

    @Test
    void onlyTheGeneratingTeacherCanReviewAndOnlyTeachersCanGenerate() throws Exception {
        String teacher = registerFresh("TEACHER");
        String otherTeacher = registerFresh("TEACHER");
        String student = registerFresh("STUDENT");
        long id = generate(teacher, "elimination", 1).get("created").get(0).get("id").asLong();

        assertThat(statusOf(post("/api/teacher/questions/" + id + "/approve"), otherTeacher)).isEqualTo(403);
        assertThat(statusOf(putRequest("/api/teacher/questions/" + id, Map.of("questionText", "hijack")), otherTeacher)).isEqualTo(403);
        assertThat(getJson("/api/teacher/questions", otherTeacher)).isEmpty();
        assertThat(statusOf(post("/api/teacher/questions/99999999/approve"), teacher)).isEqualTo(404);

        Map<String, Object> req = Map.of("topic", "intro", "difficulty", 1, "characterTheme", "batman", "count", 1);
        assertThat(statusOf(jsonPost("/api/teacher/questions/generate", req), student)).isEqualTo(403);
        assertThat(statusOf(jsonPost("/api/teacher/questions/generate", req), login(PARENT))).isEqualTo(403);
        assertThat(statusOf(jsonPost("/api/teacher/questions/generate", req), null)).isEqualTo(401);
        assertThat(statusOf(get("/api/teacher/questions"), student)).isEqualTo(403);
    }

    @Test
    void requestValidationAndDailyLimit() throws Exception {
        String teacher = registerFresh("TEACHER");
        String path = "/api/teacher/questions/generate";

        assertThat(statusOf(jsonPost(path, Map.of("topic", "intro", "difficulty", 1, "characterTheme", "batman", "count", 6)), teacher)).isEqualTo(400);
        assertThat(statusOf(jsonPost(path, Map.of("topic", "intro", "difficulty", 4, "characterTheme", "batman", "count", 1)), teacher)).isEqualTo(400);
        assertThat(statusOf(jsonPost(path, Map.of("topic", "astrology", "difficulty", 1, "characterTheme", "batman", "count", 1)), teacher)).isEqualTo(400);
        assertThat(statusOf(jsonPost(path, Map.of("topic", "intro", "difficulty", 1, "characterTheme", "ironman", "count", 1)), teacher)).isEqualTo(400);
        assertThat(statusOf(get("/api/teacher/questions?status=LOST"), teacher)).isEqualTo(400);

        // Default limit is 40 per rolling 24 h.
        for (int i = 0; i < 8; i++) {
            assertThat(generate(teacher, "substitution", 5).get("created")).hasSize(5);
        }
        mvc.perform(authed(post(path), teacher).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("topic", "intro", "difficulty", 1, "characterTheme", "batman", "count", 1))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Daily generation limit reached")));
    }

    // ── PUT helpers ───────────────────────────────────────────────

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder putRequest(String path, Object body) throws Exception {
        return put(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    private JsonNode putJson(String path, String token, Object body) throws Exception {
        String resp = mvc.perform(authed(putRequest(path, body), token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resp);
    }
}
