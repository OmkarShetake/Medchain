package com.medchain.service;

import com.medchain.dto.request.SubmitReportRequest;
import com.medchain.dto.response.FakeReportResponse;
import com.medchain.dto.response.MedicineResponse;
import com.medchain.dto.response.UserDto;
import com.medchain.entity.FakeReport;
import com.medchain.entity.Medicine;
import com.medchain.entity.MedicineUnit;
import com.medchain.entity.User;
import com.medchain.exception.ResourceNotFoundException;
import com.medchain.exception.UnauthorizedException;
import com.medchain.repository.FakeReportRepository;
import com.medchain.repository.MedicineRepository;
import com.medchain.repository.MedicineUnitRepository;
import com.medchain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final FakeReportRepository reportRepository;
    private final UserRepository userRepository;
    private final MedicineUnitRepository medicineUnitRepository;
    private final MedicineRepository medicineRepository;
    private final CloudinaryService cloudinaryService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public FakeReportResponse submitReport(SubmitReportRequest request, MultipartFile photoFile) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Upload photo if provided
        String photoUrl = null;
        if (photoFile != null && !photoFile.isEmpty()) {
            photoUrl = cloudinaryService.uploadImage(photoFile, "reports");
        }

        // Try to find medicine by QR code
        Medicine medicine = null;
        if (request.getQrCode() != null && !request.getQrCode().isEmpty()) {
            Optional<MedicineUnit> unitOpt = medicineUnitRepository.findByQrCode(request.getQrCode());
            if (unitOpt.isPresent()) {
                medicine = unitOpt.get().getMedicine();
            }
        }

        FakeReport report = FakeReport.builder()
                .qrCode(request.getQrCode())
                .medicine(medicine)
                .reportedBy(user)
                .photoUrl(photoUrl)
                .description(request.getDescription())
                .locationLat(request.getLocationLat())
                .locationLng(request.getLocationLng())
                .city(request.getCity())
                .state(request.getState())
                .status(FakeReport.ReportStatus.PENDING)
                .aiConfidenceScore(0)
                .build();

        report = reportRepository.save(report);
        log.info("Fake report submitted by user: {}", email);

        // Notify admin via WebSocket
        messagingTemplate.convertAndSend("/topic/alerts", 
                Map.of("type", "NEW_REPORT", "reportId", report.getId()));

        // Trigger AI analysis asynchronously
        // This will be implemented in PROMPT 6
        
        return mapToResponse(report);
    }

    public Page<FakeReportResponse> getMyReports(Pageable pageable) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return reportRepository.findByReportedById(user.getId(), pageable)
                .map(this::mapToResponse);
    }

    public FakeReportResponse getReportById(UUID id) {
        FakeReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        // Allow admin or report owner to view
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null && user.getRole() == User.UserRole.ADMIN) {
                return mapToResponse(report); // admin can always view
            }
            if (user != null && !report.getReportedBy().getId().equals(user.getId())) {
                throw new UnauthorizedException("Not authorized to view this report");
            }
        }

        return mapToResponse(report);
    }

    public Page<FakeReportResponse> getAllReports(FakeReport.ReportStatus status, Pageable pageable) {
        if (status != null) {
            return reportRepository.findByStatus(status, pageable)
                    .map(this::mapToResponse);
        }
        return reportRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public FakeReportResponse verifyReport(UUID reportId, String adminNotes) {
        FakeReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setStatus(FakeReport.ReportStatus.VERIFIED);
        report.setAdminNotes(adminNotes);
        report = reportRepository.save(report);

        // Notify manufacturer if medicine is identified
        if (report.getMedicine() != null) {
            UUID manufacturerUserId = report.getMedicine().getManufacturer().getUser().getId();
            notificationService.createNotification(
                    manufacturerUserId,
                    "Fake Medicine Report Verified",
                    "A fake medicine report for " + report.getMedicine().getName() + " has been verified.",
                    "REPORT_VERIFIED"
            );
        }

        // Check if same medicine has multiple reports (suggest recall)
        if (report.getMedicine() != null) {
            List<Object[]> medicinesWithMultipleReports = 
                    reportRepository.findMedicinesWithMultipleReports(3);
            
            for (Object[] result : medicinesWithMultipleReports) {
                UUID medicineId = (UUID) result[0];
                if (medicineId.equals(report.getMedicine().getId())) {
                    log.warn("Medicine {} has 3+ verified reports. Consider recall.", medicineId);
                    // Notify admin
                }
            }
        }

        log.info("Report verified: {}", reportId);
        return mapToResponse(report);
    }

    @Transactional
    public FakeReportResponse dismissReport(UUID reportId, String adminNotes) {
        FakeReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setStatus(FakeReport.ReportStatus.DISMISSED);
        report.setAdminNotes(adminNotes);
        report = reportRepository.save(report);

        // Notify reporter
        notificationService.createNotification(
                report.getReportedBy().getId(),
                "Report Dismissed",
                "Your fake medicine report has been reviewed and dismissed.",
                "REPORT_DISMISSED"
        );

        log.info("Report dismissed: {}", reportId);
        return mapToResponse(report);
    }

    public Map<String, Object> getHotspots() {
        List<Object[]> hotspotData = reportRepository.getHotspotData();

        List<Map<String, Object>> features = hotspotData.stream()
                .filter(data -> data[2] != null && data[3] != null)
                .map(data -> {
                    Map<String, Object> feature = new HashMap<>();
                    feature.put("type", "Feature");
                    
                    Map<String, Object> geometry = new HashMap<>();
                    geometry.put("type", "Point");
                    geometry.put("coordinates", Arrays.asList(data[3], data[2])); // [lng, lat]
                    feature.put("geometry", geometry);
                    
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("city", data[0] != null ? data[0].toString() : "Unknown");
                    properties.put("state", data[1] != null ? data[1].toString() : "");
                    properties.put("location", (data[0] != null ? data[0].toString() : "Unknown") +
                            (data[1] != null ? ", " + data[1].toString() : ""));
                    properties.put("count", ((Number) data[4]).longValue());
                    feature.put("properties", properties);
                    
                    return feature;
                })
                .collect(Collectors.toList());

        Map<String, Object> geoJson = new HashMap<>();
        geoJson.put("type", "FeatureCollection");
        geoJson.put("features", features);

        return geoJson;
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }
        return authentication.getName();
    }

    private FakeReportResponse mapToResponse(FakeReport report) {
        String medicineName = report.getMedicine() != null ? report.getMedicine().getName() : "Unknown";
        String reporterName = report.getReportedBy() != null ? report.getReportedBy().getName() : "Unknown";
        String city  = report.getCity()  != null ? report.getCity()  : "";
        String state = report.getState() != null ? report.getState() : "";
        String location = !city.isBlank() && !state.isBlank() ? city + ", " + state
                        : !city.isBlank()  ? city
                        : !state.isBlank() ? state
                        : "Unknown";

        return FakeReportResponse.builder()
                .id(report.getId())
                .qrCode(report.getQrCode())
                .description(report.getDescription())
                .photoUrl(report.getPhotoUrl())
                .city(report.getCity())
                .state(report.getState())
                .status(report.getStatus().name())
                .aiConfidenceScore(report.getAiConfidenceScore())
                .aiAnalysis(report.getAiAnalysis())
                .adminNotes(report.getAdminNotes())
                .createdAt(report.getCreatedAt())
                .medicine(report.getMedicine() != null ? mapToMedicineResponse(report.getMedicine()) : null)
                .reportedBy(report.getReportedBy() != null ? mapToUserDto(report.getReportedBy()) : null)
                .medicineName(medicineName)
                .reporterName(reporterName)
                .location(location)
                .build();
    }

    private MedicineResponse mapToMedicineResponse(Medicine medicine) {
        return MedicineResponse.builder()
                .id(medicine.getId())
                .name(medicine.getName())
                .genericName(medicine.getGenericName())
                .batchNumber(medicine.getBatchNumber())
                .category(medicine.getCategory())
                .description(medicine.getDescription())
                .build();
    }

    private UserDto mapToUserDto(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
