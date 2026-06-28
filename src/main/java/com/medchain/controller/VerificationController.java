package com.medchain.controller;

import com.medchain.dto.response.MedicineResponse;
import com.medchain.dto.response.VerificationResult;
import com.medchain.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/verify")
@RequiredArgsConstructor
@Tag(name = "Verification", description = "Medicine verification endpoints")
public class VerificationController {

    private final VerificationService verificationService;

    @GetMapping("/{qrCode}")
    @Operation(summary = "Verify by QR code")
    public ResponseEntity<VerificationResult> verifyMedicine(
            @PathVariable String qrCode,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request) {
        String ipAddress = getClientIP(request);
        VerificationResult result = verificationService.verifyMedicine(qrCode, lat, lng, userAgent, ipAddress);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/batch/{batchNumber}")
    @Operation(summary = "Verify by batch number", description = "Verify medicine by the batch number printed on the box")
    public ResponseEntity<VerificationResult> verifyByBatch(
            @PathVariable String batchNumber,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request) {
        String ipAddress = getClientIP(request);
        VerificationResult result = verificationService.verifyByBatchNumber(batchNumber, lat, lng, userAgent, ipAddress);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    @Operation(summary = "Search medicines by name", description = "Search registered medicines by name for manual comparison")
    public ResponseEntity<List<MedicineResponse>> searchByName(@RequestParam String name) {
        if (name == null || name.trim().length() < 2) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(verificationService.searchByName(name));
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) return request.getRemoteAddr();
        return xfHeader.split(",")[0];
    }
}
