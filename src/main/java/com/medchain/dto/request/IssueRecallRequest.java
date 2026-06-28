package com.medchain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueRecallRequest {
    
    @NotNull(message = "Medicine ID is required")
    private UUID medicineId;
    
    @NotBlank(message = "Reason is required")
    private String reason;
    
    @NotBlank(message = "Severity is required")
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    
    private List<String> affectedBatches;
    private List<String> affectedStates;
}
