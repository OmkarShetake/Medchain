package com.medchain.scheduler;

import com.medchain.repository.FakeReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskZoneCacheScheduler {

    private final FakeReportRepository reportRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedRate = 21600000) // Every 6 hours
    public void recalculateRiskZones() {
        log.info("Starting risk zone recalculation...");

        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        
        // Get verified reports from last 90 days
        List<Object[]> reportsByState = reportRepository.countVerifiedReportsByState();

        Map<String, Integer> riskScores = new HashMap<>();

        for (Object[] result : reportsByState) {
            String state = (String) result[0];
            Long count = (Long) result[1];

            if (state != null) {
                // Calculate risk score
                int baseScore = count.intValue() * 10;
                
                // Recent reports get 2x multiplier (simplified)
                int riskScore = baseScore;

                riskScores.put(state, riskScore);
            }
        }

        // Store in Redis
        String cacheKey = "riskzones:data";
        redisTemplate.opsForValue().set(cacheKey, riskScores, 6, TimeUnit.HOURS);

        // Detect newly emerging hotspots
        for (Map.Entry<String, Integer> entry : riskScores.entrySet()) {
            if (entry.getValue() > 100) { // Critical threshold
                log.warn("CRITICAL RISK ZONE DETECTED: {} with score {}", entry.getKey(), entry.getValue());
                // In production, this would alert admin
            }
        }

        log.info("Risk zone recalculation completed. {} zones analyzed", riskScores.size());
    }
}
