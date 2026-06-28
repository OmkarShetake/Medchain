package com.medchain.controller;

import com.medchain.dto.response.DashboardStats;
import com.medchain.dto.response.ManufacturerDto;
import com.medchain.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin dashboard and management endpoints")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard stats")
    public ResponseEntity<DashboardStats> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // ── Reports (delegated to ReportController at /api/v1/admin/reports/**) ──
    // Report endpoints are in ReportController to avoid duplicate mappings

    // ── Manufacturers ─────────────────────────────────────────────────────────

    @GetMapping("/manufacturers")
    @Operation(summary = "Get all manufacturers")
    public ResponseEntity<Page<ManufacturerDto>> getAllManufacturers(
            @RequestParam(required = false) Boolean verified,
            Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllManufacturers(verified, pageable));
    }

    @GetMapping("/manufacturers/{id}")
    @Operation(summary = "Get manufacturer by ID")
    public ResponseEntity<ManufacturerDto> getManufacturerById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getManufacturerById(id));
    }

    @PatchMapping("/manufacturers/{id}/verify")
    @Operation(summary = "Verify manufacturer")
    public ResponseEntity<ManufacturerDto> verifyManufacturer(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.verifyManufacturer(id));
    }

    @PatchMapping("/manufacturers/{id}/suspend")
    @Operation(summary = "Suspend manufacturer")
    public ResponseEntity<ManufacturerDto> suspendManufacturer(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.suspendManufacturer(id));
    }

    // ── Analytics ─────────────────────────────────────────────────────────────

    @GetMapping("/analytics")
    @Operation(summary = "Get analytics data")
    public ResponseEntity<Map<String, Object>> getAnalytics(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(adminService.getAnalytics(days));
    }
}
