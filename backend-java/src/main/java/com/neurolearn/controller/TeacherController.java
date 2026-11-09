package com.neurolearn.controller;

import com.neurolearn.dto.CreateClassRequest;
import com.neurolearn.dto.DashboardResponse;
import com.neurolearn.dto.TeacherAnalyticsResponse;
import com.neurolearn.dto.TeacherClassResponse;
import com.neurolearn.service.ClassService;
import com.neurolearn.service.TeacherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TEACHER only (see SecurityConfig). A teacher sees only students enrolled in
 * their own classes; the scoping lives in ClassService.
 */
@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final TeacherService teacherService;
    private final ClassService classService;

    public TeacherController(TeacherService teacherService, ClassService classService) {
        this.teacherService = teacherService;
        this.classService = classService;
    }

    /** Analytics for one class, or for all of the teacher's classes when classId is omitted. */
    @GetMapping("/analytics")
    public ResponseEntity<TeacherAnalyticsResponse> getAnalytics(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long classId
    ) {
        return ResponseEntity.ok(teacherService.getTeacherAnalytics(userDetails.getUsername(), classId));
    }

    @GetMapping("/student/{studentProfileId}")
    public ResponseEntity<DashboardResponse> getStudentDashboard(
            @PathVariable Long studentProfileId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(teacherService.getStudentDashboardForTeacher(userDetails.getUsername(), studentProfileId));
    }

    @GetMapping("/classes")
    public ResponseEntity<List<TeacherClassResponse>> listClasses(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(classService.listTeacherClasses(userDetails.getUsername()));
    }

    @PostMapping("/classes")
    public ResponseEntity<TeacherClassResponse> createClass(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateClassRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(classService.createClass(userDetails.getUsername(), request.name()));
    }

    @DeleteMapping("/classes/{classId}/students/{studentProfileId}")
    public ResponseEntity<Void> removeStudent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long classId,
            @PathVariable Long studentProfileId
    ) {
        classService.removeStudent(userDetails.getUsername(), classId, studentProfileId);
        return ResponseEntity.noContent().build();
    }
}
