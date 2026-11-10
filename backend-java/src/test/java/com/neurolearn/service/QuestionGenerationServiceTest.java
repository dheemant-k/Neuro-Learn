package com.neurolearn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurolearn.ai.LlmClient;
import com.neurolearn.ai.LlmUnavailableException;
import com.neurolearn.dto.GenerateQuestionsRequest;
import com.neurolearn.dto.GenerationResultResponse;
import com.neurolearn.entity.Question;
import com.neurolearn.entity.QuestionStatus;
import com.neurolearn.entity.Role;
import com.neurolearn.entity.User;
import com.neurolearn.exception.TooManyRequestsException;
import com.neurolearn.repository.QuestionMisconceptionRepository;
import com.neurolearn.repository.QuestionRepository;
import com.neurolearn.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** The generate → validate → dedupe → self-check pipeline, with a scripted LLM and mocked DB. */
class QuestionGenerationServiceTest {

    /** Returns queued responses in order: first the generator call, then the solver call. */
    static class ScriptedLlm implements LlmClient {
        final Deque<String> responses = new ArrayDeque<>();
        int calls = 0;

        @Override
        public String generateJson(String systemInstruction, String prompt, Map<String, Object> jsonSchema, double temperature) {
            calls++;
            if (responses.isEmpty()) throw new AssertionError("unexpected LLM call #" + calls);
            return responses.poll();
        }

        @Override
        public String modelName() {
            return "scripted";
        }
    }

    private ScriptedLlm llm;
    private QuestionRepository questions;
    private QuestionMisconceptionRepository misconceptions;
    private QuestionGenerationService service;

    private static final String GOOD_1 = """
            {"question":"Batman needs x where 2x + 3 = 11. Find x.","a":"x = 4","b":"x = 7","c":"x = 8","d":"x = -4",
             "correct":"A","explanation":"2x = 8, x = 4.","mistake_a":"","mistake_b":"added 3","mistake_c":"forgot to divide","mistake_d":"sign error"}""";
    private static final String GOOD_2 = """
            {"question":"Batman solves 3x - 2 = 7. Find x.","a":"x = 5/3","b":"x = 3","c":"x = 9","d":"x = -3",
             "correct":"B","explanation":"3x = 9, x = 3.","mistake_a":"subtracted 2","mistake_b":"","mistake_c":"forgot to divide","mistake_d":"sign error"}""";

