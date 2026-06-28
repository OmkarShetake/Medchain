package com.medchain.service;

import com.medchain.entity.Manufacturer;
import com.medchain.entity.Medicine;
import com.medchain.entity.MedicineUnit;
import com.medchain.repository.ManufacturerRepository;
import com.medchain.repository.MedicineRepository;
import com.medchain.repository.MedicineUnitRepository;
import com.medchain.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSeederService {

    private final UserRepository userRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineUnitRepository medicineUnitRepository;
    private final QRService qrService;

    @PostConstruct
    @Transactional
    public void seedDefaultMedicines() {
        // Only seed if Cipla manufacturer has no medicines
        userRepository.findByEmail("manufacturer@medchain.com").ifPresent(mfrUser ->
            manufacturerRepository.findByUserId(mfrUser.getId()).ifPresent(manufacturer -> {
                long count = medicineRepository.findByManufacturerId(
                        manufacturer.getId(), org.springframework.data.domain.Pageable.unpaged()
                ).getTotalElements();
                if (count > 0) return;

                createSampleMedicine(manufacturer, "Paracetamol 500mg", "Paracetamol",
                        "Paracetamol 500mg", "PAINKILLER", "BN-PARA-2024-001",
                        LocalDate.of(2024, 1, 1), LocalDate.of(2026, 12, 31),
                        "Fever, headache, mild pain relief", 10000);

                createSampleMedicine(manufacturer, "Amoxicillin 500mg", "Amoxicillin",
                        "Amoxicillin Trihydrate 500mg", "ANTIBIOTIC", "BN-AMOX-2024-002",
                        LocalDate.of(2024, 2, 1), LocalDate.of(2026, 11, 30),
                        "Bacterial infections treatment", 5000);

                createSampleMedicine(manufacturer, "Cetirizine 10mg", "Cetirizine",
                        "Cetirizine Hydrochloride 10mg", "OTHER", "BN-CETZ-2024-003",
                        LocalDate.of(2024, 3, 1), LocalDate.of(2027, 2, 28),
                        "Allergy, cold, runny nose relief", 8000);

                createSampleMedicine(manufacturer, "Metformin 500mg", "Metformin",
                        "Metformin Hydrochloride 500mg", "DIABETES", "BN-METF-2024-004",
                        LocalDate.of(2024, 1, 15), LocalDate.of(2026, 10, 31),
                        "Type 2 diabetes management", 6000);

                createSampleMedicine(manufacturer, "Vitamin C 500mg", "Ascorbic Acid",
                        "Ascorbic Acid 500mg", "VITAMIN", "BN-VITC-2024-005",
                        LocalDate.of(2024, 4, 1), LocalDate.of(2027, 3, 31),
                        "Immune system support, antioxidant", 15000);

                log.info("Sample medicines seeded for Cipla Pharmaceuticals");
            })
        );
    }

    private void createSampleMedicine(Manufacturer manufacturer, String name,
            String genericName, String composition, String category,
            String batchNumber, LocalDate mfgDate, LocalDate expiryDate,
            String description, int quantity) {

        if (medicineRepository.existsByBatchNumber(batchNumber)) return;

        Medicine medicine = Medicine.builder()
                .manufacturer(manufacturer)
                .name(name)
                .genericName(genericName)
                .composition(composition)
                .category(category)
                .batchNumber(batchNumber)
                .manufacturingDate(mfgDate)
                .expiryDate(expiryDate)
                .description(description)
                .quantityProduced(quantity)
                .storageInstructions("Store below 25°C in a dry place")
                .status(Medicine.MedicineStatus.ACTIVE)
                .build();

        medicine = medicineRepository.save(medicine);

        // Generate 5 sample QR code units per medicine for demo
        for (int i = 1; i <= 5; i++) {
            UUID unitId = UUID.randomUUID();
            String qrContent = "MEDCHAIN:" + unitId;

            MedicineUnit unit = MedicineUnit.builder()
                    .medicine(medicine)
                    .qrCode(qrContent)
                    .stripNumber("STRIP-" + i)
                    .status(MedicineUnit.UnitStatus.ACTIVE)
                    .distributedCity("Mumbai")
                    .distributedState("Maharashtra")
                    .distributedAt(LocalDateTime.now())
                    .build();

            medicineUnitRepository.save(unit);
        }

        log.info("Seeded medicine: {} with 5 QR units", name);
    }
}
