package com.carpool.service;

import com.carpool.entity.Trip;
import com.carpool.entity.User;
import com.carpool.entity.Vehicle;
import com.carpool.enums.TripStatus;
import com.carpool.enums.UserRole;
import com.carpool.exception.BusinessException;
import com.carpool.exception.ErrorCode;
import com.carpool.repository.TripRepository;
import com.carpool.repository.UserRepository;
import com.carpool.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final TripService tripService;
    private final TripRepository tripRepository;
    private final PasswordEncoder passwordEncoder;

    public Map<String, Object> getStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", userRepository.count());
        result.put("totalVehicles", vehicleRepository.count());
        result.put("totalTrips", tripService.countAll());
        result.put("todayNewUsers", userRepository.countTodayNewUsers());
        return result;
    }

    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream().map(this::userToMap).toList();
    }

    public Map<String, Object> getUser(Integer userId) {
        return userToMap(userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND)));
    }

    @Transactional
    public Map<String, Object> createUser(String name, String phone, String password,
                                           String role, String studentId) {
        if (name == null || name.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写姓名");
        UserRole userRole;
        try { userRole = UserRole.valueOf(role); }
        catch (IllegalArgumentException e) { throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的角色"); }
        if (userRole == UserRole.student && (studentId == null || studentId.isEmpty())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "学生角色必须填写学号");
        }
        if (studentId != null && !studentId.isEmpty() && userRepository.existsByStudentId(studentId)) {
            throw new BusinessException(ErrorCode.USER_STUDENT_ID_EXISTS);
        }
        if (password == null || password.isEmpty()) password = "123456";
        if (phone == null || phone.isEmpty()) phone = "ADMIN_" + System.currentTimeMillis() % 100000000;
        if (userRepository.existsByPhone(phone)) {
            phone = phone + "_" + System.currentTimeMillis() % 10000;
        }
        User user = User.builder().name(name).phone(phone)
                .password(passwordEncoder.encode(password)).role(userRole)
                .studentId(studentId).build();
        user = userRepository.save(user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "用户创建成功");
        result.put("user", userToMap(user));
        return result;
    }

    @Transactional
    public Map<String, Object> updateUser(Integer userId, String name, String phone,
                                           String role, String studentId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (name != null && !name.isEmpty()) user.setName(name);
        if (phone != null && !phone.isEmpty()) user.setPhone(phone);
        if (role != null && !role.isEmpty()) {
            try { user.setRole(UserRole.valueOf(role)); }
            catch (IllegalArgumentException e) { throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的角色"); }
        }
        if (studentId != null) user.setStudentId(studentId);
        if (password != null && !password.isEmpty()) user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "用户信息更新成功");
        result.put("user", userToMap(user));
        return result;
    }

    @Transactional
    public Map<String, Object> deleteUser(Integer userId, Integer currentUserId) {
        if (userId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.USER_CANNOT_DELETE_SELF);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "用户删除成功");
        return result;
    }

    public List<Map<String, Object>> getAllVehicles() {
        return vehicleRepository.findAll().stream().map(v -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", v.getId()); map.put("userId", v.getUser().getId());
            map.put("plateNumber", v.getPlateNumber()); map.put("brand", v.getBrand());
            map.put("model", v.getModel()); map.put("color", v.getColor());
            map.put("seats", v.getSeats()); map.put("year", v.getYear());
            map.put("status", v.getStatus().name()); map.put("description", v.getDescription());
            map.put("createdAt", v.getCreatedAt()); map.put("updatedAt", v.getUpdatedAt());
            Map<String, Object> driver = new LinkedHashMap<>();
            driver.put("name", v.getUser().getName());
            map.put("driver", driver);
            return map;
        }).toList();
    }

    @Transactional
    public Map<String, Object> createVehicle(String plateNumber, String brand, String model,
                                              String color, Integer seats, Integer year,
                                              String description, Integer ownerUserId) {
        if (plateNumber == null || plateNumber.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, "车牌号不能为空");
        if (vehicleRepository.findByPlateNumber(plateNumber).isPresent()) {
            throw new BusinessException(ErrorCode.VEHICLE_PLATE_EXISTS);
        }
        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Vehicle v = Vehicle.builder().user(owner).plateNumber(plateNumber)
                .brand(brand).model(model).color(color != null ? color : "")
                .seats(seats != null ? seats : 5).year(year != null ? year : 0)
                .description(description).build();
        vehicleRepository.save(v);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "车辆添加成功");
        return result;
    }

    @Transactional
    public Map<String, Object> updateVehicle(Integer vehicleId, String plateNumber, String brand,
                                              String model, String color, Integer seats,
                                              Integer year, String description) {
        Vehicle v = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VEHICLE_NOT_FOUND));
        if (plateNumber != null) v.setPlateNumber(plateNumber);
        if (brand != null) v.setBrand(brand);
        if (model != null) v.setModel(model);
        if (color != null) v.setColor(color);
        if (seats != null) v.setSeats(seats);
        if (year != null) v.setYear(year);
        if (description != null) v.setDescription(description);
        vehicleRepository.save(v);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "车辆信息更新成功");
        return result;
    }

    @Transactional
    public Map<String, Object> deleteVehicle(Integer vehicleId) {
        Vehicle v = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VEHICLE_NOT_FOUND));
        vehicleRepository.delete(v);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "车辆删除成功");
        return result;
    }

    public List<Map<String, Object>> getAllTrips() {
        return tripService.getAllTrips();
    }

    @Transactional
    public Map<String, Object> updateTrip(Integer tripId, String departureLocation, String arrivalLocation,
                                           LocalDateTime departureTime, Integer availableSeats,
                                           BigDecimal pricePerSeat, String tripStatus, String description) {
        Trip trip = tripService.getTripEntity(tripId);
        if (departureLocation != null) trip.setDepartureLocation(departureLocation);
        if (arrivalLocation != null) trip.setArrivalLocation(arrivalLocation);
        if (departureTime != null) trip.setDepartureTime(departureTime);
        if (availableSeats != null) trip.setAvailableSeats(availableSeats);
        if (pricePerSeat != null) trip.setPricePerSeat(pricePerSeat);
        if (tripStatus != null) {
            try { trip.setTripStatus(TripStatus.valueOf(tripStatus)); }
            catch (IllegalArgumentException e) { throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的状态"); }
        }
        if (description != null) trip.setDescription(description);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "行程信息更新成功");
        return result;
    }

    @Transactional
    public Map<String, Object> deleteTrip(Integer tripId) {
        Trip trip = tripService.getTripEntity(tripId);
        tripRepository.delete(trip);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "行程删除成功");
        return result;
    }

    private Map<String, Object> userToMap(User u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId()); map.put("studentId", u.getStudentId());
        map.put("name", u.getName()); map.put("phone", u.getPhone());
        map.put("role", u.getRole().name());
        if (u.getCreatedAt() != null) map.put("createdAt", u.getCreatedAt().toString());
        return map;
    }
}