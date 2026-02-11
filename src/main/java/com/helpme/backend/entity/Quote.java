package com.helpme.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quotes", uniqueConstraints = {
        @UniqueConstraint(name = "unique_provider_per_order", columnNames = { "order_id", "provider_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Enumerated(EnumType.STRING)
    private QuoteStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal distancePrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal materialPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalEst;

    @Enumerated(EnumType.STRING)
    private LocationSource locationSource;

    private Integer etaMinutes;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}