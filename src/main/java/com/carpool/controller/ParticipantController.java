package com.carpool.controller;

import com.carpool.dto.response.ApiResponse;
import com.carpool.entity.User;
import com.carpool.service.ParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "拼车接口", description = "拼车申请、审批与支付")
@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;

    @Operation(summary = "申请加入行程")
    @PostMapping("/trip/{tripId}")
    public ApiResponse<Map<String, Object>> joinTrip(@AuthenticationPrincipal User user,
                                                      @PathVariable Integer tripId,
                                                      @RequestBody(required = false) Map<String, Object> data) {
        int seats = 1;
        if (data != null && data.containsKey("seatsBooked")) {
            seats = getInt(data, "seatsBooked", 1);
        }
        return ApiResponse.success(participantService.joinTrip(tripId, user, seats));
    }

    @Operation(summary = "查看行程申请列表（司机）")
    @GetMapping("/trips/{tripId}/applications")
    public ApiResponse<List<Map<String, Object>>> getApplications(@AuthenticationPrincipal User user,
                                                                    @PathVariable Integer tripId) {
        List<Map<String, Object>> result = participantService.getTripApplications(tripId, user);
        if (result == null) return ApiResponse.error(403, "只有行程司机可以查看申请");
        return ApiResponse.success(result);
    }

    @Operation(summary = "查看乘客列表与收入（司机）")
    @GetMapping("/trips/{tripId}/passengers")
    public ApiResponse<Map<String, Object>> getPassengers(@AuthenticationPrincipal User user,
                                                           @PathVariable Integer tripId) {
        return ApiResponse.success(participantService.getTripPassengersAndIncome(tripId, user));
    }

    @Operation(summary = "审批申请（司机）")
    @PutMapping("/{id}/status")
    public ApiResponse<Map<String, Object>> handleApplication(@AuthenticationPrincipal User user,
                                                               @PathVariable Integer id,
                                                               @RequestBody Map<String, Object> data) {
        return ApiResponse.success(participantService.handleApplication(id, user,
                (String) data.get("status")));
    }

    @Operation(summary = "查看我的申请")
    @GetMapping("/student")
    public ApiResponse<List<Map<String, Object>>> getMyApplications(@AuthenticationPrincipal User user) {
        return ApiResponse.success(participantService.getStudentApplications(user));
    }

    @Operation(summary = "取消申请")
    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> cancelApplication(@AuthenticationPrincipal User user,
                                                               @PathVariable Integer id) {
        return ApiResponse.success(participantService.cancelApplication(id, user));
    }

    @Operation(summary = "支付")
    @PostMapping("/{id}/pay")
    public ApiResponse<Map<String, Object>> pay(@AuthenticationPrincipal User user,
                                                  @PathVariable Integer id) {
        return ApiResponse.success(participantService.payApplication(id, user));
    }

    private Integer getInt(Map<String, Object> data, String key, Integer defaultValue) {
        Object value = data.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); }
        catch (NumberFormatException e) { return defaultValue; }
    }
}