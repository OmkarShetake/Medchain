package com.medchain.repository;

import com.medchain.entity.ScanLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScanLogRepository extends JpaRepository<ScanLog, UUID> {
    long countByQrCode(String qrCode);
    Page<ScanLog> findByScannedById(UUID userId, Pageable pageable);
    
    @Query("SELECT COUNT(s) FROM ScanLog s WHERE s.createdAt >= :date")
    long countScansAfter(@Param("date") LocalDateTime date);
    
    @Query("SELECT s.scanResult, COUNT(s) FROM ScanLog s WHERE s.createdAt >= :date GROUP BY s.scanResult")
    List<Object[]> countByScanResultAfter(@Param("date") LocalDateTime date);
    
    @Query("SELECT DATE(s.createdAt) as date, COUNT(s) as count FROM ScanLog s " +
           "WHERE s.createdAt >= :startDate GROUP BY DATE(s.createdAt) ORDER BY date")
    List<Object[]> getScanActivityByDate(@Param("startDate") LocalDateTime startDate);
}
