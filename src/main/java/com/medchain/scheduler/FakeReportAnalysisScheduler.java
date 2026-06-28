package com.medchain.scheduler;

import com.medchain.entity.FakeReport;
import com.medchain.repository.FakeReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FakeReportAnalysisScheduler {

    private final FakeReportRepository reportRepository;

    @Scheduled(cron = "0 0 6 * * *") // Run daily at 6 AM
    @Transactional
    public void analyzePendingReports() {
        log.info("Starting fake report analysis...");

        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        
        // Find PENDING reports older than 24 hours
        List<FakeReport> pendingReports = reportRepository.findPendingReportsOlderThan(
                FakeReport.ReportStatus.PENDING,
                yesterday
        );

        int autoVerifiedCount = 0;

        for (FakeReport report : pendingReports) {
            // In a real implementation, this would call AI analysis service
            // For now, we'll just log
            
            // Auto-verify if AI confidence > 85
            if (report.getAiConfidenceScore() != null && report.getAiConfidenceScore() > 85) {
                report.setStatus(FakeReport.ReportStatus.VERIFIED);
                report.setAdminNotes("Auto-verified by AI (confidence: " + report.getAiConfidenceScore() + "%)");
                reportRepository.save(report);
                autoVerifiedCount++;
            }
        }

        log.info("Analyzed {} pending reports, auto-verified {}", pendingReports.size(), autoVerifiedCount);

        // Generate daily summary
        long newReportsToday = reportRepository.countByStatus(FakeReport.ReportStatus.PENDING);
        long verifiedToday = reportRepository.countByStatus(FakeReport.ReportStatus.VERIFIED);

        log.info("Daily Summary - New: {}, Verified: {}", newReportsToday, verifiedToday);
        log.info("Fake report analysis completed");
    }
}
