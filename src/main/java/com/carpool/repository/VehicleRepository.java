package com.carpool.repository;

import com.carpool.entity.Vehicle;
import com.carpool.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {

    Optional<Vehicle> findByPlateNumber(String plateNumber);

    List<Vehicle> findByUser(User user);

    Optional<Vehicle> findFirstByUser(User user);

    long count();
}
