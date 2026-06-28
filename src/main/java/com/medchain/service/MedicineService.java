package com.medchain.service;

import com.medchain.dto.request.CreateMedicineRequest;
import com.medchain.dto.request.QRGenerationRequest;
import com.medchain.dto.response.ManufacturerDto;
import com.medchain.dto.response.MedicineResponse;
import com.medchain.dto.response.QRCodeDto;
import com.medchain.entity.Manufacturer;
import com.medchain.entity.Medicine;
import com.medchain.entity.MedicineUnit;
import com.medchain.entity.User;
import com.medchain.exception.DuplicateResourceException;
import com.medchain.exception.ResourceNotFoundException;
import com.medchain.exception.UnauthorizedException;
import com.medchain.exception.ValidationException;
import com.medchain.repository.ManufacturerRepository;
import com.medchain.repository.MedicineRepository;
import com.medchain.repository.MedicineUnitRepository;
import com.medchain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final MedicineUnitRepository medicineUnitRepository;
    private final UserRepository userRepository;
    private final QRService qrService;

    @Transactional
    public MedicineResponse createMedicine(CreateMedicineRequest request) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Manufacturer manufacturer = manufacturerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer profile not found"));

        if (!manufacturer.getIsVerified()) {
            throw new ValidationException("Manufacturer must be verified to register medicines");
        }

        if (medicineRepository.existsByBatchNumber(request.getBatchNumber())) {
            throw new DuplicateResourceException("Batch number already exists");
        }

        Medicine medicine = Medicine.builder()
                .manufacturer(manufacturer)
                .name(request.getName())
                .genericName(request.getGenericName())
                .composition(request.getComposition())
                .category(request.getCategory())
                .batchNumber(request.getBatchNumber())
                .manufacturingDate(request.getManufacturingDate())
                .expiryDate(request.getExpiryDate())
                .storageInstructions(request.getStorageInstructions())
                .quantityProduced(request.getQuantityProduced())
                .status(Medicine.MedicineStatus.ACTIVE)
                .description(request.getDescription())
                .sideEffects(request.getSideEffects())
                .build();

        medicine = medicineRepository.save(medicine);
        log.info("Medicine created: {} by manufacturer: {}", medicine.getName(), manufacturer.getCompanyName());

        return mapToMedicineResponse(medicine);
    }

    public Page<MedicineResponse> getMyMedicines(Pageable pageable) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Manufacturer manufacturer = manufacturerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer profile not found"));

        return medicineRepository.findByManufacturerId(manufacturer.getId(), pageable)
                .map(this::mapToMedicineResponse);
    }

    public MedicineResponse getMedicineById(UUID id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));

        // Verify ownership
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Manufacturer manufacturer = manufacturerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer profile not found"));

        if (!medicine.getManufacturer().getId().equals(manufacturer.getId())) {
            throw new UnauthorizedException("Not authorized to access this medicine");
        }

        return mapToMedicineResponse(medicine);
    }

    @Transactional
    public List<QRCodeDto> generateQRCodes(UUID medicineId, QRGenerationRequest request) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));

        // Verify ownership
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Manufacturer manufacturer = manufacturerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer profile not found"));

        if (!medicine.getManufacturer().getId().equals(manufacturer.getId())) {
            throw new UnauthorizedException("Not authorized to generate QR codes for this medicine");
        }

        List<QRCodeDto> qrCodes = new ArrayList<>();
        List<MedicineUnit> units = new ArrayList<>();

        for (int i = 0; i < request.getQuantity(); i++) {
            UUID unitUuid = UUID.randomUUID();
            String qrContent = "MEDCHAIN:" + unitUuid;
            String qrImageBase64 = qrService.generateQRCode(qrContent);

            MedicineUnit unit = MedicineUnit.builder()
                    .medicine(medicine)
                    .qrCode(qrContent)
                    .stripNumber("STRIP-" + (i + 1))
                    .status(MedicineUnit.UnitStatus.ACTIVE)
                    .distributedCity(request.getDistributedCity())
                    .distributedState(request.getDistributedState())
                    .distributedAt(LocalDateTime.now())
                    .build();

            units.add(unit);

            qrCodes.add(QRCodeDto.builder()
                    .unitId(unitUuid)
                    .qrCode(qrContent)
                    .qrImageBase64(qrImageBase64)
                    .stripNumber(unit.getStripNumber())
                    .build());
        }

        medicineUnitRepository.saveAll(units);
        log.info("Generated {} QR codes for medicine: {}", request.getQuantity(), medicine.getName());

        return qrCodes;
    }

    public Page<QRCodeDto> getMedicineUnits(UUID medicineId, Pageable pageable) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));

        // Verify ownership
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Manufacturer manufacturer = manufacturerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer profile not found"));

        if (!medicine.getManufacturer().getId().equals(manufacturer.getId())) {
            throw new UnauthorizedException("Not authorized to access this medicine");
        }

        return medicineUnitRepository.findByMedicineId(medicineId, pageable)
                .map(unit -> QRCodeDto.builder()
                        .unitId(unit.getId())
                        .qrCode(unit.getQrCode())
                        .qrImageBase64(qrService.generateQRCode(unit.getQrCode()))
                        .stripNumber(unit.getStripNumber())
                        .build());
    }

    public Page<MedicineResponse> searchMedicines(String query, Pageable pageable) {
        return medicineRepository.searchMedicines(query, pageable)
                .map(this::mapToMedicineResponse);
    }

    public MedicineResponse getMedicinePublic(UUID id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));
        return mapToMedicineResponse(medicine);
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }
        return authentication.getName();
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
                .manufacturer(mapToManufacturerDto(medicine.getManufacturer()))
                .build();
    }

    private ManufacturerDto mapToManufacturerDto(Manufacturer manufacturer) {
        return ManufacturerDto.builder()
                .id(manufacturer.getId())
                .companyName(manufacturer.getCompanyName())
                .city(manufacturer.getCity())
                .state(manufacturer.getState())
                .isVerified(manufacturer.getIsVerified())
                .build();
    }
}
