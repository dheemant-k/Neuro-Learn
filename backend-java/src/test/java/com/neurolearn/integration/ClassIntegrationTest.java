package com.neurolearn.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phase 3: teachers only see students enrolled in their own classes. */
class ClassIntegrationTest extends IntegrationTestBase {

    private static final String DEMO_CODE = "GOTHAM";

    // ── Helpers ───────────────────────────────────────────────────

    private JsonNode createClass(String teacherToken, String name) throws Exception {
        String body = mvc.perform(authed(post("/api/teacher/classes"), teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private JsonNode join(String studentToken, String code) throws Exception {
        return postJson("/api/student/classes/join", studentToken, Map.of("joinCode", code));
    }

    private long profileIdOf(String studentToken) throws Exception {
        return postJson("/api/sessions/start", studentToken, Map.of("topic", "intro")).get("studentProfileId").asLong();
    }

    private Set<String> rosterEmails(String teacherToken, String query) throws Exception {
        Set<String> emails = new HashSet<>();
        for (JsonNode s : getJson("/api/teacher/analytics" + query, teacherToken).get("studentRoster")) {
            emails.add(s.get("email").asText());
        }
        return emails;
    }

    private long demoClassId(String demoTeacher) throws Exception {
        for (JsonNode c : getJson("/api/teacher/classes", demoTeacher)) {
            if (DEMO_CODE.equals(c.get("joinCode").asText())) return c.get("id").asLong();
        }
        throw new AssertionError("demo class missing");
    }

    // ── Tests ─────────────────────────────────────────────────────

    @Test
    void demoTeacherSeesOnlyTheSeededClass() throws Exception {
        String teacher = login(TEACHER);

        JsonNode classes = getJson("/api/teacher/classes", teacher);
        assertThat(classes).isNotEmpty();
        JsonNode demo = classes.get(0);
        assertThat(demo.get("joinCode").asText()).isEqualTo(DEMO_CODE);

        Set<String> roster = rosterEmails(teacher, "");
        assertThat(roster).contains("bruce@neurolearn.com", "dick@neurolearn.com", "jason@neurolearn.com");
        assertThat(roster).as("testhero@ is not enrolled").doesNotContain("testhero@neurolearn.com");
    }

    @Test
    void studentJoinsWithCodeAndAppearsInThatTeachersRoster() throws Exception {
        String teacher = registerFresh("TEACHER");
        String student = registerFresh("STUDENT");

        JsonNode created = createClass(teacher, "  Period 3 Algebra  ");
        String code = created.get("joinCode").asText();
        assertThat(created.get("name").asText()).isEqualTo("Period 3 Algebra");
        assertThat(code).hasSize(6).matches("[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{6}");
        assertThat(created.get("studentCount").asLong()).isZero();
        JsonNode empty = getJson("/api/teacher/analytics", teacher);
        assertThat(empty.get("totalStudents").asInt()).isZero();
        assertThat(empty.get("topicBottlenecks")).as("no placeholder topics for an empty class").isEmpty();

        JsonNode joined = join(student, " " + code.toLowerCase() + " ");
        assertThat(joined.get("id").asLong()).isEqualTo(created.get("id").asLong());
        assertThat(joined.get("teacherName").asText()).isEqualTo("Test TEACHER");
        join(student, code); // joining twice is harmless

        assertThat(getJson("/api/student/classes", student)).hasSize(1);
        assertThat(getJson("/api/teacher/classes", teacher).get(0).get("studentCount").asLong()).isEqualTo(1);

        JsonNode analytics = getJson("/api/teacher/analytics?classId=" + created.get("id").asLong(), teacher);
        assertThat(analytics.get("totalStudents").asInt()).isEqualTo(1);
        long profileId = analytics.get("studentRoster").get(0).get("studentProfileId").asLong();
        assertThat(statusOf(get("/api/teacher/student/" + profileId), teacher)).isEqualTo(200);
    }

    @Test
    void teachersCannotSeeEachOthersStudentsOrClasses() throws Exception {
        String demoTeacher = login(TEACHER);
        String otherTeacher = registerFresh("TEACHER");
        long demoClass = demoClassId(demoTeacher);

        long bruceId = -1;
        for (JsonNode s : getJson("/api/teacher/analytics", demoTeacher).get("studentRoster")) {
            if (s.get("email").asText().equals("bruce@neurolearn.com")) bruceId = s.get("studentProfileId").asLong();
        }
        assertThat(bruceId).isPositive();

        assertThat(rosterEmails(otherTeacher, "")).isEmpty();
        mvc.perform(authed(get("/api/teacher/student/" + bruceId), otherTeacher))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This student is not in any of your classes"));
        mvc.perform(authed(get("/api/teacher/analytics?classId=" + demoClass), otherTeacher))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This class belongs to another teacher"));
        assertThat(statusOf(delete("/api/teacher/classes/" + demoClass + "/students/" + bruceId), otherTeacher)).isEqualTo(403);
        assertThat(statusOf(get("/api/teacher/analytics?classId=99999999"), otherTeacher)).isEqualTo(404);
        assertThat(statusOf(get("/api/teacher/student/99999999"), otherTeacher)).isEqualTo(404);
    }

    @Test
    void teacherCanRemoveAStudent() throws Exception {
        String teacher = registerFresh("TEACHER");
        String student = registerFresh("STUDENT");
        JsonNode created = createClass(teacher, "Removal test");
        long classId = created.get("id").asLong();
        join(student, created.get("joinCode").asText());
        long profileId = profileIdOf(student);

        assertThat(statusOf(delete("/api/teacher/classes/" + classId + "/students/" + profileId), teacher)).isEqualTo(204);
        assertThat(getJson("/api/teacher/analytics", teacher).get("totalStudents").asInt()).isZero();
        assertThat(statusOf(get("/api/teacher/student/" + profileId), teacher)).isEqualTo(403);
        assertThat(getJson("/api/student/classes", student)).isEmpty();
        assertThat(statusOf(delete("/api/teacher/classes/" + classId + "/students/" + profileId), teacher)).isEqualTo(404);
    }

    @Test
    void bottlenecksOnlyCoverTheTeachersOwnStudents() throws Exception {
        String teacher = registerFresh("TEACHER");
        String student = registerFresh("STUDENT");
        join(student, createClass(teacher, "Bottleneck test").get("joinCode").asText());

        // One answer on 'intro' only. Demo students have progress on many other topics.
        long sessionId = postJson("/api/sessions/start", student, Map.of("topic", "intro")).get("id").asLong();
        long qid = getJson("/api/sessions/" + sessionId + "/next-question?topic=intro", student).get("question").get("id").asLong();
        answer(student, sessionId, qid, false);

        Set<String> topics = new HashSet<>();
        for (JsonNode b : getJson("/api/teacher/analytics", teacher).get("topicBottlenecks")) {
            topics.add(b.get("topic").asText());
        }
        assertThat(topics).containsExactly("intro");
    }

    @Test
    void validationAndRoleRules() throws Exception {
        String teacher = registerFresh("TEACHER");
        String student = registerFresh("STUDENT");

        mvc.perform(authed(post("/api/student/classes/join"), student)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"joinCode\":\"NOPE99\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No class found with that code"));
        assertThat(statusOf(jsonPost("/api/student/classes/join", Map.of("joinCode", "  ")), student)).isEqualTo(400);
        assertThat(statusOf(jsonPost("/api/teacher/classes", Map.of("name", "")), teacher)).isEqualTo(400);
        assertThat(statusOf(jsonPost("/api/teacher/classes", Map.of("name", "x".repeat(101))), teacher)).isEqualTo(400);

        assertThat(statusOf(jsonPost("/api/teacher/classes", Map.of("name", "Nope")), student)).isEqualTo(403);
        assertThat(statusOf(jsonPost("/api/student/classes/join", Map.of("joinCode", DEMO_CODE)), teacher)).isEqualTo(403);
        assertThat(statusOf(get("/api/student/classes"), login(PARENT))).isEqualTo(403);
        assertThat(statusOf(get("/api/teacher/classes"), null)).isEqualTo(401);
        assertThat(statusOf(get("/api/student/classes"), null)).isEqualTo(401);
    }
}
