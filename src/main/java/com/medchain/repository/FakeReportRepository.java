package com.medchain.repository;

import com.medchain.entity.FakeReport;
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
public interface FakeReportRepository extends JpaRepository<FakeReport, UUID> {
    Page<FakeReport> findByStatus(FakeReport.ReportStatus status, Pageable pageable);
    Page<FakeReport> findByReportedById(UUID userId, Pageable pageable);
    long countByStatus(FakeReport.ReportStatus status);
    
    @Query("SELECT r FROM FakeReport r WHERE r.status = :status AND r.createdAt < :date")
    List<FakeReport> findPendingReportsOlderThan(@Param("status") FakeReport.ReportStatus status, @Param("date") LocalDateTime date);
    
    @Query("SELECT r.state, COUNT(r) FROM FakeReport r WHERE r.status = 'VERIFIED' GROUP BY r.state ORDER BY COUNT(r) DESC")
    List<Object[]> countVerifiedReportsByState();
    
    @Query("SELECT r.city, r.state, r.locationLat, r.locationLng, COUNT(r) as count " +
           "FROM FakeReport r WHERE r.status = 'VERIFIED' AND r.locationLat IS NOT NULL " +
           "GROUP BY r.city, r.state, r.locationLat, r.locationLng")
    List<Object[]> getHotspotData();
    
    @Query("SELECT r.medicine.id, COUNT(r) FROM FakeReport r WHERE r.status = 'VERIFIED' " +
           "AND r.medicine IS NOT NULL GROUP BY r.medicine.id HAVING COUNT(r) >= :threshold")
    List<Object[]> findMedicinesWithMultipleReports(@Param("threshold") long threshold);
}
