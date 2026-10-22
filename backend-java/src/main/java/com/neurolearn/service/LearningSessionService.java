package com.neurolearn.service;

import com.neurolearn.adaptive.AdaptiveDecision;
import com.neurolearn.adaptive.AdaptiveEngineService;
import com.neurolearn.adaptive.BktUpdateResult;
import com.neurolearn.dto.*;
import com.neurolearn.entity.*;
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

    public LearningSessionService(
            LearningSessionRepository sessionRepository,
            QuestionAttemptRepository attemptRepository,
            QuestionRepository questionRepository,
            StudentProfileRepository profileRepository,
            TopicProgressRepository topicProgressRepository,
            UserRepository userRepository,
            AdaptiveEngineService adaptiveEngineService
    ) {
        this.sessionRepository = sessionRepository;
        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
        this.profileRepository = profileRepository;
        this.topicProgressRepository = topicProgressRepository;
        this.userRepository = userRepository;
        this.adaptiveEngineService = adaptiveEngineService;
    }

    private StudentProfile getOrCreateProfileForEmail(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + userEmail));

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

    public NextQuestionResponse getNextAdaptiveQuestion(Long sessionId, String topic) {
        return getNextAdaptiveQuestion(sessionId, topic, null);
    }

    public NextQuestionResponse getNextAdaptiveQuestion(Long sessionId, String topic, String characterTheme) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

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

        return new NextQuestionResponse(
                QuestionResponse.fromEntity(chosen),
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
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        StudentProfile profile = session.getStudentProfile();
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + request.getQuestionId()));

        QuestionAttempt attempt = new QuestionAttempt();
        attempt.setSession(session);
        attempt.setStudentProfile(profile);
        attempt.setQuestion(question);
        attempt.setCorrect(request.getCorrect());
        attempt.setResponseTimeMs(request.getResponseTimeMs() != null ? request.getResponseTimeMs() : 0L);
        attempt.setAttemptNumber(request.getAttemptNumber() != null ? request.getAttemptNumber() : 1);
        attempt.setTimestamp(OffsetDateTime.now());
        QuestionAttempt savedAttempt = attemptRepository.save(attempt);

        // XP Award
        int xpEarned = Boolean.TRUE.equals(request.getCorrect()) ? 15 : 5;
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
        BktUpdateResult bktResult = adaptiveEngineService.updateKnowledge(priorPKnow, Boolean.TRUE.equals(request.getCorrect()));

        tp.setAttempts(tp.getAttempts() + 1);
        if (Boolean.TRUE.equals(request.getCorrect())) {
            tp.setCorrectCount(tp.getCorrectCount() + 1);
        }
        tp.setPKnow(bktResult.updatedPKnow());
        tp.setMasteryScore(Math.round(bktResult.updatedPKnow() * 1000.0) / 10.0);
        topicProgressRepository.save(tp);

        profile.setPKnow(bktResult.updatedPKnow());
        profileRepository.save(profile);

        return new QuestionAttemptResponse(
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
    }

    @Transactional
    public SessionCompleteResponse completeSession(Long sessionId, String userEmail) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

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
