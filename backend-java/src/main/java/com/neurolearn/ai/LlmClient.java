package com.neurolearn.ai;

import java.util.Map;

/**
 * Provider-neutral "give me JSON matching this schema" call. Implementations:
 * {@link GeminiLlmClient} (real) and {@link FakeLlmClient} (offline, deterministic — dev and tests).
 * Selected by {@code app.ai.provider}. Never called in the student answer loop.
 */
public interface LlmClient {

    /**
     * @param systemInstruction role and rules for the model
     * @param prompt            the task
     * @param jsonSchema        JSON Schema (subset) the response must follow
     * @param temperature       0 = deterministic, higher = more varied
     * @return the model's JSON text (syntactically valid; semantics are the caller's job)
     * @throws LlmUnavailableException on network/provider errors or an empty answer
     */
    String generateJson(String systemInstruction, String prompt, Map<String, Object> jsonSchema, double temperature);

    /** Recorded on generated questions for provenance, e.g. "gemini-3.8-flash" or "fake". */
    String modelName();
}
