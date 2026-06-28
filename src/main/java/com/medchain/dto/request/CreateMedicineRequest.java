package com.medchain.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMedicineRequest {
    
    @NotBlank(message = "Medicine name is required")
    private String name;
    
    private String genericName;
    
    private String composition;
    
    private String category;
    
    @NotBlank(message = "Batch number is required")
    private String batchNumber;
    
    @NotNull(message = "Manufacturing date is required")
    private LocalDate manufacturingDate;
    
    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;
    
    private String storageInstructions;
    
    @Positive(message = "Quantity must be positive")
    private Integer quantityProduced;
    
    private String description;
    
    private String sideEffects;
}
