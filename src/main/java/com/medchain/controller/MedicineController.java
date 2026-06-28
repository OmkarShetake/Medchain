package com.medchain.controller;

import com.medchain.dto.request.CreateMedicineRequest;
import com.medchain.dto.request.QRGenerationRequest;
import com.medchain.dto.response.MedicineResponse;
import com.medchain.dto.response.QRCodeDto;
import com.medchain.service.MedicineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Medicine Management", description = "Medicine and QR code management endpoints")
public class MedicineController {

    private final MedicineService medicineService;

    // Manufacturer endpoints
    @PostMapping("/manufacturer/medicines")
    @Operation(summary = "Create medicine", description = "Register a new medicine (Manufacturer only)")
    public ResponseEntity<MedicineResponse> createMedicine(@Valid @RequestBody CreateMedicineRequest request) {
        MedicineResponse response = medicineService.createMedicine(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/manufacturer/medicines")
    @Operation(summary = "Get my medicines", description = "Get all medicines registered by manufacturer")
    public ResponseEntity<Page<MedicineResponse>> getMyMedicines(Pageable pageable) {
        Page<MedicineResponse> medicines = medicineService.getMyMedicines(pageable);
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/manufacturer/medicines/{id}")
    @Operation(summary = "Get medicine by ID", description = "Get medicine details by ID")
    public ResponseEntity<MedicineResponse> getMedicineById(@PathVariable UUID id) {
        MedicineResponse medicine = medicineService.getMedicineById(id);
        return ResponseEntity.ok(medicine);
    }

    @PostMapping("/manufacturer/medicines/{id}/generate-qr")
    @Operation(summary = "Generate QR codes", description = "Generate QR codes for medicine units")
    public ResponseEntity<List<QRCodeDto>> generateQRCodes(
            @PathVariable UUID id,
            @Valid @RequestBody QRGenerationRequest request) {
        List<QRCodeDto> qrCodes = medicineService.generateQRCodes(id, request);
        return ResponseEntity.ok(qrCodes);
    }

    @GetMapping("/manufacturer/medicines/{id}/units")
    @Operation(summary = "Get medicine units", description = "Get all QR code units for a medicine")
    public ResponseEntity<Page<QRCodeDto>> getMedicineUnits(@PathVariable UUID id, Pageable pageable) {
        Page<QRCodeDto> units = medicineService.getMedicineUnits(id, pageable);
        return ResponseEntity.ok(units);
    }

    // Public endpoints
    @GetMapping("/medicines/search")
    @Operation(summary = "Search medicines", description = "Search medicines by name, generic name, or category")
    public ResponseEntity<Page<MedicineResponse>> searchMedicines(
            @RequestParam(required = false) String query,
            Pageable pageable) {
        Page<MedicineResponse> medicines = medicineService.searchMedicines(query != null ? query : "", pageable);
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/medicines/{id}/public")
    @Operation(summary = "Get medicine public info", description = "Get public medicine information")
    public ResponseEntity<MedicineResponse> getMedicinePublic(@PathVariable UUID id) {
        MedicineResponse medicine = medicineService.getMedicinePublic(id);
        return ResponseEntity.ok(medicine);
    }
}
