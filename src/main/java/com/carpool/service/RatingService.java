package com.carpool.service;

import com.carpool.entity.Rating;
import com.carpool.entity.Trip;
import com.carpool.entity.User;
import com.carpool.exception.BusinessException;
import com.carpool.exception.ErrorCode;
import com.carpool.repository.ParticipantRepository;
import com.carpool.repository.RatingRepository;
import com.carpool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final ParticipantRepository participantRepository;
    private final TripService tripService;
    private final UserRepository userRepository;

    @Transactional
    public Map<String, Object> createRating(User rater, Integer ratedUserId, Integer tripId,
                                             Integer ratingScore, String comment) {
        if (ratingScore < 1 || ratingScore > 5) {
            throw new BusinessException(ErrorCode.RATING_SCORE_INVALID);
        }
        Trip trip = tripService.getTripEntity(tripId);
        if (trip.getTripStatus() != com.carpool.enums.TripStatus.completed) {
            throw new BusinessException(ErrorCode.RATING_TRIP_NOT_COMPLETED);
        }
        if (ratingRepository.existsByRaterAndTrip(rater, trip)) {
            throw new BusinessException(ErrorCode.RATING_ALREADY_EXISTS);
        }
        boolean isParticipant = participantRepository
                .findByTripAndParticipantStatusOrderByCreatedAtDesc(trip, com.carpool.enums.ParticipantStatus.accepted)
                .stream().anyMatch(p -> p.getUser().getId().equals(rater.getId()));
        if (!isParticipant && !trip.getDriver().getId().equals(rater.getId())) {
            throw new BusinessException(ErrorCode.RATING_NOT_PARTICIPANT);
        }
        User ratedUser;
        if (ratedUserId == null || trip.getDriver().getId().equals(ratedUserId)) {
            ratedUser = trip.getDriver();
        } else {
            ratedUser = userRepository.findById(ratedUserId).orElse(null);
        }
        if (ratedUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        Rating rating = Rating.builder().rater(rater).ratedUser(ratedUser)
                .trip(trip).ratingScore(ratingScore).comment(comment).build();
        ratingRepository.save(rating);
        Double avgRating = ratingRepository.findByRatedUserOrderByCreatedAtDesc(ratedUser)
                .stream().mapToInt(Rating::getRatingScore).average().orElse(5.0);
        ratedUser.setRating(avgRating);
        ratedUser.setTotalRatings((int) ratingRepository.findByRatedUserOrderByCreatedAtDesc(ratedUser)
                .stream().count());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "评价成功");
        return result;
    }

    public Map<String, Object> getTripRatings(Integer tripId) {
        Trip trip = tripService.getTripEntity(tripId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ratings", ratingRepository.findByTripOrderByCreatedAtDesc(trip));
        return result;
    }

    public Map<String, Object> getUserRatings(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ratings", ratingRepository.findByRatedUserOrderByCreatedAtDesc(user));
        return result;
    }
}