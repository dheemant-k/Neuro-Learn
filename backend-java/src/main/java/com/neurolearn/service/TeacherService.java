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
    private final ClassService classService;

    public TeacherService(
            StudentProfileRepository profileRepository,
            UserRepository userRepository,
            TopicProgressRepository topicProgressRepository,
            QuestionAttemptRepository attemptRepository,
            DashboardService dashboardService,
            ClassService classService
    ) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.topicProgressRepository = topicProgressRepository;
        this.attemptRepository = attemptRepository;
        this.dashboardService = dashboardService;
        this.classService = classService;
    }

    /**
     * Analytics over the teacher's own students only: one class ({@code classId}) or,
     * when {@code classId} is null, everyone enrolled in any class they teach.
     * Roster and topic bottlenecks are both limited to that set.
     */
    public TeacherAnalyticsResponse getTeacherAnalytics(String teacherEmail, Long classId) {
        Set<Long> visibleIds = classService.visibleStudentProfileIds(teacherEmail, classId);
        List<StudentProfile> profiles = visibleIds.isEmpty()
                ? List.of()
                : profileRepository.findAllById(visibleIds).stream()
                        .sorted(Comparator.comparing(StudentProfile::getId))
                        .toList();
        List<TopicProgress> allTopicProgress = visibleIds.isEmpty()
                ? List.of()
                : topicProgressRepository.findByStudentProfileIdIn(visibleIds);

        List<TeacherAnalyticsResponse.StudentRosterItem> roster = new ArrayList<>();
        for (StudentProfile sp : profiles) {
            User u = sp.getUser();
            String name = u != null ? u.getName() : "Student #" + sp.getId();
            String email = u != null ? u.getEmail() : "unknown";
            int attempts = attemptRepository.findByStudentProfileIdOrderByTimestampDesc(sp.getId()).size();
            double pKnow = sp.getPKnow() != null ? sp.getPKnow() : 0.20;
            double roundedPKnow = Math.round(pKnow * 100.0) / 100.0;
            double acc = sp.getOverallAccuracy() != null ? sp.getOverallAccuracy() : 0.0;

            String status = studentStatus(roundedPKnow);

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

        // No attempts yet → empty list (the UI shows a "no attempts yet" message instead of placeholder topics).
        List<TeacherAnalyticsResponse.TopicBottleneck> bottlenecks = new ArrayList<>();
        for (Map.Entry<String, List<TopicProgress>> entry : progressByTopic.entrySet()) {
            String topic = entry.getKey();
            List<TopicProgress> tpList = entry.getValue();

            double avgPKnow = Math.round(tpList.stream().mapToDouble(tp -> tp.getPKnow() != null ? tp.getPKnow() : 0.20).average().orElse(0.20) * 100.0) / 100.0;
            double avgAcc = Math.round(tpList.stream().mapToDouble(tp -> {
                int a = tp.getAttempts() != null ? tp.getAttempts() : 0;
                int c = tp.getCorrectCount() != null ? tp.getCorrectCount() : 0;
                return a > 0 ? (c * 100.0 / a) : 0.0;
            }).average().orElse(0.0) * 10.0) / 10.0;

            int mastered = (int) tpList.stream().filter(tp -> (tp.getPKnow() != null ? tp.getPKnow() : 0.20) >= MASTERED_P_KNOW).count();
            int struggling = (int) tpList.stream().filter(tp -> (tp.getPKnow() != null ? tp.getPKnow() : 0.20) < ON_TRACK_P_KNOW).count();

            boolean alert = isBottleneck(avgPKnow, struggling, mastered);
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

    static final double MASTERED_P_KNOW = 0.85;
    static final double ON_TRACK_P_KNOW = 0.60;
    /** Class average below this flags a topic even if few students struggle. */
    static final double BOTTLENECK_AVG_P_KNOW = 0.65;

    static String studentStatus(double pKnow) {
        if (pKnow >= MASTERED_P_KNOW) return "Mastered";
        if (pKnow >= ON_TRACK_P_KNOW) return "On Track";
        return "Needs Attention";
    }

    /**
     * A topic is a class bottleneck when at least one student struggles (P(L) &lt; 0.60) and
     * either the class average is low or strugglers are at least as many as masters.
     */
    static boolean isBottleneck(double avgPKnow, int struggling, int mastered) {
        return struggling > 0 && (avgPKnow < BOTTLENECK_AVG_P_KNOW || struggling >= mastered);
    }

    /** 404 for an unknown student, 403 if the student isn't in one of this teacher's classes. */
    public DashboardResponse getStudentDashboardForTeacher(String teacherEmail, Long studentProfileId) {
        classService.requireTeacherCanSeeStudent(teacherEmail, studentProfileId);
        return dashboardService.getDashboardDataByProfileId(studentProfileId);
    }
}
