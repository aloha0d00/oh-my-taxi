package com.carpool.service;

import com.carpool.entity.Trip;
import com.carpool.entity.User;
import com.carpool.entity.Vehicle;
import com.carpool.enums.TripStatus;
import com.carpool.exception.BusinessException;
import com.carpool.exception.ErrorCode;
import com.carpool.repository.TripRepository;
import com.carpool.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;

    @Transactional
    public Map<String, Object> addVehicle(User user, String plateNumber, String brand,
                                           String model, String color, int seats,
                                           Integer year, String description) {
        if (vehicleRepository.findByPlateNumber(plateNumber).isPresent()) {
            throw new BusinessException(ErrorCode.VEHICLE_PLATE_EXISTS);
        }
        Vehicle v = Vehicle.builder().user(user).plateNumber(plateNumber)
                .brand(brand).model(model).color(color).seats(seats)
                .year(year != null ? year : 0).description(description).build();
        vehicleRepository.save(v);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "车辆信息添加成功");
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDriverVehicles(User user) {
        List<Vehicle> vehicles = vehicleRepository.findByUser(user);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Vehicle v : vehicles) result.add(vehicleToMap(v));
        return result;
    }

    public Vehicle getFirstVehicleByUserId(User user) {
        return vehicleRepository.findFirstByUser(user).orElse(null);
    }

    public Vehicle getVehicleEntityById(Integer id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VEHICLE_NOT_FOUND));
    }

    @Transactional
    public Map<String, Object> updateVehicle(User user, Integer vehicleId, String plateNumber,
                                              String brand, String model, String color,
                                              Integer seats, Integer year, String description) {
        Vehicle v = getVehicleEntityById(vehicleId);
        if (!v.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_OWNER);
        }
        if (plateNumber != null && !plateNumber.isEmpty() && !plateNumber.equals(v.getPlateNumber())) {
            if (vehicleRepository.findByPlateNumber(plateNumber).isPresent()) {
                throw new BusinessException(ErrorCode.VEHICLE_PLATE_EXISTS);
            }
            v.setPlateNumber(plateNumber);
        }
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
    public Map<String, Object> deleteVehicle(User user, Integer vehicleId) {
        Vehicle v = getVehicleEntityById(vehicleId);
        if (!v.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_OWNER);
        }
        // 检查是否有活跃行程
        long activeTrips = tripRepository.countByVehicleAndTripStatusIn(
                v, Arrays.asList(TripStatus.pending, TripStatus.ongoing));
        if (activeTrips > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "该车辆有 " + activeTrips + " 个未完成的行程，请先处理后再删除");
        }
        vehicleRepository.delete(v);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "车辆删除成功");
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllVehicles() {
        return vehicleRepository.findAll().stream().map(v -> {
            Map<String, Object> map = vehicleToMap(v);
            Map<String, Object> driver = new LinkedHashMap<>();
            driver.put("name", v.getUser().getName());
            map.put("driver", driver);
            return map;
        }).toList();
    }

    public long countAll() { return vehicleRepository.count(); }

    private Map<String, Object> vehicleToMap(Vehicle v) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", v.getId());
        map.put("userId", v.getUser().getId());
        map.put("plateNumber", v.getPlateNumber());
        map.put("brand", v.getBrand());
        map.put("model", v.getModel());
        map.put("color", v.getColor());
        map.put("seats", v.getSeats());
        map.put("year", v.getYear());
        map.put("status", v.getStatus().name());
        map.put("description", v.getDescription());
        map.put("createdAt", v.getCreatedAt());
        map.put("updatedAt", v.getUpdatedAt());
        return map;
    }
}
