package com.neurolearn.controller;

import com.neurolearn.dto.ChildReportCardResponse;
import com.neurolearn.dto.ParentChildSummaryDto;
import com.neurolearn.dto.ParentLinkRequest;
import com.neurolearn.service.ParentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parent")
public class ParentController {

    private final ParentService parentService;

    public ParentController(ParentService parentService) {
        this.parentService = parentService;
    }

    @PostMapping("/link")
    public ResponseEntity<Map<String, String>> linkChild(
            @RequestBody ParentLinkRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String parentEmail = userDetails != null ? userDetails.getUsername() : null;
        if (parentEmail == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        try {
            String message = parentService.linkChild(parentEmail, request.getStudentEmail());
            return ResponseEntity.ok(Map.of("message", message));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/children")
    public ResponseEntity<List<ParentChildSummaryDto>> getLinkedChildren(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        if (email == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(parentService.getLinkedChildren(email));
    }

    @GetMapping("/child/{studentProfileId}")
    public ResponseEntity<ChildReportCardResponse> getChildReportCard(
            @PathVariable Long studentProfileId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        if (email == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            return ResponseEntity.ok(parentService.getChildReportCard(email, studentProfileId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }
}
