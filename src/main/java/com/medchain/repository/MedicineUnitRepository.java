package com.medchain.repository;

import com.medchain.entity.MedicineUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicineUnitRepository extends JpaRepository<MedicineUnit, UUID> {
    Optional<MedicineUnit> findByQrCode(String qrCode);
    Page<MedicineUnit> findByMedicineId(UUID medicineId, Pageable pageable);
    long countByMedicineId(UUID medicineId);
    
    @Modifying
    @Query("UPDATE MedicineUnit mu SET mu.status = :status WHERE mu.medicine.id = :medicineId")
    void updateStatusByMedicineId(@Param("medicineId") UUID medicineId, @Param("status") MedicineUnit.UnitStatus status);
}
