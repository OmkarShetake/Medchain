package com.medchain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medicine_units")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @Column(name = "qr_code", nullable = false, unique = true)
    private String qrCode;

    @Column(name = "strip_number", length = 100)
    private String stripNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UnitStatus status = UnitStatus.ACTIVE;

    @Column(name = "distributed_state", length = 100)
    private String distributedState;

    @Column(name = "distributed_city", length = 100)
    private String distributedCity;

    @Column(name = "distributed_at")
    private LocalDateTime distributedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum UnitStatus {
        ACTIVE, RECALLED, EXPIRED, VERIFIED
    }
}
