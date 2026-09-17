package com.neurolearn.service;

import com.neurolearn.adaptive.AdaptiveDecision;
import com.neurolearn.adaptive.AdaptiveEngineService;
import com.neurolearn.adaptive.PerformanceSnapshot;
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
                    StudentProfile newProfile = new StudentProfile(null, user, 1, 0.0, 0);
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
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        List<QuestionAttempt> sessionAttempts = attemptRepository.findBySessionId(sessionId);
        Set<Long> attemptedIds = sessionAttempts.stream()
                .map(a -> a.getQuestion().getId())
                .collect(Collectors.toSet());

        // Calculate performance snapshot
        int streakCorrect = 0;
        int streakIncorrect = 0;
        long totalRt = 0;
        int count = sessionAttempts.size();

        for (int i = sessionAttempts.size() - 1; i >= 0; i--) {
            boolean correct = sessionAttempts.get(i).getCorrect();
            if (correct && streakIncorrect == 0) {
                streakCorrect++;
            } else if (!correct && streakCorrect == 0) {
                streakIncorrect++;
            } else {
                break;
            }
        }

        for (QuestionAttempt qa : sessionAttempts) {
            totalRt += (qa.getResponseTimeMs() != null ? qa.getResponseTimeMs() : 5000L);
        }
        double avgRt = count > 0 ? (double) totalRt / count : 8000.0;

        int correctCount = (int) sessionAttempts.stream().filter(QuestionAttempt::getCorrect).count();
        double recentAcc = count > 0 ? (double) correctCount / count : 1.0;

        // Current difficulty determination
        int lastDifficulty = 1;
        if (!sessionAttempts.isEmpty()) {
            lastDifficulty = sessionAttempts.get(sessionAttempts.size() - 1).getQuestion().getDifficulty();
        }

        PerformanceSnapshot snapshot = new PerformanceSnapshot(
                recentAcc,
                session.getStudentProfile().getOverallAccuracy() != null ? session.getStudentProfile().getOverallAccuracy() / 100.0 : 0.0,
                avgRt,
                streakCorrect,
                streakIncorrect,
                lastDifficulty,
                0.5
        );

        AdaptiveDecision decision = adaptiveEngineService.evaluate(snapshot);
        int targetDifficulty = decision.nextDifficulty();

        // Find question with targetDifficulty, excluding already answered
        List<Question> candidates = questionRepository.findByTopicAndDifficulty(topic, targetDifficulty);
        List<Question> unattempted = candidates.stream()
                .filter(q -> !attemptedIds.contains(q.getId()))
                .collect(Collectors.toList());

        // Fallback to any unattempted under the topic
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
            List<Question> allTopicQuestions = questionRepository.findByTopic(topic);
            chosen = !allTopicQuestions.isEmpty() ? allTopicQuestions.get(0) : null;
        }

        return new NextQuestionResponse(
                QuestionResponse.fromEntity(chosen),
                targetDifficulty,
                decision.action().name(),
                decision.reason(),
                count + 1
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

        session.setQuestionsAttempted(session.getQuestionsAttempted() + 1);
        sessionRepository.save(session);

        int xpEarned = Boolean.TRUE.equals(request.getCorrect()) ? 15 : 5;
        profile.setTotalXp((profile.getTotalXp() != null ? profile.getTotalXp() : 0) + xpEarned);
        int newLevel = (profile.getTotalXp() / 100) + 1;
        profile.setCurrentLevel(newLevel);
        profileRepository.save(profile);

        TopicProgress tp = topicProgressRepository.findByStudentProfileIdAndTopic(profile.getId(), question.getTopic())
                .orElseGet(() -> {
                    TopicProgress newTp = new TopicProgress();
                    newTp.setStudentProfile(profile);
                    newTp.setTopic(question.getTopic());
                    newTp.setAttempts(0);
                    newTp.setCorrectCount(0);
                    newTp.setMasteryScore(0.0);
                    return newTp;
                });

        tp.setAttempts(tp.getAttempts() + 1);
        if (Boolean.TRUE.equals(request.getCorrect())) {
            tp.setCorrectCount(tp.getCorrectCount() + 1);
        }
        double topicMastery = tp.getAttempts() > 0 ? ((double) tp.getCorrectCount() / tp.getAttempts()) * 100.0 : 0.0;
        tp.setMasteryScore(Math.round(topicMastery * 10.0) / 10.0);
        topicProgressRepository.save(tp);

        return new QuestionAttemptResponse(
                savedAttempt.getId(),
                session.getId(),
                question.getId(),
                savedAttempt.getCorrect(),
                savedAttempt.getResponseTimeMs(),
                xpEarned,
                profile.getTotalXp(),
                profile.getCurrentLevel()
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
