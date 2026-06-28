package com.medchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResult {
    private String verdict; // GENUINE, FAKE, EXPIRED, RECALLED, NOT_FOUND
    private Integer confidence;
    private MedicineResponse medicine;
    private ManufacturerDto manufacturer;
    private ExpiryInfo expiryInfo;
    private Long scanCount;
    private String warning;
    private Boolean reportButton;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpiryInfo {
        private LocalDate expiryDate;
        private Long daysUntilExpiry;
        private Boolean isExpired;
    }
}
