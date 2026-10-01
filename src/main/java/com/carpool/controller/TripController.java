package com.carpool.controller;

import com.carpool.dto.response.ApiResponse;
import com.carpool.entity.User;
import com.carpool.enums.UserRole;
import com.carpool.exception.BusinessException;
import com.carpool.exception.ErrorCode;
import com.carpool.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Tag(name = "行程接口", description = "行程的发布、搜索与管理")
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @Operation(summary = "搜索行程（公开）")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> searchTrips(
            @RequestParam(required = false) String departureLocation,
            @RequestParam(required = false) String arrivalLocation,
            @RequestParam(required = false) String departureDate) {
        return ApiResponse.success(tripService.searchTrips(departureLocation, arrivalLocation, departureDate));
    }

    @Operation(summary = "查看行程详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getTrip(@PathVariable Integer id) {
        return ApiResponse.success(tripService.getTrip(id));
    }

    @Operation(summary = "发布行程（司机）")
    @PostMapping
    public ApiResponse<Map<String, Object>> createTrip(@AuthenticationPrincipal User user,
                                                        @RequestBody Map<String, Object> data) {
        if (user.getRole() != UserRole.driver) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有司机可以发布行程");
        }
        Integer vehicleId = getInt(data, "vehicleId");
        String departureLocation = (String) data.get("departureLocation");
        String arrivalLocation = (String) data.get("arrivalLocation");
        String departureTimeStr = (String) data.get("departureTime");
        LocalDateTime departureTime = departureTimeStr != null ?
                LocalDateTime.parse(departureTimeStr.replace("T", " ") + ":00",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
        int availableSeats = getInt(data, "availableSeats", 1);
        BigDecimal pricePerSeat = data.get("pricePerSeat") != null ?
                new BigDecimal(data.get("pricePerSeat").toString()) : BigDecimal.ZERO;
        String description = (String) data.get("description");
        return ApiResponse.success(tripService.createTrip(user, vehicleId, departureLocation, arrivalLocation,
                departureTime, availableSeats, pricePerSeat, description));
    }

    @Operation(summary = "修改行程状态（司机）")
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> updateTripStatus(@AuthenticationPrincipal User user,
                                                              @PathVariable Integer id,
                                                              @RequestBody Map<String, Object> data) {
        return ApiResponse.success(tripService.updateTripStatus(id, user, (String) data.get("tripStatus")));
    }

    @Operation(summary = "删除行程（司机）")
    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> deleteTrip(@AuthenticationPrincipal User user,
                                                        @PathVariable Integer id) {
        return ApiResponse.success(tripService.deleteTrip(id, user));
    }

    @Operation(summary = "查看我的行程（司机）")
    @GetMapping("/driver")
    public ApiResponse<List<Map<String, Object>>> getDriverTrips(@AuthenticationPrincipal User user) {
        return ApiResponse.success(tripService.getDriverTrips(user));
    }

    private Integer getInt(Map<String, Object> data, String key) {
        return getInt(data, key, null);
    }

    private Integer getInt(Map<String, Object> data, String key, Integer defaultValue) {
        Object value = data.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); }
        catch (NumberFormatException e) { return defaultValue; }
    }
}