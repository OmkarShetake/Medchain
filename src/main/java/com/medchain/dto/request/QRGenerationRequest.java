package com.medchain.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRGenerationRequest {
    
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 1000, message = "Maximum 1000 QR codes per request")
    private Integer quantity;
    
    @NotBlank(message = "Distribution city is required")
    private String distributedCity;
    
    @NotBlank(message = "Distribution state is required")
    private String distributedState;
}
