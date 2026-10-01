package com.carpool.controller;

import com.carpool.dto.response.ApiResponse;
import com.carpool.entity.User;
import com.carpool.exception.BusinessException;
import com.carpool.exception.ErrorCode;
import com.carpool.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户接口", description = "个人信息管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "查看个人信息")
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getProfile(@AuthenticationPrincipal User user) {
        return ApiResponse.success(userService.getUserProfile(user.getId()));
    }

    @Operation(summary = "查看指定用户信息")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getUserById(@AuthenticationPrincipal User user,
                                                         @PathVariable Integer id) {
        if (!user.getId().equals(id)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看其他用户信息");
        }
        return ApiResponse.success(userService.getUserProfile(id));
    }

    @Operation(summary = "修改个人信息")
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> updateProfile(@AuthenticationPrincipal User user,
                                                           @PathVariable Integer id,
                                                           @RequestBody Map<String, Object> data) {
        if (!user.getId().equals(id)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改其他用户信息");
        }
        String name = (String) data.get("name");
        String phone = (String) data.get("phone");
        return ApiResponse.success(userService.updateProfile(id, name, phone));
    }
}