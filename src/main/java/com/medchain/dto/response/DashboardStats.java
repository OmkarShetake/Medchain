package com.medchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private Long totalMedicinesRegistered;
    private Long totalQRCodesGenerated;
    private Long totalScansToday;
    private Long totalScansAllTime;
    private Long pendingReports;
    private Long verifiedReports;
    private Long activeRecalls;
    private Long verifiedManufacturers;
    private Long pendingManufacturers;
    private List<Map<String, Object>> scanActivityLast7Days;
    private List<Map<String, Object>> reportsByState;
}
