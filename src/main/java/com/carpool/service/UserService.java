package com.carpool.service;

import com.carpool.dto.request.LoginRequest;
import com.carpool.dto.request.RegisterRequest;
import com.carpool.entity.User;
import com.carpool.enums.UserRole;
import com.carpool.exception.BusinessException;
import com.carpool.exception.ErrorCode;
import com.carpool.repository.UserRepository;
import com.carpool.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        // Check phone uniqueness
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(ErrorCode.USER_PHONE_EXISTS);
        }

        // Check student ID uniqueness
        if (request.getStudentId() != null && !request.getStudentId().isEmpty()) {
            if (userRepository.existsByStudentId(request.getStudentId())) {
                throw new BusinessException(ErrorCode.USER_STUDENT_ID_EXISTS);
            }
        }

        UserRole role;
        try {
            role = UserRole.valueOf(request.getRole());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的角色类型");
        }

        User user = User.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .studentId(request.getStudentId())
                .build();

        user = userRepository.save(user);
        log.info("用户注册成功: id={}, phone={}, role={}", user.getId(), user.getPhone(), user.getRole());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "注册成功");
        result.put("user", buildUserMap(user));
        return result;
    }

    public Map<String, Object> login(LoginRequest request) {
        User user;
        UserRole role;
        try {
            role = UserRole.valueOf(request.getRole());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的角色类型");
        }

        if (role == UserRole.student) {
            user = userRepository.findByStudentId(request.getStudentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_PASSWORD_ERROR, "学号或密码错误"));
        } else {
            user = userRepository.findByPhoneAndRole(request.getPhone(), role)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_PASSWORD_ERROR, "手机号或密码错误"));
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            String key = role == UserRole.student ? "学号" : "手机号";
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR, key + "或密码错误");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getStudentId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "登录成功");
        result.put("token", token);
        result.put("user", buildUserMap(user));
        return result;
    }

    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public Map<String, Object> getUserProfile(Integer userId) {
        User user = getUserById(userId);
        return buildUserMap(user);
    }

    @Transactional
    public Map<String, Object> updateProfile(Integer userId, String name, String phone) {
        User user = getUserById(userId);
        if (name != null && !name.isEmpty()) user.setName(name);
        if (phone != null && !phone.isEmpty() && !phone.equals(user.getPhone())) {
            if (userRepository.existsByPhone(phone)) {
                throw new BusinessException(ErrorCode.USER_PHONE_EXISTS);
            }
            user.setPhone(phone);
        }
        userRepository.save(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "信息更新成功");
        result.put("user", buildUserMap(user));
        return result;
    }

    public boolean isAdmin(Integer userId) {
        return getUserById(userId).getRole() == UserRole.admin;
    }

    public boolean isDriver(Integer userId) {
        return getUserById(userId).getRole() == UserRole.driver;
    }

    private Map<String, Object> buildUserMap(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("studentId", user.getStudentId());
        map.put("name", user.getName());
        map.put("phone", user.getPhone());
        map.put("role", user.getRole().name());
        return map;
    }
}
