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
public class ManufacturerDto {
    private UUID id;
    private String companyName;
    private String licenseNumber;
    private String gstNumber;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private Boolean isVerified;
    // From linked User
    private String email;
    private String userName;
    private LocalDateTime createdAt;
}
