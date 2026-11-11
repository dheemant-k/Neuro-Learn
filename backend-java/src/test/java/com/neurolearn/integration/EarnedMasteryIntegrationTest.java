package com.neurolearn.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.neurolearn.entity.Question;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "Mastered" must be earned on hard questions (docs/evaluation.md §4.2, §6.1): a high P(L) from
 * easy answers alone no longer shows the badge anywhere.
 */
class EarnedMasteryIntegrationTest extends IntegrationTestBase {

    private JsonNode introSection(String token) throws Exception {
        for (JsonNode s : getJson("/api/progress", token).get("sections")) {
            if (s.get("sectionId").asText().equals("intro")) return s;
        }
        throw new AssertionError("intro section missing");
    }

    private JsonNode introStat(String token) throws Exception {
        for (JsonNode s : getJson("/api/dashboard", token).get("sections")) {
            if (s.get("section_id").asText().equals("intro")) return s;
        }
        throw new AssertionError("intro stat missing");
    }

    @Test
    void easyAnswersAloneDoNotEarnMasteryTwoHardOnesDo() throws Exception {
        String student = registerFresh("STUDENT");
        long sessionId = postJson("/api/sessions/start", student, Map.of("topic", "intro")).get("id").asLong();

        List<Question> easy = questionRepository.findByTopicAndDifficulty("intro", 1);
        List<Question> hard = questionRepository.findByTopicAndDifficulty("intro", 3);
        assertThat(easy).hasSizeGreaterThanOrEqualTo(4);
        assertThat(hard).hasSizeGreaterThanOrEqualTo(2);

        // Four correct easy answers push P(L) past 0.85 ...
        JsonNode last = null;
        for (int i = 0; i < 4; i++) last = answer(student, sessionId, easy.get(i).getId(), true);
        assertThat(last.get("pKnow").asDouble()).isGreaterThanOrEqualTo(0.85);
        // ... but that is not mastery anywhere.
        assertThat(last.get("isMastered").asBoolean()).isFalse();
        assertThat(introSection(student).get("isMastered").asBoolean()).isFalse();
        assertThat(introStat(student).get("is_mastered").asBoolean()).isFalse();
        // Regression: the roadmap reads "pKnow" (it used to be serialised as "pknow", so the UI showed 20%).
        assertThat(introSection(student).get("pKnow").asDouble()).isGreaterThanOrEqualTo(0.85);
        assertThat(introSection(student).has("pknow")).isFalse();

        // The engine says what is still needed instead of "Topic Mastered!".
        JsonNode next = getJson("/api/sessions/" + sessionId + "/next-question?topic=intro", student);
        assertThat(next.get("isMastered").asBoolean()).isFalse();
        assertThat(next.get("adaptiveReason").asText()).contains("answer 2 more level-3 questions correctly");

        // One hard answer: still not enough.
        JsonNode oneHard = answer(student, sessionId, hard.get(0).getId(), true);
        assertThat(oneHard.get("isMastered").asBoolean()).isFalse();

        // Second hard answer earns it — on the quiz response, roadmap and dashboard alike.
        JsonNode twoHard = answer(student, sessionId, hard.get(1).getId(), true);
        assertThat(twoHard.get("isMastered").asBoolean()).isTrue();
        assertThat(introSection(student).get("isMastered").asBoolean()).isTrue();
        assertThat(introStat(student).get("is_mastered").asBoolean()).isTrue();
    }

    @Test
    void wrongHardAnswersDoNotCount() throws Exception {
        String student = registerFresh("STUDENT");
        long sessionId = postJson("/api/sessions/start", student, Map.of("topic", "intro")).get("id").asLong();
        List<Question> easy = questionRepository.findByTopicAndDifficulty("intro", 1);
        List<Question> hard = questionRepository.findByTopicAndDifficulty("intro", 3);

        for (int i = 0; i < 4; i++) answer(student, sessionId, easy.get(i).getId(), true);
        answer(student, sessionId, hard.get(0).getId(), false);
        answer(student, sessionId, hard.get(1).getId(), false);

        assertThat(introSection(student).get("isMastered").asBoolean()).isFalse();
    }

    @Test
    void teacherRosterOnlyShowsMasteredWithHardEvidence() throws Exception {
        String teacher = registerFresh("TEACHER");
        String created = mvc.perform(authed(jsonPost("/api/teacher/classes", Map.of("name", "Mastery check")), teacher))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String code = json.readTree(created).get("joinCode").asText();
        String student = registerFresh("STUDENT");
        postJson("/api/student/classes/join", student, Map.of("joinCode", code));
        long sessionId = postJson("/api/sessions/start", student, Map.of("topic", "intro")).get("id").asLong();
        List<Question> easy = questionRepository.findByTopicAndDifficulty("intro", 1);
        for (int i = 0; i < 4; i++) answer(student, sessionId, easy.get(i).getId(), true);

        JsonNode analytics = getJson("/api/teacher/analytics", teacher);
        assertThat(analytics.get("studentRoster").get(0).get("status").asText()).isEqualTo("On Track");
        assertThat(analytics.get("topicBottlenecks").get(0).get("studentsMastered").asInt()).isZero();

        List<Question> hard = questionRepository.findByTopicAndDifficulty("intro", 3);
        answer(student, sessionId, hard.get(0).getId(), true);
        answer(student, sessionId, hard.get(1).getId(), true);

        analytics = getJson("/api/teacher/analytics", teacher);
        assertThat(analytics.get("studentRoster").get(0).get("status").asText()).isEqualTo("Mastered");
        assertThat(analytics.get("topicBottlenecks").get(0).get("studentsMastered").asInt()).isEqualTo(1);
    }
}
