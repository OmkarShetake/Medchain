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
public class DrugInteractionResponse {
    private List<Interaction> interactions;
    private String overallRisk; // LOW, MEDIUM, HIGH, CRITICAL
    private Boolean safeCombination;
    private String summary;
    private List<String> alternatives;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Interaction {
        private String drug1;
        private String drug2;
        private String severity; // NONE, MILD, MODERATE, SEVERE, DANGEROUS
        private String mechanism;
        private String effect;
        private String recommendation;
        private Boolean avoidCombination;
    }
}
