package com.neurolearn.controller;

import com.neurolearn.dto.JoinClassRequest;
import com.neurolearn.dto.StudentClassResponse;
import com.neurolearn.service.ClassService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** STUDENT only (see SecurityConfig): join a teacher's class with its code, list my classes. */
@RestController
@RequestMapping("/api/student/classes")
public class StudentClassController {

    private final ClassService classService;

    public StudentClassController(ClassService classService) {
        this.classService = classService;
    }

    @GetMapping
    public ResponseEntity<List<StudentClassResponse>> myClasses(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(classService.listStudentClasses(userDetails.getUsername()));
    }

    @PostMapping("/join")
    public ResponseEntity<StudentClassResponse> join(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody JoinClassRequest request
    ) {
        return ResponseEntity.ok(classService.joinClass(userDetails.getUsername(), request.joinCode()));
    }
}
