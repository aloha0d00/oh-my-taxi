package com.carpool.service;

import com.carpool.entity.Participant;
import com.carpool.entity.Trip;
import com.carpool.entity.User;
import com.carpool.enums.ParticipantStatus;
import com.carpool.enums.PaymentStatus;
import com.carpool.enums.TripStatus;
import com.carpool.exception.BusinessException;
import com.carpool.exception.ErrorCode;
import com.carpool.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final TripService tripService;

    @Transactional
    public Map<String, Object> joinTrip(Integer tripId, User user, Integer seatsBooked) {
        Trip trip = tripService.getTripEntity(tripId);
        if (trip.getTripStatus() != TripStatus.pending) {
            throw new BusinessException(ErrorCode.TRIP_CLOSED);
        }
        if (trip.getAvailableSeats() < seatsBooked) {
            throw new BusinessException(ErrorCode.TRIP_SEATS_INSUFFICIENT);
        }
        if (participantRepository.existsByTripAndUser(trip, user)) {
            throw new BusinessException(ErrorCode.TRIP_ALREADY_JOINED);
        }
        Participant p = Participant.builder()
                .trip(trip).user(user).seatsBooked(seatsBooked).build();
        participantRepository.save(p);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "申请成功，等待司机确认");
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTripApplications(Integer tripId, User driver) {
        Trip trip = tripService.getTripEntity(tripId);
        if (!trip.getDriver().getId().equals(driver.getId())) return null;
        List<Participant> apps = participantRepository
                .findByTripAndParticipantStatusOrderByCreatedAtDesc(trip, ParticipantStatus.pending);
        return apps.stream().map(this::participantToMap).toList();
    }

    @Transactional
    public Map<String, Object> handleApplication(Integer applicationId, User driver, String status) {
        Participant app = participantRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        Trip trip = app.getTrip();
        if (!trip.getDriver().getId().equals(driver.getId())) {
            throw new BusinessException(ErrorCode.TRIP_ONLY_DRIVER_CAN_MODIFY);
        }
        if (app.getParticipantStatus() != ParticipantStatus.pending) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_HANDLED);
        }
        ParticipantStatus newStatus = ParticipantStatus.valueOf(status);
        if (newStatus == ParticipantStatus.accepted) {
            if (trip.getAvailableSeats() < app.getSeatsBooked()) {
                throw new BusinessException(ErrorCode.TRIP_SEATS_INSUFFICIENT);
            }
            trip.setAvailableSeats(trip.getAvailableSeats() - app.getSeatsBooked());
        }
        app.setParticipantStatus(newStatus);
        participantRepository.save(app);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "申请处理成功");
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStudentApplications(User user) {
        List<Participant> apps = participantRepository.findByUserOrderByCreatedAtDesc(user);
        return apps.stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId()); map.put("tripId", p.getTrip().getId());
            map.put("seatsBooked", p.getSeatsBooked());
            map.put("participantStatus", p.getParticipantStatus().name());
            map.put("paymentStatus", p.getPaymentStatus().name());
            map.put("createdAt", p.getCreatedAt());
            Trip t = p.getTrip();
            Map<String, Object> tripMap = new LinkedHashMap<>();
            tripMap.put("departureLocation", t.getDepartureLocation());
            tripMap.put("arrivalLocation", t.getArrivalLocation());
            tripMap.put("departureTime", t.getDepartureTime() != null
                    ? t.getDepartureTime().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    : null);
            tripMap.put("pricePerSeat", t.getPricePerSeat());
            Map<String, Object> vehicle = new LinkedHashMap<>();
            vehicle.put("brand", t.getVehicle().getBrand());
            vehicle.put("model", t.getVehicle().getModel());
            tripMap.put("vehicle", vehicle);
            map.put("trip", tripMap);
            Map<String, Object> driver = new LinkedHashMap<>();
            driver.put("name", t.getDriver().getName());
            map.put("driver", driver);
            return map;
        }).toList();
    }

    @Transactional
    public Map<String, Object> cancelApplication(Integer applicationId, User user) {
        Participant app = participantRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        if (!app.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有申请人可以取消申请");
        }
        if (app.getParticipantStatus() == ParticipantStatus.completed
            || app.getParticipantStatus() == ParticipantStatus.cancelled) {
            throw new BusinessException(ErrorCode.APPLICATION_CANNOT_CANCEL);
        }
        if (app.getTrip().getTripStatus() != TripStatus.pending) {
            throw new BusinessException(ErrorCode.TRIP_CLOSED);
        }
        if (app.getParticipantStatus() == ParticipantStatus.accepted) {
            Trip trip = app.getTrip();
            trip.setAvailableSeats(trip.getAvailableSeats() + app.getSeatsBooked());
        }
        participantRepository.delete(app);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "申请取消成功");
        return result;
    }

    @Transactional
    public Map<String, Object> payApplication(Integer applicationId, User user) {
        Participant app = participantRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        if (!app.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有申请人可以支付");
        }
        if (app.getParticipantStatus() != ParticipantStatus.accepted
            && app.getParticipantStatus() != ParticipantStatus.completed) {
            throw new BusinessException(ErrorCode.APPLICATION_STATUS_INVALID);
        }
        if (app.getPaymentStatus() == PaymentStatus.paid) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已支付");
        }
        app.setPaymentStatus(PaymentStatus.paid);
        participantRepository.save(app);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "支付成功");
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTripPassengersAndIncome(Integer tripId, User driver) {
        Trip trip = tripService.getTripEntity(tripId);
        if (!trip.getDriver().getId().equals(driver.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有行程司机可以查看乘客信息");
        }
        List<Participant> passengers = participantRepository
                .findByTripAndParticipantStatusInOrderByCreatedAtDesc(trip,
                        Arrays.asList(ParticipantStatus.accepted, ParticipantStatus.completed));
        double totalIncome = 0;
        double price = trip.getPricePerSeat() != null ? trip.getPricePerSeat().doubleValue() : 0;
        for (Participant p : passengers) totalIncome += price * p.getSeatsBooked();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tripId", tripId); result.put("totalIncome", totalIncome);
        result.put("pricePerSeat", price);
        result.put("passengers", passengers.stream().map(p -> {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("id", p.getId()); pm.put("seatsBooked", p.getSeatsBooked());
            pm.put("paymentStatus", p.getPaymentStatus().name());
            pm.put("createdAt", p.getCreatedAt());
            Map<String, Object> student = new LinkedHashMap<>();
            student.put("name", p.getUser().getName());
            student.put("phone", p.getUser().getPhone());
            student.put("studentId", p.getUser().getStudentId());
            pm.put("student", student);
            return pm;
        }).toList());
        return result;
    }

    @Transactional
    public void updateStatusByTripId(Integer tripId, String status) {
        Trip trip = tripService.getTripEntity(tripId);
        ParticipantStatus targetStatus;
        if ("completed".equals(status)) {
            targetStatus = ParticipantStatus.completed;
        } else if ("cancelled".equals(status)) {
            targetStatus = ParticipantStatus.cancelled;
        } else {
            return;
        }
        // 更新所有 accepted 参与者的状态
        List<Participant> acceptedParticipants = participantRepository
                .findByTripAndParticipantStatusOrderByCreatedAtDesc(trip, ParticipantStatus.accepted);
        for (Participant p : acceptedParticipants) {
            p.setParticipantStatus(targetStatus);
            participantRepository.save(p);
        }
        // 将 pending 状态的申请标记为 cancelled（行程已关闭）
        List<Participant> pendingParticipants = participantRepository
                .findByTripAndParticipantStatusOrderByCreatedAtDesc(trip, ParticipantStatus.pending);
        for (Participant p : pendingParticipants) {
            p.setParticipantStatus(ParticipantStatus.cancelled);
            participantRepository.save(p);
        }
    }

    private Map<String, Object> participantToMap(Participant p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId()); map.put("tripId", p.getTrip().getId());
        map.put("userId", p.getUser().getId());
        map.put("seatsBooked", p.getSeatsBooked());
        map.put("participantStatus", p.getParticipantStatus().name());
        map.put("paymentStatus", p.getPaymentStatus().name());
        map.put("createdAt", p.getCreatedAt());
        Map<String, Object> student = new LinkedHashMap<>();
        student.put("name", p.getUser().getName());
        student.put("phone", p.getUser().getPhone());
        student.put("studentId", p.getUser().getStudentId());
        map.put("student", student);
        return map;
    }
}