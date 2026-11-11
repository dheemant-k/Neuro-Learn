package com.neurolearn.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.neurolearn.entity.QuestionAttempt;
import com.neurolearn.repository.QuestionAttemptRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phase 4 data foundation: server-side grading, hidden answers, decision + hint logging. */
class GradingAndLoggingIntegrationTest extends IntegrationTestBase {

    @Autowired
    private QuestionAttemptRepository attemptRepository;

    private long startSession(String token) throws Exception {
        return postJson("/api/sessions/start", token, Map.of("topic", "intro")).get("id").asLong();
    }

    private JsonNode nextQuestion(String token, long sessionId) throws Exception {
        return getJson("/api/sessions/" + sessionId + "/next-question?topic=intro&characterTheme=batman", token);
    }

    private Map<String, Object> attemptBody(long questionId, String option) {
        Map<String, Object> body = new HashMap<>();
        body.put("questionId", questionId);
        body.put("selectedOption", option);
        body.put("responseTimeMs", 2500);
        body.put("attemptNumber", 1);
        return body;
    }

    @Test
    void questionsAreServedWithoutTheAnswer() throws Exception {
        String student = registerFresh("STUDENT");
        long sessionId = startSession(student);

        JsonNode q = nextQuestion(student, sessionId).get("question");
        assertThat(q.get("optionA").asText()).isNotBlank();
        assertThat(q.get("correctAnswer").isNull()).isTrue();
        assertThat(q.get("explanation").isNull()).isTrue();

        for (JsonNode listed : getJson("/api/questions?topic=intro", student)) {
            assertThat(listed.get("correctAnswer").isNull()).isTrue();
            assertThat(listed.get("explanation").isNull()).isTrue();
        }
    }

    @Test
    void serverGradesTheSelectedOptionAndRevealsTheAnswerAfterwards() throws Exception {
        String student = registerFresh("STUDENT");
        long sessionId = startSession(student);
        long q1 = nextQuestion(student, sessionId).get("question").get("id").asLong();

        // A client claiming "correct": true for a wrong option is ignored.
        Map<String, Object> cheating = attemptBody(q1, wrongOptionOf(q1).toLowerCase());
        cheating.put("correct", true);
        JsonNode wrong = postJson("/api/sessions/" + sessionId + "/attempt", student, cheating);
        assertThat(wrong.get("correct").asBoolean()).isFalse();
        assertThat(wrong.get("selectedOption").asText()).isEqualTo(wrongOptionOf(q1));
        assertThat(wrong.get("correctAnswer").asText()).isEqualTo(correctOptionOf(q1));
        assertThat(wrong.get("xpEarned").asInt()).isEqualTo(5);

        long q2 = nextQuestion(student, sessionId).get("question").get("id").asLong();
        JsonNode right = answer(student, sessionId, q2, true);
        assertThat(right.get("correct").asBoolean()).isTrue();
        assertThat(right.get("xpEarned").asInt()).isEqualTo(15);
        assertThat(right.get("explanation").asText()).isNotBlank();

        assertThat(statusOf(jsonPost("/api/sessions/" + sessionId + "/attempt", attemptBody(q2, "E")), student)).isEqualTo(400);
        assertThat(statusOf(jsonPost("/api/sessions/" + sessionId + "/attempt", Map.of("questionId", q2)), student)).isEqualTo(400);
    }

    @Test
    void attemptsRecordOptionDifficultyAndTheDecisionThatServedThem() throws Exception {
        String student = registerFresh("STUDENT");
        long sessionId = startSession(student);
        JsonNode next = nextQuestion(student, sessionId);
        long qid = next.get("question").get("id").asLong();

        answer(student, sessionId, qid, false);

        List<QuestionAttempt> attempts = attemptRepository.findBySessionId(sessionId);
        assertThat(attempts).hasSize(1);
        QuestionAttempt a = attempts.get(0);
        assertThat(a.getSelectedOption()).isEqualTo(wrongOptionOf(qid));
        assertThat(a.getCorrect()).isFalse();
        assertThat(a.getHintsUsed()).isZero();
        assertThat(a.getDifficultyServed()).isEqualTo(next.get("question").get("difficulty").asInt());
        assertThat(a.getAdaptiveAction()).isEqualTo(next.get("adaptiveAction").asText());
        assertThat(a.getAdaptiveReason()).isEqualTo(next.get("adaptiveReason").asText());
    }

