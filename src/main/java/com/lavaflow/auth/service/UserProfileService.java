package com.lavaflow.auth.service;

import com.lavaflow.auth.dto.UpdateProfileRequest;
import com.lavaflow.auth.dto.UserProfileResponse;
import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.mapper.UserProfileMapper;
import com.lavaflow.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileMapper userProfileMapper;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        return userProfileMapper.toResponse(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUser(userId);

        userProfileMapper.updateEntity(request, user);
        user.setFullName(user.getFullName().trim());

        return userProfileMapper.toResponse(userRepository.save(user));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));
    }
}
