package com.medchain.repository;

import com.medchain.entity.Recall;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecallRepository extends JpaRepository<Recall, UUID> {
    Page<Recall> findByIsActive(Boolean isActive, Pageable pageable);
    List<Recall> findByIsActiveTrue();
    
    @Query("SELECT r FROM Recall r WHERE r.medicine.manufacturer.id = :manufacturerId")
    Page<Recall> findByManufacturerId(@Param("manufacturerId") UUID manufacturerId, Pageable pageable);
    
    long countByIsActive(Boolean isActive);
}
