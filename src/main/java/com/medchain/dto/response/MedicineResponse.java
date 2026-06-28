package com.medchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineResponse {
    private UUID id;
    private String name;
    private String genericName;
    private String composition;
    private String category;
    private String batchNumber;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private String storageInstructions;
    private Integer quantityProduced;
    private String status;
    private String description;
    private String sideEffects;
    private ManufacturerDto manufacturer;
}
