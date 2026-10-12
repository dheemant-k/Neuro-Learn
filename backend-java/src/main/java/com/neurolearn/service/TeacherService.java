package com.neurolearn.service;

import com.neurolearn.dto.DashboardResponse;
import com.neurolearn.dto.TeacherAnalyticsResponse;
import com.neurolearn.entity.StudentProfile;
import com.neurolearn.entity.TopicProgress;
import com.neurolearn.entity.User;
import com.neurolearn.repository.QuestionAttemptRepository;
import com.neurolearn.repository.StudentProfileRepository;
import com.neurolearn.repository.TopicProgressRepository;
import com.neurolearn.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeacherService {

    private final StudentProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final TopicProgressRepository topicProgressRepository;
    private final QuestionAttemptRepository attemptRepository;
    private final DashboardService dashboardService;

    public TeacherService(
            StudentProfileRepository profileRepository,
            UserRepository userRepository,
            TopicProgressRepository topicProgressRepository,
            QuestionAttemptRepository attemptRepository,
            DashboardService dashboardService
    ) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.topicProgressRepository = topicProgressRepository;
        this.attemptRepository = attemptRepository;
        this.dashboardService = dashboardService;
    }

    public TeacherAnalyticsResponse getTeacherAnalytics(String teacherEmail) {
        List<StudentProfile> profiles = profileRepository.findAll();
        List<TopicProgress> allTopicProgress = topicProgressRepository.findAll();

        List<TeacherAnalyticsResponse.StudentRosterItem> roster = new ArrayList<>();
        for (StudentProfile sp : profiles) {
            User u = sp.getUser();
            String name = u != null ? u.getName() : "Student #" + sp.getId();
            String email = u != null ? u.getEmail() : "unknown";
            int attempts = attemptRepository.findByStudentProfileIdOrderByTimestampDesc(sp.getId()).size();
            double pKnow = sp.getPKnow() != null ? sp.getPKnow() : 0.20;
            double roundedPKnow = Math.round(pKnow * 100.0) / 100.0;
            double acc = sp.getOverallAccuracy() != null ? sp.getOverallAccuracy() : 0.0;

            String status;
            if (roundedPKnow >= 0.85) {
                status = "Mastered";
            } else if (roundedPKnow >= 0.60) {
                status = "On Track";
            } else {
                status = "Needs Attention";
            }

            roster.add(new TeacherAnalyticsResponse.StudentRosterItem(
                    sp.getId(),
                    u != null ? u.getId() : null,
                    name,
                    email,
                    sp.getCurrentLevel() != null ? sp.getCurrentLevel() : 1,
                    sp.getTotalXp() != null ? sp.getTotalXp() : 0,
                    acc,
                    roundedPKnow,
                    attempts,
                    status
            ));
        }

        int totalStudents = roster.size();
        double classAvgXp = totalStudents > 0 
                ? Math.round(roster.stream().mapToInt(TeacherAnalyticsResponse.StudentRosterItem::getTotalXp).average().orElse(0.0) * 10.0) / 10.0 
                : 0.0;
        double classAvgAccuracy = totalStudents > 0 
                ? Math.round(roster.stream().mapToDouble(TeacherAnalyticsResponse.StudentRosterItem::getAccuracy).average().orElse(0.0) * 10.0) / 10.0 
                : 0.0;
        double classAvgPKnow = totalStudents > 0 
                ? Math.round(roster.stream().mapToDouble(TeacherAnalyticsResponse.StudentRosterItem::getPKnow).average().orElse(0.20) * 100.0) / 100.0 
                : 0.20;
        int totalQuestionsAttempted = roster.stream().mapToInt(TeacherAnalyticsResponse.StudentRosterItem::getQuestionsAttempted).sum();

        // Topic bottlenecks
        Map<String, List<TopicProgress>> progressByTopic = allTopicProgress.stream()
                .collect(Collectors.groupingBy(TopicProgress::getTopic));

        List<TeacherAnalyticsResponse.TopicBottleneck> bottlenecks = new ArrayList<>();
        if (progressByTopic.isEmpty()) {
            // Default curriculum topics
            bottlenecks.add(new TeacherAnalyticsResponse.TopicBottleneck("linear-equations", 0.20, 0.0, 0, 0, false, "Curriculum initialized. Awaiting student quiz attempts."));
            bottlenecks.add(new TeacherAnalyticsResponse.TopicBottleneck("elimination", 0.20, 0.0, 0, 0, false, "Curriculum initialized. Awaiting student quiz attempts."));
            bottlenecks.add(new TeacherAnalyticsResponse.TopicBottleneck("substitution", 0.20, 0.0, 0, 0, false, "Curriculum initialized. Awaiting student quiz attempts."));
        } else {
            for (Map.Entry<String, List<TopicProgress>> entry : progressByTopic.entrySet()) {
                String topic = entry.getKey();
                List<TopicProgress> tpList = entry.getValue();

                double avgPKnow = Math.round(tpList.stream().mapToDouble(tp -> tp.getPKnow() != null ? tp.getPKnow() : 0.20).average().orElse(0.20) * 100.0) / 100.0;
                double avgAcc = Math.round(tpList.stream().mapToDouble(tp -> {
                    int a = tp.getAttempts() != null ? tp.getAttempts() : 0;
                    int c = tp.getCorrectCount() != null ? tp.getCorrectCount() : 0;
                    return a > 0 ? (c * 100.0 / a) : 0.0;
                }).average().orElse(0.0) * 10.0) / 10.0;

                int mastered = (int) tpList.stream().filter(tp -> (tp.getPKnow() != null ? tp.getPKnow() : 0.20) >= 0.85).count();
                int struggling = (int) tpList.stream().filter(tp -> (tp.getPKnow() != null ? tp.getPKnow() : 0.20) < 0.60).count();

                boolean alert = struggling > 0 && (avgPKnow < 0.65 || struggling >= mastered);
                String rec = alert
                        ? "⚠️ " + struggling + " student(s) below mastery threshold on " + topic + ". Targeted review suggested."
                        : "✅ Class is proficient with average Bayesian mastery at " + Math.round(avgPKnow * 100) + "%.";

                bottlenecks.add(new TeacherAnalyticsResponse.TopicBottleneck(
                        topic,
                        avgPKnow,
                        avgAcc,
                        mastered,
                        struggling,
                        alert,
                        rec
                ));
            }
        }

        return new TeacherAnalyticsResponse(
                totalStudents,
                classAvgXp,
                classAvgAccuracy,
                classAvgPKnow,
                totalQuestionsAttempted,
                bottlenecks,
                roster
        );
    }

    public DashboardResponse getStudentDashboardForTeacher(Long studentProfileId) {
        return dashboardService.getDashboardDataByProfileId(studentProfileId);
    }
}
