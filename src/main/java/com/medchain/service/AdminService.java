package com.medchain.service;

import com.medchain.dto.response.DashboardStats;
import com.medchain.dto.response.ManufacturerDto;
import com.medchain.entity.Manufacturer;
import com.medchain.entity.User;
import com.medchain.exception.ResourceNotFoundException;
import com.medchain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final MedicineRepository medicineRepository;
    private final MedicineUnitRepository medicineUnitRepository;
    private final ScanLogRepository scanLogRepository;
    private final FakeReportRepository reportRepository;
    private final RecallRepository recallRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Cacheable(value = "dashboardCache", key = "'stats'")
    public DashboardStats getDashboardStats() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime sevenDaysAgo = today.minusDays(7);

        long totalMedicines = medicineRepository.count();
        long totalQRCodes = medicineUnitRepository.count();
        long scansToday = scanLogRepository.countScansAfter(today);
        long scansAllTime = scanLogRepository.count();
        long pendingReports = reportRepository.countByStatus(com.medchain.entity.FakeReport.ReportStatus.PENDING);
        long verifiedReports = reportRepository.countByStatus(com.medchain.entity.FakeReport.ReportStatus.VERIFIED);
        long activeRecalls = recallRepository.countByIsActive(true);
        long verifiedManufacturers = manufacturerRepository.findByIsVerified(true, Pageable.unpaged()).getTotalElements();
        long pendingManufacturers = manufacturerRepository.findByIsVerified(false, Pageable.unpaged()).getTotalElements();

        // Scan activity last 7 days
        List<Object[]> scanActivity = scanLogRepository.getScanActivityByDate(sevenDaysAgo);
        List<Map<String, Object>> scanActivityData = scanActivity.stream()
                .map(data -> Map.of(
                        "date", data[0].toString(),
                        "count", data[1]
                ))
                .collect(Collectors.toList());

        // Reports by state
        List<Object[]> reportsByState = reportRepository.countVerifiedReportsByState();
        List<Map<String, Object>> reportsByStateData = reportsByState.stream()
                .map(data -> Map.of(
                        "state", data[0] != null ? data[0] : "Unknown",
                        "count", data[1]
                ))
                .collect(Collectors.toList());

        return DashboardStats.builder()
                .totalMedicinesRegistered(totalMedicines)
                .totalQRCodesGenerated(totalQRCodes)
                .totalScansToday(scansToday)
                .totalScansAllTime(scansAllTime)
                .pendingReports(pendingReports)
                .verifiedReports(verifiedReports)
                .activeRecalls(activeRecalls)
                .verifiedManufacturers(verifiedManufacturers)
                .pendingManufacturers(pendingManufacturers)
                .scanActivityLast7Days(scanActivityData)
                .reportsByState(reportsByStateData)
                .build();
    }

    public Page<ManufacturerDto> getAllManufacturers(Boolean verified, Pageable pageable) {
        if (verified != null) {
            return manufacturerRepository.findByIsVerified(verified, pageable)
                    .map(this::mapToDto);
        }
        return manufacturerRepository.findAll(pageable)
                .map(this::mapToDto);
    }

    public ManufacturerDto getManufacturerById(UUID id) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer not found"));
        return mapToDto(manufacturer);
    }

    @Transactional
    public ManufacturerDto verifyManufacturer(UUID id) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer not found"));

        manufacturer.setIsVerified(true);
        manufacturer = manufacturerRepository.save(manufacturer);

        // Notify manufacturer
        notificationService.createNotification(
                manufacturer.getUser().getId(),
                "✅ Account Verified",
                "Your manufacturer account has been verified. You can now register medicines.",
                "MANUFACTURER_VERIFIED"
        );

        // Send email
        emailService.sendManufacturerVerifiedEmail(manufacturer);

        log.info("Manufacturer verified: {}", manufacturer.getCompanyName());
        return mapToDto(manufacturer);
    }

    @Transactional
    public ManufacturerDto suspendManufacturer(UUID id) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer not found"));

        User user = manufacturer.getUser();
        user.setIsActive(false);
        userRepository.save(user);

        // Notify manufacturer
        notificationService.createNotification(
                user.getId(),
                "⚠️ Account Suspended",
                "Your manufacturer account has been suspended. Contact admin for details.",
                "SYSTEM_ALERT"
        );

        log.info("Manufacturer suspended: {}", manufacturer.getCompanyName());
        return mapToDto(manufacturer);
    }

    public Map<String, Object> getAnalytics(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        // ── Scan Trends ───────────────────────────────────────────────────────
        List<Object[]> rawTrends = scanLogRepository.getScanActivityByDate(startDate);
        Map<String, Object> scanTrends = new HashMap<>();
        scanTrends.put("labels", rawTrends.stream().map(r -> r[0].toString()).collect(Collectors.toList()));
        scanTrends.put("values", rawTrends.stream().map(r -> ((Number) r[1]).longValue()).collect(Collectors.toList()));

        // ── Scan Results Distribution ─────────────────────────────────────────
        List<Object[]> rawResults = scanLogRepository.countByScanResultAfter(startDate);
        Map<String, Object> reportDistribution = new HashMap<>();
        reportDistribution.put("labels", rawResults.stream().map(r -> r[0].toString()).collect(Collectors.toList()));
        reportDistribution.put("values", rawResults.stream().map(r -> ((Number) r[1]).longValue()).collect(Collectors.toList()));

        // ── Reports by State (Top Categories) ────────────────────────────────
        List<Object[]> rawByState = reportRepository.countVerifiedReportsByState();
        Map<String, Object> topCategories = new HashMap<>();
        topCategories.put("labels", rawByState.stream().map(r -> r[0] != null ? r[0].toString() : "Unknown").collect(Collectors.toList()));
        topCategories.put("values", rawByState.stream().map(r -> ((Number) r[1]).longValue()).collect(Collectors.toList()));

        // ── Verification Rate ─────────────────────────────────────────────────
        long pending  = reportRepository.countByStatus(com.medchain.entity.FakeReport.ReportStatus.PENDING);
        long verified = reportRepository.countByStatus(com.medchain.entity.FakeReport.ReportStatus.VERIFIED);
        long dismissed = reportRepository.countByStatus(com.medchain.entity.FakeReport.ReportStatus.DISMISSED);
        Map<String, Object> verificationRate = new HashMap<>();
        verificationRate.put("labels", List.of("Verified", "Pending", "Dismissed"));
        verificationRate.put("values", List.of(verified, pending, dismissed));

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("scanTrends", scanTrends);
        analytics.put("reportDistribution", reportDistribution);
        analytics.put("topCategories", topCategories);
        analytics.put("verificationRate", verificationRate);
        analytics.put("period", days + " days");

        return analytics;
    }

    private ManufacturerDto mapToDto(Manufacturer manufacturer) {
        return ManufacturerDto.builder()
                .id(manufacturer.getId())
                .companyName(manufacturer.getCompanyName())
                .licenseNumber(manufacturer.getLicenseNumber())
                .gstNumber(manufacturer.getGstNumber())
                .address(manufacturer.getAddress())
                .city(manufacturer.getCity())
                .state(manufacturer.getState())
                .pincode(manufacturer.getPincode())
                .isVerified(manufacturer.getIsVerified())
                .email(manufacturer.getUser() != null ? manufacturer.getUser().getEmail() : null)
                .userName(manufacturer.getUser() != null ? manufacturer.getUser().getName() : null)
                .createdAt(manufacturer.getCreatedAt())
                .build();
    }
}
