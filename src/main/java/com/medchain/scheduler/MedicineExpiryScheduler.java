package com.medchain.scheduler;

import com.medchain.entity.Manufacturer;
import com.medchain.entity.Medicine;
import com.medchain.entity.MedicineUnit;
import com.medchain.repository.MedicineRepository;
import com.medchain.repository.MedicineUnitRepository;
import com.medchain.service.EmailService;
import com.medchain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MedicineExpiryScheduler {

    private final MedicineRepository medicineRepository;
    private final MedicineUnitRepository medicineUnitRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 1 * * *") // Run daily at 1 AM
    @Transactional
    public void checkExpiringMedicines() {
        log.info("Starting medicine expiry check...");

        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);
        LocalDate sevenDaysLater = today.plusDays(7);

        // Step 1: Find medicines expiring in 30 days
        List<Medicine> expiring30Days = medicineRepository.findExpiringBetween(today, thirtyDaysLater);
        
        Map<Manufacturer, List<Medicine>> groupedBy30Days = expiring30Days.stream()
                .collect(Collectors.groupingBy(Medicine::getManufacturer));

        for (Map.Entry<Manufacturer, List<Medicine>> entry : groupedBy30Days.entrySet()) {
            Manufacturer manufacturer = entry.getKey();
            List<Medicine> medicines = entry.getValue();

            // Send email alert
            emailService.sendMedicineExpiryAlert(manufacturer, medicines);

            // Create in-app notification
            notificationService.createNotification(
                    manufacturer.getUser().getId(),
                    "⏰ Medicine Expiry Alert",
                    medicines.size() + " medicine(s) expiring in 30 days",
                    "MEDICINE_EXPIRING"
            );
        }

        log.info("Found {} medicines expiring in 30 days", expiring30Days.size());

        // Step 2: Find medicines expiring in 7 days (URGENT)
        List<Medicine> expiring7Days = medicineRepository.findExpiringBetween(today, sevenDaysLater);
        
        Map<Manufacturer, List<Medicine>> groupedBy7Days = expiring7Days.stream()
                .collect(Collectors.groupingBy(Medicine::getManufacturer));

        for (Map.Entry<Manufacturer, List<Medicine>> entry : groupedBy7Days.entrySet()) {
            Manufacturer manufacturer = entry.getKey();
            List<Medicine> medicines = entry.getValue();

            // Send URGENT email
            emailService.sendMedicineExpiryAlert(manufacturer, medicines);

            // Create URGENT notification
            notificationService.createNotification(
                    manufacturer.getUser().getId(),
                    "🚨 URGENT: Medicine Expiry Alert",
                    medicines.size() + " medicine(s) expiring in 7 days!",
                    "MEDICINE_EXPIRING"
            );
        }

        log.info("Found {} medicines expiring in 7 days (URGENT)", expiring7Days.size());

        // Step 3: Mark expired medicines
        List<Medicine> expiredMedicines = medicineRepository.findByExpiryDateBefore(today);
        int expiredCount = 0;

        for (Medicine medicine : expiredMedicines) {
            if (medicine.getStatus() == Medicine.MedicineStatus.ACTIVE) {
                medicine.setStatus(Medicine.MedicineStatus.EXPIRED);
                medicineRepository.save(medicine);

                // Update all medicine units to EXPIRED
                medicineUnitRepository.updateStatusByMedicineId(
                        medicine.getId(),
                        MedicineUnit.UnitStatus.EXPIRED
                );

                expiredCount++;
            }
        }

        log.info("Marked {} medicines as EXPIRED", expiredCount);
        log.info("Medicine expiry check completed");
    }
}
