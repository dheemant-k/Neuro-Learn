package com.neurolearn.controller;

import com.neurolearn.dto.GenerateQuestionsRequest;
import com.neurolearn.dto.GenerationResultResponse;
import com.neurolearn.dto.TeacherQuestionResponse;
import com.neurolearn.dto.UpdateQuestionRequest;
import com.neurolearn.entity.QuestionStatus;
import com.neurolearn.service.QuestionGenerationService;
import com.neurolearn.service.TeacherQuestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

/** TEACHER only (under /api/teacher/**): generate questions with the LLM and review them. */
@RestController
@RequestMapping("/api/teacher/questions")
public class TeacherQuestionController {

    private final QuestionGenerationService generationService;
    private final TeacherQuestionService reviewService;

    public TeacherQuestionController(QuestionGenerationService generationService, TeacherQuestionService reviewService) {
        this.generationService = generationService;
        this.reviewService = reviewService;
    }

    @PostMapping("/generate")
    public ResponseEntity<GenerationResultResponse> generate(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GenerateQuestionsRequest request
    ) {
        return ResponseEntity.ok(generationService.generate(userDetails.getUsername(), request));
    }

    /** The teacher's own generated questions with this status (default PENDING). */
    @GetMapping
    public ResponseEntity<List<TeacherQuestionResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "PENDING") String status
    ) {
        QuestionStatus s;
        try {
            s = QuestionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("status must be PENDING, APPROVED or REJECTED");
        }
        return ResponseEntity.ok(reviewService.listOwn(userDetails.getUsername(), s));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeacherQuestionResponse> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody UpdateQuestionRequest request
    ) {
        return ResponseEntity.ok(reviewService.update(userDetails.getUsername(), id, request));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<TeacherQuestionResponse> approve(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        return ResponseEntity.ok(reviewService.approve(userDetails.getUsername(), id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<TeacherQuestionResponse> reject(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        return ResponseEntity.ok(reviewService.reject(userDetails.getUsername(), id));
    }
}
