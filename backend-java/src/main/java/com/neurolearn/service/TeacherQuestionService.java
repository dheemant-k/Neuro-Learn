package com.neurolearn.service;

import com.neurolearn.ai.QuestionCandidate;
import com.neurolearn.dto.TeacherQuestionResponse;
import com.neurolearn.dto.UpdateQuestionRequest;
import com.neurolearn.entity.*;
import com.neurolearn.exception.ForbiddenException;
import com.neurolearn.exception.NotFoundException;
import com.neurolearn.repository.QuestionMisconceptionRepository;
import com.neurolearn.repository.QuestionRepository;
import com.neurolearn.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Teacher review of generated questions: list, edit, approve, reject.
 * A teacher can only review questions they generated, and only while they are PENDING.
 * Approved questions join the shared question bank and can be served to any student.
 */
@Service
public class TeacherQuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionMisconceptionRepository misconceptionRepository;
    private final UserRepository userRepository;

    public TeacherQuestionService(QuestionRepository questionRepository,
                                  QuestionMisconceptionRepository misconceptionRepository,
                                  UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.misconceptionRepository = misconceptionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TeacherQuestionResponse> listOwn(String teacherEmail, QuestionStatus status) {
        User teacher = requireTeacher(teacherEmail);
        List<Question> questions = questionRepository.findByCreatedByIdAndStatusOrderByCreatedAtDesc(teacher.getId(), status);
        Map<Long, Map<String, String>> mistakes = misconceptionsFor(questions.stream().map(Question::getId).toList());
        return questions.stream()
                .map(q -> toResponse(q, mistakes.getOrDefault(q.getId(), Map.of())))
                .toList();
    }

    @Transactional
    public TeacherQuestionResponse approve(String teacherEmail, Long questionId) {
        Question q = ownPending(teacherEmail, questionId);
        q.setStatus(QuestionStatus.APPROVED);
        q.setReviewedAt(OffsetDateTime.now());
        return toResponse(q, misconceptionsFor(List.of(q.getId())).getOrDefault(q.getId(), Map.of()));
    }

    @Transactional
    public TeacherQuestionResponse reject(String teacherEmail, Long questionId) {
        Question q = ownPending(teacherEmail, questionId);
        q.setStatus(QuestionStatus.REJECTED);
        q.setReviewedAt(OffsetDateTime.now());
        return toResponse(q, misconceptionsFor(List.of(q.getId())).getOrDefault(q.getId(), Map.of()));
    }

    /** Applies edits (unset fields keep their value), then re-runs the structural checks. */
    @Transactional
    public TeacherQuestionResponse update(String teacherEmail, Long questionId, UpdateQuestionRequest edit) {
        Question q = ownPending(teacherEmail, questionId);
        Map<String, String> currentMistakes = new HashMap<>(misconceptionsFor(List.of(q.getId())).getOrDefault(q.getId(), Map.of()));
        if (edit.misconceptions() != null) {
            edit.misconceptions().forEach((k, v) -> currentMistakes.put(k.trim().toUpperCase(Locale.ROOT), v));
        }

        QuestionCandidate merged = new QuestionCandidate(
                pick(edit.questionText(), q.getQuestionText()),
                pick(edit.optionA(), q.getOptionA()),
                pick(edit.optionB(), q.getOptionB()),
                pick(edit.optionC(), q.getOptionC()),
                pick(edit.optionD(), q.getOptionD()),
                pick(edit.correctAnswer(), q.getCorrectAnswer()),
                pick(edit.explanation(), q.getExplanation()),
                currentMistakes
        );
        merged.problem().ifPresent(p -> { throw new IllegalArgumentException(p); });
        if (edit.difficulty() != null && (edit.difficulty() < 1 || edit.difficulty() > 3)) {
            throw new IllegalArgumentException("difficulty must be 1-3");
        }

        q.setQuestionText(merged.question().trim());
        q.setOptionA(merged.a().trim());
        q.setOptionB(merged.b().trim());
        q.setOptionC(merged.c().trim());
        q.setOptionD(merged.d().trim());
        q.setCorrectAnswer(merged.correctKey());
        q.setExplanation(merged.explanation().trim());
        if (edit.difficulty() != null) q.setDifficulty(edit.difficulty());

        // Replace misconceptions: one per wrong option under the (possibly new) correct answer.
        List<QuestionMisconception> existing = misconceptionRepository.findByQuestionId(q.getId());
        misconceptionRepository.deleteAll(existing);
        misconceptionRepository.flush();
        Map<String, String> saved = new TreeMap<>();
        for (String k : List.of("A", "B", "C", "D")) {
            if (k.equals(merged.correctKey())) continue;
            String m = merged.mistakes().get(k).trim();
            misconceptionRepository.save(new QuestionMisconception(q, k, m));
            saved.put(k, m);
        }
        return toResponse(q, saved);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Question ownPending(String teacherEmail, Long questionId) {
        User teacher = requireTeacher(teacherEmail);
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found: " + questionId));
        if (q.getCreatedBy() == null || !q.getCreatedBy().getId().equals(teacher.getId())) {
            throw new ForbiddenException("You can only review questions you generated");
        }
        if (q.getStatus() != QuestionStatus.PENDING) {
            throw new IllegalArgumentException("Only pending questions can be reviewed (this one is " + q.getStatus() + ")");
        }
        return q;
    }

    private User requireTeacher(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != Role.TEACHER) {
            throw new ForbiddenException("Only TEACHER accounts can review questions");
        }
        return user;
    }

    private Map<Long, Map<String, String>> misconceptionsFor(Collection<Long> questionIds) {
        if (questionIds.isEmpty()) return Map.of();
        return misconceptionRepository.findByQuestionIdIn(questionIds).stream()
                .collect(Collectors.groupingBy(m -> m.getQuestion().getId(),
                        Collectors.toMap(QuestionMisconception::getOptionKey, QuestionMisconception::getMisconception,
                                (a, b) -> a, TreeMap::new)));
    }

    private static String pick(String edited, String current) {
        return edited != null ? edited : current;
    }

    static TeacherQuestionResponse toResponse(Question q, Map<String, String> mistakes) {
        return new TeacherQuestionResponse(
                q.getId(), q.getTopic(), q.getDifficulty(), q.getCharacterTheme(), q.getQuestionText(),
                q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                q.getCorrectAnswer(), q.getExplanation(), mistakes,
                q.getStatus().name(), q.getSource().name(), q.getGeneratorModel(), q.getCreatedAt()
        );
    }
}
