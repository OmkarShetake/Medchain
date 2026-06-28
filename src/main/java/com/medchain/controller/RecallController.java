package com.medchain.controller;

import com.medchain.dto.request.IssueRecallRequest;
import com.medchain.dto.response.RecallResponse;
import com.medchain.service.RecallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Recalls", description = "Medicine recall management")
public class RecallController {

    private final RecallService recallService;

    @PostMapping("/admin/recalls")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Issue recall", description = "Issue a medicine recall (Admin only)")
    public ResponseEntity<RecallResponse> issueRecall(@Valid @RequestBody IssueRecallRequest request) {
        RecallResponse response = recallService.issueRecall(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/recalls")
    @Operation(summary = "Get active recalls", description = "Get all active medicine recalls (Public)")
    public ResponseEntity<List<RecallResponse>> getActiveRecalls() {
        List<RecallResponse> recalls = recallService.getActiveRecalls();
        return ResponseEntity.ok(recalls);
    }

    @GetMapping("/recalls/{id}")
    @Operation(summary = "Get recall by ID", description = "Get recall details by ID (Public)")
    public ResponseEntity<RecallResponse> getRecallById(@PathVariable UUID id) {
        RecallResponse recall = recallService.getRecallById(id);
        return ResponseEntity.ok(recall);
    }

    @GetMapping("/manufacturer/recalls")
    @PreAuthorize("hasRole('MANUFACTURER')")
    @Operation(summary = "Get my recalls", description = "Get recalls for manufacturer's medicines")
    public ResponseEntity<List<RecallResponse>> getMyRecalls() {
        List<RecallResponse> recalls = recallService.getMyRecalls();
        return ResponseEntity.ok(recalls);
    }

    @PatchMapping("/admin/recalls/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate recall", description = "Deactivate a recall (Admin only)")
    public ResponseEntity<RecallResponse> deactivateRecall(@PathVariable UUID id) {
        RecallResponse recall = recallService.deactivateRecall(id);
        return ResponseEntity.ok(recall);
    }
}
