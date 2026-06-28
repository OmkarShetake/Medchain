package com.medchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRCodeDto {
    private UUID unitId;
    private String qrCode;
    private String qrImageBase64;
    private String stripNumber;
}
