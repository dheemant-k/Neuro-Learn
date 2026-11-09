package com.neurolearn.controller;

import com.neurolearn.dto.NextQuestionResponse;
import com.neurolearn.dto.QuestionAttemptRequest;
import com.neurolearn.dto.QuestionAttemptResponse;
import com.neurolearn.dto.SessionCompleteResponse;
import com.neurolearn.dto.SessionResponse;
import com.neurolearn.dto.SessionStartRequest;
import com.neurolearn.service.LearningSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/** STUDENT only (see SecurityConfig). Every session call is checked against the caller's ownership. */
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
        String topic = request != null && request.getTopic() != null ? request.getTopic() : "intro";
        return ResponseEntity.ok(sessionService.startSession(userDetails.getUsername(), topic));
    }

    @GetMapping("/{id}/next-question")
    public ResponseEntity<NextQuestionResponse> getNextAdaptiveQuestion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "intro") String topic,
            @RequestParam(required = false, name = "character") String character,
            @RequestParam(required = false, name = "characterTheme") String characterTheme
    ) {
        String theme = characterTheme != null ? characterTheme : character;
        return ResponseEntity.ok(sessionService.getNextAdaptiveQuestion(id, userDetails.getUsername(), topic, theme));
    }

    @PostMapping("/{id}/attempt")
    public ResponseEntity<QuestionAttemptResponse> recordAttempt(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody QuestionAttemptRequest request
    ) {
        return ResponseEntity.ok(sessionService.recordAttempt(id, userDetails.getUsername(), request));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<SessionCompleteResponse> completeSession(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(sessionService.completeSession(id, userDetails.getUsername()));
    }
}
