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
public class ImageScanResult {
    private String verdict; // GENUINE, SUSPICIOUS, FAKE
    private Integer confidence; // 0-100
    private List<String> findings;
    private List<String> redFlags;
    private String recommendation;
}
