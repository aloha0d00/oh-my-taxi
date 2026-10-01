package com.carpool.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "角色不能为空")
    private String role;

    private String studentId;

    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;
}
