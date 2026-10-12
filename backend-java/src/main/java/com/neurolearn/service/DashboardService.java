package com.neurolearn.service;

import com.neurolearn.dto.DashboardResponse;
import com.neurolearn.entity.*;
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

    public DashboardService(
            UserRepository userRepository,
            StudentProfileRepository profileRepository,
            QuestionAttemptRepository attemptRepository,
            TopicProgressRepository topicProgressRepository
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.attemptRepository = attemptRepository;
        this.topicProgressRepository = topicProgressRepository;
    }

    public DashboardResponse getDashboardData(String userEmail) {
        StudentProfile profile = null;
        if (userEmail != null && !userEmail.isBlank()) {
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isPresent()) {
                profile = profileRepository.findByUserId(userOpt.get().getId()).orElse(null);
            }
        }
        if (profile == null) {
            List<StudentProfile> all = profileRepository.findAll();
            if (!all.isEmpty()) {
                profile = all.get(0);
            }
        }

        return buildDashboardForProfile(profile);
    }

    public DashboardResponse getDashboardDataByProfileId(Long profileId) {
        StudentProfile profile = profileRepository.findById(profileId).orElse(null);
        return buildDashboardForProfile(profile);
    }

    public DashboardResponse getDashboardDataByUserId(Long userId) {
        StudentProfile profile = profileRepository.findByUserId(userId).orElse(null);
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
        List<DashboardResponse.SectionStat> sections = new ArrayList<>();
        double totalPKnow = 0.0;

        for (TopicProgress tp : topicProgressList) {
            int ans = tp.getAttempts() != null ? tp.getAttempts() : 0;
            int cor = tp.getCorrectCount() != null ? tp.getCorrectCount() : 0;
            double acc = ans > 0 ? Math.round(((double) cor / ans) * 1000.0) / 10.0 : 0.0;
            double pKnow = tp.getPKnow() != null ? tp.getPKnow() : 0.20;
            totalPKnow += pKnow;
            boolean isMastered = pKnow >= 0.85;

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
