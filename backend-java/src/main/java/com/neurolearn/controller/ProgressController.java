package com.neurolearn.controller;

import com.neurolearn.dto.ProgressResponse;
import com.neurolearn.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping
    public ResponseEntity<ProgressResponse> getProgress(
            @RequestParam(required = false, defaultValue = "linear_eq_2var") String chapterId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(progressService.getProgress(email, chapterId));
    }

    @PostMapping("/boss-victory")
    public ResponseEntity<ProgressResponse> recordBossVictory(
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        String bossTopic = body != null ? body.get("bossTopic") : "final_boss";
        String chapterId = body != null ? body.get("chapterId") : "linear_eq_2var";
        return ResponseEntity.ok(progressService.awardBossDefeatBonus(email, bossTopic, chapterId));
    }
}
