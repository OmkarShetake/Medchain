package com.medchain.controller;

import com.medchain.repository.FakeReportRepository;
import com.medchain.repository.ManufacturerRepository;
import com.medchain.repository.MedicineRepository;
import com.medchain.repository.ScanLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Tag(name = "Public Stats", description = "Public platform statistics")
public class PublicStatsController {

    private final MedicineRepository medicineRepository;
    private final ScanLogRepository scanLogRepository;
    private final FakeReportRepository reportRepository;
    private final ManufacturerRepository manufacturerRepository;

    @GetMapping("/public")
    @Operation(summary = "Get public platform statistics")
    public ResponseEntity<Map<String, Long>> getPublicStats() {
        long totalMedicines    = medicineRepository.count();
        long totalScans        = scanLogRepository.count();
        long verifiedReports   = reportRepository.countByStatus(
                com.medchain.entity.FakeReport.ReportStatus.VERIFIED);
        long verifiedManufacturers = manufacturerRepository
                .findByIsVerified(true, Pageable.unpaged()).getTotalElements();

        return ResponseEntity.ok(Map.of(
                "totalMedicines",        totalMedicines,
                "totalScans",            totalScans,
                "fakeDetected",          verifiedReports,
                "verifiedManufacturers", verifiedManufacturers,
                "statesCovered",         28L
        ));
    }
}
