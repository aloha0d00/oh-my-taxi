package com.carpool.repository;

import com.carpool.entity.Rating;
import com.carpool.entity.Trip;
import com.carpool.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Integer> {

    List<Rating> findByTripOrderByCreatedAtDesc(Trip trip);

    List<Rating> findByRatedUserOrderByCreatedAtDesc(User ratedUser);

    boolean existsByRaterAndTrip(User rater, Trip trip);
}
