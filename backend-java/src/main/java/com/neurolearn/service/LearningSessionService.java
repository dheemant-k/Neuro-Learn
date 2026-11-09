package com.neurolearn.service;

import com.neurolearn.adaptive.AdaptiveDecision;
import com.neurolearn.adaptive.AdaptiveEngineService;
import com.neurolearn.adaptive.BktUpdateResult;
import com.neurolearn.dto.*;
import com.neurolearn.entity.*;
import com.neurolearn.exception.ForbiddenException;
import com.neurolearn.exception.NotFoundException;
import com.neurolearn.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LearningSessionService {

    private final LearningSessionRepository sessionRepository;
    private final QuestionAttemptRepository attemptRepository;
    private final QuestionRepository questionRepository;
    private final StudentProfileRepository profileRepository;
    private final TopicProgressRepository topicProgressRepository;
    private final UserRepository userRepository;
    private final AdaptiveEngineService adaptiveEngineService;
    private final SessionHintEventRepository hintRepository;

    /** With two options left after a 50:50, a blind guess is right half the time. */
    static final double FIFTY_FIFTY_GUESS_PROBABILITY = 0.5;
    private static final List<String> OPTIONS = List.of("A", "B", "C", "D");

    public LearningSessionService(
            LearningSessionRepository sessionRepository,
            QuestionAttemptRepository attemptRepository,
            QuestionRepository questionRepository,
            StudentProfileRepository profileRepository,
            TopicProgressRepository topicProgressRepository,
            UserRepository userRepository,
            AdaptiveEngineService adaptiveEngineService,
            SessionHintEventRepository hintRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
        this.profileRepository = profileRepository;
        this.topicProgressRepository = topicProgressRepository;
        this.userRepository = userRepository;
        this.adaptiveEngineService = adaptiveEngineService;
        this.hintRepository = hintRepository;
    }

    private StudentProfile getOrCreateProfileForEmail(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != Role.STUDENT) {
            // Defence in depth: SecurityConfig already limits /api/sessions to STUDENT.
            throw new ForbiddenException("Only student accounts can start learning sessions");
        }

        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    StudentProfile newProfile = new StudentProfile(null, user, 1, 0.0, 0, 0.20);
                    return profileRepository.save(newProfile);
                });
    }

    @Transactional
    public SessionResponse startSession(String userEmail, String topic) {
        StudentProfile profile = getOrCreateProfileForEmail(userEmail);

        LearningSession session = new LearningSession();
        session.setStudentProfile(profile);
        session.setStartTime(OffsetDateTime.now());
        session.setQuestionsAttempted(0);
        session.setAccuracy(0.0);

        LearningSession saved = sessionRepository.save(session);

        return new SessionResponse(
                saved.getId(),
                profile.getId(),
                saved.getStartTime(),
                null,
                0,
                0.0
        );
    }

    /**
     * Loads a session and checks it belongs to the calling user.
     * 404 if it doesn't exist, 403 if it belongs to someone else.
     */
    private LearningSession getOwnedSession(Long sessionId, String userEmail) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));
        User caller = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        User owner = session.getStudentProfile().getUser();
        if (owner == null || !owner.getId().equals(caller.getId())) {
            throw new ForbiddenException("This session belongs to another student");
        }
        return session;
    }

    @Transactional
    public NextQuestionResponse getNextAdaptiveQuestion(Long sessionId, String userEmail, String topic, String characterTheme) {
        LearningSession session = getOwnedSession(sessionId, userEmail);

        StudentProfile profile = session.getStudentProfile();
        List<QuestionAttempt> sessionAttempts = attemptRepository.findBySessionId(sessionId);
        Set<Long> attemptedIds = sessionAttempts.stream()
                .map(a -> a.getQuestion().getId())
                .collect(Collectors.toSet());

        int count = sessionAttempts.size();

        // Current difficulty determination
        int lastDifficulty = 1;
        if (!sessionAttempts.isEmpty()) {
            lastDifficulty = sessionAttempts.get(sessionAttempts.size() - 1).getQuestion().getDifficulty();
        }

        // Fetch current Bayesian knowledge belief P(L) for this student & topic
        TopicProgress tp = topicProgressRepository.findByStudentProfileIdAndTopic(profile.getId(), topic).orElse(null);
        double currentPKnow = (tp != null && tp.getPKnow() != null) ? tp.getPKnow() : 0.20;

        // Evaluate next question difficulty via Bayesian Knowledge Tracing
        AdaptiveDecision decision = adaptiveEngineService.evaluateBkt(currentPKnow, lastDifficulty, count);
        int targetDifficulty = decision.nextDifficulty();

        // Find questions matching topic, targetDifficulty, and characterTheme
        List<Question> candidates;
        if (characterTheme != null && !characterTheme.isBlank()) {
            candidates = questionRepository.findByTopicAndDifficultyAndCharacterTheme(topic, targetDifficulty, characterTheme);
        } else {
            candidates = questionRepository.findByTopicAndDifficulty(topic, targetDifficulty);
        }

        List<Question> unattempted = candidates.stream()
                .filter(q -> !attemptedIds.contains(q.getId()))
                .collect(Collectors.toList());

        // Fallback to any unattempted under the topic (with character theme first)
        if (unattempted.isEmpty() && characterTheme != null && !characterTheme.isBlank()) {
            List<Question> allThemeQuestions = questionRepository.findByTopicAndCharacterTheme(topic, characterTheme);
            unattempted = allThemeQuestions.stream()
                    .filter(q -> !attemptedIds.contains(q.getId()))
                    .collect(Collectors.toList());
        }

        // Fallback to any unattempted under topic regardless of theme
        if (unattempted.isEmpty()) {
            List<Question> allTopicQuestions = questionRepository.findByTopic(topic);
            unattempted = allTopicQuestions.stream()
                    .filter(q -> !attemptedIds.contains(q.getId()))
                    .collect(Collectors.toList());
        }

        // If all attempted, take any question from the topic
        Question chosen;
        if (!unattempted.isEmpty()) {
            chosen = unattempted.get(new Random().nextInt(unattempted.size()));
        } else {
            List<Question> allTopicQuestions = (characterTheme != null && !characterTheme.isBlank())
                    ? questionRepository.findByTopicAndCharacterTheme(topic, characterTheme)
                    : questionRepository.findByTopic(topic);
            if (allTopicQuestions.isEmpty()) {
                allTopicQuestions = questionRepository.findByTopic(topic);
            }
            chosen = !allTopicQuestions.isEmpty() ? allTopicQuestions.get(0) : null;
        }

        if (chosen != null) {
            // Remember why this question was served; copied onto the attempt when it's answered.
            session.recordServedQuestion(chosen.getId(), decision.action().name(), decision.reason());
            sessionRepository.save(session);
        }

        return new NextQuestionResponse(
                QuestionResponse.forStudent(chosen),
                targetDifficulty,
                decision.action().name(),
                decision.reason(),
                count + 1,
                decision.pKnow(),
                decision.predictedCorrectProb(),
                decision.isMastered()
        );
    }

    @Transactional
    public QuestionAttemptResponse recordAttempt(Long sessionId, String userEmail, QuestionAttemptRequest request) {
        LearningSession session = getOwnedSession(sessionId, userEmail);

        StudentProfile profile = session.getStudentProfile();
        if (request.getQuestionId() == null) {
            throw new IllegalArgumentException("questionId is required");
        }
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new NotFoundException("Question not found: " + request.getQuestionId()));
        String selected = normalizeOption(request.getSelectedOption());
        if (selected == null) {
            throw new IllegalArgumentException("selectedOption must be one of A, B, C, D");
        }

        // Graded here, never trusted from the client.
        boolean correct = selected.equals(normalizeOption(question.getCorrectAnswer()));

        List<SessionHintEvent> hints = hintRepository.findBySessionIdAndQuestionId(session.getId(), question.getId());
        boolean usedFiftyFifty = hints.stream().anyMatch(h -> h.getHintType() == SessionHintEvent.HintType.FIFTY_FIFTY);

        QuestionAttempt attempt = new QuestionAttempt();
        attempt.setSession(session);
        attempt.setStudentProfile(profile);
        attempt.setQuestion(question);
        attempt.setCorrect(correct);
        attempt.setSelectedOption(selected);
        attempt.setHintsUsed(hints.size());
        attempt.setDifficultyServed(question.getDifficulty());
        if (question.getId().equals(session.getLastQuestionId())) {
            attempt.setAdaptiveAction(session.getLastAdaptiveAction());
            attempt.setAdaptiveReason(session.getLastAdaptiveReason());
        }
        attempt.setResponseTimeMs(request.getResponseTimeMs() != null ? request.getResponseTimeMs() : 0L);
        attempt.setAttemptNumber(request.getAttemptNumber() != null ? request.getAttemptNumber() : 1);
        attempt.setTimestamp(OffsetDateTime.now());
        QuestionAttempt savedAttempt = attemptRepository.save(attempt);

        // XP Award
        int xpEarned = correct ? 15 : 5;
        int newXp = (profile.getTotalXp() != null ? profile.getTotalXp() : 0) + xpEarned;
        profile.setTotalXp(newXp);
        profile.setCurrentLevel((newXp / 100) + 1);

        // Bayesian Knowledge Tracing (BKT) Update
        TopicProgress tp = topicProgressRepository.findByStudentProfileIdAndTopic(profile.getId(), question.getTopic())
                .orElseGet(() -> {
                    TopicProgress newTp = new TopicProgress();
                    newTp.setStudentProfile(profile);
                    newTp.setTopic(question.getTopic());
                    newTp.setAttempts(0);
                    newTp.setCorrectCount(0);
                    newTp.setMasteryScore(0.0);
                    newTp.setPKnow(0.20);
                    return newTp;
                });

        double priorPKnow = tp.getPKnow() != null ? tp.getPKnow() : 0.20;
        BktUpdateResult bktResult = usedFiftyFifty
                ? adaptiveEngineService.updateKnowledge(priorPKnow, correct, FIFTY_FIFTY_GUESS_PROBABILITY)
                : adaptiveEngineService.updateKnowledge(priorPKnow, correct);

        tp.setAttempts(tp.getAttempts() + 1);
        if (correct) {
            tp.setCorrectCount(tp.getCorrectCount() + 1);
        }
        tp.setPKnow(bktResult.updatedPKnow());
        tp.setMasteryScore(Math.round(bktResult.updatedPKnow() * 1000.0) / 10.0);
        topicProgressRepository.save(tp);

        profile.setPKnow(bktResult.updatedPKnow());
        profileRepository.save(profile);

        QuestionAttemptResponse response = new QuestionAttemptResponse(
                savedAttempt.getId(),
                session.getId(),
                question.getId(),
                savedAttempt.getCorrect(),
                savedAttempt.getResponseTimeMs(),
                xpEarned,
                profile.getTotalXp(),
                profile.getCurrentLevel(),
                bktResult.updatedPKnow(),
                bktResult.isMastered(),
                bktResult.explanation()
        );
        response.setSelectedOption(selected);
        response.setCorrectAnswer(normalizeOption(question.getCorrectAnswer()));
        response.setExplanation(question.getExplanation());
        response.setHintsUsed(hints.size());
        return response;
    }

    /**
     * Records a hint/assist on a question in this session. FIFTY_FIFTY removes two wrong options
     * (decided here, since the client no longer knows the answer). Using the same hint twice
     * returns the first result. Hints can't be used after the question was answered.
     */
    @Transactional
    public HintResponse useHint(Long sessionId, String userEmail, HintRequest request) {
        LearningSession session = getOwnedSession(sessionId, userEmail);
        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new NotFoundException("Question not found: " + request.questionId()));
        SessionHintEvent.HintType type;
        try {
            type = SessionHintEvent.HintType.valueOf(request.type().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("type must be FIFTY_FIFTY or CLUE");
        }
        boolean alreadyAnswered = attemptRepository.findBySessionId(session.getId()).stream()
                .anyMatch(a -> a.getQuestion().getId().equals(question.getId()));
        if (alreadyAnswered) {
            throw new IllegalArgumentException("This question has already been answered");
        }

        SessionHintEvent event = hintRepository
                .findBySessionIdAndQuestionIdAndHintType(session.getId(), question.getId(), type)
                .orElseGet(() -> {
                    String eliminated = null;
                    if (type == SessionHintEvent.HintType.FIFTY_FIFTY) {
                        String correctOpt = normalizeOption(question.getCorrectAnswer());
                        List<String> wrong = new ArrayList<>(OPTIONS);
                        wrong.remove(correctOpt);
                        Collections.shuffle(wrong);
                        eliminated = String.join(",", wrong.subList(0, 2).stream().sorted().toList());
                    }
                    return hintRepository.save(new SessionHintEvent(session, question, type, eliminated));
                });

        List<String> eliminated = event.getEliminatedOptions() == null
                ? List.of()
                : List.of(event.getEliminatedOptions().split(","));
        int used = hintRepository.findBySessionIdAndQuestionId(session.getId(), question.getId()).size();
        return new HintResponse(type.name(), eliminated, used);
    }

    /** "a", " B " -> "A"/"B"; anything outside A-D -> null. */
    static String normalizeOption(String option) {
        if (option == null) return null;
        String o = option.trim().toUpperCase(Locale.ROOT);
        return OPTIONS.contains(o) ? o : null;
    }

    @Transactional
    public SessionCompleteResponse completeSession(Long sessionId, String userEmail) {
        LearningSession session = getOwnedSession(sessionId, userEmail);

        StudentProfile profile = session.getStudentProfile();
        List<QuestionAttempt> attempts = attemptRepository.findBySessionId(sessionId);

        int total = attempts.size();
        int correct = (int) attempts.stream().filter(QuestionAttempt::getCorrect).count();
        double accuracy = total > 0 ? ((double) correct / total) * 100.0 : 0.0;

        session.setEndTime(OffsetDateTime.now());
        session.setQuestionsAttempted(total);
        session.setAccuracy(Math.round(accuracy * 10.0) / 10.0);
        sessionRepository.save(session);

        List<QuestionAttempt> allAttempts = attemptRepository.findByStudentProfileIdOrderByTimestampDesc(profile.getId());
        int overallTotal = allAttempts.size();
        int overallCorrect = (int) allAttempts.stream().filter(QuestionAttempt::getCorrect).count();
        double overallAcc = overallTotal > 0 ? ((double) overallCorrect / overallTotal) * 100.0 : 0.0;
        profile.setOverallAccuracy(Math.round(overallAcc * 10.0) / 10.0);
        profileRepository.save(profile);

        Double topicMastery = null;
        if (!attempts.isEmpty()) {
            String lastTopic = attempts.get(attempts.size() - 1).getQuestion().getTopic();
            topicMastery = topicProgressRepository.findByStudentProfileIdAndTopic(profile.getId(), lastTopic)
                    .map(TopicProgress::getMasteryScore)
                    .orElse(accuracy);
        }

        return new SessionCompleteResponse(
                session.getId(),
                total,
                correct,
                session.getAccuracy(),
                profile.getTotalXp(),
                profile.getCurrentLevel(),
                topicMastery != null ? topicMastery : accuracy
        );
    }
}
