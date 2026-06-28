package com.medchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecallResponse {
    private UUID id;
    private MedicineResponse medicine;
    private String reason;
    private String severity;
    private List<String> affectedBatches;
    private List<String> affectedStates;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
