package com.carpool.controller;

import com.carpool.dto.response.ApiResponse;
import com.carpool.entity.User;
import com.carpool.entity.Vehicle;
import com.carpool.enums.UserRole;
import com.carpool.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "车辆接口", description = "车辆信息管理")
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @Operation(summary = "添加车辆（司机）")
    @PostMapping
    public ApiResponse<Map<String, Object>> addVehicle(@AuthenticationPrincipal User user,
                                                        @RequestBody Map<String, Object> data) {
        if (user.getRole() != UserRole.driver) {
            return ApiResponse.error(403, "只有司机可以添加车辆");
        }
        return ApiResponse.success(vehicleService.addVehicle(user,
                (String) data.get("plateNumber"), (String) data.get("brand"),
                (String) data.get("model"), (String) data.get("color"),
                getInt(data, "seats", 5), getInt(data, "year"),
                (String) data.get("description")));
    }

    @Operation(summary = "查看我的车辆（司机）")
    @GetMapping("/driver")
    public ApiResponse<List<Map<String, Object>>> getDriverVehicles(@AuthenticationPrincipal User user) {
        return ApiResponse.success(vehicleService.getDriverVehicles(user));
    }

    @Operation(summary = "查看车辆详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getVehicle(@AuthenticationPrincipal User user,
                                                        @PathVariable Integer id) {
        List<Map<String, Object>> vehicles = vehicleService.getDriverVehicles(user);
        for (Map<String, Object> v : vehicles) {
            if (Integer.valueOf(v.get("id").toString()).equals(id)) {
                return ApiResponse.success(v);
            }
        }
        return ApiResponse.error(404, "车辆不存在");
    }

    @Operation(summary = "修改车辆信息")
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> updateVehicle(@AuthenticationPrincipal User user,
                                                           @PathVariable Integer id,
                                                           @RequestBody Map<String, Object> data) {
        return ApiResponse.success(vehicleService.updateVehicle(user, id,
                (String) data.get("plateNumber"), (String) data.get("brand"),
                (String) data.get("model"), (String) data.get("color"),
                getInt(data, "seats"), getInt(data, "year"),
                (String) data.get("description")));
    }

    @Operation(summary = "删除车辆")
    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> deleteVehicle(@AuthenticationPrincipal User user,
                                                           @PathVariable Integer id) {
        return ApiResponse.success(vehicleService.deleteVehicle(user, id));
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