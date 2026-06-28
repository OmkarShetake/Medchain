package com.medchain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medchain.dto.response.ImageScanResult;
import com.medchain.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageScanService {

    private final GeminiClient geminiClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:imagescan:";
    private static final int MAX_REQUESTS_PER_HOUR = 5;
    private static final int MAX_IMAGE_DIMENSION = 1024; // resize to max 1024px
    private static final int MAX_BASE64_LENGTH = 1_000_000; // ~750KB before base64

    public ImageScanResult analyzeMedicineImage(byte[] imageBytes, String userId) {
        checkRateLimit(userId);
        return doAnalyzeImage(imageBytes);
    }

    private ImageScanResult doAnalyzeImage(byte[] imageBytes) {
        if (!geminiClient.isConfigured()) {
            log.warn("Gemini API key not configured");
            return fallbackResult();
        }

        try {
            // Compress/resize image so it fits within Gemini limits
            byte[] compressed = compressImage(imageBytes);
            String base64Image = Base64.getEncoder().encodeToString(compressed);
            String mimeType = "image/jpeg";

            log.info("Sending image to Gemini, size: {} bytes (original: {} bytes)",
                    compressed.length, imageBytes.length);

            // ── Step 1: Is this a medicine image? ────────────────────────
            String validationResponse = geminiClient.generateWithImage(
                "Is this image showing medicine packaging, a medicine box, strip, bottle, tube, or blister pack? Reply with only YES or NO.",
                base64Image, mimeType
            );

            log.info("Validation response: '{}'", validationResponse);

            if (validationResponse == null || !validationResponse.trim().toUpperCase().startsWith("YES")) {
                return ImageScanResult.builder()
                        .verdict("INVALID")
                        .confidence(0)
                        .findings(List.of())
                        .redFlags(List.of())
                        .recommendation("This is not a medicine image.")
                        .build();
            }

            // ── Step 2: Authenticity analysis ────────────────────────────
            String analysisPrompt = """
                    You are a pharmaceutical packaging authentication expert.
                    Analyze this medicine packaging image for counterfeiting or tampering.
                    
                    Evaluate these 7 points:
                    1. Print quality — sharp or blurry/smudged text?
                    2. Color — consistent and vibrant or faded/misregistered?
                    3. Hologram/security seal — present and intact?
                    4. Barcode clarity — well-printed and scannable?
                    5. Logo/brand quality — professional or amateurish?
                    6. Spelling errors on packaging?
                    7. Tampering signs — re-glued, reprinted, cut edges?
                    
                    Be strict. Mark SUSPICIOUS if anything looks off.
                    
                    Return ONLY valid JSON (no markdown, no extra text):
                    {
                      "verdict": "GENUINE or SUSPICIOUS or FAKE",
                      "confidence": <0-100>,
                      "findings": ["specific finding 1", "specific finding 2"],
                      "redFlags": [],
                      "recommendation": "one sentence advice"
                    }
                    """;

            String raw = geminiClient.generateWithImage(analysisPrompt, base64Image, mimeType);

            log.info("Analysis raw response: {}", raw != null ? raw.substring(0, Math.min(500, raw.length())) : "null");

            if (raw == null) return fallbackResult();

            // Strip markdown if present
            String json = raw.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("(?s)^```[a-z]*\\n?", "").replaceAll("```$", "").trim();
            }

            // Extract JSON object in case there's surrounding text
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }

            ImageScanResult result = objectMapper.readValue(json, ImageScanResult.class);
            if (result.getConfidence() == null || result.getConfidence() < 0) {
                result.setConfidence(50);
            }

            log.info("Image scan done: verdict={}, confidence={}", result.getVerdict(), result.getConfidence());
            return result;

        } catch (Exception e) {
            log.error("Image scan failed: {}", e.getMessage(), e);
            return fallbackResult();
        }
    }

    /**
     * Resize and compress image to stay within Gemini's limits.
     * Resizes to max 1024px and compresses to JPEG quality 0.85.
     */
    private byte[] compressImage(byte[] imageBytes) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (original == null) return imageBytes;

            // Resize if too large
            int w = original.getWidth();
            int h = original.getHeight();
            if (w > MAX_IMAGE_DIMENSION || h > MAX_IMAGE_DIMENSION) {
                double scale = Math.min((double) MAX_IMAGE_DIMENSION / w, (double) MAX_IMAGE_DIMENSION / h);
                int newW = (int) (w * scale);
                int newH = (int) (h * scale);
                BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = resized.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(original, 0, 0, newW, newH, null);
                g.dispose();
                original = resized;
            }

            // Compress to JPEG
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.85f);
            writer.setOutput(ImageIO.createImageOutputStream(out));
            writer.write(null, new IIOImage(original, null, null), params);
            writer.dispose();

            return out.toByteArray();
        } catch (Exception e) {
            log.warn("Image compression failed, using original: {}", e.getMessage());
            return imageBytes;
        }
    }

    private ImageScanResult fallbackResult() {
        return ImageScanResult.builder()
                .verdict("SUSPICIOUS")
                .confidence(0)
                .findings(List.of("AI service unavailable"))
                .redFlags(List.of())
                .recommendation("Please verify via QR code, batch number, or consult a pharmacist.")
                .build();
    }

    private void checkRateLimit(String userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) redisTemplate.expire(key, 1, TimeUnit.HOURS);
        if (count != null && count > MAX_REQUESTS_PER_HOUR) {
            throw new RateLimitException("AI image scan quota exceeded. Try again in 1 hour.", 3600);
        }
    }
}
