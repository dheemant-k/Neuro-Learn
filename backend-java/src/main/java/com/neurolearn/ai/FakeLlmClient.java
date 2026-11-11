package com.neurolearn.ai;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline stand-in for a real LLM (app.ai.provider=fake): no network, no key, deterministic.
 * Lets the whole generate → validate → self-check → review flow run in dev and CI.
 * <ul>
 *   <li>Generator schema (has "questions") → "Solve for x: a x + b = c" items with
 *       computed answers and one tagged mistake per wrong option.</li>
 *   <li>Solver schema (has "answers") → actually solves those items, so the self-check passes.</li>
 * </ul>
 */
public class FakeLlmClient implements LlmClient {

    private static final Pattern LINEAR = Pattern.compile("Solve for x:\\s*(\\d+)x \\+ (\\d+) = (\\d+)");
    private static final Pattern COUNT = Pattern.compile("Write exactly (\\d+) ");
    private static final Pattern ITEM = Pattern.compile("(?m)^Q(\\d+): (.*)$");
    private static final Pattern OPTION = Pattern.compile("(?m)^\\s*([A-D])\\) (.*)$");

    private final ObjectMapper objectMapper;
    private int batch = 0;

    public FakeLlmClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public synchronized String generateJson(String systemInstruction, String prompt, Map<String, Object> jsonSchema, double temperature) {
        Object props = jsonSchema.get("properties");
        try {
            if (props instanceof Map<?, ?> p && p.containsKey("answers")) {
                return objectMapper.writeValueAsString(Map.of("answers", solve(prompt)));
            }
            return objectMapper.writeValueAsString(Map.of("questions", generate(prompt)));
        } catch (Exception ex) {
            throw new LlmUnavailableException("Fake LLM failed", ex);
        }
    }

    private List<Map<String, Object>> generate(String prompt) {
        Matcher m = COUNT.matcher(prompt);
        int count = m.find() ? Integer.parseInt(m.group(1)) : 3;
        batch++;
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int a = 2 + ((batch * 7 + i) % 7);    // 2..8
            int x = 3 + ((batch * 3 + i * 5) % 9); // 3..11
            int b = 1 + ((batch + i * 3) % 9);    // 1..9
            int c = a * x + b;
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("question", "Solve for x: " + a + "x + " + b + " = " + c + " (practice set " + batch + "." + (i + 1) + ")");
            // Distinct by construction: x > 0, a·x > x (a ≥ 2), -x < 0, and B is a fraction string.
            q.put("a", "x = " + x);
            q.put("b", "x = " + (c + b) + "/" + a);
            q.put("c", "x = " + (c - b));
            q.put("d", "x = " + (-x));
            q.put("correct", "A");
            q.put("explanation", "Subtract " + b + " from both sides: " + a + "x = " + (c - b) + ". Divide by " + a + ": x = " + x + ".");
            q.put("mistake_a", "");
            q.put("mistake_b", "Added " + b + " to both sides instead of subtracting it.");
            q.put("mistake_c", "Removed the constant but forgot to divide by " + a + ".");
            q.put("mistake_d", "Sign error when isolating x.");
            out.add(q);
        }
        return out;
    }

    /** Solves the "Solve for x" items it generated; anything else gets "A". */
    private List<Map<String, Object>> solve(String prompt) {
        List<Map<String, Object>> answers = new ArrayList<>();
        String[] blocks = prompt.split("(?m)^(?=Q\\d+: )");
        for (String block : blocks) {
            Matcher item = ITEM.matcher(block);
            if (!item.find()) continue;
            int index = Integer.parseInt(item.group(1));
            String answer = "A";
            Matcher lin = LINEAR.matcher(item.group(2));
            if (lin.find()) {
                int a = Integer.parseInt(lin.group(1));
                int b = Integer.parseInt(lin.group(2));
                int c = Integer.parseInt(lin.group(3));
                String expected = "x = " + ((c - b) / a);
                Matcher opt = OPTION.matcher(block);
                while (opt.find()) {
                    if (opt.group(2).trim().equals(expected)) {
                        answer = opt.group(1);
                        break;
                    }
                }
            }
            answers.add(Map.of("index", index, "answer", answer));
        }
        return answers;
    }

    @Override
    public String modelName() {
        return "fake";
    }
}
