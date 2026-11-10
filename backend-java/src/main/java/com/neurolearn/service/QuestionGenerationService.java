package com.neurolearn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurolearn.ai.LlmClient;
import com.neurolearn.ai.LlmUnavailableException;
import com.neurolearn.ai.QuestionCandidate;
import com.neurolearn.dto.GenerateQuestionsRequest;
import com.neurolearn.dto.GenerationResultResponse;
import com.neurolearn.dto.GenerationResultResponse.RejectedCandidate;
import com.neurolearn.dto.TeacherQuestionResponse;
import com.neurolearn.entity.*;
import com.neurolearn.exception.ForbiddenException;
import com.neurolearn.exception.NotFoundException;
import com.neurolearn.exception.TooManyRequestsException;
import com.neurolearn.repository.QuestionMisconceptionRepository;
import com.neurolearn.repository.QuestionRepository;
import com.neurolearn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Teacher-requested, LLM-assisted question generation (FUTURE_WORKFLOW Phase 4b,
 * docs/AI_FUTURE_SCOPE.md §4 Tier 1a):
 * <ol>
 *   <li>Prompt with the section, difficulty, theme and a few existing questions as examples.</li>
 *   <li>Structural validation in code ({@link QuestionCandidate#problem()}) + duplicate check.</li>
 *   <li>Self-check: a second, independent call solves each question; keep only those where
 *       the solver agrees with the generator's answer.</li>
 *   <li>Save survivors as PENDING. Students never see them until a teacher approves.</li>
 * </ol>
 * No student data is sent to the model — only curriculum content.
 */
@Service
public class QuestionGenerationService {

    static final String PROMPT_VERSION = "qgen-v2";
    static final Set<String> THEMES = Set.of("batman", "superman", "supergirl");

    /** Plain-language description of each section, so the model knows what to test. */
    static final Map<String, String> TOPIC_DESCRIPTIONS = Map.ofEntries(
            Map.entry("intro", "Linear equations in one and two variables: what they are, forming them from statements, checking a solution"),
            Map.entry("graphing", "Graphing linear equations in two variables on the coordinate plane; slope, intercepts, reading points"),
            Map.entry("pair_of_equations", "Pairs of linear equations: consistent/inconsistent systems, intersecting/parallel/coincident lines"),
            Map.entry("substitution", "Solving a pair of linear equations in two variables by substitution"),
            Map.entry("elimination", "Solving a pair of linear equations in two variables by elimination"),
            Map.entry("word_problems", "Word problems that lead to a pair of linear equations in two variables"),
            Map.entry("final_boss", "Mixed review of linear equations in two variables (all methods)"),
            Map.entry("quad_intro", "Quadratic equations: standard form ax^2 + bx + c = 0, identifying a, b, c, checking roots"),
            Map.entry("quad_factoring", "Solving quadratic equations by factorisation"),
            Map.entry("quad_formula", "Solving quadratic equations with the quadratic formula"),
            Map.entry("quad_discriminant", "Nature of roots using the discriminant b^2 - 4ac"),
            Map.entry("quad_graphing", "Parabolas: vertex, axis of symmetry, roots as x-intercepts"),
            Map.entry("quad_word_problems", "Word problems that lead to quadratic equations"),
            Map.entry("quad_final_boss", "Mixed review of quadratic equations (all methods)")
    );

    private static final String GENERATOR_SYSTEM = """
            You write multiple-choice maths questions for Indian secondary-school students (around class 9-10) \
            in a superhero-themed learning game. Rules:
            - Exactly four options (a, b, c, d) and exactly ONE correct option. Work the answer out step by step \
            before choosing it; the answer must be mathematically correct.
            - Each wrong option must come from a specific, common student mistake. Describe that mistake in one \
            short sentence in mistake_a/b/c/d. Leave the field for the correct option as an empty string.
            - Options must all be different and in the same format (e.g. all "x = ...").
            - Use small whole numbers where possible. No trick questions, no ambiguous wording.
            - Wrap the maths in a short story line featuring the given hero (one or two sentences). \
            Never use real people's names other than the hero and their well-known world.
            - The explanation shows the solution in 2-4 short steps.
            - Write maths as PLAIN TEXT exactly like the examples: x^2, 3x + 2y = 12, √(b^2 - 4ac), ±, Δ, (a)/(b).             Never use LaTeX, never wrap maths in $ signs, no backslash commands.
            - Money is in Indian rupees (₹), never dollars.
            - Difficulty: 1 = one-step/basic recall, 2 = standard application, 3 = multi-step or word problem.
            """;

    private static final String SOLVER_SYSTEM = """
            You are checking a maths quiz. Solve each question independently and carefully, step by step, \
            then report the letter of the single correct option. Do not assume any option is correct \
            because of its position. If no option is correct, choose the closest one.
            """;

    private final LlmClient llm;
    private final QuestionRepository questionRepository;
    private final QuestionMisconceptionRepository misconceptionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;
    private final int dailyLimit;

    public QuestionGenerationService(
            LlmClient llm,
            QuestionRepository questionRepository,
            QuestionMisconceptionRepository misconceptionRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager,
            @Value("${app.ai.daily-generation-limit:40}") int dailyLimit
    ) {
        this.llm = llm;
        this.questionRepository = questionRepository;
        this.misconceptionRepository = misconceptionRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.tx = new TransactionTemplate(transactionManager);
        this.dailyLimit = dailyLimit;
    }

    public GenerationResultResponse generate(String teacherEmail, GenerateQuestionsRequest req) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (teacher.getRole() != Role.TEACHER) {
            throw new ForbiddenException("Only TEACHER accounts can generate questions");
        }
        String topic = req.topic().trim();
        if (!TOPIC_DESCRIPTIONS.containsKey(topic)) {
            throw new IllegalArgumentException("Unknown topic: " + topic);
        }
        String theme = req.characterTheme().trim().toLowerCase(Locale.ROOT);
        if (!THEMES.contains(theme)) {
            throw new IllegalArgumentException("characterTheme must be one of " + new TreeSet<>(THEMES));
        }
        long usedToday = questionRepository.countByCreatedByIdAndCreatedAtAfter(teacher.getId(), OffsetDateTime.now().minusHours(24));
        if (usedToday + req.count() > dailyLimit) {
            throw new TooManyRequestsException("Daily generation limit reached (" + dailyLimit
                    + " questions per 24 h, " + usedToday + " used). Try again later.");
        }

        // 1. Generate
        String raw = llm.generateJson(GENERATOR_SYSTEM, generatorPrompt(topic, req.difficulty(), theme, req.count()),
                generatorSchema(), 0.8);
        List<QuestionCandidate> candidates = parseCandidates(raw, req.count());

        // 2. Structural validation + duplicates
        List<RejectedCandidate> rejected = new ArrayList<>();
        Set<String> existing = new HashSet<>();
        questionRepository.findAllTextsByTopic(topic).forEach(t -> existing.add(QuestionCandidate.normalize(t)));
        List<QuestionCandidate> valid = new ArrayList<>();
        for (QuestionCandidate c : candidates) {
            Optional<String> problem = c.problem();
            if (problem.isPresent()) {
                rejected.add(new RejectedCandidate(c.question(), problem.get()));
            } else if (!existing.add(QuestionCandidate.normalize(c.question()))) {
                rejected.add(new RejectedCandidate(c.question(), "Duplicate of an existing question"));
            } else {
                valid.add(c);
            }
        }

        // 3. Self-check: an independent solve must agree with the generator's answer
        List<QuestionCandidate> verified = new ArrayList<>();
        if (!valid.isEmpty()) {
            Map<Integer, String> solved = solve(valid);
            for (int i = 0; i < valid.size(); i++) {
                QuestionCandidate c = valid.get(i);
                String solverAnswer = solved.get(i + 1);
                if (c.correctKey().equals(solverAnswer)) {
                    verified.add(c);
                } else {
                    rejected.add(new RejectedCandidate(c.question(), "Self-check disagreed: generator said "
                            + c.correctKey() + ", independent solve said " + (solverAnswer == null ? "nothing" : solverAnswer)));
                }
            }
        }

        // 4. Save as PENDING (one short transaction, after the slow LLM calls)
        List<TeacherQuestionResponse> created = tx.execute(status -> {
            List<TeacherQuestionResponse> out = new ArrayList<>();
            for (QuestionCandidate c : verified) {
                Question q = new Question(null, topic, req.difficulty(), c.question().trim(),
                        c.a().trim(), c.b().trim(), c.c().trim(), c.d().trim(),
                        c.correctKey(), c.explanation().trim(), "Math", theme);
                q.setStatus(QuestionStatus.PENDING);
                q.setSource(QuestionSource.LLM);
                q.setCreatedBy(teacher);
                q.setGeneratorModel(llm.modelName());
                q.setPromptVersion(PROMPT_VERSION);
                Question saved = questionRepository.save(q);
                Map<String, String> mistakes = new TreeMap<>();
                for (String k : List.of("A", "B", "C", "D")) {
                    if (k.equals(c.correctKey())) continue;
                    String m = c.mistakes().get(k).trim();
                    misconceptionRepository.save(new QuestionMisconception(saved, k, m));
                    mistakes.put(k, m);
                }
                out.add(TeacherQuestionService.toResponse(saved, mistakes));
            }
            return out;
        });

        return new GenerationResultResponse(created, rejected, llm.modelName());
    }

    // ── Prompts & schemas ─────────────────────────────────────────

    String generatorPrompt(String topic, int difficulty, String theme, int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("Write exactly ").append(count).append(" new questions.\n");
        sb.append("Section: ").append(TOPIC_DESCRIPTIONS.get(topic)).append(" (id: ").append(topic).append(")\n");
        sb.append("Difficulty: ").append(difficulty).append("\n");
        sb.append("Hero: ").append(capitalize(theme)).append("\n\n");

        List<Question> examples = new ArrayList<>(questionRepository.findByTopicAndDifficultyAndCharacterTheme(topic, difficulty, theme));
        if (examples.size() < 3) {
            examples.addAll(questionRepository.findByTopic(topic));
        }
        List<Question> shown = examples.stream().distinct().limit(3).toList();
        if (!shown.isEmpty()) {
            sb.append("Existing questions in this section (match the style and level; do NOT copy them):\n");
            for (Question q : shown) {
                sb.append("- ").append(q.getQuestionText()).append("\n")
                  .append("  A) ").append(q.getOptionA()).append("  B) ").append(q.getOptionB())
                  .append("  C) ").append(q.getOptionC()).append("  D) ").append(q.getOptionD())
                  .append("  Correct: ").append(q.getCorrectAnswer()).append("\n");
            }
        }
        return sb.toString();
    }

    static Map<String, Object> generatorSchema() {
        Map<String, Object> str = Map.of("type", "string");
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "object");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("question", Map.of("type", "string", "description", "Question text with a short story line featuring the hero"));
        props.put("a", str);
        props.put("b", str);
        props.put("c", str);
        props.put("d", str);
        props.put("correct", Map.of("type", "string", "enum", List.of("A", "B", "C", "D")));
        props.put("explanation", Map.of("type", "string", "description", "2-4 short solution steps"));
        props.put("mistake_a", Map.of("type", "string", "description", "Student mistake that leads to option a; empty if a is correct"));
        props.put("mistake_b", Map.of("type", "string", "description", "Student mistake that leads to option b; empty if b is correct"));
        props.put("mistake_c", Map.of("type", "string", "description", "Student mistake that leads to option c; empty if c is correct"));
        props.put("mistake_d", Map.of("type", "string", "description", "Student mistake that leads to option d; empty if d is correct"));
        item.put("properties", props);
        item.put("required", List.copyOf(props.keySet()));
        return Map.of(
                "type", "object",
                "properties", Map.of("questions", Map.of("type", "array", "items", item)),
                "required", List.of("questions")
        );
    }

    static Map<String, Object> solverSchema() {
        Map<String, Object> item = Map.of(
                "type", "object",
                "properties", Map.of(
                        "index", Map.of("type", "integer"),
                        "answer", Map.of("type", "string", "enum", List.of("A", "B", "C", "D"))
                ),
                "required", List.of("index", "answer")
        );
        return Map.of(
                "type", "object",
                "properties", Map.of("answers", Map.of("type", "array", "items", item)),
                "required", List.of("answers")
        );
    }

    /** Asks the model to solve the candidates (numbered Q1..Qn); returns index → letter. */
    private Map<Integer, String> solve(List<QuestionCandidate> candidates) {
        StringBuilder sb = new StringBuilder("Solve each question and give the letter of the correct option.\n\n");
        for (int i = 0; i < candidates.size(); i++) {
            QuestionCandidate c = candidates.get(i);
            sb.append("Q").append(i + 1).append(": ").append(c.question().trim()).append("\n");
            for (String k : List.of("A", "B", "C", "D")) {
                sb.append("  ").append(k).append(") ").append(c.option(k).trim()).append("\n");
            }
            sb.append("\n");
        }
        String raw = llm.generateJson(SOLVER_SYSTEM, sb.toString(), solverSchema(), 0.0);
        Map<Integer, String> out = new HashMap<>();
        try {
            for (JsonNode a : objectMapper.readTree(raw).path("answers")) {
                out.put(a.path("index").asInt(-1), a.path("answer").asText("").trim().toUpperCase(Locale.ROOT));
            }
        } catch (Exception ex) {
            throw new LlmUnavailableException("Unreadable self-check response", ex);
        }
        return out;
    }

    /** Accepts {"questions":[...]} or a bare array; ignores extras beyond {@code max}. */
    List<QuestionCandidate> parseCandidates(String raw, int max) {
        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (Exception ex) {
            throw new LlmUnavailableException("The generator did not return valid JSON", ex);
        }
        JsonNode list = root.isArray() ? root : root.path("questions");
        if (!list.isArray()) {
            throw new LlmUnavailableException("The generator returned no questions");
        }
        List<QuestionCandidate> out = new ArrayList<>();
        for (JsonNode n : list) {
            if (out.size() >= max) break;
            Map<String, String> mistakes = new HashMap<>();
            for (String k : List.of("a", "b", "c", "d")) {
                mistakes.put(k.toUpperCase(Locale.ROOT), text(n, "mistake_" + k));
            }
            // Normalise any LaTeX the model used anyway to the app's plain-text maths style.
            out.add(new QuestionCandidate(text(n, "question"), text(n, "a"), text(n, "b"), text(n, "c"), text(n, "d"),
                    text(n, "correct"), text(n, "explanation"), mistakes).plainText());
        }
        return out;
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
