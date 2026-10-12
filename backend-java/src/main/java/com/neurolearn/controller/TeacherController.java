package com.neurolearn.controller;

import com.neurolearn.dto.DashboardResponse;
import com.neurolearn.dto.TeacherAnalyticsResponse;
import com.neurolearn.service.TeacherService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping("/analytics")
    public ResponseEntity<TeacherAnalyticsResponse> getAnalytics(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(teacherService.getTeacherAnalytics(email));
    }

    @GetMapping("/student/{studentProfileId}")
    public ResponseEntity<DashboardResponse> getStudentDashboard(
            @PathVariable Long studentProfileId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(teacherService.getStudentDashboardForTeacher(studentProfileId));
    }
}
