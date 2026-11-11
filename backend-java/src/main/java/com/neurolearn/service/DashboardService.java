package com.neurolearn.service;

import com.neurolearn.dto.DashboardResponse;
import com.neurolearn.entity.*;
import com.neurolearn.exception.NotFoundException;
import com.neurolearn.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final StudentProfileRepository profileRepository;
    private final QuestionAttemptRepository attemptRepository;
    private final TopicProgressRepository topicProgressRepository;
    private final MasteryRule masteryRule;

    public DashboardService(
            UserRepository userRepository,
            StudentProfileRepository profileRepository,
            QuestionAttemptRepository attemptRepository,
            TopicProgressRepository topicProgressRepository,
            MasteryRule masteryRule
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.attemptRepository = attemptRepository;
        this.topicProgressRepository = topicProgressRepository;
        this.masteryRule = masteryRule;
    }

    /**
     * The caller's own dashboard. A student with no profile yet gets an empty dashboard;
     * it never falls back to another student's data.
     */
    public DashboardResponse getDashboardData(String userEmail) {
        StudentProfile profile = userRepository.findByEmail(userEmail)
                .flatMap(user -> profileRepository.findByUserId(user.getId()))
                .orElse(null);
        return buildDashboardForProfile(profile);
    }

    public DashboardResponse getDashboardDataByProfileId(Long profileId) {
        StudentProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Student not found: " + profileId));
        return buildDashboardForProfile(profile);
    }

    public DashboardResponse buildDashboardForProfile(StudentProfile profile) {
        if (profile == null) {
            return new DashboardResponse(0, 1, 0, 0, 0.0, 0.20, Collections.emptyList(), Collections.emptyList());
        }

        List<QuestionAttempt> attempts = attemptRepository.findByStudentProfileIdOrderByTimestampDesc(profile.getId());
        int totalAnswered = attempts.size();
        int totalCorrect = (int) attempts.stream().filter(QuestionAttempt::getCorrect).count();
        double accuracy = totalAnswered > 0 ? Math.round(((double) totalCorrect / totalAnswered) * 1000.0) / 10.0 : 0.0;

        // Section breakdown from TopicProgress
        List<TopicProgress> topicProgressList = topicProgressRepository.findByStudentProfileId(profile.getId());
        Map<String, Long> hardCorrect = masteryRule.hardCorrectByTopic(profile.getId());
        List<DashboardResponse.SectionStat> sections = new ArrayList<>();
        double totalPKnow = 0.0;

        for (TopicProgress tp : topicProgressList) {
            int ans = tp.getAttempts() != null ? tp.getAttempts() : 0;
            int cor = tp.getCorrectCount() != null ? tp.getCorrectCount() : 0;
            double acc = ans > 0 ? Math.round(((double) cor / ans) * 1000.0) / 10.0 : 0.0;
            double pKnow = tp.getPKnow() != null ? tp.getPKnow() : 0.20;
            totalPKnow += pKnow;
            boolean isMastered = MasteryRule.isMastered(pKnow, hardCorrect.getOrDefault(tp.getTopic(), 0L));

            sections.add(new DashboardResponse.SectionStat(tp.getTopic(), ans, cor, acc, 0, Math.round(pKnow * 100.0) / 100.0, isMastered));
        }

        double overallPKnow = topicProgressList.isEmpty() 
                ? (profile.getPKnow() != null ? profile.getPKnow() : 0.20)
                : Math.round((totalPKnow / topicProgressList.size()) * 100.0) / 100.0;

        // Recent history (last 10)
        List<DashboardResponse.HistoryItem> recentHistory = attempts.stream()
                .limit(10)
                .map(a -> new DashboardResponse.HistoryItem(
                        a.getQuestion() != null ? a.getQuestion().getTopic() : "intro",
                        a.getCorrect(),
                        a.getResponseTimeMs() != null ? Math.round((a.getResponseTimeMs() / 1000.0) * 10.0) / 10.0 : 0.0
                ))
                .collect(Collectors.toList());

        return new DashboardResponse(
                profile.getTotalXp() != null ? profile.getTotalXp() : 0,
                profile.getCurrentLevel() != null ? profile.getCurrentLevel() : 1,
                totalAnswered,
                totalCorrect,
                accuracy,
                overallPKnow,
                sections,
                recentHistory
        );
    }
}