    @Test
    void fiftyFiftyRemovesTwoWrongOptionsAndIsCountedOnTheAttempt() throws Exception {
        String student = registerFresh("STUDENT");
        long sessionId = startSession(student);
        long qid = nextQuestion(student, sessionId).get("question").get("id").asLong();
        String hintsPath = "/api/sessions/" + sessionId + "/hints";

        JsonNode hint = postJson(hintsPath, student, Map.of("questionId", qid, "type", "FIFTY_FIFTY"));
        assertThat(hint.get("eliminatedOptions")).hasSize(2);
        assertThat(hint.get("hintsUsed").asInt()).isEqualTo(1);
        for (JsonNode opt : hint.get("eliminatedOptions")) {
            assertThat(opt.asText()).isNotEqualTo(correctOptionOf(qid));
        }

        // Asking again returns the same two options and doesn't count twice.
        JsonNode again = postJson(hintsPath, student, Map.of("questionId", qid, "type", "fifty_fifty"));
        assertThat(again.get("eliminatedOptions")).isEqualTo(hint.get("eliminatedOptions"));
        assertThat(again.get("hintsUsed").asInt()).isEqualTo(1);

        JsonNode clue = postJson(hintsPath, student, Map.of("questionId", qid, "type", "CLUE"));
        assertThat(clue.get("eliminatedOptions")).isEmpty();
        assertThat(clue.get("hintsUsed").asInt()).isEqualTo(2);

        JsonNode attempt = answer(student, sessionId, qid, true);
        assertThat(attempt.get("hintsUsed").asInt()).isEqualTo(2);
        assertThat(attemptRepository.findBySessionId(sessionId).get(0).getHintsUsed()).isEqualTo(2);

        mvc.perform(authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(hintsPath), student)
                        .contentType("application/json")
                        .content(json.writeValueAsString(Map.of("questionId", qid, "type", "CLUE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This question has already been answered"));
    }

    @Test
    void correctAnswerAfterFiftyFiftyIsWeakerEvidence() throws Exception {
        // Two fresh students, same prior P(L)=0.20, both answer their first question correctly.
        String plain = registerFresh("STUDENT");
        long s1 = startSession(plain);
        long q1 = nextQuestion(plain, s1).get("question").get("id").asLong();
        double pPlain = answer(plain, s1, q1, true).get("pknow").asDouble();

        String assisted = registerFresh("STUDENT");
        long s2 = startSession(assisted);
        long q2 = nextQuestion(assisted, s2).get("question").get("id").asLong();
        postJson("/api/sessions/" + s2 + "/hints", assisted, Map.of("questionId", q2, "type", "FIFTY_FIFTY"));
        double pAssisted = answer(assisted, s2, q2, true).get("pknow").asDouble();

        assertThat(pAssisted).isLessThan(pPlain).isGreaterThan(0.20);
    }

    @Test
    void hintRulesAndOwnership() throws Exception {
        String owner = registerFresh("STUDENT");
        String other = registerFresh("STUDENT");
        long sessionId = startSession(owner);
        long qid = nextQuestion(owner, sessionId).get("question").get("id").asLong();
        String hintsPath = "/api/sessions/" + sessionId + "/hints";

        assertThat(statusOf(jsonPost(hintsPath, Map.of("questionId", qid, "type", "ANSWER_PLEASE")), owner)).isEqualTo(400);
        assertThat(statusOf(jsonPost(hintsPath, Map.of("type", "CLUE")), owner)).isEqualTo(400);
        assertThat(statusOf(jsonPost(hintsPath, Map.of("questionId", 99999999, "type", "CLUE")), owner)).isEqualTo(404);
        assertThat(statusOf(jsonPost(hintsPath, Map.of("questionId", qid, "type", "FIFTY_FIFTY")), other)).isEqualTo(403);
        assertThat(statusOf(jsonPost(hintsPath, Map.of("questionId", qid, "type", "FIFTY_FIFTY")), login(TEACHER))).isEqualTo(403);
        assertThat(statusOf(get("/api/questions?topic=intro"), null)).isEqualTo(401);
    }
}
