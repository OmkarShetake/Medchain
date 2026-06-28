package com.medchain.repository;

import com.medchain.entity.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, UUID> {
    Page<Medicine> findByManufacturerId(UUID manufacturerId, Pageable pageable);
    boolean existsByBatchNumber(String batchNumber);
    List<Medicine> findByStatus(Medicine.MedicineStatus status);
    List<Medicine> findByExpiryDateBefore(LocalDate date);
    
    @Query("SELECT m FROM Medicine m WHERE m.expiryDate BETWEEN :startDate AND :endDate AND m.status = 'ACTIVE'")
    List<Medicine> findExpiringBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT m FROM Medicine m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(m.genericName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(m.category) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Medicine> searchMedicines(@Param("query") String query, Pageable pageable);

    Optional<Medicine> findByBatchNumberIgnoreCase(String batchNumber);

    @Query("SELECT m FROM Medicine m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY m.name")
    List<Medicine> findByNameContainingIgnoreCase(@Param("name") String name);
}
