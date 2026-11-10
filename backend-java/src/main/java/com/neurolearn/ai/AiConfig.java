package com.neurolearn.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Locale;

/**
 * Chooses the LLM provider from {@code app.ai.provider}:
 * <ul>
 *   <li>{@code fake} (default) — offline, deterministic, no key needed.</li>
 *   <li>{@code gemini} — needs {@code GEMINI_API_KEY}; model from {@code GEMINI_MODEL}.</li>
 * </ul>
 */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    public LlmClient llmClient(
            @Value("${app.ai.provider:fake}") String provider,
            @Value("${app.ai.gemini.api-key:}") String geminiApiKey,
            @Value("${app.ai.gemini.model:gemini-3.8-flash}") String geminiModel,
            @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String geminiBaseUrl,
            @Value("${app.ai.timeout-seconds:60}") int timeoutSeconds,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        switch (provider.trim().toLowerCase(Locale.ROOT)) {
            case "gemini" -> {
                if (geminiApiKey == null || geminiApiKey.isBlank()) {
                    throw new IllegalStateException("app.ai.provider=gemini but GEMINI_API_KEY is not set");
                }
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(Duration.ofSeconds(10));
                factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
                log.info("LLM provider: Gemini ({})", geminiModel);
                return new GeminiLlmClient(restClientBuilder.requestFactory(factory), objectMapper,
                        geminiBaseUrl, geminiApiKey, geminiModel);
            }
            case "fake" -> {
                log.info("LLM provider: offline fake (set AI_PROVIDER=gemini and GEMINI_API_KEY for real generation)");
                return new FakeLlmClient(objectMapper);
            }
            default -> throw new IllegalStateException("Unknown app.ai.provider: " + provider + " (use fake or gemini)");
        }
    }
}
