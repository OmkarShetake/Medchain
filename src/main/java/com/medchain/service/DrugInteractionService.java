package com.medchain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medchain.dto.response.DrugInteractionResponse;
import com.medchain.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrugInteractionService {

    private final GeminiClient geminiClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:druginteraction:";
    private static final int MAX_REQUESTS_PER_HOUR = 20;

    public DrugInteractionResponse checkInteractions(List<String> medicines) {
        checkRateLimit("anonymous");
        return doCheckInteractions(medicines);
    }

    private DrugInteractionResponse doCheckInteractions(List<String> medicines) {
        if (!geminiClient.isConfigured()) {
            log.warn("Gemini API key not configured - returning fallback");
            return fallbackResponse();
        }
        try {
            String medicineList = String.join(", ", medicines);

            String prompt = """
                    You are a clinical pharmacologist AI.
                    Check drug interactions between: %s
                    
                    Return ONLY valid JSON (no markdown, no code blocks):
                    {
                      "interactions": [{"drug1":"string","drug2":"string","severity":"MILD","mechanism":"string","effect":"string","recommendation":"string","avoidCombination":false}],
                      "overallRisk": "LOW",
                      "safeCombination": true,
                      "summary": "string",
                      "alternatives": []
                    }
                    """.formatted(medicineList);

            String raw = geminiClient.generate(prompt);
            if (raw == null) return fallbackResponse();

            String json = raw.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("(?s)^```[a-z]*\\n?", "").replaceAll("```$", "").trim();
            }

            DrugInteractionResponse result = objectMapper.readValue(json, DrugInteractionResponse.class);
            log.info("Drug interaction check completed: overallRisk={}", result.getOverallRisk());
            return result;

        } catch (Exception e) {
            log.error("Drug interaction check failed: {}", e.getMessage());
            return fallbackResponse();
        }
    }

    private DrugInteractionResponse fallbackResponse() {
        return DrugInteractionResponse.builder()
                .interactions(List.of())
                .overallRisk("UNKNOWN")
                .safeCombination(false)
                .summary("AI service unavailable. Please consult a pharmacist for drug interaction advice.")
                .alternatives(List.of())
                .build();
    }

    private void checkRateLimit(String userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) redisTemplate.expire(key, 1, TimeUnit.HOURS);
        if (count != null && count > MAX_REQUESTS_PER_HOUR) {
            throw new RateLimitException("AI drug interaction quota exceeded. Try again in 1 hour.", 3600);
        }
    }
}
