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

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public GeminiLlmClient(RestClient.Builder builder, ObjectMapper objectMapper,
                           String baseUrl, String apiKey, String model) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
        this.objectMapper = objectMapper;
        this.model = model;
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

        String raw;
        try {
            raw = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            // Log status only — the body can echo the prompt.
            log.warn("Gemini call failed: HTTP {}", ex.getStatusCode().value());
            throw new LlmUnavailableException("Gemini returned HTTP " + ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            log.warn("Gemini call failed: {}", ex.getClass().getSimpleName());
            throw new LlmUnavailableException("Could not reach Gemini", ex);
        }
        return extractText(raw);
    }

    /** Joins candidates[0].content.parts[*].text. */
    String extractText(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw == null ? "" : raw);
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            StringBuilder text = new StringBuilder();
            for (JsonNode part : parts) {
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
