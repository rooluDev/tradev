package com.tradev.domain.user.service;

import com.tradev.common.exception.ErrorCode;
import com.tradev.common.exception.TradevException;
import com.tradev.domain.user.dto.MyInfoResponse;
import com.tradev.domain.user.dto.UpdateProfileRequest;
import com.tradev.domain.user.dto.UserResponse;
import com.tradev.domain.user.entity.User;
import com.tradev.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    @Value("${aws.s3.base-url}")
    private String s3BaseUrl;

    private final UserRepository userRepository;

    public UserResponse getPublicProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));
        return new UserResponse(user);
    }

    public MyInfoResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));
        return new MyInfoResponse(user);
    }

    @Transactional
    public MyInfoResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));

        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(request.getNickname())) {
                throw new TradevException(ErrorCode.USER_NICKNAME_DUPLICATED);
            }
        }

        String profileImageUrl = null;
        if (request.getProfileImageS3Key() != null) {
            profileImageUrl = s3BaseUrl + "/" + request.getProfileImageS3Key();
        }

        user.updateProfile(request.getNickname(), request.getBio(), profileImageUrl);
        return new MyInfoResponse(user);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
    }
}
