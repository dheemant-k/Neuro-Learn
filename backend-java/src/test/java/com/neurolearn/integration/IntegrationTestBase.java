package com.neurolearn.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurolearn.TestcontainersConfiguration;
import com.neurolearn.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full Spring context + real Postgres (Testcontainers) + Flyway seed data.
 * Tests go through the real security filter chain via MockMvc.
 * Demo accounts from V5 (password "password123") are available; tests that
 * change data register their own fresh users so they don't affect each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
abstract class IntegrationTestBase {

    protected static final String DEMO_PASSWORD = "password123";
    protected static final String STUDENT = "bruce@neurolearn.com";
    protected static final String STUDENT_2 = "dick@neurolearn.com";
    protected static final String TEACHER = "teacher@neurolearn.com";
    protected static final String PARENT = "parent@neurolearn.com";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected QuestionRepository questionRepository;

    /** The API hides answers before submission, so tests read them from the DB. */
    protected String correctOptionOf(long questionId) {
        return questionRepository.findById(questionId).orElseThrow().getCorrectAnswer().trim().toUpperCase();
    }

    protected String wrongOptionOf(long questionId) {
        String correct = correctOptionOf(questionId);
        return correct.equals("A") ? "B" : "A";
    }

    /** Submits an answer that is right or wrong on purpose. */
    protected JsonNode answer(String token, long sessionId, long questionId, boolean correct) throws Exception {
        return postJson("/api/sessions/" + sessionId + "/attempt", token, Map.of(
                "questionId", questionId,
                "selectedOption", correct ? correctOptionOf(questionId) : wrongOptionOf(questionId),
                "responseTimeMs", 4000,
                "attemptNumber", 1
        ));
    }

    protected String login(String email) throws Exception {
        return postJson("/api/auth/login", null, Map.of("email", email, "password", DEMO_PASSWORD))
                .get("token").asText();
    }

    /** Registers a brand-new user and returns their token. */
    protected String registerFresh(String role) throws Exception {
        String email = role.toLowerCase() + "-" + UUID.randomUUID() + "@test.local";
        return postJson("/api/auth/register", null, Map.of(
                "name", "Test " + role,
                "email", email,
                "password", "secret123",
                "role", role
        )).get("token").asText();
    }

    protected MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder req, String token) {
        return token == null ? req : req.header("Authorization", "Bearer " + token);
    }

    protected int statusOf(MockHttpServletRequestBuilder req, String token) throws Exception {
        return mvc.perform(authed(req, token)).andReturn().getResponse().getStatus();
    }

    protected JsonNode getJson(String path, String token) throws Exception {
        MvcResult r = mvc.perform(authed(get(path), token)).andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    protected JsonNode postJson(String path, String token, Object body) throws Exception {
        MvcResult r = mvc.perform(authed(post(path), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    protected MockHttpServletRequestBuilder jsonPost(String path, Object body) throws Exception {
        return post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }
}
