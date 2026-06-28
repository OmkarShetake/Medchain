package com.medchain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Direct Gemini REST API client.
 * Uses a simple API key — no GCP credentials required.
 * Get free key at: https://aistudio.google.com/apikey
 */
@Slf4j
@Component
public class GeminiClient {

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    @Value("${GEMINI_API_KEY:}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("your-gemini-api-key");
    }

    @PostConstruct
    public void logStatus() {
        if (isConfigured()) {
            log.info("Gemini API configured — key starts with: {}...", apiKey.substring(0, Math.min(8, apiKey.length())));
        } else {
            log.warn("GEMINI_API_KEY not set — AI features will return fallback responses");
        }
    }

    /** Text-only prompt */
    public String generate(String prompt) {
        if (!isConfigured()) return null;
        try {
            Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.3, "maxOutputTokens", 4000)
            );
            return doRequest(objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            log.error("Gemini text call failed: {}", e.getMessage());
            return null;
        }
    }

    /** Multimodal prompt with image */
    public String generateWithImage(String textPrompt, String base64Image, String mimeType) {
        if (!isConfigured()) return null;
        try {
            // Build multimodal request
            Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(
                        Map.of("text", textPrompt),
                        Map.of("inlineData", Map.of(
                            "mimeType", mimeType,
                            "data", base64Image
                        ))
                    )
                )),
                "generationConfig", Map.of(
                    "temperature", 0.3,
                    "maxOutputTokens", 4000
                )
            );
            String requestBody = objectMapper.writeValueAsString(body);
            log.debug("Sending image to Gemini, base64 length: {}", base64Image.length());
            return doRequest(requestBody);
        } catch (Exception e) {
            log.error("Gemini image call failed: {}", e.getMessage());
            return null;
        }
    }

    private String doRequest(String requestBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_API_URL + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warn("Gemini API status {}: {}", response.statusCode(), response.body());
            return null;
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty()) {
            log.warn("Gemini returned no candidates: {}", response.body());
            return null;
        }
        return candidates.get(0).path("content").path("parts").get(0).path("text").asText(null);
    }
}
