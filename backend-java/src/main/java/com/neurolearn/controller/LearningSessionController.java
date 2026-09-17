package com.neurolearn.controller;

import com.neurolearn.dto.*;
import com.neurolearn.service.LearningSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
public class LearningSessionController {

    private final LearningSessionService sessionService;

    public LearningSessionController(LearningSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/start")
    public ResponseEntity<SessionResponse> startSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) SessionStartRequest request
    ) {
        String email = userDetails != null ? userDetails.getUsername() : "testhero@neurolearn.com";
        String topic = request != null ? request.getTopic() : "intro";
        return ResponseEntity.ok(sessionService.startSession(email, topic));
    }

    @GetMapping("/{id}/next-question")
    public ResponseEntity<NextQuestionResponse> getNextAdaptiveQuestion(
            @PathVariable Long id,
            @RequestParam(defaultValue = "intro") String topic
    ) {
        return ResponseEntity.ok(sessionService.getNextAdaptiveQuestion(id, topic));
    }

    @PostMapping("/{id}/attempt")
    public ResponseEntity<QuestionAttemptResponse> recordAttempt(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody QuestionAttemptRequest request
    ) {
        String email = userDetails != null ? userDetails.getUsername() : "testhero@neurolearn.com";
        return ResponseEntity.ok(sessionService.recordAttempt(id, email, request));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<SessionCompleteResponse> completeSession(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails != null ? userDetails.getUsername() : "testhero@neurolearn.com";
        return ResponseEntity.ok(sessionService.completeSession(id, email));
    }
}
