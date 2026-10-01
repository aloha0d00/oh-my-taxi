package com.carpool.repository;

import com.carpool.entity.Trip;
import com.carpool.entity.User;
import com.carpool.enums.TripStatus;
import com.carpool.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Integer>,
        JpaSpecificationExecutor<Trip> {

    List<Trip> findByDriverOrderByCreatedAtDesc(User driver);

    List<Trip> findByTripStatusOrderByCreatedAtDesc(TripStatus status);

    long countByVehicleAndTripStatusIn(Vehicle vehicle, List<TripStatus> statuses);

    long count();
}
