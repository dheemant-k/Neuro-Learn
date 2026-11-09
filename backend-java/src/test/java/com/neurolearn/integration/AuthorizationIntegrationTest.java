package com.neurolearn.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Who may call what — the access rules from SecurityConfig and the per-resource checks in services. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthorizationIntegrationTest extends IntegrationTestBase {

    private String student;
    private String teacher;
    private String parent;

    @BeforeAll
    void loginDemoAccounts() throws Exception {
        student = login(STUDENT);
        teacher = login(TEACHER);
        parent = login(PARENT);
    }

    // ── Public / anonymous ────────────────────────────────────────

    @Test
    void healthIsPublic() throws Exception {
        assertThat(statusOf(get("/api/health"), null)).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/auth/me", "/api/dashboard", "/api/progress", "/api/questions/topics",
            "/api/teacher/analytics", "/api/teacher/student/1",
            "/api/parent/children", "/api/parent/child/1"
    })
    void anonymousGets401WithJsonBody(String path) throws Exception {
        mvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Login required"));
    }

    @Test
    void anonymousCannotStartSession() throws Exception {
        assertThat(statusOf(jsonPost("/api/sessions/start", Map.of("topic", "intro")), null)).isEqualTo(401);
    }

    @Test
    void invalidTokenIs401() throws Exception {
        assertThat(statusOf(get("/api/auth/me"), "not.a.jwt")).isEqualTo(401);
    }

    @Test
    void wrongPasswordIs400() throws Exception {
        mvc.perform(jsonPost("/api/auth/login", Map.of("email", STUDENT, "password", "wrong")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void unknownPathIsDenied() throws Exception {
        assertThat(statusOf(get("/api/does-not-exist"), student)).isEqualTo(403);
    }

    // ── Role rules ────────────────────────────────────────────────

    @Test
    void studentReachesStudentEndpointsOnly() throws Exception {
        assertThat(statusOf(get("/api/auth/me"), student)).isEqualTo(200);
        assertThat(statusOf(get("/api/dashboard"), student)).isEqualTo(200);
        assertThat(statusOf(get("/api/progress"), student)).isEqualTo(200);
        assertThat(statusOf(get("/api/questions/topics"), student)).isEqualTo(200);

        assertThat(statusOf(get("/api/teacher/analytics"), student)).isEqualTo(403);
        assertThat(statusOf(get("/api/teacher/student/1"), student)).isEqualTo(403);
        assertThat(statusOf(get("/api/parent/children"), student)).isEqualTo(403);
    }

    @Test
    void teacherReachesTeacherEndpointsOnly() throws Exception {
        assertThat(statusOf(get("/api/teacher/analytics"), teacher)).isEqualTo(200);

        assertThat(statusOf(get("/api/dashboard"), teacher)).isEqualTo(403);
        assertThat(statusOf(get("/api/progress"), teacher)).isEqualTo(403);
        assertThat(statusOf(jsonPost("/api/sessions/start", Map.of("topic", "intro")), teacher)).isEqualTo(403);
        assertThat(statusOf(get("/api/parent/children"), teacher)).isEqualTo(403);
    }

    @Test
    void parentReachesParentEndpointsOnly() throws Exception {
        assertThat(statusOf(get("/api/parent/children"), parent)).isEqualTo(200);

        assertThat(statusOf(get("/api/teacher/analytics"), parent)).isEqualTo(403);
        assertThat(statusOf(get("/api/dashboard"), parent)).isEqualTo(403);
        assertThat(statusOf(jsonPost("/api/sessions/start", Map.of("topic", "intro")), parent)).isEqualTo(403);
    }

    @Test
    void teacherDrillDownOfMissingStudentIs404() throws Exception {
        mvc.perform(authed(get("/api/teacher/student/999999"), teacher))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Student not found: 999999"));
    }

    // ── Parent ↔ child links ──────────────────────────────────────

    @Test
    void parentSeesLinkedChildButNotOthers() throws Exception {
        Set<Long> linked = new HashSet<>();
        for (JsonNode child : getJson("/api/parent/children", parent)) {
            linked.add(child.get("studentProfileId").asLong());
        }
        assertThat(linked).as("V5 seeds linked children for the demo parent").isNotEmpty();

        Long unlinked = null;
        for (JsonNode s : getJson("/api/teacher/analytics", teacher).get("studentRoster")) {
            long id = s.get("studentProfileId").asLong();
            if (!linked.contains(id)) { unlinked = id; break; }
        }
        assertThat(unlinked).as("roster has a student this parent is not linked to").isNotNull();

        assertThat(statusOf(get("/api/parent/child/" + linked.iterator().next()), parent)).isEqualTo(200);
        mvc.perform(authed(get("/api/parent/child/" + unlinked), parent))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not linked to this student"));
        assertThat(statusOf(get("/api/parent/child/999999"), parent)).isEqualTo(404);
    }

    // ── CORS ──────────────────────────────────────────────────────

    @Test
    void corsAllowsDevFrontendOnly() throws Exception {
        mvc.perform(options("/api/auth/me")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mvc.perform(options("/api/auth/me")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
