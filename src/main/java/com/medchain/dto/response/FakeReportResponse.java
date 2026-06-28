package com.medchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FakeReportResponse {
    private UUID id;
    private String qrCode;
    private String description;
    private String photoUrl;
    private String city;
    private String state;
    private String status;
    private Integer aiConfidenceScore;
    private String aiAnalysis;
    private String adminNotes;
    private LocalDateTime createdAt;
    private MedicineResponse medicine;
    private UserDto reportedBy;

    // Convenience fields for frontend
    private String medicineName;   // medicine.name or "Unknown"
    private String reporterName;   // reportedBy.name
    private String location;       // city + state
}
