package com.neurolearn.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Google Gemini via the REST {@code generateContent} endpoint with structured output
 * ({@code generationConfig.responseFormat.text} = JSON + schema), as documented at
 * ai.google.dev/gemini-api/docs/structured-output (checked 2026-11).
 * The API key is sent in the {@code x-goog-api-key} header, never in the URL.
 */
public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);

    /** Overload ("high demand") and rate-limit errors are usually temporary, so these are retried. */
    private static final java.util.Set<Integer> RETRYABLE = java.util.Set.of(429, 500, 502, 503, 504);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int maxAttempts;
    private final long baseBackoffMs;

    public GeminiLlmClient(RestClient.Builder builder, ObjectMapper objectMapper,
                           String baseUrl, String apiKey, String model) {
        this(builder, objectMapper, baseUrl, apiKey, model, 4, 2000);
    }

    /**
     * @param maxAttempts   total tries for retryable HTTP errors (429/5xx)
     * @param baseBackoffMs wait before the 2nd try; doubles each time (0 in tests)
     */
    public GeminiLlmClient(RestClient.Builder builder, ObjectMapper objectMapper,
                           String baseUrl, String apiKey, String model, int maxAttempts, long baseBackoffMs) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.baseBackoffMs = Math.max(0, baseBackoffMs);
    }

    @Override
    public String generateJson(String systemInstruction, String prompt, Map<String, Object> jsonSchema, double temperature) {
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "responseFormat", Map.of("text", Map.of(
                                "mimeType", "APPLICATION_JSON",
                                "schema", jsonSchema
                        ))
                )
        );

        for (int attempt = 1; ; attempt++) {
            try {
                // Read bytes and decode as UTF-8 ourselves: JSON is UTF-8 by spec, but a String body
                // with no charset in Content-Type is decoded as ISO-8859-1 ("±" -> "Â±").
                byte[] bytes = restClient.post()
                        .uri("/models/{model}:generateContent", model)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(byte[].class);
                return extractText(bytes == null ? "" : new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();
                // Log status only — the body can echo the prompt.
                if (RETRYABLE.contains(status) && attempt < maxAttempts) {
                    long wait = baseBackoffMs * (1L << (attempt - 1));
                    log.info("Gemini HTTP {} (attempt {}/{}), retrying in {} ms", status, attempt, maxAttempts, wait);
                    sleep(wait);
                    continue;
                }
                log.warn("Gemini call failed: HTTP {} after {} attempt(s)", status, attempt);
                throw new LlmUnavailableException("Gemini returned HTTP " + status
                        + (status == 503 ? " (model overloaded — try again shortly or set GEMINI_MODEL to another model)" : ""), ex);
            } catch (RestClientException ex) {
                log.warn("Gemini call failed: {}", ex.getClass().getSimpleName());
                throw new LlmUnavailableException("Could not reach Gemini", ex);
            }
        }
    }

    private static void sleep(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new LlmUnavailableException("Interrupted while waiting to retry Gemini");
        }
    }

    /** Joins candidates[0].content.parts[*].text. */
    String extractText(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw == null ? "" : raw);
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            StringBuilder text = new StringBuilder();
            for (JsonNode part : parts) {
                if (part.path("thought").asBoolean(false)) continue; // skip thinking summaries, keep the answer
                text.append(part.path("text").asText(""));
            }
            if (text.isEmpty()) {
                String finish = root.path("candidates").path(0).path("finishReason").asText("unknown");
                throw new LlmUnavailableException("Gemini returned no text (finishReason=" + finish + ")");
            }
            return text.toString();
        } catch (LlmUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LlmUnavailableException("Unreadable Gemini response", ex);
        }
    }

    @Override
    public String modelName() {
        return model;
    }
}
