package com.medchain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitReportRequest {
    
    private String qrCode;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    private BigDecimal locationLat;
    private BigDecimal locationLng;
    private String city;
    private String state;
}
