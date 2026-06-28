package com.medchain.controller;

import com.medchain.dto.request.SubmitReportRequest;
import com.medchain.dto.response.FakeReportResponse;
import com.medchain.entity.FakeReport;
import com.medchain.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Fake medicine report management")
public class ReportController {

    private final ReportService reportService;

    @PostMapping(value = "/reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit fake report", description = "Submit a fake medicine report with optional photo")
    public ResponseEntity<FakeReportResponse> submitReport(
            @Valid @RequestPart("report") SubmitReportRequest request,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        FakeReportResponse response = reportService.submitReport(request, photo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/reports")
    @Operation(summary = "Get my reports", description = "Get all reports submitted by current user")
    public ResponseEntity<Page<FakeReportResponse>> getMyReports(Pageable pageable) {
        Page<FakeReportResponse> reports = reportService.getMyReports(pageable);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "Get report by ID", description = "Get report details by ID")
    public ResponseEntity<FakeReportResponse> getReportById(@PathVariable UUID id) {
        FakeReportResponse report = reportService.getReportById(id);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all reports", description = "Get all fake medicine reports (Admin only)")
    public ResponseEntity<Page<FakeReportResponse>> getAllReports(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        FakeReport.ReportStatus reportStatus = status != null ? 
                FakeReport.ReportStatus.valueOf(status) : null;
        Page<FakeReportResponse> reports = reportService.getAllReports(reportStatus, pageable);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/admin/reports/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get report by ID (Admin)", description = "Get any report by ID (Admin only)")
    public ResponseEntity<FakeReportResponse> getReportByIdAdmin(@PathVariable UUID id) {
        FakeReportResponse report = reportService.getReportById(id);
        return ResponseEntity.ok(report);
    }

    @PatchMapping("/admin/reports/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Verify report", description = "Verify a fake medicine report (Admin only)")
    public ResponseEntity<FakeReportResponse> verifyReport(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String adminNotes = body.get("adminNotes");
        FakeReportResponse report = reportService.verifyReport(id, adminNotes);
        return ResponseEntity.ok(report);
    }

    @PatchMapping("/admin/reports/{id}/dismiss")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dismiss report", description = "Dismiss a fake medicine report (Admin only)")
    public ResponseEntity<FakeReportResponse> dismissReport(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String adminNotes = body.get("adminNotes");
        FakeReportResponse report = reportService.dismissReport(id, adminNotes);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/admin/reports/hotspots")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get hotspots", description = "Get geographic hotspots of fake medicine reports")
    public ResponseEntity<Map<String, Object>> getHotspots() {
        Map<String, Object> hotspots = reportService.getHotspots();
        return ResponseEntity.ok(hotspots);
    }
}
