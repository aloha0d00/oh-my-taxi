package com.carpool.controller;

import com.carpool.dto.response.ApiResponse;
import com.carpool.entity.User;
import com.carpool.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "评分接口", description = "行程评价管理")
@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @Operation(summary = "创建评价")
    @PostMapping("/trip")
    public ApiResponse<Map<String, Object>> createRating(@AuthenticationPrincipal User user,
                                                          @RequestBody Map<String, Object> data) {
        Integer ratedUserId = getInt(data, "ratedUserId");
        Integer tripId = getInt(data, "tripId");
        Integer ratingScore = getInt(data, "ratingScore");
        String comment = (String) data.get("comment");
        return ApiResponse.success(ratingService.createRating(user, ratedUserId,
                tripId, ratingScore, comment));
    }

    @Operation(summary = "查看行程评价")
    @GetMapping("/trip/{tripId}")
    public ApiResponse<Map<String, Object>> getTripRatings(@PathVariable Integer tripId) {
        return ApiResponse.success(ratingService.getTripRatings(tripId));
    }

    @Operation(summary = "查看用户评价")
    @GetMapping("/user/{userId}")
    public ApiResponse<Map<String, Object>> getUserRatings(@PathVariable Integer userId) {
        return ApiResponse.success(ratingService.getUserRatings(userId));
    }

    private Integer getInt(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); }
        catch (NumberFormatException e) { return null; }
    }
}