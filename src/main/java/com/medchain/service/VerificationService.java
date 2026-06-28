package com.medchain.service;

import com.medchain.dto.response.ManufacturerDto;
import com.medchain.dto.response.MedicineResponse;
import com.medchain.dto.response.VerificationResult;
import com.medchain.entity.Medicine;
import com.medchain.entity.MedicineUnit;
import com.medchain.entity.ScanLog;
import com.medchain.entity.User;
import com.medchain.repository.MedicineRepository;
import com.medchain.repository.MedicineUnitRepository;
import com.medchain.repository.ScanLogRepository;
import com.medchain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final MedicineUnitRepository medicineUnitRepository;
    private final MedicineRepository medicineRepository;
    private final ScanLogRepository scanLogRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    @Cacheable(value = "verificationCache", key = "#qrCode")
    public VerificationResult verifyMedicine(String qrCode, BigDecimal lat, BigDecimal lng, String deviceInfo, String ipAddress) {
        // Parse QR code
        if (!qrCode.startsWith("MEDCHAIN:")) {
            return buildNotFoundResult(qrCode, lat, lng, deviceInfo, ipAddress);
        }

        // Lookup medicine unit
        Optional<MedicineUnit> unitOpt = medicineUnitRepository.findByQrCode(qrCode);
        if (unitOpt.isEmpty()) {
            return buildNotFoundResult(qrCode, lat, lng, deviceInfo, ipAddress);
        }

        MedicineUnit unit = unitOpt.get();
        Medicine medicine = unit.getMedicine();

        // Increment scan counter in Redis
        String scanCountKey = "scan:count:" + qrCode;
        redisTemplate.opsForValue().increment(scanCountKey);
        Object rawCount = redisTemplate.opsForValue().get(scanCountKey);
        Long scanCount = rawCount == null ? 1L : ((Number) rawCount).longValue();

        // Check unit status
        if (unit.getStatus() == MedicineUnit.UnitStatus.RECALLED) {
            logScan(qrCode, lat, lng, ScanLog.ScanResult.RECALLED, deviceInfo, ipAddress);
            return buildRecalledResult(medicine, unit, scanCount);
        }

        // Check medicine status
        if (medicine.getStatus() == Medicine.MedicineStatus.RECALLED) {
            logScan(qrCode, lat, lng, ScanLog.ScanResult.RECALLED, deviceInfo, ipAddress);
            return buildRecalledResult(medicine, unit, scanCount);
        }

        // Check expiry
        if (medicine.getExpiryDate().isBefore(LocalDate.now())) {
            logScan(qrCode, lat, lng, ScanLog.ScanResult.EXPIRED, deviceInfo, ipAddress);
            return buildExpiredResult(medicine, unit, scanCount);
        }

        // All checks passed - GENUINE
        logScan(qrCode, lat, lng, ScanLog.ScanResult.GENUINE, deviceInfo, ipAddress);
        return buildGenuineResult(medicine, unit, scanCount);
    }

    private VerificationResult buildGenuineResult(Medicine medicine, MedicineUnit unit, Long scanCount) {
        long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), medicine.getExpiryDate());

        return VerificationResult.builder()
                .verdict("GENUINE")
                .confidence(100)
                .medicine(mapToMedicineResponse(medicine))
                .manufacturer(mapToManufacturerDto(medicine.getManufacturer()))
                .expiryInfo(VerificationResult.ExpiryInfo.builder()
                        .expiryDate(medicine.getExpiryDate())
                        .daysUntilExpiry(daysUntilExpiry)
                        .isExpired(false)
                        .build())
                .scanCount(scanCount)
                .warning(daysUntilExpiry < 30 ? "Medicine expires in " + daysUntilExpiry + " days" : null)
                .reportButton(false)
                .build();
    }

    private VerificationResult buildExpiredResult(Medicine medicine, MedicineUnit unit, Long scanCount) {
        long daysSinceExpiry = ChronoUnit.DAYS.between(medicine.getExpiryDate(), LocalDate.now());

        return VerificationResult.builder()
                .verdict("EXPIRED")
                .confidence(100)
                .medicine(mapToMedicineResponse(medicine))
                .manufacturer(mapToManufacturerDto(medicine.getManufacturer()))
                .expiryInfo(VerificationResult.ExpiryInfo.builder()
                        .expiryDate(medicine.getExpiryDate())
                        .daysUntilExpiry(-daysSinceExpiry)
                        .isExpired(true)
                        .build())
                .scanCount(scanCount)
                .warning("⚠️ Medicine expired " + daysSinceExpiry + " days ago. Do not use!")
                .reportButton(true)
                .build();
    }

    private VerificationResult buildRecalledResult(Medicine medicine, MedicineUnit unit, Long scanCount) {
        return VerificationResult.builder()
                .verdict("RECALLED")
                .confidence(100)
                .medicine(mapToMedicineResponse(medicine))
                .manufacturer(mapToManufacturerDto(medicine.getManufacturer()))
                .expiryInfo(VerificationResult.ExpiryInfo.builder()
                        .expiryDate(medicine.getExpiryDate())
                        .daysUntilExpiry(0L)
                        .isExpired(false)
                        .build())
                .scanCount(scanCount)
                .warning("🚨 This medicine has been RECALLED. Return to pharmacy immediately!")
                .reportButton(false)
                .build();
    }

    private VerificationResult buildNotFoundResult(String qrCode, BigDecimal lat, BigDecimal lng, String deviceInfo, String ipAddress) {
        logScan(qrCode, lat, lng, ScanLog.ScanResult.NOT_FOUND, deviceInfo, ipAddress);

        return VerificationResult.builder()
                .verdict("NOT_FOUND")
                .confidence(0)
                .medicine(null)
                .manufacturer(null)
                .expiryInfo(null)
                .scanCount(0L)
                .warning("❓ QR code not found in database. This may be a fake medicine!")
                .reportButton(true)
                .build();
    }

    private void logScan(String qrCode, BigDecimal lat, BigDecimal lng, ScanLog.ScanResult result, String deviceInfo, String ipAddress) {
        UUID userId = getCurrentUserId();

        ScanLog scanLog = ScanLog.builder()
                .qrCode(qrCode)
                .scannedBy(userId != null ? User.builder().id(userId).build() : null)
                .locationLat(lat)
                .locationLng(lng)
                .scanResult(result)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .build();

        scanLogRepository.save(scanLog);
        log.info("Scan logged: {} - Result: {}", qrCode, result);
    }

    private UUID getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                String email = authentication.getName();
                return userRepository.findByEmail(email).map(User::getId).orElse(null);
            }
        } catch (Exception e) {
            log.debug("Could not get current user ID: {}", e.getMessage());
        }
        return null;
    }

    private MedicineResponse mapToMedicineResponse(Medicine medicine) {
        return MedicineResponse.builder()
                .id(medicine.getId())
                .name(medicine.getName())
                .genericName(medicine.getGenericName())
                .composition(medicine.getComposition())
                .category(medicine.getCategory())
                .batchNumber(medicine.getBatchNumber())
                .manufacturingDate(medicine.getManufacturingDate())
                .expiryDate(medicine.getExpiryDate())
                .storageInstructions(medicine.getStorageInstructions())
                .quantityProduced(medicine.getQuantityProduced())
                .status(medicine.getStatus().name())
                .description(medicine.getDescription())
                .sideEffects(medicine.getSideEffects())
                .build();
    }

    private ManufacturerDto mapToManufacturerDto(com.medchain.entity.Manufacturer manufacturer) {
        return ManufacturerDto.builder()
                .id(manufacturer.getId())
                .companyName(manufacturer.getCompanyName())
                .city(manufacturer.getCity())
                .state(manufacturer.getState())
                .isVerified(manufacturer.getIsVerified())
                .build();
    }

    // ── Batch Number Verification ─────────────────────────────────────────────

    @Transactional
    public VerificationResult verifyByBatchNumber(String batchNumber, BigDecimal lat, BigDecimal lng, String deviceInfo, String ipAddress) {
        Optional<Medicine> medicineOpt = medicineRepository.findByBatchNumberIgnoreCase(batchNumber.trim());
        if (medicineOpt.isEmpty()) {
            logScan("BATCH:" + batchNumber, lat, lng, ScanLog.ScanResult.NOT_FOUND, deviceInfo, ipAddress);
            return VerificationResult.builder()
                    .verdict("NOT_FOUND")
                    .confidence(0)
                    .warning("❓ Batch number not found. This may be a fake or unregistered medicine!")
                    .reportButton(true)
                    .scanCount(0L)
                    .build();
        }

        Medicine medicine = medicineOpt.get();
        logScan("BATCH:" + batchNumber, lat, lng,
                medicine.getStatus() == Medicine.MedicineStatus.RECALLED ? ScanLog.ScanResult.RECALLED :
                medicine.getExpiryDate().isBefore(LocalDate.now()) ? ScanLog.ScanResult.EXPIRED :
                ScanLog.ScanResult.GENUINE, deviceInfo, ipAddress);

        if (medicine.getStatus() == Medicine.MedicineStatus.RECALLED) {
            return buildRecalledResultFromMedicine(medicine);
        }
        if (medicine.getExpiryDate().isBefore(LocalDate.now())) {
            return buildExpiredResultFromMedicine(medicine);
        }

        long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), medicine.getExpiryDate());
        return VerificationResult.builder()
                .verdict("GENUINE")
                .confidence(85) // slightly lower confidence than QR since no unit-level check
                .medicine(mapToMedicineResponse(medicine))
                .manufacturer(mapToManufacturerDto(medicine.getManufacturer()))
                .expiryInfo(VerificationResult.ExpiryInfo.builder()
                        .expiryDate(medicine.getExpiryDate())
                        .daysUntilExpiry(daysUntilExpiry)
                        .isExpired(false)
                        .build())
                .scanCount(0L)
                .warning(daysUntilExpiry < 30 ? "Medicine expires in " + daysUntilExpiry + " days" : null)
                .reportButton(false)
                .build();
    }

    // ── Name Search ───────────────────────────────────────────────────────────

    public List<MedicineResponse> searchByName(String name) {
        return medicineRepository.findByNameContainingIgnoreCase(name.trim())
                .stream()
                .limit(10)
                .map(this::mapToMedicineResponse)
                .toList();
    }

    private VerificationResult buildRecalledResultFromMedicine(Medicine medicine) {
        return VerificationResult.builder()
                .verdict("RECALLED")
                .confidence(100)
                .medicine(mapToMedicineResponse(medicine))
                .manufacturer(mapToManufacturerDto(medicine.getManufacturer()))
                .expiryInfo(VerificationResult.ExpiryInfo.builder()
                        .expiryDate(medicine.getExpiryDate())
                        .daysUntilExpiry(0L)
                        .isExpired(false)
                        .build())
                .scanCount(0L)
                .warning("🚨 This medicine has been RECALLED. Return to pharmacy immediately!")
                .reportButton(false)
                .build();
    }

    private VerificationResult buildExpiredResultFromMedicine(Medicine medicine) {
        long daysSinceExpiry = ChronoUnit.DAYS.between(medicine.getExpiryDate(), LocalDate.now());
        return VerificationResult.builder()
                .verdict("EXPIRED")
                .confidence(100)
                .medicine(mapToMedicineResponse(medicine))
                .manufacturer(mapToManufacturerDto(medicine.getManufacturer()))
                .expiryInfo(VerificationResult.ExpiryInfo.builder()
                        .expiryDate(medicine.getExpiryDate())
                        .daysUntilExpiry(-daysSinceExpiry)
                        .isExpired(true)
                        .build())
                .scanCount(0L)
                .warning("⚠️ Medicine expired " + daysSinceExpiry + " days ago. Do not use!")
                .reportButton(true)
                .build();
    }
}
