package com.medchain.service;

import com.medchain.dto.request.IssueRecallRequest;
import com.medchain.dto.response.MedicineResponse;
import com.medchain.dto.response.RecallResponse;
import com.medchain.entity.Medicine;
import com.medchain.entity.Manufacturer;
import com.medchain.entity.Recall;
import com.medchain.entity.User;
import com.medchain.exception.ResourceNotFoundException;
import com.medchain.exception.UnauthorizedException;
import com.medchain.repository.ManufacturerRepository;
import com.medchain.repository.MedicineRepository;
import com.medchain.repository.MedicineUnitRepository;
import com.medchain.repository.RecallRepository;
import com.medchain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecallService {

    private final RecallRepository recallRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineUnitRepository medicineUnitRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public RecallResponse issueRecall(IssueRecallRequest request) {
        String email = getCurrentUserEmail();
        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Medicine medicine = medicineRepository.findById(request.getMedicineId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));

        // Create recall
        Recall recall = Recall.builder()
                .medicine(medicine)
                .issuedBy(admin)
                .reason(request.getReason())
                .severity(Recall.Severity.valueOf(request.getSeverity()))
                .affectedBatches(request.getAffectedBatches() != null ? 
                        request.getAffectedBatches().toArray(new String[0]) : null)
                .affectedStates(request.getAffectedStates() != null ? 
                        request.getAffectedStates().toArray(new String[0]) : null)
                .isActive(true)
                .build();

        recall = recallRepository.save(recall);

        // Mark medicine as RECALLED
        medicine.setStatus(Medicine.MedicineStatus.RECALLED);
        medicineRepository.save(medicine);

        // Mark all medicine units as RECALLED
        medicineUnitRepository.updateStatusByMedicineId(
                medicine.getId(), 
                com.medchain.entity.MedicineUnit.UnitStatus.RECALLED
        );

        // Notify manufacturer
        UUID manufacturerUserId = medicine.getManufacturer().getUser().getId();
        notificationService.createNotification(
                manufacturerUserId,
                "🚨 Medicine Recall Issued",
                "A recall has been issued for " + medicine.getName() + " - " + request.getSeverity() + " severity",
                "RECALL_ISSUED"
        );

        // Broadcast alert to all users via WebSocket
        messagingTemplate.convertAndSend("/topic/alerts", Map.of(
                "type", "RECALL_ISSUED",
                "medicineId", medicine.getId(),
                "medicineName", medicine.getName(),
                "severity", request.getSeverity()
        ));

        // Send mass email to affected users (async)
        // In production, this would query users in affected states
        List<User> affectedUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.UserRole.PATIENT || u.getRole() == User.UserRole.CHEMIST)
                .limit(100) // Limit for demo
                .collect(Collectors.toList());
        
        emailService.sendRecallAlertEmail(affectedUsers, recall);

        log.info("Recall issued for medicine: {} with severity: {}", medicine.getName(), request.getSeverity());

        return mapToResponse(recall);
    }

    public List<RecallResponse> getActiveRecalls() {
        return recallRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RecallResponse> getMyRecalls() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Manufacturer manufacturer = manufacturerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer not found"));
        return recallRepository.findByManufacturerId(manufacturer.getId(), Pageable.unpaged())
                .getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RecallResponse getRecallById(UUID id) {
        Recall recall = recallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recall not found"));
        return mapToResponse(recall);
    }

    public Page<RecallResponse> getRecallsByManufacturer(UUID manufacturerId, Pageable pageable) {
        return recallRepository.findByManufacturerId(manufacturerId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public RecallResponse deactivateRecall(UUID id) {
        Recall recall = recallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recall not found"));

        recall.setIsActive(false);
        recall = recallRepository.save(recall);

        log.info("Recall deactivated: {}", id);
        return mapToResponse(recall);
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }
        return authentication.getName();
    }

    private RecallResponse mapToResponse(Recall recall) {
        return RecallResponse.builder()
                .id(recall.getId())
                .medicine(mapToMedicineResponse(recall.getMedicine()))
                .reason(recall.getReason())
                .severity(recall.getSeverity().name())
                .affectedBatches(recall.getAffectedBatches() != null ? 
                        Arrays.asList(recall.getAffectedBatches()) : null)
                .affectedStates(recall.getAffectedStates() != null ? 
                        Arrays.asList(recall.getAffectedStates()) : null)
                .isActive(recall.getIsActive())
                .createdAt(recall.getCreatedAt())
                .build();
    }

    private MedicineResponse mapToMedicineResponse(Medicine medicine) {
        return MedicineResponse.builder()
                .id(medicine.getId())
                .name(medicine.getName())
                .genericName(medicine.getGenericName())
                .batchNumber(medicine.getBatchNumber())
                .category(medicine.getCategory())
                .build();
    }
}
