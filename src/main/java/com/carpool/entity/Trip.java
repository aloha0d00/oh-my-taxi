package com.carpool.entity;

import com.carpool.enums.TripStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "departure_location", nullable = false, length = 255)
    private String departureLocation;

    @Column(name = "arrival_location", nullable = false, length = 255)
    private String arrivalLocation;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Column(name = "price_per_seat", precision = 10, scale = 2, nullable = false)
    private BigDecimal pricePerSeat;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_status", nullable = false)
    @Builder.Default
    private TripStatus tripStatus = TripStatus.pending;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "departure_latitude")
    private Float departureLatitude;

    @Column(name = "departure_longitude")
    private Float departureLongitude;

    @Column(name = "arrival_latitude")
    private Float arrivalLatitude;

    @Column(name = "arrival_longitude")
    private Float arrivalLongitude;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
