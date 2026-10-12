package com.neurolearn.service;

import com.neurolearn.dto.ChildReportCardResponse;
import com.neurolearn.dto.DashboardResponse;
import com.neurolearn.dto.ParentChildSummaryDto;
import com.neurolearn.entity.ParentStudentLink;
import com.neurolearn.entity.Role;
import com.neurolearn.entity.StudentProfile;
import com.neurolearn.entity.User;
import com.neurolearn.repository.ParentStudentLinkRepository;
import com.neurolearn.repository.StudentProfileRepository;
import com.neurolearn.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ParentService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ParentStudentLinkRepository linkRepository;
    private final DashboardService dashboardService;

    public ParentService(
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            ParentStudentLinkRepository linkRepository,
            DashboardService dashboardService
    ) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.linkRepository = linkRepository;
        this.dashboardService = dashboardService;
    }

    public String linkChild(String parentEmail, String studentEmail) {
        User parent = userRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new RuntimeException("Parent account not found"));

        if (parent.getRole() != Role.PARENT) {
            throw new RuntimeException("Only PARENT accounts can link children");
        }

        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("No student account found with email: " + studentEmail));

        if (student.getRole() != Role.STUDENT) {
            throw new RuntimeException("The account with email " + studentEmail + " is not a STUDENT account");
        }

        if (linkRepository.existsByParentIdAndStudentId(parent.getId(), student.getId())) {
            return "Already linked to " + student.getName();
        }

        ParentStudentLink link = new ParentStudentLink(parent, student);
        linkRepository.save(link);
        return "Successfully linked to student: " + student.getName();
    }

    public List<ParentChildSummaryDto> getLinkedChildren(String parentEmail) {
        User parent = userRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new RuntimeException("Parent not found"));

        List<ParentStudentLink> links = linkRepository.findByParentId(parent.getId());
        List<ParentChildSummaryDto> result = new ArrayList<>();

        for (ParentStudentLink link : links) {
            User student = link.getStudent();
            StudentProfile profile = studentProfileRepository.findByUserId(student.getId()).orElse(null);

            double pKnow = 0.20;
            double accuracy = 0.0;
            int level = 1;
            int xp = 0;
            Long profileId = null;

            if (profile != null) {
                profileId = profile.getId();
                pKnow = profile.getPKnow() != null ? profile.getPKnow() : 0.20;
                accuracy = profile.getOverallAccuracy() != null ? profile.getOverallAccuracy() : 0.0;
                level = profile.getCurrentLevel() != null ? profile.getCurrentLevel() : 1;
                xp = profile.getTotalXp() != null ? profile.getTotalXp() : 0;
            }

            result.add(new ParentChildSummaryDto(
                    profileId,
                    student.getId(),
                    student.getName(),
                    student.getEmail(),
                    level,
                    xp,
                    Math.round(accuracy * 10.0) / 10.0,
                    Math.round(pKnow * 100.0) / 100.0,
                    link.getCreatedAt()
            ));
        }
        return result;
    }

    public ChildReportCardResponse getChildReportCard(String parentEmail, Long studentProfileId) {
        User parent = userRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new RuntimeException("Parent not found"));

        StudentProfile profile = studentProfileRepository.findById(studentProfileId)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        // Verify the parent is linked to this student
        if (!linkRepository.existsByParentIdAndStudentId(parent.getId(), profile.getUser().getId())) {
            throw new RuntimeException("You are not linked to this student");
        }

        DashboardResponse dashboard = dashboardService.buildDashboardForProfile(profile);
        User student = profile.getUser();

        // Generate strengths and areas for growth from BKT section data
        List<String> strengths = new ArrayList<>();
        List<String> areasForGrowth = new ArrayList<>();

        for (DashboardResponse.SectionStat sec : dashboard.getSections()) {
            double pk = sec.getP_know() != null ? sec.getP_know() : 0.20;
            if (pk >= 0.80) {
                strengths.add(sec.getSection_id() + " — " + Math.round(pk * 100) + "% Bayesian mastery");
            } else if (pk < 0.60) {
                areasForGrowth.add(sec.getSection_id() + " — only " + Math.round(pk * 100) + "% mastery, needs review");
            }
        }

        if (strengths.isEmpty()) strengths.add("Just getting started — encourage consistent daily practice!");
        if (areasForGrowth.isEmpty()) areasForGrowth.add("No significant weak areas detected yet.");

        // Evidence-based parenting tips
        List<String> tips = generateParentingTips(dashboard, student.getName());

        return new ChildReportCardResponse(
                profile.getId(),
                student.getId(),
                student.getName(),
                student.getEmail(),
                dashboard,
                strengths,
                areasForGrowth,
                tips
        );
    }

    private List<String> generateParentingTips(DashboardResponse dashboard, String name) {
        List<String> tips = new ArrayList<>();
        double acc = dashboard.getOverall_accuracy() != null ? dashboard.getOverall_accuracy() : 0.0;
        double pk = dashboard.getOverall_p_know() != null ? dashboard.getOverall_p_know() : 0.20;
        int answered = dashboard.getTotal_answered() != null ? dashboard.getTotal_answered() : 0;

        if (answered == 0) {
            tips.add("💡 " + name + " hasn't started any sessions yet. Set a goal of 10 minutes of daily practice.");
            tips.add("🎮 The platform uses a Batman-themed story — ask " + name + " to show you their progress!");
            tips.add("📅 Consistency beats intensity: 15 minutes daily outperforms 2 hours on weekends.");
        } else {
            if (acc >= 75) {
                tips.add("🌟 " + name + " is performing well (" + acc + "% accuracy). Ask them to explain a concept back to you — teaching reinforces mastery.");
            } else if (acc >= 50) {
                tips.add("📝 " + name + " is making progress but could use support. Sit with them for one session this week to understand where they get stuck.");
            } else {
                tips.add("🤝 " + name + " may be finding this challenging (" + acc + "% accuracy). Consider reviewing the topic together — visual methods like drawing the equations help.");
            }

            if (pk >= 0.70) {
                tips.add("🏆 Bayesian mastery is strong at " + Math.round(pk * 100) + "%. Introduce a real-world challenge: ask " + name + " to calculate a recipe or a budget using simultaneous equations.");
            } else {
                tips.add("🔁 Spaced repetition works: encourage short daily sessions rather than one long weekend session.");
            }

            tips.add("💬 Ask " + name + " what their XP total is today — small gamified check-ins maintain engagement.");
            tips.add("📚 Research tip: students who explain concepts verbally to a parent retain 40% more than silent practice (Bloom, 1984).");
        }
        return tips;
    }
}
