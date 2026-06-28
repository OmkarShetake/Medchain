package com.medchain.controller;

import com.medchain.dto.response.DrugInteractionResponse;
import com.medchain.dto.response.ImageScanResult;
import com.medchain.dto.response.SymptomCheckResponse;
import com.medchain.service.DrugInteractionService;
import com.medchain.service.ImageScanService;
import com.medchain.service.SymptomCheckerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Features", description = "AI-powered medicine analysis and recommendations")
public class AIController {

    private final ImageScanService imageScanService;
    private final SymptomCheckerService symptomCheckerService;
    private final DrugInteractionService drugInteractionService;

    @PostMapping(value = "/image-scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Scan medicine image", description = "Analyze medicine packaging image for authenticity")
    public ResponseEntity<ImageScanResult> scanImage(
            @RequestPart("image") MultipartFile image,
            Authentication authentication) throws IOException {
        byte[] imageBytes = image.getBytes();
        String rateLimitIdentity = authentication != null ? authentication.getName() : "anonymous";
        ImageScanResult result = imageScanService.analyzeMedicineImage(imageBytes, rateLimitIdentity);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/symptom-check")
    @Operation(summary = "Check symptoms", description = "Get medicine recommendations based on symptoms")
    public ResponseEntity<SymptomCheckResponse> checkSymptoms(@RequestBody Map<String, String> request) {
        String symptoms = request.get("symptoms");
        String language = request.getOrDefault("language", "English");
        SymptomCheckResponse response = symptomCheckerService.checkSymptoms(symptoms, language);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/drug-interactions")
    @Operation(summary = "Check drug interactions", description = "Check interactions between multiple medicines")
    public ResponseEntity<DrugInteractionResponse> checkInteractions(@RequestBody Map<String, List<String>> request) {
        List<String> medicines = request.get("medicines");
        DrugInteractionResponse response = drugInteractionService.checkInteractions(medicines);
        return ResponseEntity.ok(response);
    }
}
