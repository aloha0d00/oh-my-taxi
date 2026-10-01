package com.carpool.repository;

import com.carpool.entity.Participant;
import com.carpool.entity.Trip;
import com.carpool.entity.User;
import com.carpool.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Integer> {

    Optional<Participant> findByTripAndUserAndParticipantStatusNot(Trip trip, User user, ParticipantStatus status);

    List<Participant> findByTripAndParticipantStatusOrderByCreatedAtDesc(Trip trip, ParticipantStatus status);

    List<Participant> findByUserOrderByCreatedAtDesc(User user);

    boolean existsByTripAndUser(Trip trip, User user);

    @Query("SELECT p FROM Participant p JOIN FETCH p.user WHERE p.trip = :trip AND p.participantStatus IN :statuses ORDER BY p.createdAt DESC")
    List<Participant> findByTripAndParticipantStatusInOrderByCreatedAtDesc(@Param("trip") Trip trip, @Param("statuses") List<ParticipantStatus> statuses);

    void deleteByTripAndParticipantStatus(Trip trip, ParticipantStatus status);
}
