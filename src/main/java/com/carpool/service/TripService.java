package com.carpool.service;

import com.carpool.entity.Trip;
import com.carpool.entity.User;
import com.carpool.entity.Vehicle;
import com.carpool.enums.TripStatus;
import com.carpool.exception.BusinessException;
import com.carpool.exception.ErrorCode;
import com.carpool.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final VehicleService vehicleService;
    private final ParticipantService participantService;

    public TripService(TripRepository tripRepository, VehicleService vehicleService,
                       @org.springframework.context.annotation.Lazy ParticipantService participantService) {
        this.tripRepository = tripRepository;
        this.vehicleService = vehicleService;
        this.participantService = participantService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchTrips(String departureLocation,
                                                  String arrivalLocation,
                                                  String departureDate) {
        List<Trip> pendingTrips = tripRepository.findByTripStatusOrderByCreatedAtDesc(TripStatus.pending);
        List<Trip> ongoingTrips = tripRepository.findByTripStatusOrderByCreatedAtDesc(TripStatus.ongoing);
        List<Trip> trips = new ArrayList<>();
        trips.addAll(pendingTrips);
        trips.addAll(ongoingTrips);
        // Filter in Java (simpler and more reliable than JPA Criteria)
        return trips.stream()
                .filter(t -> {
                    if (departureLocation != null && !departureLocation.isEmpty()) {
                        if (t.getDepartureLocation() == null || !t.getDepartureLocation().contains(departureLocation))
                            return false;
                    }
                    if (arrivalLocation != null && !arrivalLocation.isEmpty()) {
                        if (t.getArrivalLocation() == null || !t.getArrivalLocation().contains(arrivalLocation))
                            return false;
                    }
                    if (departureDate != null && !departureDate.isEmpty()) {
                        LocalDate date = LocalDate.parse(departureDate);
                        if (t.getDepartureTime() == null || !t.getDepartureTime().toLocalDate().equals(date))
                            return false;
                    }
                    return true;
                })
                .map(this::tripToMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTrip(Integer tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
        return tripToMap(trip);
    }

    @Transactional
    public Map<String, Object> createTrip(User driver, Integer vehicleId,
                                           String departureLocation, String arrivalLocation,
                                           LocalDateTime departureTime, Integer availableSeats,
                                           BigDecimal pricePerSeat, String description) {
        Vehicle vehicle = vehicleService.getVehicleEntityById(vehicleId);
        if (!vehicle.getUser().getId().equals(driver.getId())) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_OWNER);
        }
        Trip trip = Trip.builder()
                .driver(driver).vehicle(vehicle)
                .departureLocation(departureLocation).arrivalLocation(arrivalLocation)
                .departureTime(departureTime).availableSeats(availableSeats)
                .pricePerSeat(pricePerSeat).description(description)
                .departureLatitude(0f).departureLongitude(0f)
                .arrivalLatitude(0f).arrivalLongitude(0f).build();
        tripRepository.save(trip);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "行程创建成功");
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDriverTrips(User driver) {
        return tripRepository.findByDriverOrderByCreatedAtDesc(driver)
                .stream().map(this::tripToMap).toList();
    }

    @Transactional
    public Map<String, Object> deleteTrip(Integer tripId, User user) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
        if (!trip.getDriver().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.TRIP_ONLY_DRIVER_CAN_MODIFY);
        }
        if (trip.getTripStatus() == TripStatus.ongoing || trip.getTripStatus() == TripStatus.completed) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "进行中或已完成的行程无法删除");
        }
        tripRepository.delete(trip);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "行程删除成功");
        return result;
    }

    @Transactional
    public Map<String, Object> updateTripStatus(Integer tripId, User user, String status) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
        if (!trip.getDriver().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.TRIP_ONLY_DRIVER_CAN_MODIFY);
        }
        TripStatus newStatus;
        try { newStatus = TripStatus.valueOf(status); }
        catch (IllegalArgumentException e) { throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的行程状态"); }
        trip.setTripStatus(newStatus);
        tripRepository.save(trip);
        if (newStatus == TripStatus.completed || newStatus == TripStatus.cancelled) {
            participantService.updateStatusByTripId(tripId, status);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "行程状态更新成功");
        return result;
    }

    public Trip getTripEntity(Integer tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
    }

    public List<Map<String, Object>> getAllTrips() {
        return tripRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(this::tripToMap).toList();
    }

    public long countAll() { return tripRepository.count(); }

    private Map<String, Object> tripToMap(Trip t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", t.getId());
        map.put("driverId", t.getDriver().getId());
        map.put("vehicleId", t.getVehicle().getId());
        map.put("departureLocation", t.getDepartureLocation());
        map.put("arrivalLocation", t.getArrivalLocation());
        map.put("departureTime", t.getDepartureTime() != null
                ? t.getDepartureTime().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null);
        map.put("availableSeats", t.getAvailableSeats());
        map.put("pricePerSeat", t.getPricePerSeat());
        map.put("tripStatus", t.getTripStatus().name());
        map.put("description", t.getDescription());
        map.put("departureLatitude", t.getDepartureLatitude());
        map.put("departureLongitude", t.getDepartureLongitude());
        map.put("arrivalLatitude", t.getArrivalLatitude());
        map.put("arrivalLongitude", t.getArrivalLongitude());
        map.put("createdAt", t.getCreatedAt());
        map.put("updatedAt", t.getUpdatedAt());
        Map<String, Object> driver = new LinkedHashMap<>();
        driver.put("id", t.getDriver().getId());
        driver.put("name", t.getDriver().getName());
        map.put("driver", driver);
        Map<String, Object> vehicle = new LinkedHashMap<>();
        vehicle.put("id", t.getVehicle().getId());
        vehicle.put("brand", t.getVehicle().getBrand());
        vehicle.put("model", t.getVehicle().getModel());
        map.put("vehicle", vehicle);
        return map;
    }
}