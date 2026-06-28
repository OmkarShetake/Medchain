package com.medchain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medchain.dto.response.SymptomCheckResponse;
import com.medchain.entity.Medicine;
import com.medchain.exception.RateLimitException;
import com.medchain.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SymptomCheckerService {

    private final GeminiClient geminiClient;
    private final MedicineRepository medicineRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:symptom:";
    private static final int MAX_REQUESTS_PER_HOUR = 20;

    public SymptomCheckResponse checkSymptoms(String symptoms, String language) {
        checkRateLimit("anonymous");
        return doCheckSymptoms(symptoms, language);
    }

    private SymptomCheckResponse doCheckSymptoms(String symptoms, String language) {
        if (!geminiClient.isConfigured()) {
            log.warn("Gemini API key not configured - returning fallback");
            return fallbackResponse();
        }
        try {
            List<Medicine> medicines = medicineRepository.findAll().stream()
                    .limit(10).collect(Collectors.toList());

            String candidates = medicines.stream()
                    .map(m -> m.getName() + (m.getDescription() != null ? " - " + m.getDescription() : ""))
                    .collect(Collectors.joining("\n"));

            String prompt = """
                    You are a medical assistant AI for India. Respond in %s.
                    Patient symptoms: %s
                    
                    Relevant medicines from database:
                    %s
                    
                    Suggest appropriate medicines. Consider Indian generic names. Prioritize safety.
                    
                    Return ONLY valid JSON (no markdown, no code blocks):
                    {
                      "medicines": [{"name":"string","genericName":"string","dosage":"string","usage":"string","warning":"string","availableAs":"OTC"}],
                      "severity": "MILD",
                      "homeRemedies": ["remedy1","remedy2"],
                      "doctorAdvice": "string",
                      "emergencyWarning": false,
                      "emergencyReason": null
                    }
                    """.formatted(language, symptoms, candidates);

            String raw = geminiClient.generate(prompt);
            if (raw == null) return fallbackResponse();

            // Strip markdown code fences if Gemini wraps in ```json ... ```
            String json = raw.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("(?s)^```[a-z]*\\n?", "").replaceAll("```$", "").trim();
            }

            SymptomCheckResponse result = objectMapper.readValue(json, SymptomCheckResponse.class);
            log.info("Symptom check completed: severity={}", result.getSeverity());
            return result;

        } catch (Exception e) {
            log.error("Symptom check failed: {}", e.getMessage());
            return fallbackResponse();
        }
    }

    private SymptomCheckResponse fallbackResponse() {
        return SymptomCheckResponse.builder()
                .medicines(List.of())
                .severity("MODERATE")
                .homeRemedies(List.of("Rest", "Stay hydrated", "Monitor symptoms"))
                .doctorAdvice("AI service unavailable. Please consult a doctor or pharmacist.")
                .emergencyWarning(false)
                .build();
    }

    private void checkRateLimit(String userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) redisTemplate.expire(key, 1, TimeUnit.HOURS);
        if (count != null && count > MAX_REQUESTS_PER_HOUR) {
            throw new RateLimitException("AI symptom check quota exceeded. Try again in 1 hour.", 3600);
        }
    }
}
