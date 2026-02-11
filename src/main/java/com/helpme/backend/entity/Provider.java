package com.helpme.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Provider {

    @Id
    private UUID id; // Trùng với user.id

    @Column(length = 150)
    private String orgName;

    @Column(columnDefinition = "geography(Point,4326)")
    private Point baseLocation;

    @Column(columnDefinition = "text[]")
    private String[] serviceTypes;

    @Column(precision = 2, scale = 1)
    private BigDecimal ratingAvg;

    @Enumerated(EnumType.STRING)
    private KycStatus kycStatus;

    private Boolean isOnline;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}