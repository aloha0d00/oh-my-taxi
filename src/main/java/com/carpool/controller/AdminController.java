package com.carpool.controller;

import com.carpool.dto.response.ApiResponse;
import com.carpool.entity.User;
import com.carpool.service.AdminService;
import com.carpool.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Tag(name = "管理后台", description = "管理员专用接口")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "仪表盘统计")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.success(adminService.getStats());
    }

    @Operation(summary = "获取所有用户")
    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> getAllUsers() {
        return ApiResponse.success(adminService.getAllUsers());
    }

    @Operation(summary = "创建用户")
    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> createUser(@RequestBody Map<String, Object> data) {
        return ApiResponse.success(adminService.createUser(
                (String) data.get("name"), (String) data.get("phone"),
                (String) data.get("password"), (String) data.get("role"),
                (String) data.get("studentId")));
    }

    @Operation(summary = "修改用户")
    @PutMapping("/users/{id}")
    public ApiResponse<Map<String, Object>> updateUser(@PathVariable Integer id,
                                                        @RequestBody Map<String, Object> data) {
        return ApiResponse.success(adminService.updateUser(id,
                (String) data.get("name"), (String) data.get("phone"),
                (String) data.get("role"), (String) data.get("studentId"),
                (String) data.get("password")));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/users/{id}")
    public ApiResponse<Map<String, Object>> deleteUser(@AuthenticationPrincipal User user,
                                                        @PathVariable Integer id) {
        return ApiResponse.success(adminService.deleteUser(id, user.getId()));
    }

    @Operation(summary = "获取所有车辆")
    @GetMapping("/vehicles")
    public ApiResponse<List<Map<String, Object>>> getAllVehicles() {
        return ApiResponse.success(adminService.getAllVehicles());
    }

    @Operation(summary = "添加车辆")
    @PostMapping("/vehicles")
    public ApiResponse<Map<String, Object>> createVehicle(@RequestBody Map<String, Object> data) {
        return ApiResponse.success(adminService.createVehicle(
                (String) data.get("plateNumber"), (String) data.get("brand"),
                (String) data.get("model"), (String) data.get("color"),
                getInt(data, "seats"), getInt(data, "year"),
                (String) data.get("description"), getInt(data, "userId")));
    }

    @Operation(summary = "修改车辆")
    @PutMapping("/vehicles/{id}")
    public ApiResponse<Map<String, Object>> updateVehicle(@PathVariable Integer id,
                                                           @RequestBody Map<String, Object> data) {
        return ApiResponse.success(adminService.updateVehicle(id,
                (String) data.get("plateNumber"), (String) data.get("brand"),
                (String) data.get("model"), (String) data.get("color"),
                getInt(data, "seats"), getInt(data, "year"),
                (String) data.get("description")));
    }

    @Operation(summary = "删除车辆")
    @DeleteMapping("/vehicles/{id}")
    public ApiResponse<Map<String, Object>> deleteVehicle(@PathVariable Integer id) {
        return ApiResponse.success(adminService.deleteVehicle(id));
    }

    @Operation(summary = "获取所有行程")
    @GetMapping("/trips")
    public ApiResponse<List<Map<String, Object>>> getAllTrips() {
        return ApiResponse.success(adminService.getAllTrips());
    }

    @Operation(summary = "修改行程")
    @PutMapping("/trips/{id}")
    public ApiResponse<Map<String, Object>> updateTrip(@PathVariable Integer id,
                                                        @RequestBody Map<String, Object> data) {
        String departureTimeStr = (String) data.get("departureTime");
        LocalDateTime departureTime = null;
        if (departureTimeStr != null && !departureTimeStr.isEmpty()) {
            departureTime = LocalDateTime.parse(departureTimeStr.replace("T", " ") + ":00",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return ApiResponse.success(adminService.updateTrip(id,
                (String) data.get("departureLocation"), (String) data.get("arrivalLocation"),
                departureTime, getInt(data, "availableSeats"),
                data.get("pricePerSeat") != null ? new BigDecimal(data.get("pricePerSeat").toString()) : null,
                (String) data.get("tripStatus"), (String) data.get("description")));
    }

    @Operation(summary = "删除行程")
    @DeleteMapping("/trips/{id}")
    public ApiResponse<Map<String, Object>> deleteTrip(@PathVariable Integer id) {
        return ApiResponse.success(adminService.deleteTrip(id));
    }

    private Integer getInt(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); }
        catch (NumberFormatException e) { return null; }
    }
}