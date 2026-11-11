package com.neurolearn.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Request/response shape of the Gemini REST call, without network or a real key. */
class GeminiLlmClientTest {

    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta";
    private MockRestServiceServer server;
    private GeminiLlmClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiLlmClient(builder, new ObjectMapper(), BASE, "test-key", "gemini-test", 3, 0);
    }

    @Test
    void overloadIsRetriedThenSucceeds() {
        server.expect(requestTo(BASE + "/models/gemini-test:generateContent")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(BASE + "/models/gemini-test:generateContent")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(BASE + "/models/gemini-test:generateContent"))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{}\"}]}}]}", MediaType.APPLICATION_JSON));

        assertThat(client.generateJson("s", "p", Map.of(), 0.0)).isEqualTo("{}");
        server.verify();
    }

    @Test
    void givesUpAfterMaxAttemptsAndDoesNotRetryClientErrors() {
        for (int i = 0; i < 3; i++) {
            server.expect(requestTo(BASE + "/models/gemini-test:generateContent")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        }
        assertThatThrownBy(() -> client.generateJson("s", "p", Map.of(), 0.0))
                .isInstanceOf(LlmUnavailableException.class).hasMessageContaining("HTTP 503").hasMessageContaining("overloaded");
        server.verify();

        server.reset();
        server.expect(requestTo(BASE + "/models/gemini-test:generateContent")).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> client.generateJson("s", "p", Map.of(), 0.0)).hasMessageContaining("HTTP 400");
        server.verify(); // exactly one call: 400 is not retried
    }

    @Test
    void nonAsciiMathSymbolsSurviveWhenContentTypeHasNoCharset() {
        String body = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"x = 3 ± √2\"}]}}]}";
        server.expect(requestTo(BASE + "/models/gemini-test:generateContent"))
                .andRespond(withSuccess(body.getBytes(java.nio.charset.StandardCharsets.UTF_8), MediaType.APPLICATION_JSON));

        assertThat(client.generateJson("s", "p", Map.of(), 0.0)).isEqualTo("x = 3 ± √2");
    }

    @Test
    void thoughtPartsAreSkipped() {
        assertThat(client.extractText("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"thinking...\",\"thought\":true},{\"text\":\"{}\"}]}}]}"))
                .isEqualTo("{}");
    }

    @Test
    void sendsStructuredOutputRequestAndJoinsTextParts() {
        server.expect(requestTo(BASE + "/models/gemini-test:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("sys"))
                .andExpect(jsonPath("$.contents[0].role").value("user"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("prompt"))
                .andExpect(jsonPath("$.generationConfig.temperature").value(0.0))
                .andExpect(jsonPath("$.generationConfig.responseFormat.text.mimeType").value("APPLICATION_JSON"))
                .andExpect(jsonPath("$.generationConfig.responseFormat.text.schema.type").value("object"))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\"answers\\":"},{"text":"[]}"}]},"finishReason":"STOP"}]}
                        """, MediaType.APPLICATION_JSON));

        String out = client.generateJson("sys", "prompt", Map.of("type", "object", "properties", Map.of()), 0.0);

        assertThat(out).isEqualTo("{\"answers\":[]}");
        assertThat(client.modelName()).isEqualTo("gemini-test");
        server.verify();
    }

    @Test
    void apiKeyIsNotPutInTheUrl() {
        server.expect(requestTo(BASE + "/models/gemini-test:generateContent"))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{}\"}]}}]}", MediaType.APPLICATION_JSON));
        client.generateJson("s", "p", Map.of("type", "object"), 0.5);
        server.verify(); // requestTo matched the exact URL: no ?key= query parameter
    }

    @Test
    void httpErrorBecomesLlmUnavailable() {
        server.expect(requestTo(BASE + "/models/gemini-test:generateContent"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> client.generateJson("s", "p", Map.of(), 0.0))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("HTTP 401");
    }

    @Test
    void emptyCandidateBecomesLlmUnavailable() {
        server.expect(requestTo(BASE + "/models/gemini-test:generateContent"))
                .andRespond(withSuccess("{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.generateJson("s", "p", Map.of(), 0.0))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("SAFETY");
    }

    @Test
    void garbageResponseBecomesLlmUnavailable() {
        assertThatThrownBy(() -> client.extractText("not json"))
                .isInstanceOf(LlmUnavailableException.class);
        assertThat(client.extractText("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"ok\"}]}}]}")).isEqualTo("ok");
    }
}
