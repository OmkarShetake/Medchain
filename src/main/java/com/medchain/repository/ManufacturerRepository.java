package com.medchain.repository;

import com.medchain.entity.Manufacturer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ManufacturerRepository extends JpaRepository<Manufacturer, UUID> {
    Optional<Manufacturer> findByUserId(UUID userId);
    boolean existsByLicenseNumber(String licenseNumber);
    Page<Manufacturer> findByIsVerified(Boolean isVerified, Pageable pageable);
}
