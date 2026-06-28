package com.medchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymptomCheckResponse {
    private List<MedicineRecommendation> medicines;
    private String severity; // MILD, MODERATE, SEVERE
    private List<String> homeRemedies;
    private String doctorAdvice;
    private Boolean emergencyWarning;
    private String emergencyReason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicineRecommendation {
        private String name;
        private String genericName;
        private String dosage;
        private String usage;
        private String warning;
        private String availableAs; // OTC or PRESCRIPTION
    }
}
