package com.tradev.domain.user.controller;

import com.tradev.common.response.ApiResponse;
import com.tradev.domain.user.dto.MyInfoResponse;
import com.tradev.domain.user.dto.UpdateProfileRequest;
import com.tradev.domain.user.dto.UserResponse;
import com.tradev.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getPublicProfile(userId)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyInfoResponse>> getMyInfo(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyInfo(userId)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<MyInfoResponse>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(userId, request)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(@AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
