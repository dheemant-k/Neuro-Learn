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

/** PARENT only (see SecurityConfig). Report cards are limited to linked children by ParentService. */
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
        String message = parentService.linkChild(userDetails.getUsername(), request.getStudentEmail());
        return ResponseEntity.ok(Map.of("message", message));
    }

    @GetMapping("/children")
    public ResponseEntity<List<ParentChildSummaryDto>> getLinkedChildren(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(parentService.getLinkedChildren(userDetails.getUsername()));
    }

    @GetMapping("/child/{studentProfileId}")
    public ResponseEntity<ChildReportCardResponse> getChildReportCard(
            @PathVariable Long studentProfileId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(parentService.getChildReportCard(userDetails.getUsername(), studentProfileId));
    }
}
