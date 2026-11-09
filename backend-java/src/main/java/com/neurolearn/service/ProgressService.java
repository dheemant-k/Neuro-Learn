package com.neurolearn.service;

import com.neurolearn.dto.ProgressResponse;
import com.neurolearn.entity.*;
import com.neurolearn.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ProgressService {

    private static final Map<String, List<String>> CHAPTER_SECTIONS = Map.of(
            "linear_eq_2var", List.of(
                    "intro",
                    "graphing",
                    "pair_of_equations",
                    "substitution",
                    "elimination",
                    "word_problems"
            ),
            "quadratic_equations", List.of(
                    "quad_intro",
                    "quad_factoring",
                    "quad_formula",
                    "quad_discriminant",
                    "quad_graphing",
                    "quad_word_problems"
            )
    );

    private static final List<String> DEFAULT_SECTION_ORDER = CHAPTER_SECTIONS.get("linear_eq_2var");

    private final UserRepository userRepository;
    private final StudentProfileRepository profileRepository;
    private final TopicProgressRepository topicProgressRepository;

    public ProgressService(
            UserRepository userRepository,
            StudentProfileRepository profileRepository,
            TopicProgressRepository topicProgressRepository
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.topicProgressRepository = topicProgressRepository;
    }

    /** Section counts as completed (unlocks the next one) — deliberately lower than mastery. */
    static final double SECTION_UNLOCK_P_KNOW = 0.40;
    static final int SECTION_UNLOCK_CORRECT = 3;
    static final double MASTERY_P_KNOW = 0.85;
    /** Boss opens after this many completed sections (or all of them, for short chapters). */
    static final int BOSS_UNLOCK_SECTIONS = 3;

    static boolean isSectionCompleted(double pKnow, int attempts, int correct) {
        return pKnow >= SECTION_UNLOCK_P_KNOW || (attempts >= 1 && correct >= SECTION_UNLOCK_CORRECT);
    }

    static boolean isMastered(double pKnow) {
        return pKnow >= MASTERY_P_KNOW;
    }

    static boolean isBossUnlocked(int completedSections, int totalSections) {
        return completedSections >= totalSections || completedSections >= BOSS_UNLOCK_SECTIONS;
    }

    /** The caller's own profile, or null if they have none yet. Never another student's. */
    private StudentProfile getProfileForEmail(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .flatMap(user -> profileRepository.findByUserId(user.getId()))
                .orElse(null);
    }

    public ProgressResponse getProgress(String userEmail) {
        return getProgress(userEmail, "linear_eq_2var");
    }

    public ProgressResponse getProgress(String userEmail, String chapterId) {
        // No profile yet → same roadmap as a brand-new student (all sections, nothing attempted).
        StudentProfile profile = getProfileForEmail(userEmail);

        List<String> sectionOrder = CHAPTER_SECTIONS.getOrDefault(chapterId, DEFAULT_SECTION_ORDER);

        Map<String, TopicProgress> progressMap = new HashMap<>();
        if (profile != null) {
            for (TopicProgress tp : topicProgressRepository.findByStudentProfileId(profile.getId())) {
                progressMap.put(tp.getTopic(), tp);
            }
        }

        List<ProgressResponse.SectionProgressDto> dtoList = new ArrayList<>();
        boolean previousCompleted = true; // First section is always unlocked
        int completedCount = 0;
        double sumPKnow = 0.0;

        for (int i = 0; i < sectionOrder.size(); i++) {
            String secId = sectionOrder.get(i);
            TopicProgress tp = progressMap.get(secId);

            int attempts = tp != null && tp.getAttempts() != null ? tp.getAttempts() : 0;
            int correct = tp != null && tp.getCorrectCount() != null ? tp.getCorrectCount() : 0;
            double accuracy = attempts > 0 ? Math.round(((double) correct / attempts) * 1000.0) / 10.0 : 0.0;
            double pKnow = tp != null && tp.getPKnow() != null ? tp.getPKnow() : 0.20;
            sumPKnow += pKnow;

            boolean isMastered = isMastered(pKnow);
            boolean isCompleted = isSectionCompleted(pKnow, attempts, correct);
            boolean isUnlocked = (i == 0) || previousCompleted;

            if (isCompleted) {
                completedCount++;
            }

            dtoList.add(new ProgressResponse.SectionProgressDto(
                    secId,
                    attempts,
                    correct,
                    accuracy,
                    Math.round(pKnow * 100.0) / 100.0,
                    isMastered,
                    isCompleted,
                    isUnlocked
            ));

            previousCompleted = isCompleted;
        }

        boolean bossUnlocked = isBossUnlocked(completedCount, sectionOrder.size());
        double overallPKnow = Math.round((sumPKnow / sectionOrder.size()) * 100.0) / 100.0;

        return new ProgressResponse(
                profile != null && profile.getTotalXp() != null ? profile.getTotalXp() : 0,
                profile != null && profile.getCurrentLevel() != null ? profile.getCurrentLevel() : 1,
                profile != null && profile.getOverallAccuracy() != null ? profile.getOverallAccuracy() : 0.0,
                overallPKnow,
                bossUnlocked,
                dtoList
        );
    }

    @Transactional
    public ProgressResponse awardBossDefeatBonus(String userEmail) {
        return awardBossDefeatBonus(userEmail, "final_boss", "linear_eq_2var");
    }

    @Transactional
    public ProgressResponse awardBossDefeatBonus(String userEmail, String bossTopic, String chapterId) {
        StudentProfile profile = getProfileForEmail(userEmail);
        if (profile == null) {
            return getProgress(userEmail, chapterId);
        }

        int bonusXp = 100;
        int currentXp = profile.getTotalXp() != null ? profile.getTotalXp() : 0;
        int newXp = currentXp + bonusXp;
        profile.setTotalXp(newXp);
        profile.setCurrentLevel((newXp / 100) + 1);
        profileRepository.save(profile);

        String topicKey = (bossTopic != null && !bossTopic.isBlank()) ? bossTopic : "final_boss";

        // Mark boss progress as completed and mastered
        TopicProgress bossTp = topicProgressRepository.findByStudentProfileIdAndTopic(profile.getId(), topicKey)
                .orElseGet(() -> {
                    TopicProgress tp = new TopicProgress();
                    tp.setStudentProfile(profile);
                    tp.setTopic(topicKey);
                    tp.setAttempts(0);
                    tp.setCorrectCount(0);
                    tp.setMasteryScore(0.0);
                    tp.setPKnow(0.20);
                    return tp;
                });

        bossTp.setAttempts(bossTp.getAttempts() + 5);
        bossTp.setCorrectCount(bossTp.getCorrectCount() + 5);
        bossTp.setPKnow(0.95);
        bossTp.setMasteryScore(100.0);
        topicProgressRepository.save(bossTp);

        return getProgress(userEmail, chapterId);
    }
}