    @BeforeEach
    void setUp() {
        llm = new ScriptedLlm();
        questions = mock(QuestionRepository.class);
        misconceptions = mock(QuestionMisconceptionRepository.class);
        UserRepository users = mock(UserRepository.class);
        User teacher = new User(7L, "Teacher", "t@x", "hash", Role.TEACHER, null);
        when(users.findByEmail("t@x")).thenReturn(Optional.of(teacher));
        when(questions.countByCreatedByIdAndCreatedAtAfter(anyLong(), any())).thenReturn(0L);
        when(questions.findAllTextsByTopic("intro")).thenReturn(List.of("An existing question about x"));
        when(questions.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new QuestionGenerationService(llm, questions, misconceptions, users, new ObjectMapper(),
                mock(PlatformTransactionManager.class), 40);
    }

    private GenerationResultResponse run(int count) {
        return service.generate("t@x", new GenerateQuestionsRequest("intro", 1, "batman", count));
    }

    @Test
    void keepsOnlyQuestionsTheIndependentSolveAgreesWith() {
        llm.responses.add("{\"questions\":[" + GOOD_1 + "," + GOOD_2 + "]}");
        llm.responses.add("{\"answers\":[{\"index\":1,\"answer\":\"A\"},{\"index\":2,\"answer\":\"C\"}]}");

        GenerationResultResponse r = run(2);

        assertThat(r.created()).hasSize(1);
        assertThat(r.created().get(0).status()).isEqualTo(QuestionStatus.PENDING.name());
        assertThat(r.created().get(0).generatorModel()).isEqualTo("scripted");
        assertThat(r.created().get(0).misconceptions()).containsOnlyKeys("B", "C", "D");
        assertThat(r.rejected()).singleElement()
                .satisfies(x -> assertThat(x.reason()).contains("generator said B, independent solve said C"));
        verify(misconceptions, times(3)).save(any());
    }

    @Test
    void structuralProblemsAndDuplicatesAreRejectedBeforeTheSelfCheck() {
        String badOptions = GOOD_1.replace("\"b\":\"x = 7\"", "\"b\":\"x=4\"");
        String duplicate = GOOD_2.replace("Batman solves 3x - 2 = 7. Find x.", "An existing question about X");
        llm.responses.add("{\"questions\":[" + badOptions + "," + duplicate + "]}");
        // No solver response queued: nothing valid should reach the self-check.

        GenerationResultResponse r = run(2);

        assertThat(r.created()).isEmpty();
        assertThat(r.rejected()).extracting(GenerationResultResponse.RejectedCandidate::reason)
                .containsExactly("Options A and B are the same", "Duplicate of an existing question");
        assertThat(llm.calls).isEqualTo(1);
        verify(questions, never()).save(any());
    }

    @Test
    void duplicatesWithinOneBatchAreRejected() {
        llm.responses.add("{\"questions\":[" + GOOD_1 + "," + GOOD_1 + "]}");
        llm.responses.add("{\"answers\":[{\"index\":1,\"answer\":\"A\"}]}");

        GenerationResultResponse r = run(2);

        assertThat(r.created()).hasSize(1);
        assertThat(r.rejected()).singleElement().satisfies(x -> assertThat(x.reason()).isEqualTo("Duplicate of an existing question"));
    }

    @Test
    void extraQuestionsBeyondTheRequestedCountAreIgnored() {
        llm.responses.add("{\"questions\":[" + GOOD_1 + "," + GOOD_2 + "]}");
        llm.responses.add("{\"answers\":[{\"index\":1,\"answer\":\"A\"}]}");

        assertThat(run(1).created()).hasSize(1);
    }

    @Test
    void invalidJsonFromTheGeneratorIsAProviderError() {
        llm.responses.add("this is not json");
        assertThatThrownBy(() -> run(1)).isInstanceOf(LlmUnavailableException.class);
    }

    @Test
    void unknownTopicOrThemeIsABadRequestAndNoLlmCallIsMade() {
        assertThatThrownBy(() -> service.generate("t@x", new GenerateQuestionsRequest("astrology", 1, "batman", 1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown topic");
        assertThatThrownBy(() -> service.generate("t@x", new GenerateQuestionsRequest("intro", 1, "spiderman", 1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("characterTheme");
        assertThat(llm.calls).isZero();
    }

    @Test
    void dailyLimitIsEnforcedBeforeCallingTheLlm() {
        when(questions.countByCreatedByIdAndCreatedAtAfter(anyLong(), any())).thenReturn(38L);
        assertThatThrownBy(() -> run(3)).isInstanceOf(TooManyRequestsException.class).hasMessageContaining("38 used");
        assertThat(llm.calls).isZero();
    }

    @Test
    void promptContainsSectionDescriptionAndExamplesButNoStudentData() {
        Question example = new Question(1L, "intro", 1, "Example question text", "1", "2", "3", "4", "A", "e", "Math", "batman");
        when(questions.findByTopicAndDifficultyAndCharacterTheme("intro", 1, "batman")).thenReturn(List.of(example));
        when(questions.findByTopic(anyString())).thenReturn(List.of());

        String prompt = service.generatorPrompt("intro", 1, "batman", 2);

        assertThat(prompt).startsWith("Write exactly 2 new questions.")
                .contains("Linear equations in one and two variables")
                .contains("Hero: Batman")
                .contains("Example question text");
    }
}
