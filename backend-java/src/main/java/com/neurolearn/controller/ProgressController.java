package com.neurolearn.controller;

import com.neurolearn.dto.ProgressResponse;
import com.neurolearn.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** STUDENT only (see SecurityConfig). */
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
        return ResponseEntity.ok(progressService.getProgress(userDetails.getUsername(), chapterId));
    }

    @PostMapping("/boss-victory")
    public ResponseEntity<ProgressResponse> recordBossVictory(
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String bossTopic = body != null && body.get("bossTopic") != null ? body.get("bossTopic") : "final_boss";
        String chapterId = body != null && body.get("chapterId") != null ? body.get("chapterId") : "linear_eq_2var";
        return ResponseEntity.ok(progressService.awardBossDefeatBonus(userDetails.getUsername(), bossTopic, chapterId));
    }
}
